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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.LintDriver;
import com.google.common.annotations.Beta;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.MethodInvocation;

/**
 * A detector is able to find a particular problem. It might also be thought of as enforcing
 * a rule, but "rule" is a bit overloaded in ADT terminology since ViewRules are used in
 * the Rules API to allow views to specify designtime behavior in the graphical layout editor.
 * <p>
 * Each detector provides information about the issues it can find, such as an explanation
 * of how to fix the issue, the priority, the category, etc. It also has an id which is
 * used to persistently identify a particular type of error.
 * <p>
 * Detectors will be called in a predefined order:
 * <ol>
 *   <li> Manifest file
 *   <li> Resource files, in alphabetical order by resource type
 *        (therefore, "layout" is checked before "values", "values-de" is checked before
 *        "values-en" but after "values", and so on.
 *   <li> Java sources
 *   <li> Java classes
 *   <li> Proguard files
 * </ol>
 * If a detector needs information when processing a file type that comes from a type of
 * file later in the order above, they can request a second phase; see
 * {@link LintDriver#requestRepeat}.
 * <p>
 * NOTE: Detectors might be constructed just once and shared between lint runs, so
 * any per-detector state should be initialized and reset via the before/after
 * methods.
 * <p/>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public abstract class Detector {
    /** Specialized interface for detectors that scan Java source file parse trees */
    public interface JavaScanner  {
        /**
         * Create a parse tree visitor to process the parse tree. All
         * {@link JavaScanner} detectors must provide a visitor, unless they
         * either return true from {@link #appliesToResourceRefs()} or return
         * non null from {@link #getApplicableMethodNames()}.
         * <p>
         * If you return specific AST node types from
         * {@link #getApplicableNodeTypes()}, then the visitor will <b>only</b>
         * be called for the specific requested node types. This is more
         * efficient, since it allows many detectors that apply to only a small
         * part of the AST (such as method call nodes) to share iteration of the
         * majority of the parse tree.
         * <p>
         * If you return null from {@link #getApplicableNodeTypes()}, then your
         * visitor will be called from the top and all node types visited.
         * <p>
         * Note that a new visitor is created for each separate compilation
         * unit, so you can store per file state in the visitor.
         *
         * @param context the {@link Context} for the file being analyzed
         * @return a visitor, or null.
         */
        @Nullable
        AstVisitor createJavaVisitor(@NonNull JavaContext context);

        /**
         * Return the types of AST nodes that the visitor returned from
         * {@link #createJavaVisitor(JavaContext)} should visit. See the
         * documentation for {@link #createJavaVisitor(JavaContext)} for details
         * on how the shared visitor is used.
         * <p>
         * If you return null from this method, then the visitor will process
         * the full tree instead.
         * <p>
         * Note that for the shared visitor, the return codes from the visit
         * methods are ignored: returning true will <b>not</b> prune iteration
         * of the subtree, since there may be other node types interested in the
         * children. If you need to ensure that your visitor only processes a
         * part of the tree, use a full visitor instead. See the
         * OverdrawDetector implementation for an example of this.
         *
         * @return the list of applicable node types (AST node classes), or null
         */
        @Nullable
        List<Class<? extends lombok.ast.Node>> getApplicableNodeTypes();

        /**
         * Return the list of method names this detector is interested in, or
         * null. If this method returns non-null, then any AST nodes that match
         * a method call in the list will be passed to the
         * {@link #visitMethod(JavaContext, AstVisitor, MethodInvocation)}
         * method for processing. The visitor created by
         * {@link #createJavaVisitor(JavaContext)} is also passed to that
         * method, although it can be null.
         * <p>
         * This makes it easy to write detectors that focus on some fixed calls.
         * For example, the StringFormatDetector uses this mechanism to look for
         * "format" calls, and when found it looks around (using the AST's
         * {@link lombok.ast.Node#getParent()} method) to see if it's called on
         * a String class instance, and if so do its normal processing. Note
         * that since it doesn't need to do any other AST processing, that
         * detector does not actually supply a visitor.
         *
         * @return a set of applicable method names, or null.
         */
        @Nullable
        List<String> getApplicableMethodNames();

        /**
         * Method invoked for any method calls found that matches any names
         * returned by {@link #getApplicableMethodNames()}. This also passes
         * back the visitor that was created by
         * {@link #createJavaVisitor(JavaContext)}, but a visitor is not
         * required. It is intended for detectors that need to do additional AST
         * processing, but also want the convenience of not having to look for
         * method names on their own.
         *
         * @param context the context of the lint request
         * @param visitor the visitor created from
         *            {@link #createJavaVisitor(JavaContext)}, or null
         * @param node the {@link MethodInvocation} node for the invoked method
         */
        void visitMethod(
                @NonNull JavaContext context,
                @Nullable AstVisitor visitor,
                @NonNull MethodInvocation node);

        /**
         * Returns whether this detector cares about Android resource references
         * (such as {@code R.layout.main} or {@code R.string.app_name}). If it
         * does, then the visitor will look for these patterns, and if found, it
         * will invoke {@link #visitResourceReference} passing the resource type
         * and resource name. It also passes the visitor, if any, that was
         * created by {@link #createJavaVisitor(JavaContext)}, such that a
         * detector can do more than just look for resources.
         *
         * @return true if this detector wants to be notified of R resource
         *         identifiers found in the code.
         */
        boolean appliesToResourceRefs();

        /**
         * Called for any resource references (such as {@code R.layout.main}
         * found in Java code, provided this detector returned {@code true} from
         * {@link #appliesToResourceRefs()}.
         *
         * @param context the lint scanning context
         * @param visitor the visitor created from
         *            {@link #createJavaVisitor(JavaContext)}, or null
         * @param node the variable reference for the resource
         * @param type the resource type, such as "layout" or "string"
         * @param name the resource name, such as "main" from
         *            {@code R.layout.main}
         * @param isFramework whether the resource is a framework resource
         *            (android.R) or a local project resource (R)
         */
        void visitResourceReference(
                @NonNull JavaContext context,
                @Nullable AstVisitor visitor,
                @NonNull lombok.ast.Node node,
                @NonNull String type,
                @NonNull String name,
                boolean isFramework);
    }

    /** Specialized interface for detectors that scan Java class files */
    public interface ClassScanner  {
        /**
         * Checks the given class' bytecode for issues.
         *
         * @param context the context of the lint check, pointing to for example
         *            the file
         * @param classNode the root class node
         */
        void checkClass(@NonNull ClassContext context, @NonNull ClassNode classNode);

        /**
         * Returns the list of node types (corresponding to the constants in the
         * {@link AbstractInsnNode} class) that this scanner applies to. The
         * {@link #checkInstruction(ClassContext, ClassNode, MethodNode, AbstractInsnNode)}
         * method will be called for each match.
         *
         * @return an array containing all the node types this detector should be
         *         called for, or null if none.
         */
        @Nullable
        int[] getApplicableAsmNodeTypes();

        /**
         * Process a given instruction node, and register lint issues if
         * applicable.
         *
         * @param context the context of the lint check, pointing to for example
         *            the file
         * @param classNode the root class node
         * @param method the method node containing the call
         * @param instruction the actual instruction
         */
        void checkInstruction(@NonNull ClassContext context, @NonNull ClassNode classNode,
                @NonNull MethodNode method, @NonNull AbstractInsnNode instruction);

        /**
         * Return the list of method call names (in VM format, e.g. "<init>" for
         * constructors, etc) for method calls this detector is interested in,
         * or null. T his will be used to dispatch calls to
         * {@link #checkCall(ClassContext, ClassNode, MethodNode, MethodInsnNode)}
         * for only the method calls in owners that the detector is interested
         * in.
         * <p>
         * <b>NOTE</b>: If you return non null from this method, then <b>only</b>
         * {@link #checkCall(ClassContext, ClassNode, MethodNode, MethodInsnNode)}
         * will be called if a suitable method is found;
         * {@link #checkClass(ClassContext, ClassNode)} will not be called under
         * any circumstances.
         * <p>
         * This makes it easy to write detectors that focus on some fixed calls,
         * and allows lint to make a single pass over the bytecode over a class,
         * and efficiently dispatch method calls to any detectors that are
         * interested in it. Without this, each new lint check interested in a
         * single method, would be doing a complete pass through all the
         * bytecode instructions of the class via the
         * {@link #checkClass(ClassContext, ClassNode)} method, which would make
         * each newly added lint check make lint slower. Now a single dispatch
         * map is used instead, and for each encountered call in the single
         * dispatch, it looks up in the map which if any detectors are
         * interested in the given call name, and dispatches to each one in
         * turn.
         *
         * @return a list of applicable method names, or null.
         */
        @Nullable
        List<String> getApplicableCallNames();

        /**
         * Just like {@link Detector#getApplicableCallNames()}, but for the owner
         * field instead. The
         * {@link #checkCall(ClassContext, ClassNode, MethodNode, MethodInsnNode)}
         * method will be called for all {@link MethodInsnNode} instances where the
         * owner field matches any of the members returned in this node.
         * <p>
         * Note that if your detector provides both a name and an owner, the
         * method will be called for any nodes matching either the name <b>or</b>
         * the owner, not only where they match <b>both</b>. Note also that it will
         * be called twice - once for the name match, and (at least once) for the owner
         * match.
         *
         * @return a list of applicable owner names, or null.
         */
        @Nullable
        List<String> getApplicableCallOwners();

        /**
         * Process a given method call node, and register lint issues if
         * applicable. This is similar to the
         * {@link #checkInstruction(ClassContext, ClassNode, MethodNode, AbstractInsnNode)}
         * method, but has the additional advantage that it is only called for known
         * method names or method owners, according to
         * {@link #getApplicableCallNames()} and {@link #getApplicableCallOwners()}.
         *
         * @param context the context of the lint check, pointing to for example
         *            the file
         * @param classNode the root class node
         * @param method the method node containing the call
         * @param call the actual method call node
         */
        void checkCall(@NonNull ClassContext context, @NonNull ClassNode classNode,
                @NonNull MethodNode method, @NonNull MethodInsnNode call);
    }

    /** Specialized interface for detectors that scan XML files */
    public interface XmlScanner {
        /**
         * Visit the given document. The detector is responsible for its own iteration
         * through the document.
         * @param context information about the document being analyzed
         * @param document the document to examine
         */
        void visitDocument(@NonNull XmlContext context, @NonNull Document document);

        /**
         * Visit the given element.
         * @param context information about the document being analyzed
         * @param element the element to examine
         */
        void visitElement(@NonNull XmlContext context, @NonNull Element element);

        /**
         * Visit the given element after its children have been analyzed.
         * @param context information about the document being analyzed
         * @param element the element to examine
         */
        void visitElementAfter(@NonNull XmlContext context, @NonNull Element element);

        /**
         * Visit the given attribute.
         * @param context information about the document being analyzed
         * @param attribute the attribute node to examine
         */
        void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute);

        /**
         * Returns the list of elements that this detector wants to analyze. If non
         * null, this detector will be called (specifically, the
         * {@link #visitElement} method) for each matching element in the document.
         * <p>
         * If this method returns null, and {@link #getApplicableAttributes()} also returns
         * null, then the {@link #visitDocument} method will be called instead.
         *
         * @return a collection of elements, or null, or the special
         *         {@link XmlScanner#ALL} marker to indicate that every single
         *         element should be analyzed.
         */
        @Nullable
        Collection<String> getApplicableElements();

        /**
         * Returns the list of attributes that this detector wants to analyze. If non
         * null, this detector will be called (specifically, the
         * {@link #visitAttribute} method) for each matching attribute in the document.
         * <p>
         * If this method returns null, and {@link #getApplicableElements()} also returns
         * null, then the {@link #visitDocument} method will be called instead.
         *
         * @return a collection of attributes, or null, or the special
         *         {@link XmlScanner#ALL} marker to indicate that every single
         *         attribute should be analyzed.
         */
        @Nullable
        Collection<String> getApplicableAttributes();

        /**
         * Special marker collection returned by {@link #getApplicableElements()} or
         * {@link #getApplicableAttributes()} to indicate that the check should be
         * invoked on all elements or all attributes
         */
        @NonNull
        public static final List<String> ALL = new ArrayList<String>(0); // NOT Collections.EMPTY!
        // We want to distinguish this from just an *empty* list returned by the caller!
    }

    /**
     * Runs the detector. This method will not be called for certain specialized
     * detectors, such as {@link XmlScanner} and {@link JavaScanner}, where
     * there are specialized analysis methods instead such as
     * {@link XmlScanner#visitElement(XmlContext, Element)}.
     *
     * @param context the context describing the work to be done
     */
    public void run(@NonNull Context context) {
    }

    /**
     * Returns true if this detector applies to the given file
     *
     * @param context the context to check
     * @param file the file in the context to check
     * @return true if this detector applies to the given context and file
     */
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return false;
    }

    /**
     * Analysis is about to begin, perform any setup steps.
     *
     * @param context the context for the check referencing the project, lint
     *            client, etc
     */
    public void beforeCheckProject(@NonNull Context context) {
    }

    /**
     * Analysis has just been finished for the whole project, perform any
     * cleanup or report issues that require project-wide analysis.
     *
     * @param context the context for the check referencing the project, lint
     *            client, etc
     */
    public void afterCheckProject(@NonNull Context context) {
    }

    /**
     * Analysis is about to begin for the given library project, perform any setup steps.
     *
     * @param context the context for the check referencing the project, lint
     *            client, etc
     */
    public void beforeCheckLibraryProject(@NonNull Context context) {
    }

    /**
     * Analysis has just been finished for the given library project, perform any
     * cleanup or report issues that require library-project-wide analysis.
     *
     * @param context the context for the check referencing the project, lint
     *            client, etc
     */
    public void afterCheckLibraryProject(@NonNull Context context) {
    }

    /**
     * Analysis is about to be performed on a specific file, perform any setup
     * steps.
     * <p>
     * Note: When this method is called at the beginning of checking an XML
     * file, the context is guaranteed to be an instance of {@link XmlContext},
     * and similarly for a Java source file, the context will be a
     * {@link JavaContext} and so on.
     *
     * @param context the context for the check referencing the file to be
     *            checked, the project, etc.
     */
    public void beforeCheckFile(@NonNull Context context) {
    }

    /**
     * Analysis has just been finished for a specific file, perform any cleanup
     * or report issues found
     * <p>
     * Note: When this method is called at the end of checking an XML
     * file, the context is guaranteed to be an instance of {@link XmlContext},
     * and similarly for a Java source file, the context will be a
     * {@link JavaContext} and so on.
     *
     * @param context the context for the check referencing the file to be
     *            checked, the project, etc.
     */
    public void afterCheckFile(@NonNull Context context) {
    }

    /**
     * Returns the expected speed of this detector
     *
     * @return the expected speed of this detector
     */
    @NonNull
    public Speed getSpeed() {
        return Speed.NORMAL;
    }

    // ---- Dummy implementations to make implementing XmlScanner easier: ----

    @SuppressWarnings("javadoc")
    public void visitDocument(@NonNull XmlContext context, @NonNull Document document) {
        // This method must be overridden if your detector does
        // not return something from getApplicableElements or
        // getApplicableATtributes
        assert false;
    }

    @SuppressWarnings("javadoc")
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        // This method must be overridden if your detector returns
        // tag names from getApplicableElements
        assert false;
    }

    @SuppressWarnings("javadoc")
    public void visitElementAfter(@NonNull XmlContext context, @NonNull Element element) {
    }

    @SuppressWarnings("javadoc")
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        // This method must be overridden if your detector returns
        // attribute names from getApplicableAttributes
        assert false;
    }

    @SuppressWarnings("javadoc")
    @Nullable
    public Collection<String> getApplicableElements() {
        return null;
    }

    @Nullable
    @SuppressWarnings("javadoc")
    public Collection<String> getApplicableAttributes() {
        return null;
    }

    // ---- Dummy implementations to make implementing JavaScanner easier: ----

    @Nullable @SuppressWarnings("javadoc")
    public List<String> getApplicableMethodNames() {
        return null;
    }

    @Nullable @SuppressWarnings("javadoc")
    public AstVisitor createJavaVisitor(@NonNull JavaContext context) {
        return null;
    }

    @Nullable @SuppressWarnings("javadoc")
    public List<Class<? extends lombok.ast.Node>> getApplicableNodeTypes() {
        return null;
    }

    @SuppressWarnings("javadoc")
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {
    }

    @SuppressWarnings("javadoc")
    public boolean appliesToResourceRefs() {
        return false;
    }

    @SuppressWarnings("javadoc")
    public void visitResourceReference(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull lombok.ast.Node node, @NonNull String type, @NonNull String name,
            boolean isFramework) {
    }

    // ---- Dummy implementations to make implementing a ClassScanner easier: ----

    @SuppressWarnings("javadoc")
    public void checkClass(@NonNull ClassContext context, @NonNull ClassNode classNode) {
    }

    @SuppressWarnings("javadoc")
    @Nullable
    public List<String> getApplicableCallNames() {
        return null;
    }

    @SuppressWarnings("javadoc")
    @Nullable
    public List<String> getApplicableCallOwners() {
        return null;
    }

    @SuppressWarnings("javadoc")
    public void checkCall(@NonNull ClassContext context, @NonNull ClassNode classNode,
            @NonNull MethodNode method, @NonNull MethodInsnNode call) {
    }

    @SuppressWarnings("javadoc")
    @Nullable
    public int[] getApplicableAsmNodeTypes() {
        return null;
    }

    @SuppressWarnings("javadoc")
    public void checkInstruction(@NonNull ClassContext context, @NonNull ClassNode classNode,
            @NonNull MethodNode method, @NonNull AbstractInsnNode instruction) {
    }

}
