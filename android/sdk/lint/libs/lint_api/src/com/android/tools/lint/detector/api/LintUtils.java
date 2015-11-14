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

package com.android.tools.lint.detector.api;

import static com.android.SdkConstants.ANDROID_MANIFEST_XML;
import static com.android.SdkConstants.BIN_FOLDER;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.FolderTypeRelationship;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.tools.lint.client.api.LintClient;
import com.android.utils.PositionXmlParser;
import com.google.common.annotations.Beta;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import lombok.ast.ImportDeclaration;


/**
 * Useful utility methods related to lint.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class LintUtils {
    /**
     * Format a list of strings, and cut of the list at {@code maxItems} if the
     * number of items are greater.
     *
     * @param strings the list of strings to print out as a comma separated list
     * @param maxItems the maximum number of items to print
     * @return a comma separated list
     */
    @NonNull
    public static String formatList(@NonNull List<String> strings, int maxItems) {
        StringBuilder sb = new StringBuilder(20 * strings.size());

        for (int i = 0, n = strings.size(); i < n; i++) {
            if (sb.length() > 0) {
                sb.append(", "); //$NON-NLS-1$
            }
            sb.append(strings.get(i));

            if (maxItems > 0 && i == maxItems - 1 && n > maxItems) {
                sb.append(String.format("... (%1$d more)", n - i - 1));
                break;
            }
        }

        return sb.toString();
    }

    /**
     * Determine if the given type corresponds to a resource that has a unique
     * file
     *
     * @param type the resource type to check
     * @return true if the given type corresponds to a file-type resource
     */
    public static boolean isFileBasedResourceType(@NonNull ResourceType type) {
        List<ResourceFolderType> folderTypes = FolderTypeRelationship.getRelatedFolders(type);
        for (ResourceFolderType folderType : folderTypes) {
            if (folderType != ResourceFolderType.VALUES) {
                if (type == ResourceType.ID) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the given file represents an XML file
     *
     * @param file the file to be checked
     * @return true if the given file is an xml file
     */
    public static boolean isXmlFile(@NonNull File file) {
        String string = file.getName();
        return string.regionMatches(true, string.length() - DOT_XML.length(),
                DOT_XML, 0, DOT_XML.length());
    }

    /**
     * Case insensitive ends with
     *
     * @param string the string to be tested whether it ends with the given
     *            suffix
     * @param suffix the suffix to check
     * @return true if {@code string} ends with {@code suffix},
     *         case-insensitively.
     */
    public static boolean endsWith(@NonNull String string, @NonNull String suffix) {
        return string.regionMatches(true /* ignoreCase */, string.length() - suffix.length(),
                suffix, 0, suffix.length());
    }

    /**
     * Case insensitive starts with
     *
     * @param string the string to be tested whether it starts with the given prefix
     * @param prefix the prefix to check
     * @param offset the offset to start checking with
     * @return true if {@code string} starts with {@code prefix},
     *         case-insensitively.
     */
    public static boolean startsWith(@NonNull String string, @NonNull String prefix, int offset) {
        return string.regionMatches(true /* ignoreCase */, offset, prefix, 0, prefix.length());
    }

    /**
     * Returns the basename of the given filename, unless it's a dot-file such as ".svn".
     *
     * @param fileName the file name to extract the basename from
     * @return the basename (the filename without the file extension)
     */
    public static String getBaseName(@NonNull String fileName) {
        int extension = fileName.indexOf('.');
        if (extension > 0) {
            return fileName.substring(0, extension);
        } else {
            return fileName;
        }
    }

    /**
     * Returns the children elements of the given node
     *
     * @param node the parent node
     * @return a list of element children, never null
     */
    @NonNull
    public static List<Element> getChildren(@NonNull Node node) {
        NodeList childNodes = node.getChildNodes();
        List<Element> children = new ArrayList<Element>(childNodes.getLength());
        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                children.add((Element) child);
            }
        }

        return children;
    }

    /**
     * Returns the <b>number</b> of children of the given node
     *
     * @param node the parent node
     * @return the count of element children
     */
    public static int getChildCount(@NonNull Node node) {
        NodeList childNodes = node.getChildNodes();
        int childCount = 0;
        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                childCount++;
            }
        }

        return childCount;
    }

    /**
     * Returns true if the given element is the root element of its document
     *
     * @param element the element to test
     * @return true if the element is the root element
     */
    public static boolean isRootElement(Element element) {
        return element == element.getOwnerDocument().getDocumentElement();
    }

    /**
     * Returns the given id without an {@code @id/} or {@code @+id} prefix
     *
     * @param id the id to strip
     * @return the stripped id, never null
     */
    @NonNull
    public static String stripIdPrefix(@Nullable String id) {
        if (id == null) {
            return "";
        } else if (id.startsWith(NEW_ID_PREFIX)) {
            return id.substring(NEW_ID_PREFIX.length());
        } else if (id.startsWith(ID_PREFIX)) {
            return id.substring(ID_PREFIX.length());
        }

        return id;
    }

    /**
     * Returns true if the given two id references match. This is similar to
     * String equality, but it also considers "{@code @+id/foo == @id/foo}.
     *
     * @param id1 the first id to compare
     * @param id2 the second id to compare
     * @return true if the two id references refer to the same id
     */
    public static boolean idReferencesMatch(String id1, String id2) {
        if (id1.startsWith(NEW_ID_PREFIX)) {
            if (id2.startsWith(NEW_ID_PREFIX)) {
                return id1.equals(id2);
            } else {
                assert id2.startsWith(ID_PREFIX);
                return ((id1.length() - id2.length())
                            == (NEW_ID_PREFIX.length() - ID_PREFIX.length()))
                        && id1.regionMatches(NEW_ID_PREFIX.length(), id2,
                                ID_PREFIX.length(),
                                id2.length() - ID_PREFIX.length());
            }
        } else {
            assert id1.startsWith(ID_PREFIX);
            if (id2.startsWith(ID_PREFIX)) {
                return id1.equals(id2);
            } else {
                assert id2.startsWith(NEW_ID_PREFIX);
                return (id2.length() - id1.length()
                            == (NEW_ID_PREFIX.length() - ID_PREFIX.length()))
                        && id2.regionMatches(NEW_ID_PREFIX.length(), id1,
                                ID_PREFIX.length(),
                                id1.length() - ID_PREFIX.length());
            }
        }
    }

    /**
     * Computes the edit distance (number of insertions, deletions or substitutions
     * to edit one string into the other) between two strings. In particular,
     * this will compute the Levenshtein distance.
     * <p>
     * See http://en.wikipedia.org/wiki/Levenshtein_distance for details.
     *
     * @param s the first string to compare
     * @param t the second string to compare
     * @return the edit distance between the two strings
     */
    public static int editDistance(@NonNull String s, @NonNull String t) {
        int m = s.length();
        int n = t.length();
        int[][] d = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= n; j++) {
            d[0][j] = j;
        }
        for (int j = 1; j <= n; j++) {
            for (int i = 1; i <= m; i++) {
                if (s.charAt(i - 1) == t.charAt(j - 1)) {
                    d[i][j] = d[i - 1][j - 1];
                } else {
                    int deletion = d[i - 1][j] + 1;
                    int insertion = d[i][j - 1] + 1;
                    int substitution = d[i - 1][j - 1] + 1;
                    d[i][j] = Math.min(deletion, Math.min(insertion, substitution));
                }
            }
        }

        return d[m][n];
    }

    /**
     * Returns true if assertions are enabled
     *
     * @return true if assertions are enabled
     */
    @SuppressWarnings("all")
    public static boolean assertionsEnabled() {
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true; // Intentional side-effect
        return assertionsEnabled;
    }

    /**
     * Returns the layout resource name for the given layout file
     *
     * @param layoutFile the file pointing to the layout
     * @return the layout resource name, not including the {@code @layout}
     *         prefix
     */
    public static String getLayoutName(File layoutFile) {
        String name = layoutFile.getName();
        int dotIndex = name.indexOf('.');
        if (dotIndex != -1) {
            name = name.substring(0, dotIndex);
        }
        return name;
    }

    /**
     * Splits the given path into its individual parts, attempting to be
     * tolerant about path separators (: or ;). It can handle possibly ambiguous
     * paths, such as {@code c:\foo\bar:\other}, though of course these are to
     * be avoided if possible.
     *
     * @param path the path variable to split, which can use both : and ; as
     *            path separators.
     * @return the individual path components as an iterable of strings
     */
    public static Iterable<String> splitPath(String path) {
        if (path.indexOf(';') != -1) {
            return Splitter.on(';').omitEmptyStrings().trimResults().split(path);
        }

        List<String> combined = new ArrayList<String>();
        Iterables.addAll(combined, Splitter.on(':').omitEmptyStrings().trimResults().split(path));
        for (int i = 0, n = combined.size(); i < n; i++) {
            String p = combined.get(i);
            if (p.length() == 1 && i < n - 1 && Character.isLetter(p.charAt(0))
                    // Technically, Windows paths do not have to have a \ after the :,
                    // which means it would be using the current directory on that drive,
                    // but that's unlikely to be the case in a path since it would have
                    // unpredictable results
                    && !combined.get(i+1).isEmpty() && combined.get(i+1).charAt(0) == '\\') {
                combined.set(i, p + ':' + combined.get(i+1));
                combined.remove(i+1);
                n--;
                continue;
            }
        }

        return combined;
    }

    /**
     * Computes the shared parent among a set of files (which may be null).
     *
     * @param files the set of files to be checked
     * @return the closest common ancestor file, or null if none was found
     */
    @Nullable
    public static File getCommonParent(@NonNull List<File> files) {
        int fileCount = files.size();
        if (fileCount == 0) {
            return null;
        } else if (fileCount == 1) {
            return files.get(0);
        } else if (fileCount == 2) {
            return getCommonParent(files.get(0), files.get(1));
        } else {
            File common = files.get(0);
            for (int i = 1; i < fileCount; i++) {
                common = getCommonParent(common, files.get(i));
                if (common == null) {
                    return null;
                }
            }

            return common;
        }
    }

    /**
     * Computes the closest common parent path between two files.
     *
     * @param file1 the first file to be compared
     * @param file2 the second file to be compared
     * @return the closest common ancestor file, or null if the two files have
     *         no common parent
     */
    @Nullable
    public static File getCommonParent(@NonNull File file1, @NonNull File file2) {
        if (file1.equals(file2)) {
            return file1;
        } else if (file1.getPath().startsWith(file2.getPath())) {
            return file2;
        } else if (file2.getPath().startsWith(file1.getPath())) {
            return file1;
        } else {
            // Dumb and simple implementation
            File first = file1.getParentFile();
            while (first != null) {
                File second = file2.getParentFile();
                while (second != null) {
                    if (first.equals(second)) {
                        return first;
                    }
                    second = second.getParentFile();
                }

                first = first.getParentFile();
            }
        }
        return null;
    }

    private static final String UTF_8 = "UTF-8";                 //$NON-NLS-1$
    private static final String UTF_16 = "UTF_16";               //$NON-NLS-1$
    private static final String UTF_16LE = "UTF_16LE";           //$NON-NLS-1$

    /**
     * Returns the encoded String for the given file. This is usually the
     * same as {@code Files.toString(file, Charsets.UTF8}, but if there's a UTF byte order mark
     * (for UTF8, UTF_16 or UTF_16LE), use that instead.
     *
     * @param client the client to use for I/O operations
     * @param file the file to read from
     * @return the string
     * @throws IOException if the file cannot be read properly
     */
    @NonNull
    public static String getEncodedString(
            @NonNull LintClient client,
            @NonNull File file) throws IOException {
        byte[] bytes = client.readBytes(file);
        if (endsWith(file.getName(), DOT_XML)) {
            return PositionXmlParser.getXmlString(bytes);
        }

        return LintUtils.getEncodedString(bytes);
    }

    /**
     * Returns the String corresponding to the given data. This is usually the
     * same as {@code new String(data)}, but if there's a UTF byte order mark
     * (for UTF8, UTF_16 or UTF_16LE), use that instead.
     * <p>
     * NOTE: For XML files, there is the additional complication that there
     * could be a {@code encoding=} attribute in the prologue. For those files,
     * use {@link PositionXmlParser#getXmlString(byte[])} instead.
     *
     * @param data the byte array to construct the string from
     * @return the string
     */
    @NonNull
    public static String getEncodedString(@Nullable byte[] data) {
        if (data == null) {
            return "";
        }

        int offset = 0;
        String defaultCharset = UTF_8;
        String charset = null;
        // Look for the byte order mark, to see if we need to remove bytes from
        // the input stream (and to determine whether files are big endian or little endian) etc
        // for files which do not specify the encoding.
        // See http://unicode.org/faq/utf_bom.html#BOM for more.
        if (data.length > 4) {
            if (data[0] == (byte)0xef && data[1] == (byte)0xbb && data[2] == (byte)0xbf) {
                // UTF-8
                defaultCharset = charset = UTF_8;
                offset += 3;
            } else if (data[0] == (byte)0xfe && data[1] == (byte)0xff) {
                //  UTF-16, big-endian
                defaultCharset = charset = UTF_16;
                offset += 2;
            } else if (data[0] == (byte)0x0 && data[1] == (byte)0x0
                    && data[2] == (byte)0xfe && data[3] == (byte)0xff) {
                // UTF-32, big-endian
                defaultCharset = charset = "UTF_32";    //$NON-NLS-1$
                offset += 4;
            } else if (data[0] == (byte)0xff && data[1] == (byte)0xfe
                    && data[2] == (byte)0x0 && data[3] == (byte)0x0) {
                // UTF-32, little-endian. We must check for this *before* looking for
                // UTF_16LE since UTF_32LE has the same prefix!
                defaultCharset = charset = "UTF_32LE";  //$NON-NLS-1$
                offset += 4;
            } else if (data[0] == (byte)0xff && data[1] == (byte)0xfe) {
                //  UTF-16, little-endian
                defaultCharset = charset = UTF_16LE;
                offset += 2;
            }
        }
        int length = data.length - offset;

        // Guess encoding by searching for an encoding= entry in the first line.
        boolean seenOddZero = false;
        boolean seenEvenZero = false;
        for (int lineEnd = offset; lineEnd < data.length; lineEnd++) {
            if (data[lineEnd] == 0) {
                if ((lineEnd - offset) % 1 == 0) {
                    seenEvenZero = true;
                } else {
                    seenOddZero = true;
                }
            } else if (data[lineEnd] == '\n' || data[lineEnd] == '\r') {
                break;
            }
        }

        if (charset == null) {
            charset = seenOddZero ? UTF_16 : seenEvenZero ? UTF_16LE : UTF_8;
        }

        String text = null;
        try {
            text = new String(data, offset, length, charset);
        } catch (UnsupportedEncodingException e) {
            try {
                if (charset != defaultCharset) {
                    text = new String(data, offset, length, defaultCharset);
                }
            } catch (UnsupportedEncodingException u) {
                // Just use the default encoding below
            }
        }
        if (text == null) {
            text = new String(data, offset, length);
        }
        return text;
    }

    /**
     * Returns true if the given class node represents a static inner class.
     *
     * @param classNode the inner class to be checked
     * @return true if the class node represents an inner class that is static
     */
    public static boolean isStaticInnerClass(@NonNull ClassNode classNode) {
        // Note: We can't just filter out static inner classes like this:
        //     (classNode.access & Opcodes.ACC_STATIC) != 0
        // because the static flag only appears on methods and fields in the class
        // file. Instead, look for the synthetic this pointer.

        @SuppressWarnings("rawtypes") // ASM API
        List fieldList = classNode.fields;
        for (Object f : fieldList) {
            FieldNode field = (FieldNode) f;
            if (field.name.startsWith("this$") && (field.access & Opcodes.ACC_SYNTHETIC) != 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the previous opcode prior to the given node, ignoring label and
     * line number nodes
     *
     * @param node the node to look up the previous opcode for
     * @return the previous opcode, or {@link Opcodes#NOP} if no previous node
     *         was found
     */
    public static int getPrevOpcode(@NonNull AbstractInsnNode node) {
        AbstractInsnNode prev = getPrevInstruction(node);
        if (prev != null) {
            return prev.getOpcode();
        } else {
            return Opcodes.NOP;
        }
    }

    /**
     * Returns the previous instruction prior to the given node, ignoring label
     * and line number nodes.
     *
     * @param node the node to look up the previous instruction for
     * @return the previous instruction, or null if no previous node was found
     */
    @Nullable
    public static AbstractInsnNode getPrevInstruction(@NonNull AbstractInsnNode node) {
        AbstractInsnNode prev = node;
        while (true) {
            prev = prev.getPrevious();
            if (prev == null) {
                return null;
            } else {
                int type = prev.getType();
                if (type != AbstractInsnNode.LINE && type != AbstractInsnNode.LABEL
                        && type != AbstractInsnNode.FRAME) {
                    return prev;
                }
            }
        }
    }

    /**
     * Returns the next opcode after to the given node, ignoring label and line
     * number nodes
     *
     * @param node the node to look up the next opcode for
     * @return the next opcode, or {@link Opcodes#NOP} if no next node was found
     */
    public static int getNextOpcode(@NonNull AbstractInsnNode node) {
        AbstractInsnNode next = getNextInstruction(node);
        if (next != null) {
            return next.getOpcode();
        } else {
            return Opcodes.NOP;
        }
    }

    /**
     * Returns the next instruction after to the given node, ignoring label and
     * line number nodes.
     *
     * @param node the node to look up the next node for
     * @return the next instruction, or null if no next node was found
     */
    @Nullable
    public static AbstractInsnNode getNextInstruction(@NonNull AbstractInsnNode node) {
        AbstractInsnNode next = node;
        while (true) {
            next = next.getNext();
            if (next == null) {
                return null;
            } else {
                int type = next.getType();
                if (type != AbstractInsnNode.LINE && type != AbstractInsnNode.LABEL
                        && type != AbstractInsnNode.FRAME) {
                    return next;
                }
            }
        }
    }

    /**
     * Returns true if the given directory represents an Android project
     * directory. Note: This doesn't necessarily mean it's an Eclipse directory,
     * only that it looks like it contains a logical Android project -- one
     * including a manifest file, a resource folder, etc.
     *
     * @param dir the directory to check
     * @return true if the directory looks like an Android project
     */
    public static boolean isProjectDir(@NonNull File dir) {
        boolean hasManifest = new File(dir, ANDROID_MANIFEST_XML).exists();
        if (hasManifest) {
            // Special case: the bin/ folder can also contain a copy of the
            // manifest file, but this is *not* a project directory
            if (dir.getName().equals(BIN_FOLDER)) {
                // ...unless of course it just *happens* to be a project named bin, in
                // which case we peek at its parent to see if this is the case
                dir = dir.getParentFile();
                if (dir != null && isProjectDir(dir)) {
                    // Yes, it's a bin/ directory inside a real project: ignore this dir
                    return false;
                }
            }
        }

        return hasManifest;
    }

    /**
     * Look up the locale and region from the given parent folder name and
     * return it as a combined string, such as "en", "en-rUS", etc, or null if
     * no language is specified.
     *
     * @param folderName the folder name
     * @return the locale+region string or null
     */
    @Nullable
    public static String getLocaleAndRegion(@NonNull String folderName) {
         if (folderName.equals("values")) { //$NON-NLS-1$
            return null;
         }

         String locale = null;

         for (String qualifier : Splitter.on('-').split(folderName)) {
            int qualifierLength = qualifier.length();
            if (qualifierLength == 2) {
                 char first = qualifier.charAt(0);
                char second = qualifier.charAt(1);
                 if (first >= 'a' && first <= 'z' && second >= 'a' && second <= 'z') {
                    locale = qualifier;
                }
            } else if (qualifierLength == 3 && qualifier.charAt(0) == 'r' && locale != null) {
                char first = qualifier.charAt(1);
                char second = qualifier.charAt(2);
                if (first >= 'A' && first <= 'Z' && second >= 'A' && second <= 'Z') {
                    return locale + '-' + qualifier;
                }
                break;
             }
         }

         return locale;
     }

    /**
     * Returns true if the given class (specified by a fully qualified class
     * name) name is imported in the given compilation unit either through a fully qualified
     * import or by a wildcard import.
     *
     * @param compilationUnit the compilation unit
     * @param fullyQualifiedName the fully qualified class name
     * @return true if the given imported name refers to the given fully
     *         qualified name
     */
    public static boolean isImported(
            @NonNull lombok.ast.Node compilationUnit,
            @NonNull String fullyQualifiedName) {
        int dotIndex = fullyQualifiedName.lastIndexOf('.');
        int dotLength = fullyQualifiedName.length() - dotIndex;

        boolean imported = false;
        for (lombok.ast.Node rootNode : compilationUnit.getChildren()) {
            if (rootNode instanceof ImportDeclaration) {
                ImportDeclaration importDeclaration = (ImportDeclaration) rootNode;
                String fqn = importDeclaration.asFullyQualifiedName();
                if (fqn.equals(fullyQualifiedName)) {
                    return true;
                } else if (fullyQualifiedName.regionMatches(dotIndex, fqn,
                        fqn.length() - dotLength, dotLength)) {
                    // This import is importing the class name using some other prefix, so there
                    // fully qualified class name cannot be imported under that name
                    return false;
                } else if (importDeclaration.astStarImport()
                        && fqn.regionMatches(0, fqn, 0, dotIndex + 1)) {
                    imported = true;
                    // but don't break -- keep searching in case there's a non-wildcard
                    // import of the specific class name, e.g. if we're looking for
                    // android.content.SharedPreferences.Editor, don't match on the following:
                    //   import android.content.SharedPreferences.*;
                    //   import foo.bar.Editor;
                }
            }
        }

        return imported;
    }
}
