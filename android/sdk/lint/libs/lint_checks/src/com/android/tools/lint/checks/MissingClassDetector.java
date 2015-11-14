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
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_PACKAGE;
import static com.android.SdkConstants.CONSTRUCTOR_NAME;
import static com.android.SdkConstants.TAG_ACTIVITY;
import static com.android.SdkConstants.TAG_APPLICATION;
import static com.android.SdkConstants.TAG_PROVIDER;
import static com.android.SdkConstants.TAG_RECEIVER;
import static com.android.SdkConstants.TAG_SERVICE;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector.ClassScanner;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Location.Handle;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.collect.Maps;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Checks to ensure that classes referenced in the manifest actually exist and are included
 *
 */
public class MissingClassDetector extends LayoutDetector implements ClassScanner {
    /** Manifest-referenced classes missing from the project or libraries */
    public static final Issue MISSING = Issue.create(
        "MissingRegistered", //$NON-NLS-1$
        "Ensures that classes referenced in the manifest are present in the project or libraries",

        "If a class is referenced in the manifest, it must also exist in the project (or in one " +
        "of the libraries included by the project. This check helps uncover typos in " +
        "registration names, or attempts to rename or move classes without updating the " +
        "manifest file properly.",

        Category.CORRECTNESS,
        8,
        Severity.ERROR,
        MissingClassDetector.class,
        EnumSet.of(Scope.MANIFEST, Scope.CLASS_FILE, Scope.JAVA_LIBRARIES)).setMoreInfo(
        "http://developer.android.com/guide/topics/manifest/manifest-intro.html"); //$NON-NLS-1$

    /** Are activity, service, receiver etc subclasses instantiatable? */
    public static final Issue INSTANTIATABLE = Issue.create(
        "Instantiatable", //$NON-NLS-1$
        "Ensures that classes registered in the manifest file are instantiatable",

        "Activities, services, broadcast receivers etc. registered in the manifest file " +
        "must be \"instiantable\" by the system, which means that the class must be " +
        "public, it must have an empty public constructor, and if it's an inner class, " +
        "it must be a static inner class.",

        Category.CORRECTNESS,
        6,
        Severity.WARNING,
        MissingClassDetector.class,
        Scope.CLASS_FILE_SCOPE);

    /** Is the right character used for inner class separators? */
    public static final Issue INNERCLASS = Issue.create(
        "InnerclassSeparator", //$NON-NLS-1$
        "Ensures that inner classes are referenced using '$' instead of '.' in class names",

        "When you reference an inner class in a manifest file, you must use '$' instead of '.' " +
        "as the separator character, e.g. Outer$Inner instead of Outer.Inner.\n" +
        "\n" +
        "(If you get this warning for a class which is not actually an inner class, it's " +
        "because you are using uppercase characters in your package name, which is not " +
        "conventional.)",

        Category.CORRECTNESS,
        3,
        Severity.WARNING,
        MissingClassDetector.class,
        Scope.MANIFEST_SCOPE);

    private Map<String, Location.Handle> mReferencedClasses;

    /** Constructs a new {@link MissingClassDetector} */
    public MissingClassDetector() {
    }

