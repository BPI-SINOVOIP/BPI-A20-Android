/*
 * Copyright 2010, The Android Open Source Project
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#ifndef GLWebViewState_h
#define GLWebViewState_h

#if USE(ACCELERATED_COMPOSITING)

#include "Color.h"
#include "DrawExtra.h"
#include "GLExtras.h"
#include "IntRect.h"
#include "SkCanvas.h"
#include "SkRect.h"
#include "SkRegion.h"
#include "SurfaceCollectionManager.h"
#include <utils/threads.h>

// Performance measurements probe
// To use it, enable the visual indicators in debug mode.
// turning off the visual indicators will flush the measures.
// #define MEASURES_PERF
#define MAX_MEASURES_PERF 2000

// Prefetch and render 1 tiles ahead of the scroll
// TODO: We should either dynamically change the outer bound by detecting the
// HW limit or save further in the GPU memory consumption.
#define TILE_PREFETCH_DISTANCE 1

namespace WebCore {

class BaseLayerAndroid;
class LayerAndroid;
class ScrollableLayerAndroid;
class TexturesResult;

/////////////////////////////////////////////////////////////////////////////////
// GL Architecture
/////////////////////////////////////////////////////////////////////////////////
//
// To draw things, WebView use a tree of layers. The root of that tree is a
// BaseLayerAndroid, which may have numerous LayerAndroid over it. The content
// of those layers are SkPicture, the content of the BaseLayer is an PictureSet.
//
// When drawing, we therefore have one large "surface" that is the BaseLayer,
// and (possibly) additional surfaces (usually smaller), which are the
// LayerAndroids. The BaseLayer usually corresponds to the normal web page
// content, the Layers are used for some parts such as specific divs (e.g. fixed
// position divs, or elements using CSS3D transforms, or containing video,
// plugins, etc.).
//
// *** NOTE: The GL drawing architecture only paints the BaseLayer for now.
//
// The rendering model is to use tiles to display the BaseLayer (as obviously a
// BaseLayer's area can be arbitrarly large). The idea is to compute a set of
// tiles covering the visibleContentRect's area, paint those tiles using the webview's
// content (i.e. the BaseLayer's PictureSet), then display those tiles.
// We check which tile we should use at every frame.
//
// Overview
// ---------
//
// The tiles are grouped into a TiledPage -- basically a map of tiles covering
// the BaseLayer's surface. When drawing, we ask the TiledPage to prepare()
// itself then draw itself on screen. The prepare() function is the one
// that schedules tiles to be painted -- i.e. the subset of tiles that intersect
// with the current visibleContentRect. When they are ready, we can display
// the TiledPage.
//
// Note that BaseLayerAndroid::drawGL() will return true to the java side if
// there is a need to be called again (i.e. if we do not have up to date
// textures or a transition is going on).
//
// Tiles are implemented as a Tile. It knows how to paint itself with the
// PictureSet, and to display itself. A GL texture is usually associated to it.
//
// We also works with two TiledPages -- one to display the page at the
// current scale factor, and another we use to paint the page at a different
// scale factor. I.e. when we zoom, we use TiledPage A, with its tiles scaled
// accordingly (and therefore possible loss of quality): this is fast as it's
// purely a hardware operation. When the user is done zooming, we ask for
// TiledPage B to be painted at the new scale factor, covering the
// visibleContentRect's area. When B is ready, we swap it with A.
//
// Texture allocation
// ------------------
//
// Obviously we cannot have every Tile having a GL texture -- we need to
// get the GL textures from an existing pool, and reuse them.
//
// The way we do it is that when we call TiledPage::prepare(), we group the
// tiles we need (i.e. in the visibleContentRect and dirty) into a TilesSet and call
// Tile::reserveTexture() for each tile (which ensures there is a specific
// GL textures backing the Tiles).
//
// reserveTexture() will ask the TilesManager for a texture. The allocation
// mechanism goal is to (in order):
// - prefers to allocate the same texture as the previous time
// - prefers to allocate textures that are as far from the visibleContentRect as possible
// - prefers to allocate textures that are used by different TiledPages
//
// Note that to compute the distance of each tile from the visibleContentRect, each time
// we prepare() a TiledPage. Also during each prepare() we compute which tiles
// are dirty based on the info we have received from webkit.
//
// Tile Invalidation
// ------------------
//
// We do not want to redraw a tile if the tile is up-to-date. A tile is
// considered to be dirty an in need of redrawing in the following cases
//  - the tile has acquires a new texture
//  - webkit invalidates all or part of the tiles contents
//
// To handle the case of webkit invalidation we store two ids (counters) of the
// pictureSets in the tile.  The first id (A) represents the pictureSet used to
// paint the tile and the second id (B) represents the pictureSet in which the
// tile was invalidated by webkit. Thus, if A < B then tile is dirty.
//
// Since invalidates can occur faster than a full tiled page update, the tiled
// page is protected by a 'lock' (m_baseLayerUpdate) that is set to true to
// defer updates to the background layer, giving the foreground time to render
// content instead of constantly flushing with invalidates. See
// lockBaseLayerUpdate() & unlockBaseLayerUpdate().
//
// Painting scheduling
// -------------------
//
// The next operation is to schedule this TilesSet to be painted
// (TilesManager::schedulePaintForTilesSet()). TexturesGenerator
// will get the TilesSet and ask the Tiles in it to be painted.
//
// Tile::paintBitmap() will paint the texture using the BaseLayer's
// PictureSet (calling TiledPage::paintBaseLayerContent() which in turns
// calls GLWebViewState::paintBaseLayerContent()).
//
// Note that TexturesGenerator is running in a separate thread, the textures
// are shared using EGLImages (this is necessary to not slow down the rendering
// speed -- updating GL textures in the main GL thread would slow things down).
//
/////////////////////////////////////////////////////////////////////////////////

class GLWebViewState {
public:
    GLWebViewState();
    ~GLWebViewState();

    bool setBaseLayer(BaseLayerAndroid* layer, bool showVisualIndicator,
                      bool isPictureAfterFirstLayout);
    void paintExtras();

    GLExtras* glExtras() { return &m_glExtras; }

    void setIsScrolling(bool isScrolling) { m_isScrolling = isScrolling; }
    bool isScrolling() { return m_isScrolling || m_isVisibleContentRectScrolling; }

    bool setLayersRenderingMode(TexturesResult&);

    int drawGL(IntRect& rect, SkRect& visibleContentRect, IntRect* invalRect,
               IntRect& screenRect, int titleBarHeight,
               IntRect& clip, float scale,
               bool* collectionsSwappedPtr, bool* newCollectionHasAnimPtr,
               bool shouldDraw);

#ifdef MEASURES_PERF
    void dumpMeasures();
#endif

    void addDirtyArea(const IntRect& rect);
    void resetLayersDirtyArea();
    void doFrameworkFullInval();
    bool inUnclippedDraw() { return m_inUnclippedDraw; }

    bool goingDown() { return m_goingDown; }
    bool goingLeft() { return m_goingLeft; }

    float scale() { return m_scale; }

    // Currently, we only use 3 modes : kAllTextures, kClippedTextures and
    // kSingleSurfaceRendering ( for every mode > kClippedTextures ) .
    enum LayersRenderingMode {
        kAllTextures              = 0, // all layers are drawn with textures fully covering them
        kClippedTextures          = 1, // all layers are drawn, but their textures will be clipped
        kScrollableAndFixedLayers = 2, // only scrollable and fixed layers will be drawn
        kFixedLayers              = 3, // only fixed layers will be drawn
        kSingleSurfaceRendering   = 4  // no layers will be drawn on separate textures
                                       // -- everything is drawn on the base surface.
    };

    LayersRenderingMode layersRenderingMode() { return m_layersRenderingMode; }
    bool isSingleSurfaceRenderingMode() { return m_layersRenderingMode == kSingleSurfaceRendering; }
    void scrollLayer(int layerId, int x, int y);

private:
    void setVisibleContentRect(const SkRect& visibleContentRect, float scale);
    double setupDrawing(const IntRect& invScreenRect, const SkRect& visibleContentRect,
                        const IntRect& screenRect, int titleBarHeight,
                        const IntRect& screenClip, float scale);
    void showFrameInfo(const IntRect& rect, bool collectionsSwapped);
    void clearRectWithColor(const IntRect& rect, float r, float g,
                            float b, float a);
    double m_prevDrawTime;

    SkRect m_visibleContentRect;
    IntRect m_frameworkLayersInval;
    bool m_doFrameworkFullInval;
    bool m_inUnclippedDraw;

#ifdef MEASURES_PERF
    unsigned int m_totalTimeCounter;
    int m_timeCounter;
    double m_delayTimes[MAX_MEASURES_PERF];
    bool m_measurePerfs;
#endif
    GLExtras m_glExtras;

    bool m_isScrolling;
    bool m_isVisibleContentRectScrolling;
    bool m_goingDown;
    bool m_goingLeft;

    float m_scale;

    LayersRenderingMode m_layersRenderingMode;
    SurfaceCollectionManager m_surfaceCollectionManager;
};

} // namespace WebCore

#endif // USE(ACCELERATED_COMPOSITING)
#endif // GLWebViewState_h
