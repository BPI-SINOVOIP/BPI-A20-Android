/*
 * Copyright (C) 2011 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.ex.photo.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.ex.photo.Intents;
import com.android.ex.photo.PhotoViewCallbacks;
import com.android.ex.photo.PhotoViewCallbacks.CursorChangedListener;
import com.android.ex.photo.PhotoViewCallbacks.OnScreenListener;
import com.android.ex.photo.R;
import com.android.ex.photo.adapters.PhotoPagerAdapter;
import com.android.ex.photo.loaders.PhotoBitmapLoader;
import com.android.ex.photo.util.ImageUtils;
import com.android.ex.photo.views.PhotoView;
import com.android.ex.photo.views.ProgressBarWrapper;

/**
 * Displays a photo.
 */
public class PhotoViewFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Bitmap>, OnClickListener, OnScreenListener, CursorChangedListener {
    /**
     * Interface for components that are internally scrollable left-to-right.
     */
    public static interface HorizontallyScrollable {
        /**
         * Return {@code true} if the component needs to receive right-to-left
         * touch movements.
         *
         * @param origX the raw x coordinate of the initial touch
         * @param origY the raw y coordinate of the initial touch
         */

        public boolean interceptMoveLeft(float origX, float origY);

        /**
         * Return {@code true} if the component needs to receive left-to-right
         * touch movements.
         *
         * @param origX the raw x coordinate of the initial touch
         * @param origY the raw y coordinate of the initial touch
         */
        public boolean interceptMoveRight(float origX, float origY);
    }

    protected final static String STATE_INTENT_KEY =
            "com.android.mail.photo.fragments.PhotoViewFragment.INTENT";

    // Loader IDs
    protected final static int LOADER_ID_PHOTO = 1;
    protected final static int LOADER_ID_THUMBNAIL = 2;

    /** The size of the photo */
    public static Integer sPhotoSize;

    /** The URL of a photo to display */
    protected String mResolvedPhotoUri;
    protected String mThumbnailUri;
    /** The intent we were launched with */
    protected Intent mIntent;
    protected PhotoViewCallbacks mCallback;
    protected PhotoPagerAdapter mAdapter;

    protected PhotoView mPhotoView;
    protected ImageView mPhotoPreviewImage;
    protected TextView mEmptyText;
    protected ImageView mRetryButton;
    protected ProgressBarWrapper mPhotoProgressBar;

    protected final int mPosition;

    /** Whether or not the fragment should make the photo full-screen */
    protected boolean mFullScreen;

    /** Whether or not this fragment will only show the loading spinner */
    protected final boolean mOnlyShowSpinner;

    /** Whether or not the progress bar is showing valid information about the progress stated */
    protected boolean mProgressBarNeeded = true;

    protected View mPhotoPreviewAndProgress;

    public PhotoViewFragment() {
        mPosition = -1;
        mOnlyShowSpinner = false;
        mProgressBarNeeded = true;
    }