    @Override
    public @NonNull Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements XmlScanner ----

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                TAG_APPLICATION,
                TAG_ACTIVITY,
                TAG_SERVICE,
                TAG_RECEIVER,
                TAG_PROVIDER
        );
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {

        Element root = element.getOwnerDocument().getDocumentElement();
        Attr classNameNode = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
        if (classNameNode == null) {
            return;
        }
        String className = classNameNode.getValue();
        if (className.isEmpty()) {
            return;
        }

        String pkg = root.getAttribute(ATTR_PACKAGE);
        String fqcn;
        if (className.startsWith(".")) { //$NON-NLS-1$
            fqcn = pkg + className;
        } else if (className.indexOf('.') == -1) {
            // According to the <activity> manifest element documentation, this is not
            // valid ( http://developer.android.com/guide/topics/manifest/activity-element.html )
            // but it appears in manifest files and appears to be supported by the runtime
            // so handle this in code as well:
            fqcn = pkg + '.' + className;
        } else { // else: the class name is already a fully qualified class name
            fqcn = className;
        }

        String signature = ClassContext.getInternalName(fqcn);
        if (signature.isEmpty() || signature.startsWith(ANDROID_PKG_PREFIX)) {
            return;
        }

        if (mReferencedClasses == null) {
            mReferencedClasses = Maps.newHashMapWithExpectedSize(16);
        }

        Handle handle = context.parser.createLocationHandle(context, element);
        mReferencedClasses.put(signature, handle);

        if (signature.indexOf('$') != -1) {
            if (className.indexOf('$') == -1 && className.indexOf('.', 1) > 0) {
                boolean haveUpperCase = false;
                for (int i = 0, n = pkg.length(); i < n; i++) {
                    if (Character.isUpperCase(pkg.charAt(i))) {
                        haveUpperCase = true;
                        break;
                    }
                }
                if (!haveUpperCase) {
                    String message = String.format("Use '$' instead of '.' for inner classes " +
                            "(or use only lowercase letters in package names)", className);
                    Location location = context.getLocation(classNameNode);
                    context.report(INNERCLASS, element, location, message, null);
                }
            }

            // The internal name contains a $ which means it's an inner class.
            // The conversion from fqcn to internal name is a bit ambiguous:
            // "a.b.C.D" usually means "inner class D in class C in package a.b".
            // However, it can (see issue 31592) also mean class D in package "a.b.C".
            // To make sure we don't falsely complain that foo/Bar$Baz doesn't exist,
            // in case the user has actually created a package named foo/Bar and a proper
            // class named Baz, we register *both* into the reference map.
            // When generating errors we'll look for these an rip them back out if
            // it looks like one of the two variations have been seen.
            signature = signature.replace('$', '/');
            mReferencedClasses.put(signature, handle);
        }
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (!context.getProject().isLibrary()
                && mReferencedClasses != null && !mReferencedClasses.isEmpty()
                && context.getDriver().getScope().contains(Scope.CLASS_FILE)) {
            List<String> classes = new ArrayList<String>(mReferencedClasses.keySet());
            Collections.sort(classes);
            for (String owner : classes) {
                Location.Handle handle = mReferencedClasses.get(owner);
                String fqcn = ClassContext.getFqcn(owner);

                String signature = ClassContext.getInternalName(fqcn);
                if (!signature.equals(owner)) {
                    if (!mReferencedClasses.containsKey(signature)) {
                        continue;
                    }
                } else {
                    signature = signature.replace('$', '/');
                    if (!mReferencedClasses.containsKey(signature)) {
                        continue;
                    }
                }
                mReferencedClasses.remove(owner);

                String message = String.format(
                        "Class referenced in the manifest, %1$s, was not found in the " +
                        "project or the libraries", fqcn);
                Location location = handle.resolve();
                context.report(MISSING, location, message, null);
            }
        }
    }

    // ---- Implements ClassScanner ----

    @Override
    public void checkClass(@NonNull ClassContext context, @NonNull ClassNode classNode) {
        String curr = classNode.name;
        if (mReferencedClasses != null && mReferencedClasses.containsKey(curr)) {
            mReferencedClasses.remove(curr);

            // Ensure that the class is public, non static and has a null constructor!

            if ((classNode.access & Opcodes.ACC_PUBLIC) == 0) {
                context.report(INSTANTIATABLE, context.getLocation(classNode), String.format(
                        "This class should be public (%1$s)",
                            ClassContext.createSignature(classNode.name, null, null)),
                        null);
                return;
            }

            if (classNode.name.indexOf('$') != -1 && !LintUtils.isStaticInnerClass(classNode)) {
                context.report(INSTANTIATABLE, context.getLocation(classNode), String.format(
                        "This inner class should be static (%1$s)",
                            ClassContext.createSignature(classNode.name, null, null)),
                        null);
                return;
            }

            boolean hasDefaultConstructor = false;
            @SuppressWarnings("rawtypes") // ASM API
            List methodList = classNode.methods;
            for (Object m : methodList) {
                MethodNode method = (MethodNode) m;
                if (method.name.equals(CONSTRUCTOR_NAME)) {
                    if (method.desc.equals("()V")) { //$NON-NLS-1$
                        // The constructor must be public
                        if ((method.access & Opcodes.ACC_PUBLIC) != 0) {
                            hasDefaultConstructor = true;
                        } else {
                            context.report(INSTANTIATABLE, context.getLocation(method, classNode),
                                    "The default constructor must be public",
                                    null);
                            // Also mark that we have a constructor so we don't complain again
                            // below since we've already emitted a more specific error related
                            // to the default constructor
                            hasDefaultConstructor = true;
                        }
                    }
                }
            }

            if (!hasDefaultConstructor) {
                context.report(INSTANTIATABLE, context.getLocation(classNode), String.format(
                        "This class should provide a default constructor (a public " +
                        "constructor with no arguments) (%1$s)",
                            ClassContext.createSignature(classNode.name, null, null)),
                        null);
            }
        }
    }
}
