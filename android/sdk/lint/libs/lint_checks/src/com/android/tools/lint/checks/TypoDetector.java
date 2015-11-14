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

import static com.android.tools.lint.checks.TypoLookup.isLetter;
import static com.android.SdkConstants.TAG_STRING;
import static com.google.common.base.Objects.equal;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Check which looks for likely typos in Strings.
 * <p>
 * TODO:
 * <ul>
 * <li> Add check of Java String literals too!
 * <li> Add support for <b>additional</b> languages. The typo detector is now
 *      multilingual and looks for typos-*locale*.txt files to use. However,
 *      we need to seed it with additional typo databases. I did some searching
 *      and came up with some alternatives. Here's the strategy I used:
 *      Used Google Translate to translate "Wikipedia Common Misspellings", and
 *      then I went to google.no, google.fr etc searching with that translation, and
 *      came up with what looks like wikipedia language local lists of typos.
 *      This is how I found the Norwegian one for example:
 *      <br>
 *         http://no.wikipedia.org/wiki/Wikipedia:Liste_over_alminnelige_stavefeil/Maskinform
 *      <br>
 *     Here are some additional possibilities not yet processed:
 *      <ul>
 *        <li> French: http://fr.wikipedia.org/wiki/Wikip%C3%A9dia:Liste_de_fautes_d'orthographe_courantes
 *            (couldn't find a machine-readable version there?)
 *         <li> Swedish:
 *              http://sv.wikipedia.org/wiki/Wikipedia:Lista_%C3%B6ver_vanliga_spr%C3%A5kfel
 *              (couldn't find a machine-readable version there?)
 *        <li> German
 *              http://de.wikipedia.org/wiki/Wikipedia:Liste_von_Tippfehlern/F%C3%BCr_Maschinen
 *       </ul>
 * <li> Consider also digesting files like
 *       http://sv.wikipedia.org/wiki/Wikipedia:AutoWikiBrowser/Typos
 *       See http://en.wikipedia.org/wiki/Wikipedia:AutoWikiBrowser/User_manual.
 * </ul>
 */
public class TypoDetector extends ResourceXmlDetector {
    private @Nullable TypoLookup mLookup;
    private @Nullable String mLastLanguage;
    private @Nullable String mLastRegion;
    private @Nullable String mLanguage;
    private @Nullable String mRegion;

    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "Typos", //$NON-NLS-1$
            "Looks for typos in messages",

            "This check looks through the string definitions, and if it finds any words " +
            "that look like likely misspellings, they are flagged.",
            Category.MESSAGES,
            7,
            Severity.WARNING,
            TypoDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** Constructs a new detector */
    public TypoDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.VALUES;
    }

    /** Look up the locale and region from the given parent folder name and store it
     * in {@link #mLanguage} and {@link #mRegion} */
    private void initLocale(@NonNull String parent) {
        mLanguage = null;
        mRegion = null;

        if (parent.equals("values")) { //$NON-NLS-1$
            return;
        }

        for (String qualifier : Splitter.on('-').split(parent)) {
            int qualifierLength = qualifier.length();
            if (qualifierLength == 2) {
                char first = qualifier.charAt(0);
                char second = qualifier.charAt(1);
                if (first >= 'a' && first <= 'z' && second >= 'a' && second <= 'z') {
                    mLanguage = qualifier;
                }
            } else if (qualifierLength == 3 && qualifier.charAt(0) == 'r') {
                char first = qualifier.charAt(1);
                char second = qualifier.charAt(2);
                if (first >= 'A' && first <= 'Z' && second >= 'A' && second <= 'Z') {
                    mRegion = new String(new char[] { first, second }); // Don't include the "r"
                }
                break;
            }
        }
    }

    @Override
    public void beforeCheckFile(@NonNull Context context) {
        initLocale(context.file.getParentFile().getName());
        if (mLanguage == null) {
            mLanguage = "en"; //$NON-NLS-1$
        }

        if (!equal(mLastLanguage, mLanguage) || !equal(mLastRegion, mRegion)) {
            mLookup = TypoLookup.get(context.getClient(), mLanguage, mRegion);
            mLastLanguage = mLanguage;
            mLastRegion = mRegion;
        }
    }

    @Override
    public @NonNull Speed getSpeed() {
        return Speed.NORMAL;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(TAG_STRING);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (mLookup == null) {
            return;
        }

        visit(context, element);
    }

    private void visit(XmlContext context, Node node) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            // TODO: Figure out how to deal with entities
            check(context, node, node.getNodeValue());
        } else {
            NodeList children = node.getChildNodes();
            for (int i = 0, n = children.getLength(); i < n; i++) {
                visit(context, children.item(i));
            }
        }
    }

    private void check(XmlContext context, Node node, String text) {
        int max = text.length();
        int index = 0;
        boolean checkedTypos = false;
        while (index < max) {
            while (index < max && !Character.isLetter(text.charAt(index))) {
                index++;
            }
            if (index == max) {
                return;
            }
            int begin = index;
            while (index < max && Character.isLetter(text.charAt(index))) {
                if (text.charAt(index) >= 0x80) {
                    // Switch to UTF-8 handling for this string
                    if (checkedTypos) {
                        // If we've already checked words we may have reported typos
                        // so create a substring from the current word and on.
                        byte[] utf8Text = text.substring(begin).getBytes(Charsets.UTF_8);
                        check(context, node, utf8Text, 0, utf8Text.length, text, begin);
                    } else {
                        // If all we've done so far is skip whitespace (common scenario)
                        // then no need to substring the text, just re-search with the
                        // UTF-8 routines
                        byte[] utf8Text = text.getBytes(Charsets.UTF_8);
                        check(context, node, utf8Text, 0, utf8Text.length, text, 0);
                    }
                    return;
                }
                index++;
            }

            int end = index;
            checkedTypos = true;
            List<String> replacements = mLookup.getTypos(text, begin, end);
            if (replacements != null) {
                reportTypo(context, node, text, begin, replacements);
            }

            index = end + 1;
        }
    }

    private void check(XmlContext context, Node node, byte[] utf8Text,
            int byteStart, int byteEnd, String text, int charStart) {
        int index = byteStart;
        while (index < byteEnd) {
            // Find beginning of word
            while (index < byteEnd) {
                byte b = utf8Text[index];
                if (isLetter(b)) {
                    break;
                }
                index++;
                if ((b & 0x80) == 0 || (b & 0xC0) == 0xC0) {
                    // First characters in UTF-8 are always ASCII (0 high bit) or 11XXXXXX
                    charStart++;
                }
            }

            if (index == byteEnd) {
                return;
            }
            int charEnd = charStart;
            int begin = index;

            // Find end of word. Unicode has the nice property that even 2nd, 3rd and 4th
            // bytes won't match these ASCII characters (because the high bit must be set there)
            while (index < byteEnd) {
                byte b = utf8Text[index];
                if (!isLetter(b)) {
                    break;
                }
                index++;
                if ((b & 0x80) == 0 || (b & 0xC0) == 0xC0) {
                    // First characters in UTF-8 are always ASCII (0 high bit) or 11XXXXXX
                    charEnd++;
                }
            }

            int end = index;
            List<String> replacements = mLookup.getTypos(utf8Text, begin, end);
            if (replacements != null) {
                reportTypo(context, node, text, charStart, replacements);
            }

            charStart = charEnd;
        }
    }

    /** Report the typo found at the given offset and suggest the given replacements */
    private void reportTypo(XmlContext context, Node node, String text, int begin,
            List<String> replacements) {
        if (replacements.size() < 2) {
            return;
        }

        String typo = replacements.get(0);
        String word = text.substring(begin, begin + typo.length());

        String first = null;
        String message;

        boolean isCapitalized = Character.isUpperCase(word.charAt(0));
        StringBuilder sb = new StringBuilder();
        for (int i = 1, n = replacements.size(); i < n; i++) {
            String replacement = replacements.get(i);
            if (first == null) {
                first = replacement;
            }
            if (sb.length() > 0) {
                sb.append(" or ");
            }
            sb.append('"');
            if (isCapitalized) {
                sb.append(Character.toUpperCase(replacement.charAt(0))
                        + replacement.substring(1));
            } else {
                sb.append(replacement);
            }
            sb.append('"');
        }

        if (first != null && first.equalsIgnoreCase(word)) {
            if (first.equals(word)) {
                return;
            }
            message = String.format(
                    "\"%1$s\" is usually capitalized as \"%2$s\"",
                    word, first);
        } else {
            message = String.format(
                    "\"%1$s\" is a common misspelling; did you mean %2$s ?",
                    word, sb.toString());
        }

        int end = begin + word.length();
        context.report(ISSUE, node, context.getLocation(node, begin, end), message, null);
    }

    /** Returns the suggested replacements, if any, for the given typo. The error
     * message <b>must</b> be one supplied by lint.
     *
     * @param errorMessage the error message
     * @return a list of replacement words suggested by the error message
     */
    @Nullable
    public static List<String> getSuggestions(@NonNull String errorMessage) {
        // The words are all in quotes; the first word is the misspelling,
        // the other words are the suggested replacements
        List<String> words = new ArrayList<String>();
        // Skip the typo
        int index = errorMessage.indexOf('"');
        index = errorMessage.indexOf('"', index + 1);
        index++;

        while (true) {
            index = errorMessage.indexOf('"', index);
            if (index == -1) {
                break;
            }
            index++;
            int start = index;
            index = errorMessage.indexOf('"', index);
            if (index == -1) {
                index = errorMessage.length();
            }
            words.add(errorMessage.substring(start, index));
            index++;
        }

        return words;
    }

    /**
     * Returns the typo word in the error message from this detector
     *
     * @param errorMessage the error message produced earlier by this detector
     * @return the typo
     */
    @Nullable
    public static String getTypo(@NonNull String errorMessage) {
        // The words are all in quotes
        int index = errorMessage.indexOf('"');
        int start = index + 1;
        index = errorMessage.indexOf('"', start);
        if (index != -1) {
            return errorMessage.substring(start, index);
        }

        return null;
    }
}
