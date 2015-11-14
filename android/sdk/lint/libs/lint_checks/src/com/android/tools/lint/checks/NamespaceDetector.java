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

import static com.android.SdkConstants.ANDROID_PKG_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.AUTO_URI;
import static com.android.SdkConstants.URI_PREFIX;
import static com.android.SdkConstants.XMLNS_PREFIX;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks for various issues related to XML namespaces
 */
public class NamespaceDetector extends LayoutDetector {
    /** Typos in the namespace */
    public static final Issue TYPO = Issue.create(
            "NamespaceTypo", //$NON-NLS-1$
            "Looks for misspellings in namespace declarations",

            "Accidental misspellings in namespace declarations can lead to some very " +
            "obscure error messages. This check looks for potential misspellings to " +
            "help track these down.",
            Category.CORRECTNESS,
            8,
            Severity.WARNING,
            NamespaceDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** Unused namespace declarations */
    public static final Issue UNUSED = Issue.create(
            "UnusedNamespace", //$NON-NLS-1$
            "Finds unused namespaces in XML documents",

            "Unused namespace declarations take up space and require processing that is not " +
            "necessary",

            Category.CORRECTNESS,
            1,
            Severity.WARNING,
            NamespaceDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** Using custom namespace attributes in a library project */
    public static final Issue CUSTOMVIEW = Issue.create(
            "LibraryCustomView", //$NON-NLS-1$
            "Flags custom attributes in libraries, which must use the res-auto-namespace instead",

            "When using a custom view with custom attributes in a library project, the layout " +
            "must use the special namespace " + AUTO_URI + " instead of a URI which includes " +
            "the library project's own package. This will be used to automatically adjust the " +
            "namespace of the attributes when the library resources are merged into the " +
            "application project.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            NamespaceDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** Prefix relevant for custom namespaces */
    private static final String XMLNS_ANDROID = "xmlns:android";                    //$NON-NLS-1$
    private static final String XMLNS_A = "xmlns:a";                                //$NON-NLS-1$

    private Map<String, Attr> mUnusedNamespaces;
    private boolean mCheckUnused;
    private boolean mCheckCustomAttrs;

    /** Constructs a new {@link NamespaceDetector} */
    public NamespaceDetector() {
    }

    @Override
    public @NonNull Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public void visitDocument(@NonNull XmlContext context, @NonNull Document document) {
        boolean haveCustomNamespace = false;
        Element root = document.getDocumentElement();
        NamedNodeMap attributes = root.getAttributes();
        for (int i = 0, n = attributes.getLength(); i < n; i++) {
            Node item = attributes.item(i);
            if (item.getNodeName().startsWith(XMLNS_PREFIX)) {
                String value = item.getNodeValue();

                if (!value.equals(ANDROID_URI)) {
                    Attr attribute = (Attr) item;

                    if (value.startsWith(URI_PREFIX)) {
                        haveCustomNamespace = true;
                        if (mUnusedNamespaces == null) {
                            mUnusedNamespaces = new HashMap<String, Attr>();
                        }
                        mUnusedNamespaces.put(item.getNodeName().substring(XMLNS_PREFIX.length()),
                                attribute);
                    }

                    String name = attribute.getName();
                    if (!name.equals(XMLNS_ANDROID) && !name.equals(XMLNS_A)) {
                        continue;
                    }

                    if (!context.isEnabled(TYPO)) {
                        continue;
                    }

                    if (name.equals(XMLNS_A)) {
                        // For the "android" prefix we always assume that the namespace prefix
                        // should be our expected prefix, but for the "a" prefix we make sure
                        // that it's at least "close"; if you're bound it to something completely
                        // different, don't complain.
                        if (LintUtils.editDistance(ANDROID_URI, value) > 4) {
                            continue;
                        }
                    }

                    if (value.equalsIgnoreCase(ANDROID_URI)) {
                        context.report(TYPO, attribute, context.getLocation(attribute),
                                String.format(
                                    "URI is case sensitive: was \"%1$s\", expected \"%2$s\"",
                                    value, ANDROID_URI), null);
                    } else {
                        context.report(TYPO, attribute, context.getLocation(attribute),
                                String.format(
                                    "Unexpected namespace URI bound to the \"android\" " +
                                    "prefix, was %1$s, expected %2$s", value, ANDROID_URI),
                                null);
                    }
                }
            }
        }

        if (haveCustomNamespace) {
            mCheckCustomAttrs = context.isEnabled(CUSTOMVIEW) && context.getProject().isLibrary();
            mCheckUnused = context.isEnabled(UNUSED);
            checkElement(context, document.getDocumentElement());

            if (mCheckUnused && mUnusedNamespaces.size() > 0) {
                for (Map.Entry<String, Attr> entry : mUnusedNamespaces.entrySet()) {
                    String prefix = entry.getKey();
                    Attr attribute = entry.getValue();
                    context.report(UNUSED, attribute, context.getLocation(attribute),
                            String.format("Unused namespace %1$s", prefix), null);
                }
            }
        }
    }

    private void checkElement(XmlContext context, Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            if (mCheckCustomAttrs) {
                String tag = node.getNodeName();
                if (tag.indexOf('.') != -1
                        // Don't consider android.support.* and android.app.FragmentBreadCrumbs etc
                        && !tag.startsWith(ANDROID_PKG_PREFIX)) {
                    NamedNodeMap attributes = ((Element) node).getAttributes();
                    for (int i = 0, n = attributes.getLength(); i < n; i++) {
                        Attr attribute = (Attr) attributes.item(i);
                        String uri = attribute.getNamespaceURI();
                        if (uri != null && uri.length() > 0 && uri.startsWith(URI_PREFIX)
                                && !uri.equals(ANDROID_URI)) {
                            context.report(CUSTOMVIEW, attribute, context.getLocation(attribute),
                                "When using a custom namespace attribute in a library project, " +
                                "use the namespace \"" + AUTO_URI + "\" instead.", null);
                        }
                    }
                }
            }

            if (mCheckUnused) {
                NamedNodeMap attributes = ((Element) node).getAttributes();
                for (int i = 0, n = attributes.getLength(); i < n; i++) {
                    Attr attribute = (Attr) attributes.item(i);
                    String prefix = attribute.getPrefix();
                    if (prefix != null) {
                        mUnusedNamespaces.remove(prefix);
                    }
                }
            }

            NodeList childNodes = node.getChildNodes();
            for (int i = 0, n = childNodes.getLength(); i < n; i++) {
                checkElement(context, childNodes.item(i));
            }
        }
    }
}
