/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.webkit.cts;

import android.content.Context;
import android.content.res.AssetManager;
import android.cts.util.PollingCheck;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.Rect;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CacheManager;
import android.webkit.CacheManager.CacheResult;
import android.webkit.ConsoleMessage;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebIconDatabase;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebView.PictureListener;
import android.webkit.WebViewClient;
import android.webkit.WebViewDatabase;
import android.webkit.cts.WebViewOnUiThread.WaitForLoadedClient;
import android.webkit.cts.WebViewOnUiThread.WaitForProgressClient;
import android.widget.LinearLayout;


import junit.framework.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;

public class WebViewTest extends ActivityInstrumentationTestCase2<WebViewStubActivity> {
    private static final String LOGTAG = "WebViewTest";
    private static final int INITIAL_PROGRESS = 100;
    private static long TEST_TIMEOUT = 20000L;
    /**
     * This is the minimum number of milliseconds to wait for scrolling to
     * start. If no scrolling has started before this timeout then it is
     * assumed that no scrolling will happen.
     */
    private static final long MIN_SCROLL_WAIT_MS = 1000;
    /**
     * Once scrolling has started, this is the interval that scrolling
     * is checked to see if there is a change. If no scrolling change
     * has happened in the given time then it is assumed that scrolling
     * has stopped.
     */
    private static final long SCROLL_WAIT_INTERVAL_MS = 200;

    private WebView mWebView;
    private CtsTestServer mWebServer;
    private WebViewOnUiThread mOnUiThread;
    private WebIconDatabase mIconDb;

    public WebViewTest() {
        super("com.android.cts.stub", WebViewStubActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mWebView = getActivity().getWebView();
        File f = getActivity().getFileStreamPath("snapshot");
        if (f.exists()) {
            f.delete();
        }
        mOnUiThread = new WebViewOnUiThread(this, mWebView);
    }

    @Override
    protected void tearDown() throws Exception {
        mOnUiThread.cleanUp();
        if (mWebServer != null) {
            mWebServer.shutdown();
        }
        if (mIconDb != null) {
            mIconDb.removeAllIcons();
            mIconDb.close();
            mIconDb = null;
        }
        super.tearDown();
    }

    private void startWebServer(boolean secure) throws Exception {
        assertNull(mWebServer);
        mWebServer = new CtsTestServer(getActivity(), secure);
    }

    @UiThreadTest
    public void testConstructor() {
        new WebView(getActivity());
        new WebView(getActivity(), null);
        new WebView(getActivity(), null, 0);
    }

    @UiThreadTest
    public void testFindAddress() {
        /*
         * Info about USPS
         * http://en.wikipedia.org/wiki/Postal_address#United_States
         * http://www.usps.com/
         */
        // full address, invalid zip code
        assertNull(WebView.findAddress("455 LARKSPUR DRIVE CALIFORNIA SPRINGS CALIFORNIA 92926"));
        // full address
        assertEquals("455 LARKSPUR DRIVE CALIFORNIA SPRINGS CALIFORNIA 92826",
                WebView.findAddress("455 LARKSPUR DRIVE CALIFORNIA SPRINGS CALIFORNIA 92826"));
        // full address ( with abbreviated street type and state)
        assertEquals("455 LARKSPUR DR CALIFORNIA SPRINGS CA 92826",
                WebView.findAddress("455 LARKSPUR DR CALIFORNIA SPRINGS CA 92826"));
        // misspell the state ( CALIFORNIA -> CALIFONIA )
        assertNull(WebView.findAddress("455 LARKSPUR DRIVE CALIFORNIA SPRINGS CALIFONIA 92826"));
        // without optional zip code
        assertEquals("455 LARKSPUR DR CALIFORNIA SPRINGS CA",
                WebView.findAddress("455 LARKSPUR DR CALIFORNIA SPRINGS CA"));
        // house number, street name and street type are missing
        assertNull(WebView.findAddress("CALIFORNIA SPRINGS CA"));
        // city & state are missing
        assertNull(WebView.findAddress("455 LARKSPUR DR"));
    }

    @SuppressWarnings("deprecation")
    @UiThreadTest
    public void testGetZoomControls() {
         WebSettings settings = mWebView.getSettings();
         assertTrue(settings.supportZoom());
         View zoomControls = mWebView.getZoomControls();
         assertNotNull(zoomControls);

         // disable zoom support
         settings.setSupportZoom(false);
         assertFalse(settings.supportZoom());
         assertNull(mWebView.getZoomControls());
    }

    @UiThreadTest
    public void testInvokeZoomPicker() throws Exception {
        WebSettings settings = mWebView.getSettings();
        assertTrue(settings.supportZoom());
        startWebServer(false);
        String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        mWebView.invokeZoomPicker();
    }

    @UiThreadTest
    public void testZoom() {
        WebSettings settings = mWebView.getSettings();
        settings.setSupportZoom(false);
        assertFalse(settings.supportZoom());
        float currScale = mWebView.getScale();
        float previousScale = currScale;

        // can zoom in or out although zoom support is disabled in web settings
        assertTrue(mWebView.zoomIn());
        currScale = mWebView.getScale();
        assertTrue(currScale > previousScale);

        // zoom in
        assertTrue(mWebView.zoomOut());
        previousScale = currScale;
        currScale = mWebView.getScale();
        assertTrue(currScale < previousScale);

        // enable zoom support
        settings.setSupportZoom(true);
        assertTrue(settings.supportZoom());
        currScale = mWebView.getScale();

        assertTrue(mWebView.zoomIn());
        previousScale = currScale;
        currScale = mWebView.getScale();
        assertTrue(currScale > previousScale);

        // zoom in until it reaches maximum scale
        while (currScale > previousScale) {
            mWebView.zoomIn();
            previousScale = currScale;
            currScale = mWebView.getScale();
        }

        // can not zoom in further
        assertFalse(mWebView.zoomIn());
        previousScale = currScale;
        currScale = mWebView.getScale();
        assertEquals(currScale, previousScale);

        // zoom out
        assertTrue(mWebView.zoomOut());
        previousScale = currScale;
        currScale = mWebView.getScale();
        assertTrue(currScale < previousScale);

        // zoom out until it reaches minimum scale
        while (currScale < previousScale) {
            mWebView.zoomOut();
            previousScale = currScale;
            currScale = mWebView.getScale();
        }

        // can not zoom out further
        assertFalse(mWebView.zoomOut());
        previousScale = currScale;
        currScale = mWebView.getScale();
        assertEquals(currScale, previousScale);
    }

    @UiThreadTest
    public void testSetScrollBarStyle() {
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        assertFalse(mWebView.overlayHorizontalScrollbar());
        assertFalse(mWebView.overlayVerticalScrollbar());

        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        assertTrue(mWebView.overlayHorizontalScrollbar());
        assertTrue(mWebView.overlayVerticalScrollbar());

        mWebView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
        assertFalse(mWebView.overlayHorizontalScrollbar());
        assertFalse(mWebView.overlayVerticalScrollbar());

        mWebView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        assertTrue(mWebView.overlayHorizontalScrollbar());
        assertTrue(mWebView.overlayVerticalScrollbar());
    }

    @UiThreadTest
    public void testScrollBarOverlay() throws Throwable {
        mWebView.setHorizontalScrollbarOverlay(true);
        mWebView.setVerticalScrollbarOverlay(false);
        assertTrue(mWebView.overlayHorizontalScrollbar());
        assertFalse(mWebView.overlayVerticalScrollbar());

        mWebView.setHorizontalScrollbarOverlay(false);
        mWebView.setVerticalScrollbarOverlay(true);
        assertFalse(mWebView.overlayHorizontalScrollbar());
        assertTrue(mWebView.overlayVerticalScrollbar());
    }

    @UiThreadTest
    public void testLoadUrl() throws Exception {
        assertNull(mWebView.getUrl());
        assertNull(mWebView.getOriginalUrl());
        assertEquals(INITIAL_PROGRESS, mWebView.getProgress());

        startWebServer(false);
        String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals(100, mWebView.getProgress());
        assertEquals(url, mWebView.getUrl());
        assertEquals(url, mWebView.getOriginalUrl());
        assertEquals(TestHtmlConstants.HELLO_WORLD_TITLE, mWebView.getTitle());
    }

    @SuppressWarnings("deprecation")
    @UiThreadTest
    public void testGetVisibleTitleHeight() throws Exception {
        startWebServer(false);
        String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals(0, mWebView.getVisibleTitleHeight());
    }

    @UiThreadTest
    public void testGetOriginalUrl() throws Throwable {
        startWebServer(false);
        final String finalUrl = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        final String redirectUrl =
                mWebServer.getRedirectingAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);

        assertNull(mWebView.getUrl());
        assertNull(mWebView.getOriginalUrl());

        // By default, WebView sends an intent to ask the system to
        // handle loading a new URL. We set a WebViewClient as
        // WebViewClient.shouldOverrideUrlLoading() returns false, so
        // the WebView will load the new URL.
        mOnUiThread.setWebViewClient(new WaitForLoadedClient(mOnUiThread));
        mOnUiThread.loadUrlAndWaitForCompletion(redirectUrl);

        assertEquals(finalUrl, mWebView.getUrl());
        assertEquals(redirectUrl, mWebView.getOriginalUrl());
    }

