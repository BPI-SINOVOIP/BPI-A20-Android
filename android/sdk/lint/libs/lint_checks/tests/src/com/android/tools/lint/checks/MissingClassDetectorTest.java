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
import com.android.tools.lint.detector.api.Scope;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

@SuppressWarnings("javadoc")
public class MissingClassDetectorTest extends AbstractCheckTest {
    private EnumSet<Scope> mScopes;

    @Override
    protected Detector getDetector() {
        return new MissingClassDetector();
    }

    @Override
    protected EnumSet<Scope> getLintScope(List<File> file) {
        return mScopes;
    }

    public void test() throws Exception {
        mScopes = null;
        assertEquals(
            "AndroidManifest.xml:13: Error: Class referenced in the manifest, test.pkg.TestProvider, was not found in the project or the libraries [MissingRegistered]\n" +
            "        <activity android:name=\".TestProvider\" />\n" +
            "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:14: Error: Class referenced in the manifest, test.pkg.TestProvider2, was not found in the project or the libraries [MissingRegistered]\n" +
            "        <service android:name=\"test.pkg.TestProvider2\" />\n" +
            "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:15: Error: Class referenced in the manifest, test.pkg.TestService, was not found in the project or the libraries [MissingRegistered]\n" +
            "        <provider android:name=\".TestService\" />\n" +
            "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:16: Error: Class referenced in the manifest, test.pkg.OnClickActivity, was not found in the project or the libraries [MissingRegistered]\n" +
            "        <receiver android:name=\"OnClickActivity\" />\n" +
            "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "AndroidManifest.xml:17: Error: Class referenced in the manifest, test.pkg.TestReceiver, was not found in the project or the libraries [MissingRegistered]\n" +
            "        <service android:name=\"TestReceiver\" />\n" +
            "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "5 errors, 0 warnings\n",

            lintProject(
                "bytecode/AndroidManifestWrongRegs.xml=>AndroidManifest.xml",
                "bytecode/.classpath=>.classpath"
            ));
    }

    public void testIncrementalInManifest() throws Exception {
        mScopes = Scope.MANIFEST_SCOPE;
        assertEquals(
            "No warnings.",

            lintProject(
                "bytecode/AndroidManifestWrongRegs.xml=>AndroidManifest.xml",
                "bytecode/.classpath=>.classpath"
            ));
    }

    public void testOkClasses() throws Exception {
        mScopes = null;
        assertEquals(
            "No warnings.",

            lintProject(
                "bytecode/AndroidManifestWrongRegs.xml=>AndroidManifest.xml",
                "bytecode/.classpath=>.classpath",
                "bytecode/OnClickActivity.java.txt=>src/test/pkg/OnClickActivity.java",
                "bytecode/OnClickActivity.class.data=>bin/classes/test/pkg/OnClickActivity.class",
                "bytecode/TestService.java.txt=>src/test/pkg/TestService.java",
                "bytecode/TestService.class.data=>bin/classes/test/pkg/TestService.class",
                "bytecode/TestProvider.java.txt=>src/test/pkg/TestProvider.java",
                "bytecode/TestProvider.class.data=>bin/classes/test/pkg/TestProvider.class",
                "bytecode/TestProvider2.java.txt=>src/test/pkg/TestProvider2.java",
                "bytecode/TestProvider2.class.data=>bin/classes/test/pkg/TestProvider2.class",
                "bytecode/TestReceiver.java.txt=>src/test/pkg/TestReceiver.java",
                "bytecode/TestReceiver.class.data=>bin/classes/test/pkg/TestReceiver.class"
            ));
    }

    public void testOkLibraries() throws Exception {
        mScopes = null;
        assertEquals(
            "No warnings.",

            lintProject(
                "bytecode/AndroidManifestWrongRegs.xml=>AndroidManifest.xml",
                "bytecode/.classpath=>.classpath",
                "bytecode/classes.jar=>libs/classes.jar"
            ));
    }

    public void testLibraryProjects() throws Exception {
        mScopes = null;
        File master = getProjectDir("MasterProject",
                // Master project
                "bytecode/AndroidManifestWrongRegs.xml=>AndroidManifest.xml",
                "multiproject/main.properties=>project.properties",
                "bytecode/.classpath=>.classpath"
        );
        File library = getProjectDir("LibraryProject",
                // Library project
                "multiproject/library-manifest.xml=>AndroidManifest.xml",
                "multiproject/library.properties=>project.properties",
                "bytecode/OnClickActivity.java.txt=>src/test/pkg/OnClickActivity.java",
                "bytecode/OnClickActivity.class.data=>bin/classes/test/pkg/OnClickActivity.class",
                "bytecode/TestService.java.txt=>src/test/pkg/TestService.java",
                "bytecode/TestService.class.data=>bin/classes/test/pkg/TestService.class",
                "bytecode/TestProvider.java.txt=>src/test/pkg/TestProvider.java",
                "bytecode/TestProvider.class.data=>bin/classes/test/pkg/TestProvider.class",
                "bytecode/TestProvider2.java.txt=>src/test/pkg/TestProvider2.java",
                "bytecode/TestProvider2.class.data=>bin/classes/test/pkg/TestProvider2.class"
                // Missing TestReceiver: Test should complain about just that class
        );
        assertEquals(
           "MasterProject/AndroidManifest.xml:17: Error: Class referenced in the manifest, test.pkg.TestReceiver, was not found in the project or the libraries [MissingRegistered]\n" +
           "        <service android:name=\"TestReceiver\" />\n" +
           "        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
           "1 errors, 0 warnings\n",

           checkLint(Arrays.asList(master, library)));
    }