    public PhotoViewFragment(Intent intent, int position, PhotoPagerAdapter adapter,
            boolean onlyShowSpinner) {
        mIntent = intent;
        mPosition = position;
        mAdapter = adapter;
        mOnlyShowSpinner = onlyShowSpinner;
        mProgressBarNeeded = true;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallback = (PhotoViewCallbacks) activity;
        if (mCallback == null) {
            throw new IllegalArgumentException(
                    "Activity must be a derived class of PhotoViewActivity");
        }

        if (sPhotoSize == null) {
            final DisplayMetrics metrics = new DisplayMetrics();
            final WindowManager wm =
                    (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
            final ImageUtils.ImageSize imageSize = ImageUtils.sUseImageSize;
            wm.getDefaultDisplay().getMetrics(metrics);
            switch (imageSize) {
                case EXTRA_SMALL: {
                    // Use a photo that's 80% of the "small" size
                    sPhotoSize = (Math.min(metrics.heightPixels, metrics.widthPixels) * 800) / 1000;
                    break;
                }

                case SMALL:
                case NORMAL:
                default: {
                    sPhotoSize = Math.min(metrics.heightPixels, metrics.widthPixels);
                    break;
                }
            }
        }
    }

    @Override
    public void onDetach() {
        mCallback = null;
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            final Bundle state = savedInstanceState.getBundle(STATE_INTENT_KEY);
            if (state != null) {
                mIntent = new Intent().putExtras(state);
            }
        }

        if (mIntent != null) {
            mResolvedPhotoUri = mIntent.getStringExtra(Intents.EXTRA_RESOLVED_PHOTO_URI);
            mThumbnailUri = mIntent.getStringExtra(Intents.EXTRA_THUMBNAIL_URI);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.photo_fragment_view, container, false);

        mPhotoView = (PhotoView) view.findViewById(R.id.photo_view);
        mPhotoView.setMaxInitialScale(mIntent.getFloatExtra(Intents.EXTRA_MAX_INITIAL_SCALE, 1));
        mPhotoView.setOnClickListener(this);
        mPhotoView.setFullScreen(mFullScreen, false);
        mPhotoView.enableImageTransforms(false);

        mPhotoPreviewAndProgress = view.findViewById(R.id.photo_preview);
        mPhotoPreviewImage = (ImageView) view.findViewById(R.id.photo_preview_image);
        final ProgressBar indeterminate =
                (ProgressBar) view.findViewById(R.id.indeterminate_progress);
        final ProgressBar determinate =
                (ProgressBar) view.findViewById(R.id.determinate_progress);
        mPhotoProgressBar = new ProgressBarWrapper(determinate, indeterminate, true);
        mEmptyText = (TextView) view.findViewById(R.id.empty_text);
        mRetryButton = (ImageView) view.findViewById(R.id.retry_button);

        // Don't call until we've setup the entire view
        setViewVisibility();

        return view;
    }

    @Override
    public void onResume() {
        mCallback.addScreenListener(this);
        mCallback.addCursorListener(this);

        getLoaderManager().initLoader(LOADER_ID_THUMBNAIL, null, this);

        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Remove listeners
        mCallback.removeCursorListener(this);
        mCallback.removeScreenListener(this);
        resetPhotoView();
    }