    @UiThreadTest
    public void testStopLoading() throws Exception {
        assertNull(mWebView.getUrl());
        assertEquals(INITIAL_PROGRESS, mWebView.getProgress());

        startWebServer(false);
        String url = mWebServer.getDelayedAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mWebView.loadUrl(url);
        mWebView.stopLoading();
        new PollingCheck() {
            @Override
            protected boolean check() {
                return 100 == mWebView.getProgress();
            }
        }.run();
        assertNull(mWebView.getUrl());
    }

    @UiThreadTest
    public void testGoBackAndForward() throws Exception {
        assertGoBackOrForwardBySteps(false, -1);
        assertGoBackOrForwardBySteps(false, 1);

        startWebServer(false);
        String url1 = mWebServer.getAssetUrl(TestHtmlConstants.HTML_URL1);
        String url2 = mWebServer.getAssetUrl(TestHtmlConstants.HTML_URL2);
        String url3 = mWebServer.getAssetUrl(TestHtmlConstants.HTML_URL3);

        mOnUiThread.loadUrlAndWaitForCompletion(url1);
        pollingCheckWebBackForwardList(url1, 0, 1);
        assertGoBackOrForwardBySteps(false, -1);
        assertGoBackOrForwardBySteps(false, 1);

        mOnUiThread.loadUrlAndWaitForCompletion(url2);
        pollingCheckWebBackForwardList(url2, 1, 2);
        assertGoBackOrForwardBySteps(true, -1);
        assertGoBackOrForwardBySteps(false, 1);

        mOnUiThread.loadUrlAndWaitForCompletion(url3);
        pollingCheckWebBackForwardList(url3, 2, 3);
        assertGoBackOrForwardBySteps(true, -2);
        assertGoBackOrForwardBySteps(false, 1);

        mWebView.goBack();
        pollingCheckWebBackForwardList(url2, 1, 3);
        assertGoBackOrForwardBySteps(true, -1);
        assertGoBackOrForwardBySteps(true, 1);

        mWebView.goForward();
        pollingCheckWebBackForwardList(url3, 2, 3);
        assertGoBackOrForwardBySteps(true, -2);
        assertGoBackOrForwardBySteps(false, 1);

        mWebView.goBackOrForward(-2);
        pollingCheckWebBackForwardList(url1, 0, 3);
        assertGoBackOrForwardBySteps(false, -1);
        assertGoBackOrForwardBySteps(true, 2);

        mWebView.goBackOrForward(2);
        pollingCheckWebBackForwardList(url3, 2, 3);
        assertGoBackOrForwardBySteps(true, -2);
        assertGoBackOrForwardBySteps(false, 1);
    }

    @UiThreadTest
    public void testAddJavascriptInterface() throws Exception {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);

        final class DummyJavaScriptInterface {
            private boolean mWasProvideResultCalled;
            private String mResult;

            private synchronized String waitForResult() {
                while (!mWasProvideResultCalled) {
                    try {
                        wait(TEST_TIMEOUT);
                    } catch (InterruptedException e) {
                        continue;
                    }
                    if (!mWasProvideResultCalled) {
                        Assert.fail("Unexpected timeout");
                    }
                }
                return mResult;
            }

            public synchronized boolean wasProvideResultCalled() {
                return mWasProvideResultCalled;
            }

            @JavascriptInterface
            public synchronized void provideResult(String result) {
                mWasProvideResultCalled = true;
                mResult = result;
                notify();
            }
        }

        final DummyJavaScriptInterface obj = new DummyJavaScriptInterface();
        mWebView.addJavascriptInterface(obj, "dummy");
        assertFalse(obj.wasProvideResultCalled());

        startWebServer(false);
        String url = mWebServer.getAssetUrl(TestHtmlConstants.ADD_JAVA_SCRIPT_INTERFACE_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals("Original title", obj.waitForResult());
    }

    @UiThreadTest
    public void testAddJavascriptInterfaceNullObject() throws Exception {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        String setTitleToPropertyTypeHtml = "<html><head></head>" +
                "<body onload=\"document.title = typeof window.injectedObject;\"></body></html>";

        // Test that the property is initially undefined.
        mOnUiThread.loadDataAndWaitForCompletion(setTitleToPropertyTypeHtml,
                "text/html", null);
        assertEquals("undefined", mWebView.getTitle());

        // Test that adding a null object has no effect.
        mWebView.addJavascriptInterface(null, "injectedObject");
        mOnUiThread.loadDataAndWaitForCompletion(setTitleToPropertyTypeHtml,
                "text/html", null);
        assertEquals("undefined", mWebView.getTitle());

        // Test that adding an object gives an object type.
        final Object obj = new Object();
        mWebView.addJavascriptInterface(obj, "injectedObject");
        mOnUiThread.loadDataAndWaitForCompletion(setTitleToPropertyTypeHtml,
                "text/html", null);
        assertEquals("object", mWebView.getTitle());

        // Test that trying to replace with a null object has no effect.
        mWebView.addJavascriptInterface(null, "injectedObject");
        mOnUiThread.loadDataAndWaitForCompletion(setTitleToPropertyTypeHtml,
                "text/html", null);
        assertEquals("object", mWebView.getTitle());
    }

    @UiThreadTest
    public void testRemoveJavascriptInterface() throws Exception {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        String setTitleToPropertyTypeHtml = "<html><head></head>" +
                "<body onload=\"document.title = typeof window.injectedObject;\"></body></html>";

        // Test that adding an object gives an object type.
        mWebView.addJavascriptInterface(new Object(), "injectedObject");
        mOnUiThread.loadDataAndWaitForCompletion(setTitleToPropertyTypeHtml,
                "text/html", null);
        assertEquals("object", mWebView.getTitle());

        // Test that reloading the page after removing the object leaves the property undefined.
        mWebView.removeJavascriptInterface("injectedObject");
        mOnUiThread.loadDataAndWaitForCompletion(setTitleToPropertyTypeHtml,
                "text/html", null);
        assertEquals("undefined", mWebView.getTitle());
    }

    public void testUseRemovedJavascriptInterface() throws Throwable {
        class RemovedObject {
            @Override
            @JavascriptInterface
            public String toString() {
                return "removedObject";
            }

            @JavascriptInterface
            public void remove() throws Throwable {
                mOnUiThread.removeJavascriptInterface("removedObject");
                System.gc();
            }
        }
        class ResultObject {
            private String mResult;
            private boolean mIsResultAvailable;

            @JavascriptInterface
            public synchronized void setResult(String result) {
                mResult = result;
                mIsResultAvailable = true;
                notify();
            }
            public synchronized String getResult() {
                while (!mIsResultAvailable) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
                return mResult;
            }
        }
        final ResultObject resultObject = new ResultObject();

        // Test that an object is still usable if removed while the page is in use, even if we have
        // no external references to it.
        mOnUiThread.getSettings().setJavaScriptEnabled(true);
        mOnUiThread.addJavascriptInterface(new RemovedObject(), "removedObject");
        mOnUiThread.addJavascriptInterface(resultObject, "resultObject");
        mOnUiThread.loadDataAndWaitForCompletion("<html><head></head>" +
                "<body onload=\"window.removedObject.remove();" +
                "resultObject.setResult(removedObject.toString());\"></body></html>",
                "text/html", null);
        assertEquals("removedObject", resultObject.getResult());
    }

