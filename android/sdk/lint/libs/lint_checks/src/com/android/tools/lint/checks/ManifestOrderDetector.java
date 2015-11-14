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

import static com.android.SdkConstants.ANDROID_MANIFEST_XML;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_MIN_SDK_VERSION;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_PACKAGE;
import static com.android.SdkConstants.ATTR_TARGET_SDK_VERSION;
import static com.android.SdkConstants.TAG_ACTIVITY;
import static com.android.SdkConstants.TAG_APPLICATION;
import static com.android.SdkConstants.TAG_PROVIDER;
import static com.android.SdkConstants.TAG_RECEIVER;
import static com.android.SdkConstants.TAG_SERVICE;
import static com.android.SdkConstants.TAG_USES_LIBRARY;
import static com.android.SdkConstants.TAG_USES_PERMISSION;
import static com.android.SdkConstants.TAG_USES_SDK;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Checks for issues in AndroidManifest files such as declaring elements in the
 * wrong order.
 */
public class ManifestOrderDetector extends Detector implements Detector.XmlScanner {

    /** Wrong order of elements in the manifest */
    public static final Issue ORDER = Issue.create(
            "ManifestOrder", //$NON-NLS-1$
            "Checks for manifest problems like <uses-sdk> after the <application> tag",
            "The <application> tag should appear after the elements which declare " +
            "which version you need, which features you need, which libraries you " +
            "need, and so on. In the past there have been subtle bugs (such as " +
            "themes not getting applied correctly) when the `<application>` tag appears " +
            "before some of these other elements, so it's best to order your " +
            "manifest in the logical dependency order.",
            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            ManifestOrderDetector.class,
            Scope.MANIFEST_SCOPE);

    /** Missing a {@code <uses-sdk>} element */
    public static final Issue USES_SDK = Issue.create(
            "UsesMinSdkAttributes", //$NON-NLS-1$
            "Checks that the minimum SDK and target SDK attributes are defined",

            "The manifest should contain a `<uses-sdk>` element which defines the " +
            "minimum minimum API Level required for the application to run, " +
            "as well as the target version (the highest API level you have tested " +
            "the version for.)",

            Category.CORRECTNESS,
            9,
            Severity.WARNING,
            ManifestOrderDetector.class,
            Scope.MANIFEST_SCOPE).setMoreInfo(
            "http://developer.android.com/guide/topics/manifest/uses-sdk-element.html"); //$NON-NLS-1$

    /** Using a targetSdkVersion that isn't recent */
    public static final Issue TARGET_NEWER = Issue.create(
            "OldTargetApi", //$NON-NLS-1$
            "Checks that the manifest specifies a targetSdkVersion that is recent",

            "When your application runs on a version of Android that is more recent than your " +
            "`targetSdkVersion` specifies that it has been tested with, various compatibility " +
            "modes kick in. This ensures that your application continues to work, but it may " +
            "look out of place. For example, if the `targetSdkVersion` is less than 14, your " +
            "app may get an option button in the UI.\n" +
            "\n" +
            "To fix this issue, set the `targetSdkVersion` to the highest available value. Then " +
            "test your app to make sure everything works correctly. You may want to consult " +
            "the compatibility notes to see what changes apply to each version you are adding " +
            "support for: " +
            "http://developer.android.com/reference/android/os/Build.VERSION_CODES.html",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            ManifestOrderDetector.class,
            Scope.MANIFEST_SCOPE).setMoreInfo(
            "http://developer.android.com/reference/android/os/Build.VERSION_CODES.html"); //$NON-NLS-1$

    /** Using multiple {@code <uses-sdk>} elements */
    public static final Issue MULTIPLE_USES_SDK = Issue.create(
            "MultipleUsesSdk", //$NON-NLS-1$
            "Checks that the <uses-sdk> element appears at most once",

            "The `<uses-sdk>` element should appear just once; the tools will *not* merge the " +
            "contents of all the elements so if you split up the atttributes across multiple " +
            "elements, only one of them will take effect. To fix this, just merge all the " +
            "attributes from the various elements into a single <uses-sdk> element.",

            Category.CORRECTNESS,
            6,
            Severity.FATAL,
            ManifestOrderDetector.class,
            Scope.MANIFEST_SCOPE).setMoreInfo(
            "http://developer.android.com/guide/topics/manifest/uses-sdk-element.html"); //$NON-NLS-1$

    /** Missing a {@code <uses-sdk>} element */
    public static final Issue WRONG_PARENT = Issue.create(
            "WrongManifestParent", //$NON-NLS-1$
            "Checks that various manifest elements are declared in the right place",

            "The `<uses-library>` element should be defined as a direct child of the " +
            "`<application>` tag, not the `<manifest>` tag or an `<activity>` tag. Similarly, " +
            "a `<uses-sdk>` tag much be declared at the root level, and so on. This check " +
            "looks for incorrect declaration locations in the manifest, and complains " +
            "if an element is found in the wrong place.",

            Category.CORRECTNESS,
            6,
            Severity.FATAL,
            ManifestOrderDetector.class,
            EnumSet.of(Scope.MANIFEST)).setMoreInfo(
            "http://developer.android.com/guide/topics/manifest/manifest-intro.html"); //$NON-NLS-1$