    @Override
    public void onDestroyView() {
        // Clean up views and other components
        if (mPhotoView != null) {
            mPhotoView.clear();
            mPhotoView = null;
        }

        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mIntent != null) {
            outState.putParcelable(STATE_INTENT_KEY, mIntent.getExtras());
        }
    }

    @Override
    public Loader<Bitmap> onCreateLoader(int id, Bundle args) {
        if(mOnlyShowSpinner) {
            return null;
        }
        switch (id) {
            case LOADER_ID_PHOTO:
                return new PhotoBitmapLoader(getActivity(), mResolvedPhotoUri);
            case LOADER_ID_THUMBNAIL:
                return new PhotoBitmapLoader(getActivity(), mThumbnailUri);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Bitmap> loader, Bitmap data) {
        // If we don't have a view, the fragment has been paused. We'll get the cursor again later.
        if (getView() == null) {
            return;
        }

        final int id = loader.getId();
        switch (id) {
            case LOADER_ID_PHOTO:
                if (data != null) {
                    bindPhoto(data);
                    enableImageTransforms(true);
                    mPhotoPreviewAndProgress.setVisibility(View.GONE);
                    mProgressBarNeeded = false;
                } else {
                    // Received a null result for the full size image.  Instead attempt to load the
                    // thumbnail
                    Handler handler = new Handler();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            getLoaderManager().initLoader(LOADER_ID_THUMBNAIL, null,
                                                          PhotoViewFragment.this);
                        }
                    });
                }
                break;
            case LOADER_ID_THUMBNAIL:
                mProgressBarNeeded = false;
                if (isPhotoBound()) {
                    // There is need to do anything with the thumbnail image, as the full size
                    // image is being shown.
                    mPhotoPreviewAndProgress.setVisibility(View.GONE);
                    return;
                } else if (data == null) {
                    // no preview, show default
                    mPhotoPreviewImage.setVisibility(View.VISIBLE);
                    mPhotoPreviewImage.setImageResource(R.drawable.default_image);
                } else {
                    bindPhoto(data);
                    enableImageTransforms(false);
                    Handler handler = new Handler();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            getLoaderManager().initLoader(LOADER_ID_PHOTO, null,
                                PhotoViewFragment.this);
                        }
                    });
                }
                break;
            default:
                break;
        }

        if (mProgressBarNeeded == false) {
            // Hide the progress bar as it isn't needed anymore.
            mPhotoProgressBar.setVisibility(View.GONE);
        }

        mCallback.setViewActivated();
        setViewVisibility();
    }

    /**
     * Binds an image to the photo view.
     */
    private void bindPhoto(Bitmap bitmap) {
        if (mPhotoView != null) {
            mPhotoView.bindPhoto(bitmap);
        }
    }

    /**
     * Enable or disable image transformations. When transformations are enabled, this view
     * consumes all touch events.
     */
    public void enableImageTransforms(boolean enable) {
        mPhotoView.enableImageTransforms(enable);
    }

    /**
     * Resets the photo view to it's default state w/ no bound photo.
     */
    private void resetPhotoView() {
        if (mPhotoView != null) {
            mPhotoView.bindPhoto(null);
        }
    }

    @Override
    public void onLoaderReset(Loader<Bitmap> loader) {
        // Do nothing
    }

    @Override
    public void onClick(View v) {
        mCallback.toggleFullScreen();
    }

    @Override
    public void onFullScreenChanged(boolean fullScreen) {
        setViewVisibility();
    }

    @Override
    public void onViewActivated() {
        if (!mCallback.isFragmentActive(this)) {
            // we're not in the foreground; reset our view
            resetViews();
        } else {
            mCallback.onFragmentVisible(this);
        }
    }

    /**
     * Reset the views to their default states
     */
    public void resetViews() {
        if (mPhotoView != null) {
            mPhotoView.resetTransformations();
        }
    }

    @Override
    public boolean onInterceptMoveLeft(float origX, float origY) {
        if (!mCallback.isFragmentActive(this)) {
            // we're not in the foreground; don't intercept any touches
            return false;
        }

        return (mPhotoView != null && mPhotoView.interceptMoveLeft(origX, origY));
    }

    @Override
    public boolean onInterceptMoveRight(float origX, float origY) {
        if (!mCallback.isFragmentActive(this)) {
            // we're not in the foreground; don't intercept any touches
            return false;
        }

        return (mPhotoView != null && mPhotoView.interceptMoveRight(origX, origY));
    }

    /**
     * Returns {@code true} if a photo has been bound. Otherwise, returns {@code false}.
     */
    public boolean isPhotoBound() {
        return (mPhotoView != null && mPhotoView.isPhotoBound());
    }

    /**
     * Sets view visibility depending upon whether or not we're in "full screen" mode.
     */
    private void setViewVisibility() {
        final boolean fullScreen = mCallback.isFragmentFullScreen(this);
        final boolean hide = fullScreen;

        setFullScreen(hide);
    }

    /**
     * Sets full-screen mode for the views.
     */
    public void setFullScreen(boolean fullScreen) {
        mFullScreen = fullScreen;
    }

    @Override
    public void onCursorChanged(Cursor cursor) {
        if (cursor.moveToPosition(mPosition) && !isPhotoBound()) {
            final LoaderManager manager = getLoaderManager();
            final Loader<Bitmap> fakeLoader = manager.getLoader(LOADER_ID_PHOTO);
            if (fakeLoader == null) {
                return;
            }

            final PhotoBitmapLoader loader =
                    (PhotoBitmapLoader) fakeLoader;
            mResolvedPhotoUri = mAdapter.getPhotoUri(cursor);
            loader.setPhotoUri(mResolvedPhotoUri);
            loader.forceLoad();
        }
    }

    public ProgressBarWrapper getPhotoProgressBar() {
        return mPhotoProgressBar;
    }

    public TextView getEmptyText() {
        return mEmptyText;
    }

    public ImageView getRetryButton() {
        return mRetryButton;
    }

    public boolean isProgressBarNeeded() {
        return mProgressBarNeeded;
    }
}
