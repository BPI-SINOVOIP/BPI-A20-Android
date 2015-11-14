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

import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.DOT_JAVA;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.VIEW_TAG;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.ast.AstVisitor;
import lombok.ast.Cast;
import lombok.ast.Expression;
import lombok.ast.MethodInvocation;
import lombok.ast.Select;
import lombok.ast.StrictListAccessor;

/** Detector for finding inconsistent usage of views and casts */
public class ViewTypeDetector extends ResourceXmlDetector implements Detector.JavaScanner {
    /** Mismatched view types */
    public static final Issue ISSUE = Issue.create("WrongViewCast", //$NON-NLS-1$
            "Looks for incorrect casts to views that according to the XML are of a different type",
            "Keeps track of the view types associated with ids and if it finds a usage of " +
            "the id in the Java code it ensures that it is treated as the same type.",
            Category.CORRECTNESS,
            9,
            Severity.ERROR,
            ViewTypeDetector.class,
            EnumSet.of(Scope.ALL_RESOURCE_FILES, Scope.ALL_JAVA_FILES));

    private Map<String, String> mIdToViewTag = new HashMap<String, String>(50);

    @Override
    public @NonNull Speed getSpeed() {
        return Speed.SLOW;
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.LAYOUT;
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        if (LintUtils.endsWith(file.getName(), DOT_JAVA)) {
            return true;
        }

        return super.appliesTo(context, file);
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return Collections.singletonList(ATTR_ID);
    }

    /** Special marker value which means that an id is used for multiple different view types;
     * in this case we should figure out which ids are reachable from different activities
     * and do a more fine grained analysis to report casting problems, but for now we just
     * mark these id's to be ignored instead
     */
    private static final String IGNORE = "#ignore#";

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        String view = attribute.getOwnerElement().getTagName();
        String value = attribute.getValue();
        String id = null;
        if (value.startsWith(ID_PREFIX)) {
            id = value.substring(ID_PREFIX.length());
        } else if (value.startsWith(NEW_ID_PREFIX)) {
            id = value.substring(NEW_ID_PREFIX.length());
        } // else: could be @android id

        if (id != null) {
            if (view.equals(VIEW_TAG)) {
                view = attribute.getOwnerElement().getAttribute(ATTR_CLASS);
            }

            String existing = mIdToViewTag.get(id);
            if (existing != null && !existing.equals(view)) {
                view = IGNORE;
            }
            mIdToViewTag.put(id, view);
        }
    }

    // ---- Implements Detector.JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("findViewById"); //$NON-NLS-1$
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        assert node.astName().getDescription().equals("findViewById");
        if (node.getParent() instanceof Cast) {
            Cast cast = (Cast) node.getParent();
            String castType = cast.astTypeReference().getTypeName();
            StrictListAccessor<Expression, MethodInvocation> args = node.astArguments();
            if (args.size() == 1) {
                Expression first = args.first();
                // TODO: Do flow analysis as in the StringFormatDetector in order
                // to handle variable references too
                if (first instanceof Select) {
                    String resource = first.toString();
                    if (resource.startsWith("R.id.")) { //$NON-NLS-1$
                        String id = ((Select) first).astIdentifier().astValue();
                        String layoutType = mIdToViewTag.get(id);
                        if (layoutType != null) {
                            checkCompatible(context, castType, layoutType, cast);
                        }
                    }
                }
            }
        }
    }

    /** Check if the view and cast type are compatible */
    private void checkCompatible(JavaContext context, String castType, String layoutType,
            Cast node) {
        if (layoutType != null && !layoutType.equals(IGNORE) && !layoutType.equals(castType)) {
            if (!context.getSdkInfo().isSubViewOf(castType, layoutType)) {
                String message = String.format(
                        "Unexpected cast to %1$s: layout tag was %2$s",
                        castType, layoutType);
                context.report(ISSUE, node, context.parser.getLocation(context, node), message,
                        null);
            }
        }
    }
}