    /** Missing a {@code <uses-sdk>} element */
    public static final Issue DUPLICATE_ACTIVITY = Issue.create(
            "DuplicateActivity", //$NON-NLS-1$
            "Checks that an activity is registered only once in the manifest",

            "An activity should only be registered once in the manifest. If it is " +
            "accidentally registered more than once, then subtle errors can occur, " +
            "since attribute declarations from the two elements are not merged, so " +
            "you may accidentally remove previous declarations.",

            Category.CORRECTNESS,
            5,
            Severity.ERROR,
            ManifestOrderDetector.class,
            EnumSet.of(Scope.MANIFEST));

    /** Not explicitly defining allowBackup */
    public static final Issue ALLOW_BACKUP = Issue.create(
            "AllowBackup", //$NON-NLS-1$
            "Ensure that allowBackup is explicitly set in the application's manifest",

            "The allowBackup attribute determines if an application's data can be backed up " +
            "and restored. It is documented at " +
            "http://developer.android.com/reference/android/R.attr.html#allowBackup\n" +
            "\n" +
            "By default, this flag is set to `true`. When this flag is set to `true`, " +
            "application data can be backed up and restored by the user using `adb backup` " +
            "and `adb restore`.\n" +
            "\n" +
            "This may have security consequences for an application. `adb backup` allows " +
            "users who have enabled USB debugging to copy application data off of the " +
            "device. Once backed up, all application data can be read by the user. " +
            "`adb restore` allows creation of application data from a source specified " +
            "by the user. Following a restore, applications should not assume that the " +
            "data, file permissions, and directory permissions were created by the " +
            "application itself.\n" +
            "\n" +
            "Setting `allowBackup=\"false\"` opts an application out of both backup and " +
            "restore.\n" +
            "\n" +
            "To fix this warning, decide whether your application should support backup, " +
            "and explicitly set `android:allowBackup=(true|false)\"`",

            Category.SECURITY,
            3,
            Severity.WARNING,
            ManifestOrderDetector.class,
            EnumSet.of(Scope.MANIFEST)).setMoreInfo(
                    "http://developer.android.com/reference/android/R.attr.html#allowBackup");

    /** Constructs a new {@link ManifestOrderDetector} check */
    public ManifestOrderDetector() {
    }

    private boolean mSeenApplication;

    /** Number of times we've seen the <uses-sdk> element */
    private int mSeenUsesSdk;

    /** Activities we've encountered */
    private Set<String> mActivities = new HashSet<String>();

    /** Package declared in the manifest */
    private String mPackage;

