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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_TYPE;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.RELATIVE_LAYOUT;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.VALUE_ID;
import static com.android.tools.lint.detector.api.LintUtils.stripIdPrefix;

import com.android.annotations.NonNull;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.client.api.IDomParser;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Location.Handle;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.utils.Pair;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks for duplicate ids within a layout and within an included layout
 */
public class WrongIdDetector extends LayoutDetector {

    /** Ids bound to widgets in any of the layout files */
    private Set<String> mGlobalIds = new HashSet<String>(100);

    /** Ids bound to widgets in the current layout file */
    private Set<String> mFileIds;

    /** Ids declared in a value's file, e.g. {@code <item type="id" name="foo"/>} */
    private Set<String> mDeclaredIds;

    /**
     * Location handles for the various id references that were not found as
     * defined in the same layout, to be checked after the whole project has
     * been scanned
     */
    private List<Pair<String, Location.Handle>> mHandles;

    /** List of RelativeLayout elements in the current layout */
    private List<Element> mRelativeLayouts;

    /** Reference to an unknown id */
    public static final Issue UNKNOWN_ID = Issue.create(
            "UnknownId", //$NON-NLS-1$
            "Checks for id references in RelativeLayouts that are not defined elsewhere",
            "The `@+id/` syntax refers to an existing id, or creates a new one if it has " +
            "not already been defined elsewhere. However, this means that if you have a " +
            "typo in your reference, or if the referred view no longer exists, you do not " +
            "get a warning since the id will be created on demand. This check catches " +
            "errors where you have renamed an id without updating all of the references to " +
            "it.",
            Category.CORRECTNESS,
            8,
            Severity.FATAL,
            WrongIdDetector.class,
            Scope.ALL_RESOURCES_SCOPE);

    /** Reference to an id that is not in the current layout */
    public static final Issue UNKNOWN_ID_LAYOUT = Issue.create(
            "UnknownIdInLayout", //$NON-NLS-1$
            "Makes sure that @+id references refer to views in the same layout",

            "The `@+id/` syntax refers to an existing id, or creates a new one if it has " +
            "not already been defined elsewhere. However, this means that if you have a " +
            "typo in your reference, or if the referred view no longer exists, you do not " +
            "get a warning since the id will be created on demand.\n" +
            "\n" +
            "This is sometimes intentional, for example where you are referring to a view " +
            "which is provided in a different layout via an include. However, it is usually " +
            "an accident where you have a typo or you have renamed a view without updating " +
            "all the references to it.",

            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            WrongIdDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** Constructs a duplicate id check */
    public WrongIdDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.LAYOUT || folderType == ResourceFolderType.VALUES;
    }

