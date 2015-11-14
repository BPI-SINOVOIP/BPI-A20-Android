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

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.ClassScanner;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;

import org.objectweb.asm.tree.ClassNode;

/**
 * Checks that Handler implementations are top level classes or static.
 * See the corresponding check in the android.os.Handler source code.
 */
public class HandlerDetector extends Detector implements ClassScanner {

    /** Potentially leaking handlers */
    public static final Issue ISSUE = Issue.create(
        "HandlerLeak", //$NON-NLS-1$
        "Ensures that Handler classes do not hold on to a reference to an outer class",

        "In Android, Handler classes should be static or leaks might occur. " +
        "Messages enqueued on the application thread's MessageQueue also retain their " +
        "target Handler. If the Handler is an inner class, its outer class will be " +
        "retained as well. To avoid leaking the outer class, declare the Handler as a " +
        "static nested class with a WeakReference to its outer class.",

        Category.PERFORMANCE,
        4,
        Severity.WARNING,
        HandlerDetector.class,
        Scope.CLASS_FILE_SCOPE);

    /** Constructs a new {@link HandlerDetector} */
    public HandlerDetector() {
    }

    @Override
    public @NonNull Speed getSpeed() {
        return Speed.FAST;
    }

    // ---- Implements ClassScanner ----

    @Override
    public void checkClass(@NonNull ClassContext context, @NonNull ClassNode classNode) {
        if (classNode.name.indexOf('$') == -1) {
            return;
        }

        if (context.getDriver().isSubclassOf(classNode, "android/os/Handler") //$NON-NLS-1$
                && !LintUtils.isStaticInnerClass(classNode)) {
            Location location = context.getLocation(classNode);
            context.report(ISSUE, location, String.format(
                    "This Handler class should be static or leaks might occur (%1$s)",
                        ClassContext.createSignature(classNode.name, null, null)),
                    null);
        }
    }
}