    @Override
    public @NonNull Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return file.getName().equals(ANDROID_MANIFEST_XML);
    }

    @Override
    public void beforeCheckFile(@NonNull Context context) {
        mSeenApplication = false;
        mSeenUsesSdk = 0;
    }

    @Override
    public void afterCheckFile(@NonNull Context context) {
        if (mSeenUsesSdk == 0 && context.isEnabled(USES_SDK)) {
            context.report(USES_SDK, Location.create(context.file),
                    "Manifest should specify a minimum API level with " +
                    "<uses-sdk android:minSdkVersion=\"?\" />; if it really supports " +
                    "all versions of Android set it to 1.", null);
        }
    }

    // ---- Implements Detector.XmlScanner ----

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                TAG_APPLICATION,
                TAG_USES_PERMISSION,
                "permission",              //$NON-NLS-1$
                "permission-tree",         //$NON-NLS-1$
                "permission-group",        //$NON-NLS-1$
                TAG_USES_SDK,
                "uses-configuration",      //$NON-NLS-1$
                "uses-feature",            //$NON-NLS-1$
                "supports-screens",        //$NON-NLS-1$
                "compatible-screens",      //$NON-NLS-1$
                "supports-gl-texture",     //$NON-NLS-1$
                TAG_USES_LIBRARY,
                TAG_ACTIVITY,
                TAG_SERVICE,
                TAG_PROVIDER,
                TAG_RECEIVER
        );
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        String tag = element.getTagName();
        Node parentNode = element.getParentNode();

        if (tag.equals(TAG_USES_LIBRARY) || tag.equals(TAG_ACTIVITY) || tag.equals(TAG_SERVICE)
                || tag.equals(TAG_PROVIDER) || tag.equals(TAG_RECEIVER)) {
            if (!TAG_APPLICATION.equals(parentNode.getNodeName())
                    && context.isEnabled(WRONG_PARENT)) {
                context.report(WRONG_PARENT, element, context.getLocation(element),
                        String.format(
                        "The <%1$s> element must be a direct child of the <application> element",
                        tag), null);
            }

            if (tag.equals(TAG_ACTIVITY)) {
                Attr nameNode = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
                if (nameNode != null) {
                    String name = nameNode.getValue();
                    if (!name.isEmpty()) {
                        if (name.charAt(0) == '.') {
                            name = getPackage(element) + name;
                        } else if (name.indexOf('.') == -1) {
                            name = getPackage(element) + '.' + name;
                        }
                        if (mActivities.contains(name)) {
                            String message = String.format(
                                    "Duplicate registration for activity %1$s", name);
                            context.report(DUPLICATE_ACTIVITY, element,
                                    context.getLocation(nameNode), message, null);
                        } else {
                            mActivities.add(name);
                        }
                    }
                }
            }

            return;
        }

        if (parentNode != element.getOwnerDocument().getDocumentElement()
                && context.isEnabled(WRONG_PARENT)) {
            context.report(WRONG_PARENT, element, context.getLocation(element),
                    String.format(
                    "The <%1$s> element must be a direct child of the " +
                    "<manifest> root element", tag), null);
        }

        if (tag.equals(TAG_USES_SDK)) {
            mSeenUsesSdk++;

            if (mSeenUsesSdk == 2) { // Only warn when we encounter the first one
                Location location = context.getLocation(element);

                // Link up *all* encountered locations in the document
                NodeList elements = element.getOwnerDocument().getElementsByTagName(TAG_USES_SDK);
                Location secondary = null;
                for (int i = elements.getLength() - 1; i >= 0; i--) {
                    Element e = (Element) elements.item(i);
                    if (e != element) {
                        Location l = context.getLocation(e);
                        l.setSecondary(secondary);
                        l.setMessage("Also appears here");
                        secondary = l;
                    }
                }
                location.setSecondary(secondary);

                if (context.isEnabled(MULTIPLE_USES_SDK)) {
                    context.report(MULTIPLE_USES_SDK, element, location,
                        "There should only be a single <uses-sdk> element in the manifest:" +
                        " merge these together", null);
                }
                return;
            }

            if (!element.hasAttributeNS(ANDROID_URI, ATTR_MIN_SDK_VERSION)) {
                if (context.isEnabled(USES_SDK)) {
                    context.report(USES_SDK, element, context.getLocation(element),
                        "<uses-sdk> tag should specify a minimum API level with " +
                        "android:minSdkVersion=\"?\"", null);
                }
            }

            if (!element.hasAttributeNS(ANDROID_URI, ATTR_TARGET_SDK_VERSION)) {
                // Warn if not setting target SDK -- but only if the min SDK is somewhat
                // old so there's some compatibility stuff kicking in (such as the menu
                // button etc)
                if (context.isEnabled(USES_SDK)) {
                    context.report(USES_SDK, element, context.getLocation(element),
                        "<uses-sdk> tag should specify a target API level (the " +
                        "highest verified version; when running on later versions, " +
                        "compatibility behaviors may be enabled) with " +
                        "android:targetSdkVersion=\"?\"", null);
                }
            } else if (context.isEnabled(TARGET_NEWER)){
                String target = element.getAttributeNS(ANDROID_URI, ATTR_TARGET_SDK_VERSION);
                try {
                    int api = Integer.parseInt(target);
                    if (api < context.getClient().getHighestKnownApiLevel()) {
                        context.report(TARGET_NEWER, element, context.getLocation(element),
                                "Not targeting the latest versions of Android; compatibility " +
                                "modes apply. Consider testing and updating this version. " +
                                "Consult the android.os.Build.VERSION_CODES javadoc for details.",
                                null);
                    }
                } catch (NumberFormatException nufe) {
                    // Ignore: AAPT will enforce this.
                }
            }
        }

        if (tag.equals(TAG_APPLICATION)) {
            mSeenApplication = true;
            if (!element.hasAttributeNS(ANDROID_URI, SdkConstants.ATTR_ALLOW_BACKUP)
                    && context.isEnabled(ALLOW_BACKUP)) {
                context.report(ALLOW_BACKUP, element, context.getLocation(element),
                        String.format("Should explicitly set android:allowBackup to true or " +
                            "false (it's true by default, and that can have some security " +
                            "implications for the application's data)", tag), null);
            }
        } else if (mSeenApplication) {
            if (context.isEnabled(ORDER)) {
                context.report(ORDER, element, context.getLocation(element),
                    String.format("<%1$s> tag appears after <application> tag", tag), null);
            }

            // Don't complain for *every* element following the <application> tag
            mSeenApplication = false;
        }
    }

    private String getPackage(Element element) {
        if (mPackage == null) {
            return element.getOwnerDocument().getDocumentElement().getAttribute(ATTR_PACKAGE);
        }

        return mPackage;
    }
}
