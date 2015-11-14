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
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Project;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("javadoc")
public class ManifestOrderDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new ManifestOrderDetector();
    }

    private Set<Issue> mEnabled = new HashSet<Issue>();

    @Override
    protected TestConfiguration getConfiguration(Project project) {
        return new TestConfiguration() {
            @Override
            public boolean isEnabled(Issue issue) {
                return super.isEnabled(issue) && mEnabled.contains(issue);
            }
        };
    }

    public void testOrderOk() throws Exception {
        mEnabled = Collections.singleton(ManifestOrderDetector.ORDER);
        assertEquals(
                "No warnings.",
                lintProject(
                        "AndroidManifest.xml",
                        "res/values/strings.xml"));
    }

    public void testBrokenOrder() throws Exception {
        mEnabled = Collections.singleton(ManifestOrderDetector.ORDER);
        assertEquals(
            "AndroidManifest.xml:16: Warning: <uses-sdk> tag appears after <application> tag [ManifestOrder]\n" +
            "   <uses-sdk android:minSdkVersion=\"Froyo\" />\n" +
            "   ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 1 warnings\n" +
            "",

            lintProject(
                    "broken-manifest.xml=>AndroidManifest.xml",
                    "res/values/strings.xml"));
    }

    public void testMissingUsesSdk() throws Exception {
        mEnabled = Collections.singleton(ManifestOrderDetector.USES_SDK);
        assertEquals(
            "AndroidManifest.xml: Warning: Manifest should specify a minimum API level with <uses-sdk android:minSdkVersion=\"?\" />; if it really supports all versions of Android set it to 1. [UsesMinSdkAttributes]\n" +
            "0 errors, 1 warnings\n",
            lintProject(
                    "missingusessdk.xml=>AndroidManifest.xml",
                    "res/values/strings.xml"));
    }

    public void testMissingMinSdk() throws Exception {
        mEnabled = Collections.singleton(ManifestOrderDetector.USES_SDK);
        assertEquals(
            "AndroidManifest.xml:7: Warning: <uses-sdk> tag should specify a minimum API level with android:minSdkVersion=\"?\" [UsesMinSdkAttributes]\n" +
            "    <uses-sdk android:targetSdkVersion=\"10\" />\n" +
            "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 1 warnings\n" +
            "",
            lintProject(
                    "missingmin.xml=>AndroidManifest.xml",
                    "res/values/strings.xml"));
    }

    public void testMissingTargetSdk() throws Exception {
        mEnabled = Collections.singleton(ManifestOrderDetector.USES_SDK);
        assertEquals(
            "AndroidManifest.xml:7: Warning: <uses-sdk> tag should specify a target API level (the highest verified version; when running on later versions, compatibility behaviors may be enabled) with android:targetSdkVersion=\"?\" [UsesMinSdkAttributes]\n" +
            "    <uses-sdk android:minSdkVersion=\"10\" />\n" +
            "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 1 warnings\n",
            lintProject(
                    "missingtarget.xml=>AndroidManifest.xml",
                    "res/values/strings.xml"));
    }

    public void testOldTargetSdk() throws Exception {
        mEnabled = Collections.singleton(ManifestOrderDetector.TARGET_NEWER);
        assertEquals(
            "AndroidManifest.xml:7: Warning: Not targeting the latest versions of Android; compatibility modes apply. Consider testing and updating this version. Consult the android.os.Build.VERSION_CODES javadoc for details. [OldTargetApi]\n" +
            "    <uses-sdk android:minSdkVersion=\"10\" android:targetSdkVersion=\"14\" />\n" +
            "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "0 errors, 1 warnings\n",
            lintProject(
                    "oldtarget.xml=>AndroidManifest.xml",
                    "res/values/strings.xml"));
    }

    public void testMultipleSdk() throws Exception {
        mEnabled = Collections.singleton(ManifestOrderDetector.MULTIPLE_USES_SDK);
        assertEquals(
            "AndroidManifest.xml:8: Error: There should only be a single <uses-sdk> element in the manifest: merge these together [MultipleUsesSdk]\n" +
            "    <uses-sdk android:targetSdkVersion=\"14\" />\n" +
            "    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "    AndroidManifest.xml:7: Also appears here\n" +
            "    AndroidManifest.xml:9: Also appears here\n" +
            "1 errors, 0 warnings\n",

            lintProject(
                    "multiplesdk.xml=>AndroidManifest.xml",
                    "res/values/strings.xml"));
    }

    public void testWrongLocation() throws Exception {
        mEnabled = Collections.singleton(ManifestOrderDetector.WRONG_PARENT);
        assertEquals(
            "AndroidManifest.xml:8: Error: The <uses-sdk> element must be a direct child of the <manifest> root element [WrongManifestParent]\n" +
            "       <uses-sdk android:minSdkVersion=\"Froyo\" />\n" +
            "       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:9: Error: The <uses-permission> element must be a direct child of the <manifest> root element [WrongManifestParent]\n" +
            "       <uses-permission />\n" +
            "       ~~~~~~~~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:10: Error: The <permission> element must be a direct child of the <manifest> root element [WrongManifestParent]\n" +
            "       <permission />\n" +
            "       ~~~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:11: Error: The <permission-tree> element must be a direct child of the <manifest> root element [WrongManifestParent]\n" +
            "       <permission-tree />\n" +
            "       ~~~~~~~~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:12: Error: The <permission-group> element must be a direct child of the <manifest> root element [WrongManifestParent]\n" +
            "       <permission-group />\n" +
            "       ~~~~~~~~~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:14: Error: The <uses-sdk> element must be a direct child of the <manifest> root element [WrongManifestParent]\n" +
            "       <uses-sdk />\n" +
            "       ~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:15: Error: The <uses-configuration> element must be a direct child of the <manifest> root element [WrongManifestParent]\n" +
            "       <uses-configuration />\n" +
            "       ~~~~~~~~~~~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:16: Error: The <uses-feature> element must be a direct child of the <manifest> root element [WrongManifestParent]\n" +
            "       <uses-feature />\n" +
            "       ~~~~~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:17: Error: The <supports-screens> element must be a direct child of the <manifest> root element [WrongManifestParent]\n" +
            "       <supports-screens />\n" +
            "       ~~~~~~~~~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:18: Error: The <compatible-screens> element must be a direct child of the <manifest> root element [WrongManifestParent]\n" +
            "       <compatible-screens />\n" +
            "       ~~~~~~~~~~~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:19: Error: The <supports-gl-texture> element must be a direct child of the <manifest> root element [WrongManifestParent]\n" +
            "       <supports-gl-texture />\n" +
            "       ~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:24: Error: The <uses-library> element must be a direct child of the <application> element [WrongManifestParent]\n" +
            "   <uses-library />\n" +
            "   ~~~~~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:25: Error: The <activity> element must be a direct child of the <application> element [WrongManifestParent]\n" +
            "   <activity android:name=\".HelloWorld\"\n" +
            "   ^\n" +
            "13 errors, 0 warnings\n" +
            "",

            lintProject("broken-manifest2.xml=>AndroidManifest.xml"));
    }

    public void testDuplicateActivity() throws Exception {
        mEnabled = Collections.singleton(ManifestOrderDetector.DUPLICATE_ACTIVITY);
        assertEquals(
            "AndroidManifest.xml:16: Error: Duplicate registration for activity com.example.helloworld.HelloWorld [DuplicateActivity]\n" +
            "       <activity android:name=\"com.example.helloworld.HelloWorld\"\n" +
            "                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "1 errors, 0 warnings\n" +
            "",

            lintProject(
                    "duplicate-manifest.xml=>AndroidManifest.xml",
                    "res/values/strings.xml"));
    }

    public void testIgnoreDuplicateActivity() throws Exception {
        mEnabled = Collections.singleton(ManifestOrderDetector.DUPLICATE_ACTIVITY);
        assertEquals(
            "No warnings.",

            lintProject(
                    "duplicate-manifest-ignore.xml=>AndroidManifest.xml",
                    "res/values/strings.xml"));
    }

    public void testAllowBackup() throws Exception {
        mEnabled = Collections.singleton(ManifestOrderDetector.ALLOW_BACKUP);
        assertEquals(
                "AndroidManifest.xml:9: Warning: Should explicitly set android:allowBackup to " +
                "true or false (it's true by default, and that can have some security " +
                "implications for the application's data) [AllowBackup]\n" +
                "    <application\n" +
                "    ^\n" +
                "0 errors, 1 warnings\n",
                lintProject(
                        "AndroidManifest.xml",
                        "res/values/strings.xml"));
    }

    public void testAllowBackupOk() throws Exception {
        mEnabled = Collections.singleton(ManifestOrderDetector.ALLOW_BACKUP);
        assertEquals(
                "No warnings.",
                lintProject(
                        "allowbackup.xml=>AndroidManifest.xml",
                        "res/values/strings.xml"));
    }

    public void testAllowIgnore() throws Exception {
        mEnabled = Collections.singleton(ManifestOrderDetector.ALLOW_BACKUP);
        assertEquals(
                "No warnings.",
                lintProject(
                        "allowbackup_ignore.xml=>AndroidManifest.xml",
                        "res/values/strings.xml"));
    }

}