    @Override
    public @NonNull Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return Collections.singletonList(ATTR_ID);
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(RELATIVE_LAYOUT, TAG_ITEM);
    }

    @Override
    public void beforeCheckFile(@NonNull Context context) {
        mFileIds = new HashSet<String>();
        mRelativeLayouts = null;
    }

    @Override
    public void afterCheckFile(@NonNull Context context) {
        if (mRelativeLayouts != null) {
            for (Element layout : mRelativeLayouts) {
                NodeList children = layout.getChildNodes();
                for (int j = 0, childCount = children.getLength(); j < childCount; j++) {
                    Node child = children.item(j);
                    if (child.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }
                    Element element = (Element) child;
                    NamedNodeMap attributes = element.getAttributes();
                    for (int i = 0, n = attributes.getLength(); i < n; i++) {
                        Attr attr = (Attr) attributes.item(i);
                        String value = attr.getValue();
                        if ((value.startsWith(NEW_ID_PREFIX) ||
                                value.startsWith(ID_PREFIX))
                                && ANDROID_URI.equals(attr.getNamespaceURI())
                                && attr.getLocalName().startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
                            if (!idDefined(mFileIds, value)) {
                                // Stash a reference to this id and location such that
                                // we can check after the *whole* layout has been processed,
                                // since it's too early to conclude here that the id does
                                // not exist (you are allowed to have forward references)
                                XmlContext xmlContext = (XmlContext) context;
                                IDomParser parser = xmlContext.parser;
                                Handle handle = parser.createLocationHandle(xmlContext, attr);
                                handle.setClientData(attr);

                                if (mHandles == null) {
                                    mHandles = new ArrayList<Pair<String,Handle>>();
                                }
                                mHandles.add(Pair.of(value, handle));
                            }
                        }
                    }
                }
            }
        }

        mFileIds = null;
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (mHandles != null) {
            boolean checkSameLayout = context.isEnabled(UNKNOWN_ID_LAYOUT);
            boolean checkExists = context.isEnabled(UNKNOWN_ID);
            boolean projectScope = context.getScope().contains(Scope.ALL_RESOURCE_FILES);
            for (Pair<String, Handle> pair : mHandles) {
                String id = pair.getFirst();
                boolean isBound = idDefined(mGlobalIds, id);
                if (!isBound && checkExists && projectScope) {
                    Handle handle = pair.getSecond();
                    boolean isDeclared = idDefined(mDeclaredIds, id);
                    id = stripIdPrefix(id);
                    String suggestionMessage;
                    List<String> suggestions = getSpellingSuggestions(id, mGlobalIds);
                    if (suggestions.size() > 1) {
                        suggestionMessage = String.format(" Did you mean one of {%2$s} ?",
                                id, Joiner.on(", ").join(suggestions));
                    } else if (suggestions.size() > 0) {
                        suggestionMessage = String.format(" Did you mean %2$s ?",
                                id, suggestions.get(0));
                    } else {
                        suggestionMessage = "";
                    }
                    String message;
                    if (isDeclared) {
                        message = String.format(
                                "The id \"%1$s\" is defined but not assigned to any views.%2$s",
                                id, suggestionMessage);
                    } else {
                        message = String.format(
                                "The id \"%1$s\" is not defined anywhere.%2$s",
                                id, suggestionMessage);
                    }
                    report(context, UNKNOWN_ID, handle, message);
                } else if (checkSameLayout && (!projectScope || isBound)) {
                    // The id was defined, but in a different layout. Usually not intentional
                    // (might be referring to a random other view that happens to have the same
                    // name.)
                    Handle handle = pair.getSecond();
                    report(context, UNKNOWN_ID_LAYOUT, handle,
                            String.format(
                                    "The id \"%1$s\" is not referring to any views in this layout",
                                    stripIdPrefix(id)));
                }
            }
        }
    }

    private void report(Context context, Issue issue, Handle handle, String message) {
        Location location = handle.resolve();
        Object clientData = handle.getClientData();
        if (clientData instanceof Node) {
            if (context.getDriver().isSuppressed(issue, (Node) clientData)) {
                return;
            }
        }

        context.report(issue, location, message, null);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (element.getTagName().equals(RELATIVE_LAYOUT)) {
            if (mRelativeLayouts == null) {
                mRelativeLayouts = new ArrayList<Element>();
            }
            mRelativeLayouts.add(element);
        } else {
            assert element.getTagName().equals(TAG_ITEM);
            String type = element.getAttribute(ATTR_TYPE);
            if (VALUE_ID.equals(type)) {
                String name = element.getAttribute(ATTR_NAME);
                if (name.length() > 0) {
                    if (mDeclaredIds == null) {
                        mDeclaredIds = Sets.newHashSet();
                    }
                    mDeclaredIds.add(ID_PREFIX + name);
                }
            }
        }
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        assert attribute.getName().equals(ATTR_ID) || attribute.getLocalName().equals(ATTR_ID);
        String id = attribute.getValue();
        mFileIds.add(id);
        mGlobalIds.add(id);
    }

    private static boolean idDefined(Set<String> ids, String id) {
        if (ids == null) {
            return false;
        }
        boolean definedLocally = ids.contains(id);
        if (!definedLocally) {
            if (id.startsWith(NEW_ID_PREFIX)) {
                definedLocally = ids.contains(ID_PREFIX +
                        id.substring(NEW_ID_PREFIX.length()));
            } else if (id.startsWith(ID_PREFIX)) {
                definedLocally = ids.contains(NEW_ID_PREFIX +
                        id.substring(ID_PREFIX.length()));
            }
        }

        return definedLocally;
    }

    private List<String> getSpellingSuggestions(String id, Collection<String> ids) {
        int maxDistance = id.length() >= 4 ? 2 : 1;

        // Look for typos and try to match with custom views and android views
        Multimap<Integer, String> matches = ArrayListMultimap.create(2, 10);
        int count = 0;
        if (ids.size() > 0) {
            for (String matchWith : ids) {
                matchWith = stripIdPrefix(matchWith);
                if (Math.abs(id.length() - matchWith.length()) > maxDistance) {
                    // The string lengths differ more than the allowed edit distance;
                    // no point in even attempting to compute the edit distance (requires
                    // O(n*m) storage and O(n*m) speed, where n and m are the string lengths)
                    continue;
                }
                int distance = LintUtils.editDistance(id, matchWith);
                if (distance <= maxDistance) {
                    matches.put(distance, matchWith);
                }

                if (count++ > 100) {
                    // Make sure that for huge projects we don't completely grind to a halt
                    break;
                }
            }
        }

        for (int i = 0; i < maxDistance; i++) {
            Collection<String> s = matches.get(i);
            if (s != null && s.size() > 0) {
                List<String> suggestions = new ArrayList<String>(s);
                Collections.sort(suggestions);
                return suggestions;
            }
        }

        return Collections.emptyList();
    }
}
