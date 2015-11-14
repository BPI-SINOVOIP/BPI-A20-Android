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

package com.android.photoeditor.filters;

import com.android.photoeditor.Photo;

/**
 * Highlight filter applied to the image.
 */
public class HighlightFilter extends Filter {

    private float white;

    /**
     * Sets the highlight level.
     *
     * @param highlight ranges from 0 to 1.
     */
    public void setHighlight(float highlight) {
        white = 1f - highlight * 0.5f;
        validate();
    }

    @Override
    public void process(Photo src, Photo dst) {
        ImageUtils.nativeWhiteBlack(src.bitmap(), dst.bitmap(), white, 0f);
    }
}