    public void testCapturePicture() throws Exception, Throwable {
        startWebServer(false);
        final String url = mWebServer.getAssetUrl(TestHtmlConstants.BLANK_PAGE_URL);
        // showing the blank page will make the picture filled with background color
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        getInstrumentation().waitForIdleSync();

        class PictureRunnable implements Runnable {
            private Picture mPicture;
            @Override
            public void run() {
                mPicture = mWebView.capturePicture();
                Bitmap b = Bitmap.createBitmap(mPicture.getWidth(), mPicture.getHeight(),
                        Config.ARGB_8888);
                mPicture.draw(new Canvas(b));
                // default color is white
                assertBitmapFillWithColor(b, Color.WHITE);

                mWebView.setBackgroundColor(Color.CYAN);
                mOnUiThread.reloadAndWaitForCompletion();
            }
            public Picture getPicture() {
                return mPicture;
            }
        }
        PictureRunnable runnable = new PictureRunnable();
        runTestOnUiThread(runnable);
        getInstrumentation().waitForIdleSync();

        // the content of the picture will not be updated automatically
        Picture picture = runnable.getPicture();
        Bitmap b = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), Config.ARGB_8888);
        picture.draw(new Canvas(b));
        assertBitmapFillWithColor(b, Color.WHITE);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                // update the content
                Picture p = mWebView.capturePicture();
                Bitmap b = Bitmap.createBitmap(p.getWidth(), p.getHeight(), Config.ARGB_8888);
                p.draw(new Canvas(b));
                assertBitmapFillWithColor(b, Color.CYAN);
            }
        });
    }

    public void testSetPictureListener() throws Exception, Throwable {
        final class MyPictureListener implements PictureListener {
            public int callCount;
            public WebView webView;
            public Picture picture;

            @Override
            public void onNewPicture(WebView view, Picture picture) {
                // Need to inform the listener tracking new picture
                // for the "page loaded" knowledge since it has been replaced.
                mOnUiThread.onNewPicture();
                this.callCount += 1;
                this.webView = view;
                this.picture = picture;
            }
        }

        final MyPictureListener listener = new MyPictureListener();
        startWebServer(false);
        final String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.setPictureListener(listener);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return listener.callCount > 0;
            }
        }.run();
        assertEquals(mWebView, listener.webView);
        assertNotNull(listener.picture);

        final int oldCallCount = listener.callCount;
        final String newUrl = mWebServer.getAssetUrl(TestHtmlConstants.SMALL_IMG_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(newUrl);
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return listener.callCount > oldCallCount;
            }
        }.run();
    }

    public void testSaveAndRestorePicture() throws Throwable {
        mWebView.setBackgroundColor(Color.CYAN);
        startWebServer(false);
        final String url = mWebServer.getAssetUrl(TestHtmlConstants.BLANK_PAGE_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        getInstrumentation().waitForIdleSync();

        final Bundle bundle = new Bundle();
        final File f = getActivity().getFileStreamPath("snapshot");
        if (f.exists()) {
            f.delete();
        }

        try {
            assertTrue(bundle.isEmpty());
            assertEquals(0, f.length());
            assertTrue(mOnUiThread.savePicture(bundle, f));

            // File saving is done in a separate thread.
            new PollingCheck() {
                @Override
                protected boolean check() {
                    return f.length() > 0;
                }
            }.run();

            assertFalse(bundle.isEmpty());

            Picture p = Picture.createFromStream(new FileInputStream(f));
            Bitmap b = Bitmap.createBitmap(p.getWidth(), p.getHeight(), Config.ARGB_8888);
            p.draw(new Canvas(b));
            assertBitmapFillWithColor(b, Color.CYAN);

            mOnUiThread.setBackgroundColor(Color.WHITE);
            mOnUiThread.reloadAndWaitForCompletion();
            getInstrumentation().waitForIdleSync();

            runTestOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Bitmap b = Bitmap.createBitmap(mWebView.getWidth(), mWebView.getHeight(),
                            Config.ARGB_8888);
                    mWebView.draw(new Canvas(b));
                    assertBitmapFillWithColor(b, Color.WHITE);

                    // restorePicture is only supported in software rendering
                    mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    assertTrue(mWebView.restorePicture(bundle, f));
                }
            });
            getInstrumentation().waitForIdleSync();
            // Cannot test whether the picture has been restored successfully.
            // Drawing the webview into a canvas will draw white, but on the display it is cyan
        } finally {
            if (f.exists()) {
                f.delete();
            }
        }
    }

    @UiThreadTest
    public void testAccessHttpAuthUsernamePassword() {
        try {
            WebViewDatabase.getInstance(getActivity()).clearHttpAuthUsernamePassword();

            String host = "http://localhost:8080";
            String realm = "testrealm";
            String userName = "user";
            String password = "password";

            String[] result = mWebView.getHttpAuthUsernamePassword(host, realm);
            assertNull(result);

            mWebView.setHttpAuthUsernamePassword(host, realm, userName, password);
            result = mWebView.getHttpAuthUsernamePassword(host, realm);
            assertNotNull(result);
            assertEquals(userName, result[0]);
            assertEquals(password, result[1]);

            String newPassword = "newpassword";
            mWebView.setHttpAuthUsernamePassword(host, realm, userName, newPassword);
            result = mWebView.getHttpAuthUsernamePassword(host, realm);
            assertNotNull(result);
            assertEquals(userName, result[0]);
            assertEquals(newPassword, result[1]);

            String newUserName = "newuser";
            mWebView.setHttpAuthUsernamePassword(host, realm, newUserName, newPassword);
            result = mWebView.getHttpAuthUsernamePassword(host, realm);
            assertNotNull(result);
            assertEquals(newUserName, result[0]);
            assertEquals(newPassword, result[1]);

            // the user is set to null, can not change any thing in the future
            mWebView.setHttpAuthUsernamePassword(host, realm, null, password);
            result = mWebView.getHttpAuthUsernamePassword(host, realm);
            assertNotNull(result);
            assertNull(result[0]);
            assertEquals(password, result[1]);

            mWebView.setHttpAuthUsernamePassword(host, realm, userName, null);
            result = mWebView.getHttpAuthUsernamePassword(host, realm);
            assertNotNull(result);
            assertEquals(userName, result[0]);
            assertEquals(null, result[1]);

            mWebView.setHttpAuthUsernamePassword(host, realm, null, null);
            result = mWebView.getHttpAuthUsernamePassword(host, realm);
            assertNotNull(result);
            assertNull(result[0]);
            assertNull(result[1]);

            mWebView.setHttpAuthUsernamePassword(host, realm, newUserName, newPassword);
            result = mWebView.getHttpAuthUsernamePassword(host, realm);
            assertNotNull(result);
            assertEquals(newUserName, result[0]);
            assertEquals(newPassword, result[1]);
        } finally {
            WebViewDatabase.getInstance(getActivity()).clearHttpAuthUsernamePassword();
        }
    }

    @UiThreadTest
    public void testSavePassword() {
        WebViewDatabase db = WebViewDatabase.getInstance(getActivity());
        try {
            db.clearUsernamePassword();

            String host = "http://localhost:8080";
            String userName = "user";
            String password = "password";
            assertFalse(db.hasUsernamePassword());
            mWebView.savePassword(host, userName, password);
            assertTrue(db.hasUsernamePassword());
        } finally {
            db.clearUsernamePassword();
        }
    }

    public void testLoadData() throws Throwable {
        final String HTML_CONTENT =
                "<html><head><title>Hello,World!</title></head><body></body>" +
                "</html>";
        assertNull(mOnUiThread.getTitle());
        mOnUiThread.loadDataAndWaitForCompletion(HTML_CONTENT,
                "text/html", null);
        assertEquals("Hello,World!", mOnUiThread.getTitle());

        startWebServer(false);
        final ChromeClient webChromeClient = new ChromeClient(mOnUiThread);
        final String crossOriginUrl = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.getSettings().setJavaScriptEnabled(true);
                mOnUiThread.setWebChromeClient(webChromeClient);
                mOnUiThread.loadDataAndWaitForCompletion(
                        "<html><head></head><body onload=\"" +
                        "document.title = " +
                        "document.getElementById('frame').contentWindow.location.href;" +
                        "\"><iframe id=\"frame\" src=\"" + crossOriginUrl + "\"></body></html>",
                        "text/html", null);
            }
        });
        assertEquals(ConsoleMessage.MessageLevel.ERROR, webChromeClient.getMessageLevel(10000));
    }

    @UiThreadTest
    public void testLoadDataWithBaseUrl() throws Throwable {
        assertNull(mWebView.getTitle());
        assertNull(mWebView.getUrl());
        String imgUrl = TestHtmlConstants.SMALL_IMG_URL; // relative

        // Check that we can access relative URLs and that reported URL is supplied history URL.
        startWebServer(false);
        String baseUrl = mWebServer.getAssetUrl("foo.html");
        String historyUrl = "random";
        String dbPath = getActivity().getFilesDir().toString() + "/icons";
        mIconDb = WebIconDatabase.getInstance();
        mIconDb.open(dbPath);
        mWebServer.resetRequestState();
        // force the favicon to be loaded first
        mOnUiThread.loadDataWithBaseURLAndWaitForCompletion(baseUrl,
                "<html><body></body></html>",
                "text/html", "UTF-8", historyUrl);
        new PollingCheck() {
            @Override
            protected boolean check() {
                String lastRequestedUrl = mWebServer.getLastRequestUrl();
                return lastRequestedUrl != null
                        && lastRequestedUrl.endsWith("favicon.ico");
            }
        }.run();
        mOnUiThread.loadDataWithBaseURLAndWaitForCompletion(baseUrl,
                "<html><body><img src=\"" + imgUrl + "\"/></body></html>",
                "text/html", "UTF-8", historyUrl);
        assertTrue(mWebServer.getLastRequestUrl().endsWith(imgUrl));
        assertEquals(historyUrl, mWebView.getUrl());

        // Check that reported URL is "about:blank" when supplied history URL
        // is null.
        imgUrl = TestHtmlConstants.LARGE_IMG_URL;
        mOnUiThread.loadDataWithBaseURLAndWaitForCompletion(baseUrl,
                "<html><body><img src=\"" + imgUrl + "\"/></body></html>",
                "text/html", "UTF-8", null);
        assertTrue(mWebServer.getLastRequestUrl().endsWith(imgUrl));
        assertEquals("about:blank", mWebView.getUrl());

        // Test that JavaScript can access content from the same origin as the base URL.
        mWebView.getSettings().setJavaScriptEnabled(true);
        final String crossOriginUrl = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.loadDataWithBaseURLAndWaitForCompletion(baseUrl,
                "<html><head></head><body onload=\"" +
                "document.title = document.getElementById('frame').contentWindow.location.href;" +
                "\"><iframe id=\"frame\" src=\"" + crossOriginUrl + "\"></body></html>",
                "text/html", "UTF-8", null);
        assertEquals(crossOriginUrl, mWebView.getTitle());

        // Check that when the base URL uses the 'data' scheme, a 'data' scheme URL is used and the
        // history URL is ignored.
        mOnUiThread.loadDataWithBaseURLAndWaitForCompletion("data:foo",
                "<html><body>bar</body></html>", "text/html", "UTF-8",
                historyUrl);
        assertTrue(mWebView.getUrl().indexOf("data:text/html,") == 0);
        assertTrue(mWebView.getUrl().indexOf("bar") > 0);
    }

    @UiThreadTest
    public void testFindAll() {
        String p = "<p>Find all instances of find on the page and highlight them.</p>";

        mOnUiThread.loadDataAndWaitForCompletion("<html><body>" + p
                + "</body></html>", "text/html", null);

        assertEquals(2, mWebView.findAll("find"));
    }

    public void testFindNext() throws Throwable {
        // Reset the scaling so that finding the next "all" text will require scrolling.
        mOnUiThread.setInitialScale(100);

        DisplayMetrics metrics = mOnUiThread.getDisplayMetrics();
        int dimension = Math.max(metrics.widthPixels, metrics.heightPixels);
        // create a paragraph high enough to take up the entire screen
        String p = "<p style=\"height:" + dimension + "px;\">" +
                "Find all instances of a word on the page and highlight them.</p>";

        mOnUiThread.loadDataAndWaitForCompletion("<html><body>" + p + p + "</body></html>", "text/html", null);

        // highlight all the strings found
        mOnUiThread.findAll("all");
        getInstrumentation().waitForIdleSync();

        int previousScrollY = mOnUiThread.getScrollY();

        // Focus "all" in the second page and assert that the view scrolls.
        mOnUiThread.findNext(true);
        waitForScrollingComplete(previousScrollY);
        assertTrue(mOnUiThread.getScrollY() > previousScrollY);
        previousScrollY = mOnUiThread.getScrollY();

        // Focus "all" in the first page and assert that the view scrolls.
        mOnUiThread.findNext(true);
        waitForScrollingComplete(previousScrollY);
        assertTrue(mOnUiThread.getScrollY() < previousScrollY);
        previousScrollY = mOnUiThread.getScrollY();

        // Focus "all" in the second page and assert that the view scrolls.
        mOnUiThread.findNext(false);
        waitForScrollingComplete(previousScrollY);
        assertTrue(mOnUiThread.getScrollY() > previousScrollY);
        previousScrollY = mOnUiThread.getScrollY();

        // Focus "all" in the first page and assert that the view scrolls.
        mOnUiThread.findNext(false);
        waitForScrollingComplete(previousScrollY);
        assertTrue(mOnUiThread.getScrollY() < previousScrollY);
        previousScrollY = mOnUiThread.getScrollY();

        // clear the result
        mOnUiThread.clearMatches();
        getInstrumentation().waitForIdleSync();

        // can not scroll any more
        mOnUiThread.findNext(false);
        waitForScrollingComplete(previousScrollY);
        assertTrue(mOnUiThread.getScrollY() == previousScrollY);

        mOnUiThread.findNext(true);
        waitForScrollingComplete(previousScrollY);
        assertTrue(mOnUiThread.getScrollY() == previousScrollY);
    }

    public void testDocumentHasImages() throws Exception, Throwable {
        final class DocumentHasImageCheckHandler extends Handler {
            private boolean mReceived;
            private int mMsgArg1;
            public DocumentHasImageCheckHandler(Looper looper) {
                super(looper);
            }
            @Override
            public void handleMessage(Message msg) {
                synchronized(this) {
                    mReceived = true;
                    mMsgArg1 = msg.arg1;
                }
            }
            public synchronized boolean hasCalledHandleMessage() {
                return mReceived;
            }
            public synchronized int getMsgArg1() {
                return mMsgArg1;
            }
        }

        startWebServer(false);
        final String imgUrl = mWebServer.getAssetUrl(TestHtmlConstants.SMALL_IMG_URL);

        // Create a handler on the UI thread.
        final DocumentHasImageCheckHandler handler =
            new DocumentHasImageCheckHandler(mWebView.getHandler().getLooper());

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mOnUiThread.loadDataAndWaitForCompletion("<html><body><img src=\""
                        + imgUrl + "\"/></body></html>", "text/html", null);
                Message response = new Message();
                response.setTarget(handler);
                assertFalse(handler.hasCalledHandleMessage());
                mWebView.documentHasImages(response);
            }
        });
        new PollingCheck() {
            @Override
            protected boolean check() {
                return handler.hasCalledHandleMessage();
            }
        }.run();
        assertEquals(1, handler.getMsgArg1());
    }

    public void testPageScroll() throws Throwable {
        DisplayMetrics metrics = mOnUiThread.getDisplayMetrics();
        int dimension = 2 * Math.max(metrics.widthPixels, metrics.heightPixels);
        String p = "<p style=\"height:" + dimension + "px;\">" +
                "Scroll by half the size of the page.</p>";
        mOnUiThread.loadDataAndWaitForCompletion("<html><body>" + p
                + p + "</body></html>", "text/html", null);
        getInstrumentation().waitForIdleSync();

        assertTrue(mOnUiThread.pageDown(false));

        do {
            getInstrumentation().waitForIdleSync();
        } while (mOnUiThread.pageDown(false));

        getInstrumentation().waitForIdleSync();
        int bottomScrollY = mOnUiThread.getScrollY();

        assertTrue(mOnUiThread.pageUp(false));

        do {
            getInstrumentation().waitForIdleSync();
        } while (mOnUiThread.pageUp(false));

        getInstrumentation().waitForIdleSync();
        int topScrollY = mOnUiThread.getScrollY();

        // jump to the bottom
        assertTrue(mOnUiThread.pageDown(true));
        getInstrumentation().waitForIdleSync();
        assertEquals(bottomScrollY, mOnUiThread.getScrollY());

        // jump to the top
        assertTrue(mOnUiThread.pageUp(true));
        getInstrumentation().waitForIdleSync();
        assertEquals(topScrollY, mOnUiThread.getScrollY());
    }

    public void testGetContentHeight() throws Throwable {
        mOnUiThread.loadDataAndWaitForCompletion(
                "<html><body></body></html>", "text/html", null);
        getInstrumentation().waitForIdleSync();

        final int pageHeight = 600;
        // set the margin to 0
        final String p = "<p style=\"height:" + pageHeight
                + "px;margin:0px auto;\">Get the height of HTML content.</p>";
        assertEquals(mOnUiThread.getHeight(), mOnUiThread.getContentHeight() * mOnUiThread.getScale(), 2f);
        mOnUiThread.loadDataAndWaitForCompletion("<html><body>" + p
                + "</body></html>", "text/html", null);
        getInstrumentation().waitForIdleSync();

        assertTrue(mOnUiThread.getContentHeight() > pageHeight);
        int extraSpace = mOnUiThread.getContentHeight() - pageHeight;

        mOnUiThread.loadDataAndWaitForCompletion("<html><body>" + p
                + p + "</body></html>", "text/html", null);
        getInstrumentation().waitForIdleSync();
        assertEquals(pageHeight + pageHeight + extraSpace,
                mOnUiThread.getContentHeight());
    }

    @UiThreadTest
    public void testClearCache() throws Exception {
        final File cacheFileBaseDir = CacheManager.getCacheFileBaseDir();
        mWebView.clearCache(true);
        assertEquals(0, cacheFileBaseDir.list().length);

        startWebServer(false);
        final String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                CacheResult result = CacheManager.getCacheFile(url, null);
                return result != null;
            }
        }.run();
        int cacheFileCount = cacheFileBaseDir.list().length;
        assertTrue(cacheFileCount > 0);

        mWebView.clearCache(false);
        // the cache files are still there
        // can not check other effects of the method
        assertEquals(cacheFileCount, cacheFileBaseDir.list().length);

        mWebView.clearCache(true);
        // check the files are deleted
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return cacheFileBaseDir.list().length == 0;
            }
        }.run();
    }

    @UiThreadTest
    public void testPlatformNotifications() {
        WebView.enablePlatformNotifications();
        WebView.disablePlatformNotifications();
    }

    @UiThreadTest
    public void testAccessPluginList() {
        assertNotNull(WebView.getPluginList());

        // can not find a way to install plugins
        mWebView.refreshPlugins(false);
    }

    @UiThreadTest
    public void testDestroy() {
        // Create a new WebView, since we cannot call destroy() on a view in the hierarchy
        WebView localWebView = new WebView(getActivity());
        localWebView.destroy();
    }

    public void testFlingScroll() throws Throwable {
        DisplayMetrics metrics = mOnUiThread.getDisplayMetrics();
        int dimension = 2 * Math.max(metrics.widthPixels, metrics.heightPixels);
        String p = "<p style=\"height:" + dimension + "px;" +
                "width:" + dimension + "px\">Test fling scroll.</p>";
        mOnUiThread.loadDataAndWaitForCompletion("<html><body>" + p
                + "</body></html>", "text/html", null);
        getInstrumentation().waitForIdleSync();

        int previousScrollX = mOnUiThread.getScrollX();
        int previousScrollY = mOnUiThread.getScrollY();

        mOnUiThread.flingScroll(100, 100);

        int timeSlice = 500;
        Thread.sleep(timeSlice);
        assertTrue(mOnUiThread.getScrollX() > previousScrollX);
        assertTrue(mOnUiThread.getScrollY() > previousScrollY);

        previousScrollY = mOnUiThread.getScrollY();
        previousScrollX = mOnUiThread.getScrollX();
        Thread.sleep(timeSlice);
        assertTrue(mOnUiThread.getScrollX() >= previousScrollX);
        assertTrue(mOnUiThread.getScrollY() >= previousScrollY);

        previousScrollY = mOnUiThread.getScrollY();
        previousScrollX = mOnUiThread.getScrollX();
        Thread.sleep(timeSlice);
        assertTrue(mOnUiThread.getScrollX() >= previousScrollX);
        assertTrue(mOnUiThread.getScrollY() >= previousScrollY);
    }

    public void testRequestFocusNodeHref() throws Throwable {
        final String links = "<DL><p><DT><A HREF=\"" + TestHtmlConstants.HTML_URL1
                + "\">HTML_URL1</A><DT><A HREF=\"" + TestHtmlConstants.HTML_URL2
                + "\">HTML_URL2</A></DL><p>";
        mOnUiThread.loadDataAndWaitForCompletion("<html><body>" + links + "</body></html>", "text/html", null);
        getInstrumentation().waitForIdleSync();

        final HrefCheckHandler handler = new HrefCheckHandler(mWebView.getHandler().getLooper());
        final Message hrefMsg = new Message();
        hrefMsg.setTarget(handler);

        // focus on first link
        handler.reset();
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        mOnUiThread.requestFocusNodeHref(hrefMsg);
        new PollingCheck() {
            @Override
            protected boolean check() {
                boolean done = false;
                if (handler.hasCalledHandleMessage()) {
                    if (handler.mResultUrl != null) {
                        done = true;
                    } else {
                        handler.reset();
                        Message newMsg = new Message();
                        newMsg.setTarget(handler);
                        mOnUiThread.requestFocusNodeHref(newMsg);
                    }
                }
                return done;
            }
        }.run();
        assertEquals(TestHtmlConstants.HTML_URL1, handler.getResultUrl());

        // focus on second link
        handler.reset();
        final Message hrefMsg2 = new Message();
        hrefMsg2.setTarget(handler);
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);
        mOnUiThread.requestFocusNodeHref(hrefMsg2);
        new PollingCheck() {
            @Override
            protected boolean check() {
                boolean done = false;
                if (handler.hasCalledHandleMessage()) {
                    if (handler.mResultUrl != null &&
                            handler.mResultUrl.equals(TestHtmlConstants.HTML_URL2)) {
                        done = true;
                    } else {
                        handler.reset();
                        Message newMsg = new Message();
                        newMsg.setTarget(handler);
                        mOnUiThread.requestFocusNodeHref(newMsg);
                    }
                }
                return done;
            }
        }.run();
        assertEquals(TestHtmlConstants.HTML_URL2, handler.getResultUrl());

        mOnUiThread.requestFocusNodeHref(null);
    }

    public void testRequestImageRef() throws Exception, Throwable {
        final class ImageLoaded {
            public boolean mImageLoaded;

            @JavascriptInterface
            public void loaded() {
                mImageLoaded = true;
            }
        }
        final ImageLoaded imageLoaded = new ImageLoaded();
        runTestOnUiThread(new Runnable() {
            public void run() {
                mOnUiThread.getSettings().setJavaScriptEnabled(true);
            }
        });
        mOnUiThread.addJavascriptInterface(imageLoaded, "imageLoaded");
        AssetManager assets = getActivity().getAssets();
        Bitmap bitmap = BitmapFactory.decodeStream(assets.open(TestHtmlConstants.LARGE_IMG_URL));
        int imgWidth = bitmap.getWidth();
        int imgHeight = bitmap.getHeight();

        startWebServer(false);
        final String imgUrl = mWebServer.getAssetUrl(TestHtmlConstants.LARGE_IMG_URL);
        mOnUiThread.loadDataAndWaitForCompletion(
                "<html><head><title>Title</title><style type=\"text/css\">"
                + "#imgElement { -webkit-transform: translate3d(0,0,1); }"
                + "#imgElement.finish { -webkit-transform: translate3d(0,0,0);"
                + " -webkit-transition-duration: 1ms; }</style>"
                + "<script type=\"text/javascript\">function imgLoad() {"
                + "imgElement = document.getElementById('imgElement');"
                + "imgElement.addEventListener('webkitTransitionEnd',"
                + "function(e) { imageLoaded.loaded(); });"
                + "imgElement.className = 'finish';}</script>"
                + "</head><body><img id=\"imgElement\" src=\"" + imgUrl
                + "\" width=\"" + imgWidth + "\" height=\"" + imgHeight
                + "\" onLoad=\"imgLoad()\"/></body></html>", "text/html", null);
        new PollingCheck() {
            @Override
            protected boolean check() {
                return imageLoaded.mImageLoaded;
            }
        }.run();
        getInstrumentation().waitForIdleSync();

        final HrefCheckHandler handler = new HrefCheckHandler(mWebView.getHandler().getLooper());
        final Message msg = new Message();
        msg.setTarget(handler);

        // touch the image
        handler.reset();
        int[] location = mOnUiThread.getLocationOnScreen();

        long time = SystemClock.uptimeMillis();
        getInstrumentation().sendPointerSync(
                MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN,
                        location[0] + imgWidth / 2,
                        location[1] + imgHeight / 2, 0));
        getInstrumentation().waitForIdleSync();
        mOnUiThread.requestImageRef(msg);
        new PollingCheck() {
            @Override
            protected boolean check() {
                boolean done = false;
                if (handler.hasCalledHandleMessage()) {
                    if (handler.mResultUrl != null) {
                        done = true;
                    } else {
                        handler.reset();
                        Message newMsg = new Message();
                        newMsg.setTarget(handler);
                        mOnUiThread.requestImageRef(newMsg);
                    }
                }
                return done;
            }
        }.run();
        assertEquals(imgUrl, handler.mResultUrl);
    }

    @UiThreadTest
    public void testDebugDump() {
        mWebView.debugDump();
    }

    public void testGetHitTestResult() throws Throwable {
        final String anchor = "<p><a href=\"" + TestHtmlConstants.EXT_WEB_URL1
                + "\">normal anchor</a></p>";
        final String blankAnchor = "<p><a href=\"\">blank anchor</a></p>";
        final String form = "<p><form><input type=\"text\" name=\"Test\"><br>"
                + "<input type=\"submit\" value=\"Submit\"></form></p>";
        String phoneNo = "3106984000";
        final String tel = "<p><a href=\"tel:" + phoneNo + "\">Phone</a></p>";
        String email = "test@gmail.com";
        final String mailto = "<p><a href=\"mailto:" + email + "\">Email</a></p>";
        String location = "shanghai";
        final String geo = "<p><a href=\"geo:0,0?q=" + location + "\">Location</a></p>";

        mOnUiThread.loadDataWithBaseURLAndWaitForCompletion("fake://home",
                "<html><body>" + anchor + blankAnchor + form + tel + mailto +
                geo + "</body></html>", "text/html", "UTF-8", null);
        getInstrumentation().waitForIdleSync();

        // anchor
        moveFocusDown();
        HitTestResult hitTestResult = mOnUiThread.getHitTestResult();
        assertEquals(HitTestResult.SRC_ANCHOR_TYPE, hitTestResult.getType());
        assertEquals(TestHtmlConstants.EXT_WEB_URL1, hitTestResult.getExtra());

        // blank anchor
        moveFocusDown();
        hitTestResult = mOnUiThread.getHitTestResult();
        assertEquals(HitTestResult.SRC_ANCHOR_TYPE, hitTestResult.getType());
        assertEquals("fake://home", hitTestResult.getExtra());

        // text field
        moveFocusDown();
        hitTestResult = mOnUiThread.getHitTestResult();
        assertEquals(HitTestResult.EDIT_TEXT_TYPE, hitTestResult.getType());
        assertNull(hitTestResult.getExtra());

        // submit button
        moveFocusDown();
        hitTestResult = mOnUiThread.getHitTestResult();
        assertEquals(HitTestResult.UNKNOWN_TYPE, hitTestResult.getType());
        assertNull(hitTestResult.getExtra());

        // phone number
        moveFocusDown();
        hitTestResult = mOnUiThread.getHitTestResult();
        assertEquals(HitTestResult.PHONE_TYPE, hitTestResult.getType());
        assertEquals(phoneNo, hitTestResult.getExtra());

        // email
        moveFocusDown();
        hitTestResult = mOnUiThread.getHitTestResult();
        assertEquals(HitTestResult.EMAIL_TYPE, hitTestResult.getType());
        assertEquals(email, hitTestResult.getExtra());

        // geo address
        moveFocusDown();
        hitTestResult = mOnUiThread.getHitTestResult();
        assertEquals(HitTestResult.GEO_TYPE, hitTestResult.getType());
        assertEquals(location, hitTestResult.getExtra());
    }

    public void testSetInitialScale() throws Throwable {
        final String p = "<p style=\"height:1000px;width:1000px\">Test setInitialScale.</p>";
        final float defaultScale =
            getInstrumentation().getTargetContext().getResources().getDisplayMetrics().density;

        mOnUiThread.loadDataAndWaitForCompletion("<html><body>" + p
                + "</body></html>", "text/html", null);
        getInstrumentation().waitForIdleSync();

        assertEquals(defaultScale, mOnUiThread.getScale(), .01f);

        mOnUiThread.setInitialScale(0);
        // modify content to fool WebKit into re-loading
        mOnUiThread.loadDataAndWaitForCompletion("<html><body>" + p
                + "2" + "</body></html>", "text/html", null);
        getInstrumentation().waitForIdleSync();

        assertEquals(defaultScale, mOnUiThread.getScale(), .01f);

        mOnUiThread.setInitialScale(50);
        mOnUiThread.loadDataAndWaitForCompletion("<html><body>" + p
                + "3" + "</body></html>", "text/html", null);
        getInstrumentation().waitForIdleSync();

        assertEquals(0.5f, mOnUiThread.getScale(), .02f);

        mOnUiThread.setInitialScale(0);
        mOnUiThread.loadDataAndWaitForCompletion("<html><body>" + p
                + "4" + "</body></html>", "text/html", null);
        getInstrumentation().waitForIdleSync();

        assertEquals(defaultScale, mOnUiThread.getScale(), .01f);
    }

    @UiThreadTest
    public void testGetFavicon() throws Exception {
        startWebServer(false);
        String url = mWebServer.getAssetUrl(TestHtmlConstants.TEST_FAVICON_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        mWebView.getFavicon();
        // ToBeFixed: Favicon is not loaded automatically.
        // assertNotNull(mWebView.getFavicon());
    }

    @UiThreadTest
    public void testClearHistory() throws Exception {
        startWebServer(false);
        String url1 = mWebServer.getAssetUrl(TestHtmlConstants.HTML_URL1);
        String url2 = mWebServer.getAssetUrl(TestHtmlConstants.HTML_URL2);
        String url3 = mWebServer.getAssetUrl(TestHtmlConstants.HTML_URL3);

        mOnUiThread.loadUrlAndWaitForCompletion(url1);
        pollingCheckWebBackForwardList(url1, 0, 1);

        mOnUiThread.loadUrlAndWaitForCompletion(url2);
        pollingCheckWebBackForwardList(url2, 1, 2);

        mOnUiThread.loadUrlAndWaitForCompletion(url3);
        pollingCheckWebBackForwardList(url3, 2, 3);

        mWebView.clearHistory();

        // only current URL is left after clearing
        pollingCheckWebBackForwardList(url3, 0, 1);
    }

    @UiThreadTest
    public void testSaveAndRestoreState() throws Throwable {
        // nothing to save
        assertNull(mWebView.saveState(new Bundle()));

        startWebServer(false);
        String url1 = mWebServer.getAssetUrl(TestHtmlConstants.HTML_URL1);
        String url2 = mWebServer.getAssetUrl(TestHtmlConstants.HTML_URL2);
        String url3 = mWebServer.getAssetUrl(TestHtmlConstants.HTML_URL3);

        // make a history list
        mOnUiThread.loadUrlAndWaitForCompletion(url1);
        pollingCheckWebBackForwardList(url1, 0, 1);
        mOnUiThread.loadUrlAndWaitForCompletion(url2);
        pollingCheckWebBackForwardList(url2, 1, 2);
        mOnUiThread.loadUrlAndWaitForCompletion(url3);
        pollingCheckWebBackForwardList(url3, 2, 3);

        // save the list
        Bundle bundle = new Bundle();
        WebBackForwardList saveList = mWebView.saveState(bundle);
        assertNotNull(saveList);
        assertEquals(3, saveList.getSize());
        assertEquals(2, saveList.getCurrentIndex());
        assertEquals(url1, saveList.getItemAtIndex(0).getUrl());
        assertEquals(url2, saveList.getItemAtIndex(1).getUrl());
        assertEquals(url3, saveList.getItemAtIndex(2).getUrl());

        // change the content to a new "blank" web view without history
        final WebView newWebView = new WebView(getActivity());

        WebBackForwardList copyListBeforeRestore = newWebView.copyBackForwardList();
        assertNotNull(copyListBeforeRestore);
        assertEquals(0, copyListBeforeRestore.getSize());

        // restore the list
        final WebBackForwardList restoreList = newWebView.restoreState(bundle);
        assertNotNull(restoreList);
        assertEquals(3, restoreList.getSize());
        assertEquals(2, saveList.getCurrentIndex());
        /* ToBeFixed: The WebHistoryItems do not get inflated. Uncomment remaining tests when fixed.
        // wait for the list items to get inflated
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return restoreList.getItemAtIndex(0).getUrl() != null &&
                       restoreList.getItemAtIndex(1).getUrl() != null &&
                       restoreList.getItemAtIndex(2).getUrl() != null;
            }
        }.run();
        assertEquals(url1, restoreList.getItemAtIndex(0).getUrl());
        assertEquals(url2, restoreList.getItemAtIndex(1).getUrl());
        assertEquals(url3, restoreList.getItemAtIndex(2).getUrl());

        WebBackForwardList copyListAfterRestore = newWebView.copyBackForwardList();
        assertNotNull(copyListAfterRestore);
        assertEquals(3, copyListAfterRestore.getSize());
        assertEquals(2, copyListAfterRestore.getCurrentIndex());
        assertEquals(url1, copyListAfterRestore.getItemAtIndex(0).getUrl());
        assertEquals(url2, copyListAfterRestore.getItemAtIndex(1).getUrl());
        assertEquals(url3, copyListAfterRestore.getItemAtIndex(2).getUrl());
        */
    }

    public void testSetWebViewClient() throws Throwable {
        final class MockWebViewClient extends WaitForLoadedClient {
            private boolean mOnScaleChangedCalled = false;
            public MockWebViewClient() {
                super(mOnUiThread);
            }
            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                super.onScaleChanged(view, oldScale, newScale);
                mOnScaleChangedCalled = true;
            }
            public boolean onScaleChangedCalled() {
                return mOnScaleChangedCalled;
            }
        }

        final MockWebViewClient webViewClient = new MockWebViewClient();
        mOnUiThread.setWebViewClient(webViewClient);
        getInstrumentation().waitForIdleSync();
        assertFalse(webViewClient.onScaleChangedCalled());

        mOnUiThread.zoomIn();
        getInstrumentation().waitForIdleSync();
        assertTrue(webViewClient.onScaleChangedCalled());
    }

    @UiThreadTest
    public void testSetAndGetCertificate() {
        assertNull(mWebView.getCertificate());
        SslCertificate certificate = new SslCertificate("foo", "bar", new Date(42), new Date(43));
        mWebView.setCertificate(certificate);
        assertEquals(certificate, mWebView.getCertificate());
    }

    @UiThreadTest
    public void testInsecureSiteClearsCertificate() throws Throwable {
        final SslCertificate certificate =
                new SslCertificate("foo", "bar", new Date(42), new Date(43));
        startWebServer(false);
        final String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mWebView.setCertificate(certificate);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertNull(mWebView.getCertificate());
    }

    @UiThreadTest
    public void testSecureSiteSetsCertificate() throws Throwable {
        final class MockWebViewClient extends WaitForLoadedClient {
            public MockWebViewClient() {
                super(mOnUiThread);
            }
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        }

        startWebServer(true);
        final String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.setWebViewClient(new MockWebViewClient());
        mWebView.setCertificate(null);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        SslCertificate cert = mWebView.getCertificate();
        assertNotNull(cert);
        assertEquals("Android", cert.getIssuedTo().getUName());
    }

    @UiThreadTest
    public void testClearSslPreferences() {
        // FIXME: Implement this. See http://b/5378046.
        mWebView.clearSslPreferences();
    }

    public void testOnReceivedSslError() throws Throwable {
        final class MockWebViewClient extends WaitForLoadedClient {
            private String mErrorUrl;
            private WebView mWebView;

            public MockWebViewClient() {
                super(mOnUiThread);
            }
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                mWebView = view;
                mErrorUrl = error.getUrl();
                handler.proceed();
            }
            public String errorUrl() {
                return mErrorUrl;
            }
            public WebView webView() {
                return mWebView;
            }
        }

        startWebServer(true);
        final String errorUrl = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        final MockWebViewClient webViewClient = new MockWebViewClient();
        mOnUiThread.setWebViewClient(webViewClient);
        mOnUiThread.clearSslPreferences();
        mOnUiThread.loadUrlAndWaitForCompletion(errorUrl);

        assertEquals(mWebView, webViewClient.webView());
        assertEquals(errorUrl, webViewClient.errorUrl());
    }

    public void testOnReceivedSslErrorProceed() throws Throwable {
        final class MockWebViewClient extends WaitForLoadedClient {
            public MockWebViewClient() {
                super(mOnUiThread);
            }
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        }

        startWebServer(true);
        final String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.setWebViewClient(new MockWebViewClient());
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals(TestHtmlConstants.HELLO_WORLD_TITLE, mOnUiThread.getTitle());
    }

    public void testOnReceivedSslErrorCancel() throws Throwable {
        final class MockWebViewClient extends WaitForLoadedClient {
            public MockWebViewClient() {
                super(mOnUiThread);
            }
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
            }
        }

        startWebServer(true);
        final String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.setWebViewClient(new MockWebViewClient());
        mOnUiThread.clearSslPreferences();
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertFalse(TestHtmlConstants.HELLO_WORLD_TITLE.equals(mOnUiThread.getTitle()));
    }

    public void testSslErrorProceedResponseReusedForSameHost() throws Throwable {
        // Load the first page. We expect a call to
        // WebViewClient.onReceivedSslError().
        final SslErrorWebViewClient webViewClient = new SslErrorWebViewClient();
        startWebServer(true);
        final String firstUrl = mWebServer.getAssetUrl(TestHtmlConstants.HTML_URL1);
        mOnUiThread.setWebViewClient(webViewClient);
        mOnUiThread.clearSslPreferences();
        mOnUiThread.loadUrlAndWaitForCompletion(firstUrl);
        assertTrue(webViewClient.wasOnReceivedSslErrorCalled());

        // Load the second page. We don't expect a call to
        // WebViewClient.onReceivedSslError(), but the page should load.
        webViewClient.resetWasOnReceivedSslErrorCalled();
        final String sameHostUrl = mWebServer.getAssetUrl(TestHtmlConstants.HTML_URL2);
        mOnUiThread.loadUrlAndWaitForCompletion(sameHostUrl);
        assertFalse(webViewClient.wasOnReceivedSslErrorCalled());
        assertEquals("Second page", mOnUiThread.getTitle());
    }

    public void testSslErrorProceedResponseNotReusedForDifferentHost() throws Throwable {
        // Load the first page. We expect a call to
        // WebViewClient.onReceivedSslError().
        final SslErrorWebViewClient webViewClient = new SslErrorWebViewClient();
        startWebServer(true);
        final String firstUrl = mWebServer.getAssetUrl(TestHtmlConstants.HTML_URL1);
        mOnUiThread.setWebViewClient(webViewClient);
        mOnUiThread.clearSslPreferences();
        mOnUiThread.loadUrlAndWaitForCompletion(firstUrl);
        assertTrue(webViewClient.wasOnReceivedSslErrorCalled());

        // Load the second page. We expect another call to
        // WebViewClient.onReceivedSslError().
        webViewClient.resetWasOnReceivedSslErrorCalled();
        // The test server uses the host "localhost". "127.0.0.1" works as an
        // alias, but will be considered unique by the WebView.
        final String differentHostUrl = mWebServer.getAssetUrl(TestHtmlConstants.HTML_URL2).replace(
                "localhost", "127.0.0.1");
        mOnUiThread.loadUrlAndWaitForCompletion(differentHostUrl);
        assertTrue(webViewClient.wasOnReceivedSslErrorCalled());
        assertEquals("Second page", mOnUiThread.getTitle());
    }

    public void testRequestChildRectangleOnScreen() throws Throwable {
        DisplayMetrics metrics = mOnUiThread.getDisplayMetrics();
        int dimension = 2 * Math.max(metrics.widthPixels, metrics.heightPixels);
        String p = "<p style=\"height:" + dimension + "px;width:" + dimension + "px\">&nbsp;</p>";
        mOnUiThread.loadDataAndWaitForCompletion("<html><body>" + p
                + "</body></html>", "text/html", null);
        getInstrumentation().waitForIdleSync();

        int origX = mOnUiThread.getScrollX();
        int origY = mOnUiThread.getScrollY();

        metrics = mOnUiThread.getDisplayMetrics();
        dimension = 2 * Math.max(metrics.widthPixels, metrics.heightPixels);
        int half = dimension / 2;
        Rect rect = new Rect(half, half, half + 1, half + 1);
        assertTrue(mOnUiThread.requestChildRectangleOnScreen(mWebView, rect, true));
        assertTrue(mOnUiThread.getScrollX() > origX);
        assertTrue(mOnUiThread.getScrollY() > origY);
    }

    public void testSetDownloadListener() throws Throwable {
        final class MyDownloadListener implements DownloadListener {
            public String url;
            public String mimeType;
            public long contentLength;
            public String contentDisposition;
            public boolean called;

            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition,
                    String mimetype, long contentLength) {
                this.url = url;
                this.mimeType = mimetype;
                this.contentLength = contentLength;
                this.contentDisposition = contentDisposition;
                this.called = true;
            }
        }

        final String mimeType = "application/octet-stream";
        final int length = 100;
        final MyDownloadListener listener = new MyDownloadListener();

        startWebServer(false);
        final String url = mWebServer.getBinaryUrl(mimeType, length);

        // By default, WebView sends an intent to ask the system to
        // handle loading a new URL. We set WebViewClient as
        // WebViewClient.shouldOverrideUrlLoading() returns false, so
        // the WebView will load the new URL.
        mOnUiThread.setDownloadListener(listener);
        mOnUiThread.loadDataAndWaitForCompletion(
                "<html><body><a href=\"" + url
                + "\">link</a></body></html>",
                "text/html", null);
        // Wait for layout to complete before setting focus.
        getInstrumentation().waitForIdleSync();
        assertTrue(mOnUiThread.requestFocus(View.FOCUS_DOWN, null));
        getInstrumentation().waitForIdleSync();
        getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);
        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return listener.called;
            }
        }.run();
        assertEquals(url, listener.url);
        assertTrue(listener.contentDisposition.contains("test.bin"));
        // ToBeFixed: uncomment the following tests after fixing the framework
        // assertEquals(mimeType, listener.mimeType);
        // assertEquals(length, listener.contentLength);
    }

    @UiThreadTest
    public void testSetLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(600, 800);
        mWebView.setLayoutParams(params);
        assertSame(params, mWebView.getLayoutParams());
    }

    @UiThreadTest
    public void testSetMapTrackballToArrowKeys() {
        mWebView.setMapTrackballToArrowKeys(true);
    }

    @UiThreadTest
    public void testSetNetworkAvailable() throws Exception {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        startWebServer(false);
        String url = mWebServer.getAssetUrl(TestHtmlConstants.NETWORK_STATE_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        assertEquals("ONLINE", mWebView.getTitle());

        mWebView.setNetworkAvailable(false);
        mOnUiThread.reloadAndWaitForCompletion();
        assertEquals("OFFLINE", mWebView.getTitle());

        mWebView.setNetworkAvailable(true);
        mOnUiThread.reloadAndWaitForCompletion();
        assertEquals("ONLINE", mWebView.getTitle());
    }

    public void testSetWebChromeClient() throws Throwable {
        final class MockWebChromeClient extends WaitForProgressClient {
            private boolean mOnProgressChanged = false;

            public MockWebChromeClient() {
                super(mOnUiThread);
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                mOnProgressChanged = true;
            }
            public boolean onProgressChangedCalled() {
                return mOnProgressChanged;
            }
        }

        final MockWebChromeClient webChromeClient = new MockWebChromeClient();

        mOnUiThread.setWebChromeClient(webChromeClient);
        getInstrumentation().waitForIdleSync();
        assertFalse(webChromeClient.onProgressChangedCalled());

        startWebServer(false);
        final String url = mWebServer.getAssetUrl(TestHtmlConstants.HELLO_WORLD_URL);
        mOnUiThread.loadUrlAndWaitForCompletion(url);
        getInstrumentation().waitForIdleSync();

        new PollingCheck(TEST_TIMEOUT) {
            @Override
            protected boolean check() {
                return webChromeClient.onProgressChangedCalled();
            }
        }.run();
    }

    public void testPauseResumeTimers() throws Throwable {
        class Monitor {
            private boolean mIsUpdated;

            @JavascriptInterface
            public synchronized void update() {
                mIsUpdated  = true;
                notify();
            }
            public synchronized boolean waitForUpdate() {
                while (!mIsUpdated) {
                    try {
                        // This is slightly flaky, as we can't guarantee that
                        // this is a sufficient time limit, but there's no way
                        // around this.
                        wait(1000);
                        if (!mIsUpdated) {
                            return false;
                        }
                    } catch (InterruptedException e) {
                    }
                }
                mIsUpdated = false;
                return true;
            }
        };
        final Monitor monitor = new Monitor();
        final String updateMonitorHtml = "<html>" +
                "<body onload=\"monitor.update();\"></body></html>";

        // Test that JavaScript is executed even with timers paused.
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebView.getSettings().setJavaScriptEnabled(true);
                mWebView.addJavascriptInterface(monitor, "monitor");
                mWebView.pauseTimers();
                mOnUiThread.loadDataAndWaitForCompletion(updateMonitorHtml,
                        "text/html", null);
            }
        });
        assertTrue(monitor.waitForUpdate());

        // Start a timer and test that it does not fire.
        mOnUiThread.loadDataAndWaitForCompletion(
                "<html><body onload='setTimeout(function(){monitor.update();},100)'>" +
                "</body></html>", "text/html", null);
        assertFalse(monitor.waitForUpdate());

        // Resume timers and test that the timer fires.
        mOnUiThread.resumeTimers();
        assertTrue(monitor.waitForUpdate());
    }

    // verify query parameters can be passed correctly to android asset files
    public void testAndroidAssetQueryParam() {

        WebSettings settings = mOnUiThread.getSettings();
        settings.setJavaScriptEnabled(true);
        // test passing a parameter
        String fileUrl = TestHtmlConstants.getFileUrl(TestHtmlConstants.PARAM_ASSET_URL+"?val=SUCCESS");
        mOnUiThread.loadUrlAndWaitForCompletion(fileUrl);
        assertEquals("SUCCESS", mOnUiThread.getTitle());
    }

    // verify anchors work correctly for android asset files
    public void testAndroidAssetAnchor() {

        WebSettings settings = mOnUiThread.getSettings();
        settings.setJavaScriptEnabled(true);
        // test using an anchor
        String fileUrl = TestHtmlConstants.getFileUrl(TestHtmlConstants.ANCHOR_ASSET_URL+"#anchor");
        mOnUiThread.loadUrlAndWaitForCompletion(fileUrl);
        assertEquals("anchor", mOnUiThread.getTitle());
    }


    @UiThreadTest
    public void testInternals() {
        // Do not test these APIs. They are implementation details.
    }

    private static class HrefCheckHandler extends Handler {
        private boolean mHadRecieved;

        private String mResultUrl;

        public HrefCheckHandler(Looper looper) {
            super(looper);
        }

        public boolean hasCalledHandleMessage() {
            return mHadRecieved;
        }

        public String getResultUrl() {
            return mResultUrl;
        }

        public void reset(){
            mResultUrl = null;
            mHadRecieved = false;
        }

        @Override
        public void handleMessage(Message msg) {
            mResultUrl = msg.getData().getString("url");
            mHadRecieved = true;
        }
    }

    private void moveFocusDown() throws Throwable {
        // send down key and wait for idle
        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        // waiting for idle isn't always sufficient for the key to be fully processed
        Thread.sleep(500);
    }

    private void pollingCheckWebBackForwardList(final String currUrl, final int currIndex,
            final int size) {
        new PollingCheck() {
            @Override
            protected boolean check() {
                WebBackForwardList list = mWebView.copyBackForwardList();
                return checkWebBackForwardList(list, currUrl, currIndex, size);
            }
        }.run();
    }

    private boolean checkWebBackForwardList(WebBackForwardList list, String currUrl,
            int currIndex, int size) {
        return (list != null)
                && (list.getSize() == size)
                && (list.getCurrentIndex() == currIndex)
                && list.getItemAtIndex(currIndex).getUrl().equals(currUrl);
    }

    private void assertGoBackOrForwardBySteps(boolean expected, int steps) {
        // skip if steps equals to 0
        if (steps == 0)
            return;

        int start = steps > 0 ? 1 : steps;
        int end = steps > 0 ? steps : -1;

        // check all the steps in the history
        for (int i = start; i <= end; i++) {
            assertEquals(expected, mWebView.canGoBackOrForward(i));

            // shortcut methods for one step
            if (i == 1) {
                assertEquals(expected, mWebView.canGoForward());
            } else if (i == -1) {
                assertEquals(expected, mWebView.canGoBack());
            }
        }
    }

    private void assertBitmapFillWithColor(Bitmap bitmap, int color) {
        for (int i = 0; i < bitmap.getWidth(); i ++)
            for (int j = 0; j < bitmap.getHeight(); j ++) {
                assertEquals(color, bitmap.getPixel(i, j));
            }
    }

    // Find b1 inside b2
    private boolean checkBitmapInsideAnother(Bitmap b1, Bitmap b2) {
        int w = b1.getWidth();
        int h = b1.getHeight();

        for (int i = 0; i < (b2.getWidth()-w+1); i++) {
            for (int j = 0; j < (b2.getHeight()-h+1); j++) {
                if (checkBitmapInsideAnother(b1, b2, i, j))
                    return true;
            }
        }
        return false;
    }

    private boolean comparePixel(int p1, int p2, int maxError) {
        int err;
        err = Math.abs(((p1&0xff000000)>>>24) - ((p2&0xff000000)>>>24));
        if (err > maxError)
            return false;

        err = Math.abs(((p1&0x00ff0000)>>>16) - ((p2&0x00ff0000)>>>16));
        if (err > maxError)
            return false;

        err = Math.abs(((p1&0x0000ff00)>>>8) - ((p2&0x0000ff00)>>>8));
        if (err > maxError)
            return false;

        err = Math.abs(((p1&0x000000ff)>>>0) - ((p2&0x000000ff)>>>0));
        if (err > maxError)
            return false;

        return true;
    }

    private boolean checkBitmapInsideAnother(Bitmap b1, Bitmap b2, int x, int y) {
        for (int i = 0; i < b1.getWidth(); i++)
            for (int j = 0; j < b1.getHeight(); j++) {
                if (!comparePixel(b1.getPixel(i, j), b2.getPixel(x + i, y + j), 10)) {
                    return false;
                }
            }
        return true;
    }

    /**
     * Waits at least MIN_SCROLL_WAIT_MS for scrolling to start. Once started,
     * scrolling is checked every SCROLL_WAIT_INTERVAL_MS for changes. Once
     * changes have stopped, the function exits. If no scrolling has happened
     * then the function exits after MIN_SCROLL_WAIT milliseconds.
     * @param previousScrollY The Y scroll position prior to waiting.
     */
    private void waitForScrollingComplete(int previousScrollY)
            throws InterruptedException {
        int scrollY = previousScrollY;
        // wait at least MIN_SCROLL_WAIT for something to happen.
        long noChangeMinWait = SystemClock.uptimeMillis() + MIN_SCROLL_WAIT_MS;
        boolean scrollChanging = false;
        boolean scrollChanged = false;
        boolean minWaitExpired = false;
        while (scrollChanging || (!scrollChanged && !minWaitExpired)) {
            Thread.sleep(SCROLL_WAIT_INTERVAL_MS);
            int oldScrollY = scrollY;
            scrollY = mOnUiThread.getScrollY();
            scrollChanging = (scrollY != oldScrollY);
            scrollChanged = (scrollY != previousScrollY);
            minWaitExpired = (SystemClock.uptimeMillis() > noChangeMinWait);
        }
    }

    // Note that this class is not thread-safe.
    final class SslErrorWebViewClient extends WaitForLoadedClient {
        private boolean mWasOnReceivedSslErrorCalled;

        public SslErrorWebViewClient() {
            super(mOnUiThread);
        }
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            mWasOnReceivedSslErrorCalled = true;
            handler.proceed();
        }
        public void resetWasOnReceivedSslErrorCalled() {
            mWasOnReceivedSslErrorCalled = false;
        }
        public boolean wasOnReceivedSslErrorCalled() {
            return mWasOnReceivedSslErrorCalled;
        }
    }
}
