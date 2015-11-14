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
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import java.io.File;
import java.util.Collections;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.ConstructorDeclaration;
import lombok.ast.Expression;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.IntegralLiteral;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Return;
import lombok.ast.StrictListAccessor;

/** Detector looking for Toast.makeText() without a corresponding show() call */
public class ToastDetector extends Detector implements Detector.JavaScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "ShowToast", //$NON-NLS-1$
            "Looks for code creating a Toast but forgetting to call show() on it",

            "`Toast.makeText()` creates a `Toast` but does *not* show it. You must call " +
            "`show()` on the resulting object to actually make the `Toast` appear.",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            ToastDetector.class,
            Scope.JAVA_FILE_SCOPE);


    /** Constructs a new {@link ToastDetector} check */
    public ToastDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }


    // ---- Implements JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("makeText"); //$NON-NLS-1$
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
        assert node.astName().astValue().equals("makeText");
        if (node.astOperand() == null) {
            // "makeText()" in the code with no operand
            return;
        }

        String operand = node.astOperand().toString();
        if (!(operand.equals("Toast") || operand.endsWith(".Toast"))) {
            return;
        }

        // Make sure you pass the right kind of duration: it's not a delay, it's
        //  LENGTH_SHORT or LENGTH_LONG
        // (see http://code.google.com/p/android/issues/detail?id=3655)
        StrictListAccessor<Expression, MethodInvocation> args = node.astArguments();
        if (args.size() == 3) {
            Expression duration = args.last();
            if (duration instanceof IntegralLiteral) {
                context.report(ISSUE, context.getLocation(duration),
                        "Expected duration Toast.LENGTH_SHORT or Toast.LENGTH_LONG, a custom " +
                        "duration value is not supported",
                        null);
            }
        }

        Node method = findSurroundingMethod(node.getParent());
        if (method == null) {
            return;
        }

        ShowFinder finder = new ShowFinder(node);
        method.accept(finder);
        if (!finder.isShowCalled()) {
            context.report(ISSUE, method, context.getLocation(node),
                    "Toast created but not shown: did you forget to call show() ?", null);
        }
    }

    private class ShowFinder extends ForwardingAstVisitor {
        /** Whether we've found the show method */
        private boolean mFound;
        /** The target makeText call */
        private MethodInvocation mTarget;
        /** Whether we've seen the target makeText node yet */
        private boolean mSeenTarget;

        private ShowFinder(MethodInvocation target) {
            mTarget = target;
        }

        @Override
        public boolean visitMethodInvocation(MethodInvocation node) {
            if (node == mTarget) {
                mSeenTarget = true;
            } else if ((mSeenTarget || node.astOperand() == mTarget)
                    && "show".equals(node.astName().astValue())) { //$NON-NLS-1$
                // TODO: Do more flow analysis to see whether we're really calling show
                // on the right type of object?
                mFound = true;
            }

            return true;
        }

        @Override
        public boolean visitReturn(Return node) {
            if (node.astValue() == mTarget) {
                // If you just do "return Toast.makeText(...) don't warn
                mFound = true;
            }
            return super.visitReturn(node);
        }

        boolean isShowCalled() {
            return mFound;
        }
    }
}
