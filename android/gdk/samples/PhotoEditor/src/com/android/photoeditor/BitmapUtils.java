/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.photoeditor;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utils for bitmap operations.
 */
public class BitmapUtils {

    private static final String TAG = "BitmapUtils";
    private static final int DEFAULT_COMPRESS_QUALITY = 90;
    private static final int INDEX_ORIENTATION = 0;

    private static final String[] IMAGE_PROJECTION = new String[] {
        ImageColumns.ORIENTATION
    };

    private final Context context;

    public BitmapUtils(Context context) {
        this.context = context;
    }

    /**
     * Returns an immutable bitmap from subset of source bitmap transformed by the given matrix.
     */
    public static Bitmap createBitmap(Bitmap bitmap, Matrix m) {
        // TODO: Re-implement createBitmap to avoid ARGB -> RBG565 conversion on platforms
        // prior to honeycomb.
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    private void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Rect getBitmapBounds(Uri uri) {
        Rect bounds = new Rect();
        InputStream is = null;

        try {
            is = context.getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);

            bounds.right = options.outWidth;
            bounds.bottom = options.outHeight;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            closeStream(is);
        }

        return bounds;
    }

    private int getOrientation(Uri uri) {
        int orientation = 0;
        Cursor cursor = context.getContentResolver().query(uri, IMAGE_PROJECTION, null, null, null);
        if ((cursor != null) && cursor.moveToNext()) {
            orientation = cursor.getInt(INDEX_ORIENTATION);
        }
        return orientation;
    }

    /**
     * Decodes immutable bitmap that keeps aspect-ratio and spans most within the given rectangle.
     */
    private Bitmap decodeBitmap(Uri uri, int width, int height) {
        InputStream is = null;
        Bitmap bitmap = null;

        try {
            // TODO: Take max pixels allowed into account for calculation to avoid possible OOM.
            Rect bounds = getBitmapBounds(uri);
            int sampleSize = Math.max(bounds.width() / width, bounds.height() / height);
            sampleSize = Math.min(sampleSize,
                    Math.max(bounds.width() / height, bounds.height() / width));

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = Math.max(sampleSize, 1);
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            is = context.getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(is, null, options);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException: " + uri);
        } finally {
            closeStream(is);
        }

        // Scale down the sampled bitmap if it's still larger than the desired dimension.
        if (bitmap != null) {
            float scale = Math.min((float) width / bitmap.getWidth(),
                    (float) height / bitmap.getHeight());
            scale = Math.max(scale, Math.min((float) height / bitmap.getWidth(),
                    (float) width / bitmap.getHeight()));
            if (scale < 1) {
                Matrix m = new Matrix();
                m.setScale(scale, scale);
                Bitmap transformed = createBitmap(bitmap, m);
                bitmap.recycle();
                return transformed;
            }
        }
        return bitmap;
    }

    /**
     * Gets decoded bitmap that keeps orientation as well.
     */
    public Bitmap getBitmap(Uri uri, int width, int height) {
        Bitmap bitmap = decodeBitmap(uri, width, height);

        // Rotate the decoded bitmap according to its orientation if it's necessary.
        if (bitmap != null) {
            int orientation = getOrientation(uri);
            if (orientation != 0) {
                Matrix m = new Matrix();
                m.setRotate(orientation);
                Bitmap transformed = createBitmap(bitmap, m);
                bitmap.recycle();
                return transformed;
            }
        }
        return bitmap;
    }

    /**
     * Saves the bitmap by given directory, filename, and format; if the directory is given null,
     * then saves it under the cache directory.
     */
    public File saveBitmap(
            Bitmap bitmap, String directory, String filename, CompressFormat format) {

        if (directory == null) {
            directory = context.getCacheDir().getAbsolutePath();
        } else {
            // Check if the given directory exists or try to create it.
            File file = new File(directory);
            if (!file.isDirectory() && !file.mkdirs()) {
                return null;
            }
        }

        File file = null;
        OutputStream os = null;

        try {
            filename = (format == CompressFormat.PNG) ? filename + ".png" : filename + ".jpg";
            file = new File(directory, filename);
            os = new FileOutputStream(file);
            bitmap.compress(format, DEFAULT_COMPRESS_QUALITY, os);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            closeStream(os);
        }
        return file;
    }
}
