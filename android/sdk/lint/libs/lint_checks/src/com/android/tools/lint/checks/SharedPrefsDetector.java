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
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import java.io.File;
import java.util.Collections;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.ConstructorDeclaration;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Return;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;

/**
 * Detector looking for SharedPreferences.edit() calls without a corresponding
 * commit() or apply() call
 */
public class SharedPrefsDetector extends Detector implements Detector.JavaScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "CommitPrefEdits", //$NON-NLS-1$
            "Looks for code editing a SharedPreference but forgetting to call commit() on it",

            "After calling `edit()` on a `SharedPreference`, you must call `commit()` " +
            "or `apply()` on the editor to save the results.",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            SharedPrefsDetector.class,
            Scope.JAVA_FILE_SCOPE);

    /** Constructs a new {@link SharedPrefsDetector} check */
    public SharedPrefsDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }


    // ---- Implements JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("edit"); //$NON-NLS-1$
    }

    private Node findSurroundingMethod(Node scope) {
        while (scope != null) {
            Class<? extends Node> type = scope.getClass();
            // The Lombok AST uses a flat hierarchy of node type implementation classes
            // so no need to do instanceof stuff here.
            if (type == MethodDeclaration.class || type == ConstructorDeclaration.class) {
                return scope;
            }

            scope = scope.getParent();
        }

        return null;
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
        assert node.astName().astValue().equals("edit");
        if (node.astOperand() == null) {
            return;
        }

        // Looking for the specific pattern where you assign the edit() result
        // to a local variable; this means we won't recognize some other usages
        // of the API (e.g. assigning it to a previously declared variable) but
        // is needed until we have type attribution in the AST itself.
        if (!(node.getParent() instanceof VariableDefinitionEntry &&
                node.getParent().getParent() instanceof VariableDefinition)) {
            return;
        }
        VariableDefinition definition = (VariableDefinition) node.getParent().getParent();
        String type = definition.astTypeReference().toString();
        if (!type.endsWith("SharedPreferences.Editor")) {                   //$NON-NLS-1$
            if (!type.equals("Editor") ||                                   //$NON-NLS-1$
                    !LintUtils.isImported(context.compilationUnit,
                            "android.content.SharedPreferences.Editor")) {  //$NON-NLS-1$
                return;
            }
        }

        Node method = findSurroundingMethod(node.getParent());
        if (method == null) {
            return;
        }

        CommitFinder finder = new CommitFinder(node);
        method.accept(finder);
        if (!finder.isCommitCalled()) {
            context.report(ISSUE, method, context.getLocation(node),
                    "SharedPreferences.edit() without a corresponding commit() or apply() call",
                    null);
        }
    }

    private class CommitFinder extends ForwardingAstVisitor {
        /** Whether we've found one of the commit/cancel methods */
        private boolean mFound;
        /** The target edit call */
        private MethodInvocation mTarget;
        /** Whether we've seen the target edit node yet */
        private boolean mSeenTarget;

        private CommitFinder(MethodInvocation target) {
            mTarget = target;
        }

        @Override
        public boolean visitMethodInvocation(MethodInvocation node) {
            if (node == mTarget) {
                mSeenTarget = true;
            } else if (mSeenTarget || node.astOperand() == mTarget) {
                String name = node.astName().astValue();
                if ("commit".equals(name) || "apply".equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$
                    // TODO: Do more flow analysis to see whether we're really calling commit/apply
                    // on the right type of object?
                    mFound = true;
                }
            }

            return true;
        }

        @Override
        public boolean visitReturn(Return node) {
            if (node.astValue() == mTarget) {
                // If you just do "return editor.commit() don't warn
                mFound = true;
            }
            return super.visitReturn(node);
        }

        boolean isCommitCalled() {
            return mFound;
        }
    }
}
