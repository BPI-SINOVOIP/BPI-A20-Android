/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.tools.lint.checks;

import com.android.tools.lint.detector.api.Detector;

@SuppressWarnings("javadoc")
public class InefficientWeightDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new InefficientWeightDetector();
    }

    public void testWeights() throws Exception {
        assertEquals(
            "res/layout/inefficient_weight.xml:10: Warning: Use a layout_width of 0dip instead of match_parent for better performance [InefficientWeight]\n" +
            "     android:layout_width=\"match_parent\"\n" +
            "     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "res/layout/inefficient_weight.xml:24: Warning: Use a layout_height of 0dip instead of wrap_content for better performance [InefficientWeight]\n" +
            "      android:layout_height=\"wrap_content\"\n" +
            "      ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 2 warnings\n" +
            "",
            lintFiles("res/layout/inefficient_weight.xml"));
    }

    public void testWeights2() throws Exception {
        assertEquals(
            "res/layout/nested_weights.xml:23: Warning: Nested weights are bad for performance [NestedWeights]\n" +
            "            android:layout_weight=\"1\"\n" +
            "            ~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 1 warnings\n" +
            "",
            lintFiles("res/layout/nested_weights.xml"));
    }

    public void testWeights3() throws Exception {
        assertEquals(
            "res/layout/baseline_weights.xml:2: Warning: Set android:baselineAligned=\"false\" on this element for better performance [DisableBaselineAlignment]\n" +
            "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "^\n" +
            "0 errors, 1 warnings\n" +
            "",
            lintFiles("res/layout/baseline_weights.xml"));
    }

    public void testNoVerticalWeights3() throws Exception {
        // Orientation=vertical
        assertEquals(
            "No warnings.",
            lintFiles("res/layout/baseline_weights2.xml"));
    }

    public void testNoVerticalWeights4() throws Exception {
        // Orientation not specified ==> horizontal
        assertEquals(
            "res/layout/baseline_weights3.xml:2: Warning: Set android:baselineAligned=\"false\" on this element for better performance [DisableBaselineAlignment]\n" +
            "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "^\n" +
            "0 errors, 1 warnings\n" +
            "",
            lintFiles("res/layout/baseline_weights3.xml"));
    }

    public void testSuppressed() throws Exception {
        assertEquals(
            "No warnings.",

            lintFiles("res/layout/inefficient_weight2.xml"));
    }

    public void testNestedWeights() throws Exception {
        // Regression test for http://code.google.com/p/android/issues/detail?id=22889
        // (Comment 8)
        assertEquals(
                "No warnings.",

                lintFiles("res/layout/nested_weights2.xml"));
    }
}
