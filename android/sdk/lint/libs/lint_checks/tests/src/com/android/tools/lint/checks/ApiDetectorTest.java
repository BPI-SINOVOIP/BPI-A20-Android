/*
 * Copyright (C) 2012 The Android Open Source Project
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
public class ApiDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new ApiDetector();
    }

    public void testXmlApi1() throws Exception {
        assertEquals(
            "res/color/colors.xml:9: Error: @android:color/holo_red_light requires API level 14 (current min is 1) [NewApi]\n" +
            "        <item name=\"android:windowBackground\">  @android:color/holo_red_light </item>\n" +
            "                                                ^\n" +
            "res/layout/layout.xml:9: Error: View requires API level 5 (current min is 1): <QuickContactBadge> [NewApi]\n" +
            "    <QuickContactBadge\n" +
            "    ^\n" +
            "res/layout/layout.xml:15: Error: View requires API level 11 (current min is 1): <CalendarView> [NewApi]\n" +
            "    <CalendarView\n" +
            "    ^\n" +
            "res/layout/layout.xml:21: Error: View requires API level 14 (current min is 1): <GridLayout> [NewApi]\n" +
            "    <GridLayout\n" +
            "    ^\n" +
            "res/layout/layout.xml:22: Error: @android:attr/actionBarSplitStyle requires API level 14 (current min is 1) [NewApi]\n" +
            "        foo=\"@android:attr/actionBarSplitStyle\"\n" +
            "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "res/layout/layout.xml:23: Error: @android:color/holo_red_light requires API level 14 (current min is 1) [NewApi]\n" +
            "        bar=\"@android:color/holo_red_light\"\n" +
            "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "res/values/themes.xml:9: Error: @android:color/holo_red_light requires API level 14 (current min is 1) [NewApi]\n" +
            "        <item name=\"android:windowBackground\">  @android:color/holo_red_light </item>\n" +
            "                                                ^\n" +
            "7 errors, 0 warnings\n" +
            "",

            lintProject(
                "apicheck/minsdk1.xml=>AndroidManifest.xml",
                "apicheck/layout.xml=>res/layout/layout.xml",
                "apicheck/themes.xml=>res/values/themes.xml",
                "apicheck/themes.xml=>res/color/colors.xml"
                ));
    }

    public void testXmlApi14() throws Exception {
        assertEquals(
                "No warnings.",

                lintProject(
                    "apicheck/minsdk14.xml=>AndroidManifest.xml",
                    "apicheck/layout.xml=>res/layout/layout.xml",
                    "apicheck/themes.xml=>res/values/themes.xml",
                    "apicheck/themes.xml=>res/color/colors.xml"
                    ));
    }

    public void testXmlApiFolderVersion11() throws Exception {
        assertEquals(
            "res/color-v11/colors.xml:9: Error: @android:color/holo_red_light requires API level 14 (current min is 1) [NewApi]\n" +
            "        <item name=\"android:windowBackground\">  @android:color/holo_red_light </item>\n" +
            "                                                ^\n" +
            "res/layout-v11/layout.xml:21: Error: View requires API level 14 (current min is 1): <GridLayout> [NewApi]\n" +
            "    <GridLayout\n" +
            "    ^\n" +
            "res/layout-v11/layout.xml:22: Error: @android:attr/actionBarSplitStyle requires API level 14 (current min is 1) [NewApi]\n" +
            "        foo=\"@android:attr/actionBarSplitStyle\"\n" +
            "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "res/layout-v11/layout.xml:23: Error: @android:color/holo_red_light requires API level 14 (current min is 1) [NewApi]\n" +
            "        bar=\"@android:color/holo_red_light\"\n" +
            "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "res/values-v11/themes.xml:9: Error: @android:color/holo_red_light requires API level 14 (current min is 1) [NewApi]\n" +
            "        <item name=\"android:windowBackground\">  @android:color/holo_red_light </item>\n" +
            "                                                ^\n" +
            "5 errors, 0 warnings\n" +
            "",

            lintProject(
                "apicheck/minsdk1.xml=>AndroidManifest.xml",
                "apicheck/layout.xml=>res/layout-v11/layout.xml",
                "apicheck/themes.xml=>res/values-v11/themes.xml",
                "apicheck/themes.xml=>res/color-v11/colors.xml"
                ));
    }

    public void testXmlApiFolderVersion14() throws Exception {
        assertEquals(
                "No warnings.",

                lintProject(
                    "apicheck/minsdk1.xml=>AndroidManifest.xml",
                    "apicheck/layout.xml=>res/layout-v14/layout.xml",
                    "apicheck/themes.xml=>res/values-v14/themes.xml",
                    "apicheck/themes.xml=>res/color-v14/colors.xml"
                    ));
    }

    public void testApi1() throws Exception {
        assertEquals(
            "src/foo/bar/ApiCallTest.java:18: Error: Class requires API level 8 (current min is 1): org.w3c.dom.DOMLocator [NewApi]\n" +
            " public void method(Chronometer chronometer, DOMLocator locator) {\n" +
            "                                             ~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:20: Error: Call requires API level 11 (current min is 1): android.app.Activity#getActionBar [NewApi]\n" +
            "  getActionBar(); // API 11\n" +
            "  ~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:23: Error: Class requires API level 8 (current min is 1): org.w3c.dom.DOMError [NewApi]\n" +
            "  DOMError error = null; // API 8\n" +
            "  ~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:24: Error: Class requires API level 8 (current min is 1): org.w3c.dom.DOMErrorHandler [NewApi]\n" +
            "  Class<?> clz = DOMErrorHandler.class; // API 8\n" +
            "                 ~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:27: Error: Call requires API level 3 (current min is 1): android.widget.Chronometer#getOnChronometerTickListener [NewApi]\n" +
            "  chronometer.getOnChronometerTickListener(); // API 3 \n" +
            "              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:30: Error: Call requires API level 11 (current min is 1): android.widget.Chronometer#setTextIsSelectable [NewApi]\n" +
            "  chronometer.setTextIsSelectable(true); // API 11\n" +
            "              ~~~~~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:33: Error: Field requires API level 11 (current min is 1): dalvik.bytecode.OpcodeInfo#MAXIMUM_VALUE [NewApi]\n" +
            "  int field = OpcodeInfo.MAXIMUM_VALUE; // API 11\n" +
            "                         ~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:38: Error: Class requires API level 14 (current min is 1): android.app.ApplicationErrorReport.BatteryInfo [NewApi]\n" +
            "  BatteryInfo batteryInfo = getReport().batteryInfo;\n" +
            "  ^\n" +
            "src/foo/bar/ApiCallTest.java:38: Error: Field requires API level 14 (current min is 1): android.app.ApplicationErrorReport#batteryInfo [NewApi]\n" +
            "  BatteryInfo batteryInfo = getReport().batteryInfo;\n" +
            "              ~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:41: Error: Field requires API level 11 (current min is 1): android.graphics.PorterDuff.Mode#OVERLAY [NewApi]\n" +
            "  Mode mode = PorterDuff.Mode.OVERLAY; // API 11\n" +
            "                              ~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:45: Error: Class requires API level 14 (current min is 1): android.widget.GridLayout [NewApi]\n" +
            " GridLayout getGridLayout() { // API 14\n" +
            "            ~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:49: Error: Class requires API level 14 (current min is 1): android.app.ApplicationErrorReport [NewApi]\n" +
            " private ApplicationErrorReport getReport() {\n" +
            "                                ~~~~~~~~~\n" +
            "12 errors, 0 warnings\n" +
            "",

            lintProject(
                "apicheck/classpath=>.classpath",
                "apicheck/minsdk1.xml=>AndroidManifest.xml",
                "apicheck/ApiCallTest.java.txt=>src/foo/bar/ApiCallTest.java",
                "apicheck/ApiCallTest.class.data=>bin/classes/foo/bar/ApiCallTest.class"
                ));
    }

    public void testApi2() throws Exception {
        assertEquals(
            "src/foo/bar/ApiCallTest.java:18: Error: Class requires API level 8 (current min is 2): org.w3c.dom.DOMLocator [NewApi]\n" +
            " public void method(Chronometer chronometer, DOMLocator locator) {\n" +
            "                                             ~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:20: Error: Call requires API level 11 (current min is 2): android.app.Activity#getActionBar [NewApi]\n" +
            "  getActionBar(); // API 11\n" +
            "  ~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:23: Error: Class requires API level 8 (current min is 2): org.w3c.dom.DOMError [NewApi]\n" +
            "  DOMError error = null; // API 8\n" +
            "  ~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:24: Error: Class requires API level 8 (current min is 2): org.w3c.dom.DOMErrorHandler [NewApi]\n" +
            "  Class<?> clz = DOMErrorHandler.class; // API 8\n" +
            "                 ~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:27: Error: Call requires API level 3 (current min is 2): android.widget.Chronometer#getOnChronometerTickListener [NewApi]\n" +
            "  chronometer.getOnChronometerTickListener(); // API 3 \n" +
            "              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:30: Error: Call requires API level 11 (current min is 2): android.widget.Chronometer#setTextIsSelectable [NewApi]\n" +
            "  chronometer.setTextIsSelectable(true); // API 11\n" +
            "              ~~~~~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:33: Error: Field requires API level 11 (current min is 2): dalvik.bytecode.OpcodeInfo#MAXIMUM_VALUE [NewApi]\n" +
            "  int field = OpcodeInfo.MAXIMUM_VALUE; // API 11\n" +
            "                         ~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:38: Error: Class requires API level 14 (current min is 2): android.app.ApplicationErrorReport.BatteryInfo [NewApi]\n" +
            "  BatteryInfo batteryInfo = getReport().batteryInfo;\n" +
            "  ^\n" +
            "src/foo/bar/ApiCallTest.java:38: Error: Field requires API level 14 (current min is 2): android.app.ApplicationErrorReport#batteryInfo [NewApi]\n" +
            "  BatteryInfo batteryInfo = getReport().batteryInfo;\n" +
            "              ~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:41: Error: Field requires API level 11 (current min is 2): android.graphics.PorterDuff.Mode#OVERLAY [NewApi]\n" +
            "  Mode mode = PorterDuff.Mode.OVERLAY; // API 11\n" +
            "                              ~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:45: Error: Class requires API level 14 (current min is 2): android.widget.GridLayout [NewApi]\n" +
            " GridLayout getGridLayout() { // API 14\n" +
            "            ~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:49: Error: Class requires API level 14 (current min is 2): android.app.ApplicationErrorReport [NewApi]\n" +
            " private ApplicationErrorReport getReport() {\n" +
            "                                ~~~~~~~~~\n" +
            "12 errors, 0 warnings\n" +
            "",

            lintProject(
                "apicheck/classpath=>.classpath",
                "apicheck/minsdk2.xml=>AndroidManifest.xml",
                "apicheck/ApiCallTest.java.txt=>src/foo/bar/ApiCallTest.java",
                "apicheck/ApiCallTest.class.data=>bin/classes/foo/bar/ApiCallTest.class"
                ));
    }

    public void testApi4() throws Exception {
        assertEquals(
            "src/foo/bar/ApiCallTest.java:18: Error: Class requires API level 8 (current min is 4): org.w3c.dom.DOMLocator [NewApi]\n" +
            " public void method(Chronometer chronometer, DOMLocator locator) {\n" +
            "                                             ~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:20: Error: Call requires API level 11 (current min is 4): android.app.Activity#getActionBar [NewApi]\n" +
            "  getActionBar(); // API 11\n" +
            "  ~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:23: Error: Class requires API level 8 (current min is 4): org.w3c.dom.DOMError [NewApi]\n" +
            "  DOMError error = null; // API 8\n" +
            "  ~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:24: Error: Class requires API level 8 (current min is 4): org.w3c.dom.DOMErrorHandler [NewApi]\n" +
            "  Class<?> clz = DOMErrorHandler.class; // API 8\n" +
            "                 ~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:30: Error: Call requires API level 11 (current min is 4): android.widget.Chronometer#setTextIsSelectable [NewApi]\n" +
            "  chronometer.setTextIsSelectable(true); // API 11\n" +
            "              ~~~~~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:33: Error: Field requires API level 11 (current min is 4): dalvik.bytecode.OpcodeInfo#MAXIMUM_VALUE [NewApi]\n" +
            "  int field = OpcodeInfo.MAXIMUM_VALUE; // API 11\n" +
            "                         ~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:38: Error: Class requires API level 14 (current min is 4): android.app.ApplicationErrorReport.BatteryInfo [NewApi]\n" +
            "  BatteryInfo batteryInfo = getReport().batteryInfo;\n" +
            "  ^\n" +
            "src/foo/bar/ApiCallTest.java:38: Error: Field requires API level 14 (current min is 4): android.app.ApplicationErrorReport#batteryInfo [NewApi]\n" +
            "  BatteryInfo batteryInfo = getReport().batteryInfo;\n" +
            "              ~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:41: Error: Field requires API level 11 (current min is 4): android.graphics.PorterDuff.Mode#OVERLAY [NewApi]\n" +
            "  Mode mode = PorterDuff.Mode.OVERLAY; // API 11\n" +
            "                              ~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:45: Error: Class requires API level 14 (current min is 4): android.widget.GridLayout [NewApi]\n" +
            " GridLayout getGridLayout() { // API 14\n" +
            "            ~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:49: Error: Class requires API level 14 (current min is 4): android.app.ApplicationErrorReport [NewApi]\n" +
            " private ApplicationErrorReport getReport() {\n" +
            "                                ~~~~~~~~~\n" +
            "11 errors, 0 warnings\n" +
            "",

            lintProject(
                "apicheck/classpath=>.classpath",
                "apicheck/minsdk4.xml=>AndroidManifest.xml",
                "apicheck/ApiCallTest.java.txt=>src/foo/bar/ApiCallTest.java",
                "apicheck/ApiCallTest.class.data=>bin/classes/foo/bar/ApiCallTest.class"
                ));
    }

    public void testApi10() throws Exception {
        assertEquals(
            "src/foo/bar/ApiCallTest.java:20: Error: Call requires API level 11 (current min is 10): android.app.Activity#getActionBar [NewApi]\n" +
            "  getActionBar(); // API 11\n" +
            "  ~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:30: Error: Call requires API level 11 (current min is 10): android.widget.Chronometer#setTextIsSelectable [NewApi]\n" +
            "  chronometer.setTextIsSelectable(true); // API 11\n" +
            "              ~~~~~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:33: Error: Field requires API level 11 (current min is 10): dalvik.bytecode.OpcodeInfo#MAXIMUM_VALUE [NewApi]\n" +
            "  int field = OpcodeInfo.MAXIMUM_VALUE; // API 11\n" +
            "                         ~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:38: Error: Class requires API level 14 (current min is 10): android.app.ApplicationErrorReport.BatteryInfo [NewApi]\n" +
            "  BatteryInfo batteryInfo = getReport().batteryInfo;\n" +
            "  ^\n" +
            "src/foo/bar/ApiCallTest.java:38: Error: Field requires API level 14 (current min is 10): android.app.ApplicationErrorReport#batteryInfo [NewApi]\n" +
            "  BatteryInfo batteryInfo = getReport().batteryInfo;\n" +
            "              ~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:41: Error: Field requires API level 11 (current min is 10): android.graphics.PorterDuff.Mode#OVERLAY [NewApi]\n" +
            "  Mode mode = PorterDuff.Mode.OVERLAY; // API 11\n" +
            "                              ~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:45: Error: Class requires API level 14 (current min is 10): android.widget.GridLayout [NewApi]\n" +
            " GridLayout getGridLayout() { // API 14\n" +
            "            ~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest.java:49: Error: Class requires API level 14 (current min is 10): android.app.ApplicationErrorReport [NewApi]\n" +
            " private ApplicationErrorReport getReport() {\n" +
            "                                ~~~~~~~~~\n" +
            "8 errors, 0 warnings\n" +
            "",

            lintProject(
                "apicheck/classpath=>.classpath",
                "apicheck/minsdk10.xml=>AndroidManifest.xml",
                "apicheck/ApiCallTest.java.txt=>src/foo/bar/ApiCallTest.java",
                "apicheck/ApiCallTest.class.data=>bin/classes/foo/bar/ApiCallTest.class"
                ));
        }

    public void testApi14() throws Exception {
        assertEquals(
            "No warnings.",

            lintProject(
                "apicheck/classpath=>.classpath",
                "apicheck/minsdk14.xml=>AndroidManifest.xml",
                "apicheck/ApiCallTest.java.txt=>src/foo/bar/ApiCallTest.java",
                "apicheck/ApiCallTest.class.data=>bin/classes/foo/bar/ApiCallTest.class"
                ));
    }

    public void testInheritStatic() throws Exception {
        assertEquals(
            "src/foo/bar/ApiCallTest5.java:16: Error: Call requires API level 11 (current min is 2): android.view.View#resolveSizeAndState [NewApi]\n" +
            "        int measuredWidth = View.resolveSizeAndState(widthMeasureSpec,\n" +
            "                                 ~~~~~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest5.java:18: Error: Call requires API level 11 (current min is 2): android.view.View#resolveSizeAndState [NewApi]\n" +
            "        int measuredHeight = resolveSizeAndState(heightMeasureSpec,\n" +
            "                             ~~~~~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest5.java:20: Error: Call requires API level 11 (current min is 2): android.view.View#combineMeasuredStates [NewApi]\n" +
            "        View.combineMeasuredStates(0, 0);\n" +
            "             ~~~~~~~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiCallTest5.java:21: Error: Call requires API level 11 (current min is 2): android.view.View#combineMeasuredStates [NewApi]\n" +
            "        ApiCallTest5.combineMeasuredStates(0, 0);\n" +
            "                     ~~~~~~~~~~~~~~~~~~~~~\n" +
            "4 errors, 0 warnings\n" +
            "",

            lintProject(
                "apicheck/classpath=>.classpath",
                "apicheck/minsdk2.xml=>AndroidManifest.xml",
                "apicheck/ApiCallTest5.java.txt=>src/foo/bar/ApiCallTest5.java",
                "apicheck/ApiCallTest5.class.data=>bin/classes/foo/bar/ApiCallTest5.class"
                ));
    }

    public void testInheritLocal() throws Exception {
        // Test virtual dispatch in a local class which extends some other local class (which
        // in turn extends an Android API)
        assertEquals(
            "src/test/pkg/ApiCallTest3.java:10: Error: Call requires API level 11 (current min is 1): android.app.Activity#getActionBar [NewApi]\n" +
            "  getActionBar(); // API 11\n" +
            "  ~~~~~~~~~~~~\n" +
            "1 errors, 0 warnings\n" +
            "",

            lintProject(
                "apicheck/classpath=>.classpath",
                "apicheck/minsdk1.xml=>AndroidManifest.xml",
                "apicheck/Intermediate.java.txt=>src/test/pkg/Intermediate.java",
                "apicheck/ApiCallTest3.java.txt=>src/test/pkg/ApiCallTest3.java",
                "apicheck/ApiCallTest3.class.data=>bin/classes/test/pkg/ApiCallTest3.class",
                "apicheck/Intermediate.class.data=>bin/classes/test/pkg/Intermediate.class"
                ));
    }

    public void testViewClassLayoutReference() throws Exception {
        assertEquals(
            "res/layout/view.xml:9: Error: View requires API level 5 (current min is 1): <QuickContactBadge> [NewApi]\n" +
            "    <view\n" +
            "    ^\n" +
            "res/layout/view.xml:16: Error: View requires API level 11 (current min is 1): <CalendarView> [NewApi]\n" +
            "    <view\n" +
            "    ^\n" +
            "res/layout/view.xml:24: Error: ?android:attr/dividerHorizontal requires API level 11 (current min is 1) [NewApi]\n" +
            "        unknown=\"?android:attr/dividerHorizontal\"\n" +
            "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "res/layout/view.xml:25: Error: ?android:attr/textColorLinkInverse requires API level 11 (current min is 1) [NewApi]\n" +
            "        android:textColor=\"?android:attr/textColorLinkInverse\" />\n" +
            "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "4 errors, 0 warnings\n" +
            "",

            lintProject(
                    "apicheck/minsdk1.xml=>AndroidManifest.xml",
                    "apicheck/view.xml=>res/layout/view.xml"
                ));
    }

    public void testIOException() throws Exception {
        // See http://code.google.com/p/android/issues/detail?id=35190
        assertEquals(
            "src/test/pkg/ApiCallTest6.java:8: Error: Call requires API level 9 (current min is 1): new java.io.IOException [NewApi]\n" +
            "        IOException ioException = new IOException(throwable);\n" +
            "        ~~~~~~~~~~~\n" +
            "1 errors, 0 warnings\n",

            lintProject(
                    "apicheck/classpath=>.classpath",
                    "apicheck/minsdk1.xml=>AndroidManifest.xml",
                    "apicheck/Intermediate.java.txt=>src/test/pkg/Intermediate.java",
                    "apicheck/ApiCallTest6.java.txt=>src/test/pkg/ApiCallTest6.java",
                    "apicheck/ApiCallTest6.class.data=>bin/classes/test/pkg/ApiCallTest6.class"
                ));
    }


    // Test suppressing errors -- on classes, methods etc.

    public void testSuppress() throws Exception {
        assertEquals(
            // These errors are correctly -not- suppressed because they
            // appear in method3 (line 74-98) which is annotated with a
            // @SuppressLint annotation specifying only an unrelated issue id
            "src/foo/bar/SuppressTest1.java:74: Error: Class requires API level 8 (current min is 1): org.w3c.dom.DOMLocator [NewApi]\n" +
            " public void method3(Chronometer chronometer, DOMLocator locator) {\n" +
            "                                              ~~~~~~~~~~\n" +
            "src/foo/bar/SuppressTest1.java:76: Error: Call requires API level 11 (current min is 1): android.app.Activity#getActionBar [NewApi]\n" +
            "  getActionBar(); // API 11\n" +
            "  ~~~~~~~~~~~~\n" +
            "src/foo/bar/SuppressTest1.java:79: Error: Class requires API level 8 (current min is 1): org.w3c.dom.DOMError [NewApi]\n" +
            "  DOMError error = null; // API 8\n" +
            "  ~~~~~~~~\n" +
            "src/foo/bar/SuppressTest1.java:80: Error: Class requires API level 8 (current min is 1): org.w3c.dom.DOMErrorHandler [NewApi]\n" +
            "  Class<?> clz = DOMErrorHandler.class; // API 8\n" +
            "                 ~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/SuppressTest1.java:83: Error: Call requires API level 3 (current min is 1): android.widget.Chronometer#getOnChronometerTickListener [NewApi]\n" +
            "  chronometer.getOnChronometerTickListener(); // API 3\n" +
            "              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/SuppressTest1.java:86: Error: Call requires API level 11 (current min is 1): android.widget.Chronometer#setTextIsSelectable [NewApi]\n" +
            "  chronometer.setTextIsSelectable(true); // API 11\n" +
            "              ~~~~~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/SuppressTest1.java:89: Error: Field requires API level 11 (current min is 1): dalvik.bytecode.OpcodeInfo#MAXIMUM_VALUE [NewApi]\n" +
            "  int field = OpcodeInfo.MAXIMUM_VALUE; // API 11\n" +
            "                         ~~~~~~~~~~~~~\n" +
            "src/foo/bar/SuppressTest1.java:94: Error: Class requires API level 14 (current min is 1): android.app.ApplicationErrorReport.BatteryInfo [NewApi]\n" +
            "  BatteryInfo batteryInfo = getReport().batteryInfo;\n" +
            "  ^\n" +
            "src/foo/bar/SuppressTest1.java:94: Error: Field requires API level 14 (current min is 1): android.app.ApplicationErrorReport#batteryInfo [NewApi]\n" +
            "  BatteryInfo batteryInfo = getReport().batteryInfo;\n" +
            "              ~~~~~~~~~~~\n" +
            "src/foo/bar/SuppressTest1.java:97: Error: Field requires API level 11 (current min is 1): android.graphics.PorterDuff.Mode#OVERLAY [NewApi]\n" +
            "  Mode mode = PorterDuff.Mode.OVERLAY; // API 11\n" +
            "                              ~~~~~~~\n" +

            // Note: These annotations are within the methods, not ON the methods, so they have
            // no effect (because they don't end up in the bytecode)

            "src/foo/bar/SuppressTest4.java:16: Error: Class requires API level 14 (current min is 1): android.app.ApplicationErrorReport [NewApi]\n" +
            "  ApplicationErrorReport report = null;\n" +
            "  ~~~~~~~~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/SuppressTest4.java:19: Error: Class requires API level 14 (current min is 1): android.app.ApplicationErrorReport.BatteryInfo [NewApi]\n" +
            "  BatteryInfo batteryInfo = report.batteryInfo;\n" +
            "  ^\n" +
            "src/foo/bar/SuppressTest4.java:19: Error: Field requires API level 14 (current min is 1): android.app.ApplicationErrorReport#batteryInfo [NewApi]\n" +
            "  BatteryInfo batteryInfo = report.batteryInfo;\n" +
            "              ~~~~~~~~~~~\n" +
            "13 errors, 0 warnings\n",

            lintProject(
                "apicheck/classpath=>.classpath",
                "apicheck/minsdk1.xml=>AndroidManifest.xml",
                "apicheck/SuppressTest1.java.txt=>src/foo/bar/SuppressTest1.java",
                "apicheck/SuppressTest1.class.data=>bin/classes/foo/bar/SuppressTest1.class",
                "apicheck/SuppressTest2.java.txt=>src/foo/bar/SuppressTest2.java",
                "apicheck/SuppressTest2.class.data=>bin/classes/foo/bar/SuppressTest2.class",
                "apicheck/SuppressTest3.java.txt=>src/foo/bar/SuppressTest3.java",
                "apicheck/SuppressTest3.class.data=>bin/classes/foo/bar/SuppressTest3.class",
                "apicheck/SuppressTest4.java.txt=>src/foo/bar/SuppressTest4.java",
                "apicheck/SuppressTest4.class.data=>bin/classes/foo/bar/SuppressTest4.class"
                ));
    }

    public void testSuppressInnerClasses() throws Exception {
        assertEquals(
            // These errors are correctly -not- suppressed because they
            // appear outside the middle inner class suppressing its own errors
            // and its child's errors
            "src/test/pkg/ApiCallTest4.java:9: Error: Call requires API level 14 (current min is 1): new android.widget.GridLayout [NewApi]\n" +
            "        new GridLayout(null, null, 0);\n" +
            "            ~~~~~~~~~~\n" +
            "src/test/pkg/ApiCallTest4.java:38: Error: Call requires API level 14 (current min is 1): new android.widget.GridLayout [NewApi]\n" +
            "            new GridLayout(null, null, 0);\n" +
            "                ~~~~~~~~~~\n" +
            "2 errors, 0 warnings\n",

            lintProject(
                "apicheck/classpath=>.classpath",
                "apicheck/minsdk1.xml=>AndroidManifest.xml",
                "apicheck/ApiCallTest4.java.txt=>src/test/pkg/ApiCallTest4.java",
                "apicheck/ApiCallTest4.class.data=>bin/classes/test/pkg/ApiCallTest4.class",
                "apicheck/ApiCallTest4$1.class.data=>bin/classes/test/pkg/ApiCallTest4$1.class",
                "apicheck/ApiCallTest4$InnerClass1.class.data=>bin/classes/test/pkg/ApiCallTest4$InnerClass1.class",
                "apicheck/ApiCallTest4$InnerClass2.class.data=>bin/classes/test/pkg/ApiCallTest4$InnerClass2.class",
                "apicheck/ApiCallTest4$InnerClass1$InnerInnerClass1.class.data=>bin/classes/test/pkg/ApiCallTest4$InnerClass1$InnerInnerClass1.class"
                ));
    }

    public void testApiTargetAnnotation() throws Exception {
        assertEquals(
            "src/foo/bar/ApiTargetTest.java:13: Error: Class requires API level 8 (current min is 1): org.w3c.dom.DOMErrorHandler [NewApi]\n" +
            "  Class<?> clz = DOMErrorHandler.class; // API 8\n" +
            "                 ~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiTargetTest.java:25: Error: Class requires API level 8 (current min is 4): org.w3c.dom.DOMErrorHandler [NewApi]\n" +
            "  Class<?> clz = DOMErrorHandler.class; // API 8\n" +
            "                 ~~~~~~~~~~~~~~~\n" +
            "src/foo/bar/ApiTargetTest.java:39: Error: Class requires API level 8 (current min is 7): org.w3c.dom.DOMErrorHandler [NewApi]\n" +
            "   Class<?> clz = DOMErrorHandler.class; // API 8\n" +
            "                  ~~~~~~~~~~~~~~~\n" +
            "3 errors, 0 warnings\n" +
            "",

            lintProject(
                "apicheck/classpath=>.classpath",
                "apicheck/minsdk1.xml=>AndroidManifest.xml",
                "apicheck/ApiTargetTest.java.txt=>src/foo/bar/ApiTargetTest.java",
                "apicheck/ApiTargetTest.class.data=>bin/classes/foo/bar/ApiTargetTest.class",
                "apicheck/ApiTargetTest$LocalClass.class.data=>bin/classes/foo/bar/ApiTargetTest$LocalClass.class"
                ));
    }

    public void testTargetAnnotationInner() throws Exception {
        assertEquals(
            "src/test/pkg/ApiTargetTest2.java:32: Error: Call requires API level 14 (current min is 3): new android.widget.GridLayout [NewApi]\n" +
            "                        new GridLayout(null, null, 0);\n" +
            "                            ~~~~~~~~~~\n" +
            "1 errors, 0 warnings\n",

            lintProject(
                "apicheck/classpath=>.classpath",
                "apicheck/minsdk1.xml=>AndroidManifest.xml",
                "apicheck/ApiTargetTest2.java.txt=>src/test/pkg/ApiTargetTest2.java",
                "apicheck/ApiTargetTest2.class.data=>bin/classes/test/pkg/ApiTargetTest2.class",
                "apicheck/ApiTargetTest2$1.class.data=>bin/classes/test/pkg/ApiTargetTest2$1.class",
                "apicheck/ApiTargetTest2$1$2.class.data=>bin/classes/test/pkg/ApiTargetTest2$1$2.class",
                "apicheck/ApiTargetTest2$1$1.class.data=>bin/classes/test/pkg/ApiTargetTest2$1$1.class"
                ));
    }

    public void testSkipAndroidSupportInAospHalf() throws Exception {
        String expected;
        if (System.getenv("ANDROID_BUILD_TOP") != null) {
            expected = "No warnings.";
        } else {
            expected = "bin/classes/android/support/foo/Foo.class: Error: Class requires API level 8 (current min is 1): org.w3c.dom.DOMError [NewApi]\n" +
                    "1 errors, 0 warnings\n";
        }

        assertEquals(
            expected,

            lintProject(
                "apicheck/classpath=>.classpath",
                "apicheck/minsdk1.xml=>AndroidManifest.xml",
                "apicheck/ApiCallTest2.java.txt=>src/src/android/support/foo/Foo.java",
                "apicheck/ApiCallTest2.class.data=>bin/classes/android/support/foo/Foo.class"
                ));
    }

    public void testSuper() throws Exception {
        // See http://code.google.com/p/android/issues/detail?id=36384
        assertEquals(
            "src/test/pkg/ApiCallTest7.java:8: Error: Call requires API level 9 (current min is 4): new java.io.IOException [NewApi]\n" +
            "        super(message, cause); // API 9\n" +
            "        ~~~~~\n" +
            "src/test/pkg/ApiCallTest7.java:12: Error: Call requires API level 9 (current min is 4): new java.io.IOException [NewApi]\n" +
            "        super.toString(); throw new IOException((Throwable) null); // API 9\n" +
            "                                    ~~~~~~~~~~~\n" +
            "2 errors, 0 warnings\n",

            lintProject(
                    "apicheck/classpath=>.classpath",
                    "apicheck/minsdk4.xml=>AndroidManifest.xml",
                    "apicheck/ApiCallTest7.java.txt=>src/test/pkg/ApiCallTest7.java",
                    "apicheck/ApiCallTest7.class.data=>bin/classes/test/pkg/ApiCallTest7.class"
                ));
    }

    public void testEnums() throws Exception {
        // See http://code.google.com/p/android/issues/detail?id=36951
        assertEquals(
            "src/test/pkg/TestEnum.java:26: Error: Enum value requires API level 11 (current min is 4): android.graphics.PorterDuff.Mode#OVERLAY [NewApi]\n" +
            "            case OVERLAY: {\n" +
            "                 ~~~~~~~\n" +
            "src/test/pkg/TestEnum.java:37: Error: Enum value requires API level 11 (current min is 4): android.graphics.PorterDuff.Mode#OVERLAY [NewApi]\n" +
            "            case OVERLAY: {\n" +
            "                 ~~~~~~~\n" +
            "src/test/pkg/TestEnum.java:61: Error: Class requires API level 11 (current min is 4): android.renderscript.Element.DataType [NewApi]\n" +
            "        switch (type) {\n" +
            "        ^\n" +
            "src/test/pkg/TestEnum.java:61: Error: Enum for switch requires API level 11 (current min is 4): android.renderscript.Element.DataType [NewApi]\n" +
            "        switch (type) {\n" +
            "        ^\n" +
            "4 errors, 0 warnings\n",

            lintProject(
                    "apicheck/classpath=>.classpath",
                    "apicheck/minsdk4.xml=>AndroidManifest.xml",
                    "apicheck/TestEnum.java.txt=>src/test/pkg/TestEnum.java",
                    "apicheck/TestEnum.class.data=>bin/classes/test/pkg/TestEnum.class"
                ));
    }
}