    public void testInnerClassStatic() throws Exception {
        mScopes = null;
        assertEquals(
            "src/test/pkg/Foo.java:8: Warning: This inner class should be static (test.pkg.Foo.Baz) [Instantiatable]\n" +
            "    public class Baz extends Activity {\n" +
            "    ^\n" +
            "0 errors, 1 warnings\n",

            lintProject(
                "registration/AndroidManifest.xml=>AndroidManifest.xml",
                "bytecode/.classpath=>.classpath",
                "registration/Foo.java.txt=>src/test/pkg/Foo.java",
                "registration/Foo.class.data=>bin/classes/test/pkg/Foo.class",
                "registration/Foo$Bar.class.data=>bin/classes/test/pkg/Foo$Bar.class",
                "registration/Foo$Baz.class.data=>bin/classes/test/pkg/Foo$Baz.class"
            ));
    }

    public void testInnerClassPublic() throws Exception {
        mScopes = null;
        assertEquals(
            "src/test/pkg/Foo/Bar.java:6: Warning: The default constructor must be public [Instantiatable]\n" +
            "    private Bar() {\n" +
            "    ^\n" +
            "0 errors, 1 warnings\n",

            lintProject(
                "registration/AndroidManifestInner.xml=>AndroidManifest.xml",
                "bytecode/.classpath=>.classpath",
                "registration/Bar.java.txt=>src/test/pkg/Foo/Bar.java",
                "registration/Bar.class.data=>bin/classes/test/pkg/Foo/Bar.class"
            ));
    }

    public void testInnerClass() throws Exception {
        mScopes = null;
        assertEquals(
            "AndroidManifest.xml:14: Error: Class referenced in the manifest, test.pkg.Foo.Bar, was not found in the project or the libraries [MissingRegistered]\n" +
            "        <activity\n" +
            "        ^\n" +
            "AndroidManifest.xml:23: Error: Class referenced in the manifest, test.pkg.Foo.Baz, was not found in the project or the libraries [MissingRegistered]\n" +
            "        <activity\n" +
            "        ^\n" +
            "2 errors, 0 warnings\n",

            lintProject(
                "registration/AndroidManifest.xml=>AndroidManifest.xml",
                "bytecode/.classpath=>.classpath",
                "registration/Foo.java.txt=>src/test/pkg/Foo.java"
            ));
    }

    public void testInnerClass2() throws Exception {
        mScopes = null;
        assertEquals(
            "AndroidManifest.xml:14: Error: Class referenced in the manifest, test.pkg.Foo.Bar, was not found in the project or the libraries [MissingRegistered]\n" +
            "        <activity\n" +
            "        ^\n" +
            "1 errors, 0 warnings\n",

            lintProject(
                "registration/AndroidManifestInner.xml=>AndroidManifest.xml",
                "bytecode/.classpath=>.classpath",
                "registration/Bar.java.txt=>src/test/pkg/Foo/Bar.java"
            ));
    }

    public void testWrongSeparator1() throws Exception {
        mScopes = null;
        assertEquals(
            "AndroidManifest.xml:14: Error: Class referenced in the manifest, test.pkg.Foo.Bar, was not found in the project or the libraries [MissingRegistered]\n" +
            "        <activity\n" +
            "        ^\n" +
            "1 errors, 0 warnings\n",

            lintProject(
                "registration/AndroidManifestWrong.xml=>AndroidManifest.xml",
                "bytecode/.classpath=>.classpath",
                "registration/Bar.java.txt=>src/test/pkg/Foo/Bar.java"
            ));
    }

    public void testWrongSeparator2() throws Exception {
        mScopes = null;
        assertEquals(
            "AndroidManifest.xml:14: Error: Class referenced in the manifest, test.pkg.Foo.Bar, was not found in the project or the libraries [MissingRegistered]\n" +
            "        <activity\n" +
            "        ^\n" +
            "AndroidManifest.xml:15: Warning: Use '$' instead of '.' for inner classes (or use only lowercase letters in package names) [InnerclassSeparator]\n" +
            "            android:name=\".Foo.Bar\"\n" +
            "            ~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "1 errors, 1 warnings\n",

            lintProject(
                "registration/AndroidManifestWrong2.xml=>AndroidManifest.xml",
                "bytecode/.classpath=>.classpath",
                "registration/Bar.java.txt=>src/test/pkg/Foo/Bar.java"
            ));
    }
}
