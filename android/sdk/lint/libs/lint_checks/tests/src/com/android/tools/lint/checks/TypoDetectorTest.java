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

import java.util.Arrays;

@SuppressWarnings("javadoc")
public class TypoDetectorTest extends AbstractCheckTest {
    @Override
    protected Detector getDetector() {
        return new TypoDetector();
    }

    public void testPlainValues() throws Exception {
        assertEquals(
            "res/values/strings.xml:6: Warning: \"Andriod\" is a common misspelling; did you mean \"Android\" ? [Typos]\n" +
            "    <string name=\"s2\">Andriod activites!</string>\n" +
            "                      ^\n" +
            "res/values/strings.xml:6: Warning: \"activites\" is a common misspelling; did you mean \"activities\" ? [Typos]\n" +
            "    <string name=\"s2\">Andriod activites!</string>\n" +
            "                              ^\n" +
            "res/values/strings.xml:8: Warning: \"Cmoputer\" is a common misspelling; did you mean \"Computer\" ? [Typos]\n" +
            "    <string name=\"s3\"> (Cmoputer </string>\n" +
            "                        ^\n" +
            "res/values/strings.xml:10: Warning: \"throught\" is a common misspelling; did you mean \"thought\" or \"through\" or \"throughout\" ? [Typos]\n" +
            "    <string name=\"s4\"><b>throught</b></string>\n" +
            "                         ^\n" +
            "res/values/strings.xml:12: Warning: \"Seach\" is a common misspelling; did you mean \"Search\" ? [Typos]\n" +
            "    <string name=\"s5\">Seach</string>\n" +
            "                      ^\n" +
            "res/values/strings.xml:16: Warning: \"Tuscon\" is a common misspelling; did you mean \"Tucson\" ? [Typos]\n" +
            "    <string name=\"s7\">Tuscon tuscon</string>\n" +
            "                      ^\n" +
            "res/values/strings.xml:20: Warning: \"Ok\" is usually capitalized as \"OK\" [Typos]\n" +
            "    <string name=\"dlg_button_ok\">Ok</string>\n" +
            "                                 ^\n" +
            "0 errors, 7 warnings\n" +
            "",
            lintProject("res/values/typos.xml=>res/values/strings.xml"));
    }

    public void testEnLanguage() throws Exception {
        assertEquals(
            "res/values-en-rUS/strings-en.xml:6: Warning: \"Andriod\" is a common misspelling; did you mean \"Android\" ? [Typos]\n" +
            "    <string name=\"s2\">Andriod activites!</string>\n" +
            "                      ^\n" +
            "res/values-en-rUS/strings-en.xml:6: Warning: \"activites\" is a common misspelling; did you mean \"activities\" ? [Typos]\n" +
            "    <string name=\"s2\">Andriod activites!</string>\n" +
            "                              ^\n" +
            "res/values-en-rUS/strings-en.xml:8: Warning: \"Cmoputer\" is a common misspelling; did you mean \"Computer\" ? [Typos]\n" +
            "    <string name=\"s3\"> (Cmoputer </string>\n" +
            "                        ^\n" +
            "res/values-en-rUS/strings-en.xml:10: Warning: \"throught\" is a common misspelling; did you mean \"thought\" or \"through\" or \"throughout\" ? [Typos]\n" +
            "    <string name=\"s4\"><b>throught</b></string>\n" +
            "                         ^\n" +
            "res/values-en-rUS/strings-en.xml:12: Warning: \"Seach\" is a common misspelling; did you mean \"Search\" ? [Typos]\n" +
            "    <string name=\"s5\">Seach</string>\n" +
            "                      ^\n" +
            "res/values-en-rUS/strings-en.xml:16: Warning: \"Tuscon\" is a common misspelling; did you mean \"Tucson\" ? [Typos]\n" +
            "    <string name=\"s7\">Tuscon tuscon</string>\n" +
            "                      ^\n" +
            "res/values-en-rUS/strings-en.xml:20: Warning: \"Ok\" is usually capitalized as \"OK\" [Typos]\n" +
            "    <string name=\"dlg_button_ok\">Ok</string>\n" +
            "                                 ^\n" +
            "0 errors, 7 warnings\n" +
            "",
            lintProject("res/values/typos.xml=>res/values-en-rUS/strings-en.xml"));
    }

    public void testNorwegian() throws Exception {
        // UTF-8 handling
        assertEquals(
            "res/values-nb/typos.xml:6: Warning: \"Andriod\" is a common misspelling; did you mean \"Android\" ? [Typos]\n" +
            "    <string name=\"s2\">Mer morro med Andriod</string>\n" +
            "                                    ^\n" +
            "res/values-nb/typos.xml:6: Warning: \"morro\" is a common misspelling; did you mean \"moro\" ? [Typos]\n" +
            "    <string name=\"s2\">Mer morro med Andriod</string>\n" +
            "                          ^\n" +
            "res/values-nb/typos.xml:8: Warning: \"Parallel\" is a common misspelling; did you mean \"Parallell\" ? [Typos]\n" +
            "    <string name=\"s3\"> Parallel </string>\n" +
            "                       ^\n" +
            "res/values-nb/typos.xml:10: Warning: \"altid\" is a common misspelling; did you mean \"alltid\" ? [Typos]\n" +
            "    <string name=\"s4\"><b>altid</b></string>\n" +
            "                         ^\n" +
            "res/values-nb/typos.xml:12: Warning: \"Altid\" is a common misspelling; did you mean \"Alltid\" ? [Typos]\n" +
            "    <string name=\"s5\">Altid</string>\n" +
            "                      ^\n" +
            "res/values-nb/typos.xml:18: Warning: \"karri�re\" is a common misspelling; did you mean \"karri�re\" ? [Typos]\n" +
            "    <string name=\"s7\">Koding er en spennende karri�re</string>\n" +
            "                                             ^\n" +
            "0 errors, 6 warnings\n" +
            "",
            lintProject("res/values-nb/typos.xml"));
    }

    public void testGerman() throws Exception {
        // Test globbing and multiple word matching
        assertEquals(
            "res/values-de/typos.xml:6: Warning: \"befindet eine\" is a common misspelling; did you mean \"befindet sich eine\" ? [Typos]\n" +
            "           wo befindet eine ip\n" +
            "              ^\n" +
            "res/values-de/typos.xml:9: Warning: \"Authorisierungscode\" is a common misspelling; did you mean \"Autorisierungscode\" ? [Typos]\n" +
            "    <string name=\"s2\">(Authorisierungscode!)</string>\n" +
            "                       ^\n" +
            "res/values-de/typos.xml:10: Warning: \"zur�ck gefoobaren\" is a common misspelling; did you mean \"zur�ckgefoobaren\" ? [Typos]\n" +
            "    <string name=\"s3\">   zur�ck gefoobaren!</string>\n" +
            "                         ^\n" +
            "0 errors, 3 warnings\n" +
            "",
            lintProject("res/values-de/typos.xml"));
    }

    public void testOk() throws Exception {
        assertEquals(
            "No warnings.",
            lintProject("res/values/typos.xml=>res/values-xy/strings.xml"));
    }

    public void testGetReplacements() {
        String s = "\"throught\" is a common misspelling; did you mean \"thought\" or " +
                   "\"through\" or \"throughout\" ?\n";
        assertEquals("throught", TypoDetector.getTypo(s));
        assertEquals(Arrays.asList("thought", "through", "throughout"),
                TypoDetector.getSuggestions(s));
    }
}
