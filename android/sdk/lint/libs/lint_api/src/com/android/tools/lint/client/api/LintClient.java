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

package com.android.tools.lint.client.api;

import static com.android.SdkConstants.CLASS_FOLDER;
import static com.android.SdkConstants.DOT_JAR;
import static com.android.SdkConstants.GEN_FOLDER;
import static com.android.SdkConstants.LIBS_FOLDER;
import static com.android.SdkConstants.SRC_FOLDER;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkManager;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Severity;
import com.android.utils.StdLogger;
import com.android.utils.StdLogger.Level;
import com.google.common.annotations.Beta;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Information about the tool embedding the lint analyzer. IDEs and other tools
 * implementing lint support will extend this to integrate logging, displaying errors,
 * etc.
 * <p/>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public abstract class LintClient {
    private static final String PROP_BIN_DIR  = "com.android.tools.lint.bindir";  //$NON-NLS-1$

    /**
     * Returns a configuration for use by the given project. The configuration
     * provides information about which issues are enabled, any customizations
     * to the severity of an issue, etc.
     * <p>
     * By default this method returns a {@link DefaultConfiguration}.
     *
     * @param project the project to obtain a configuration for
     * @return a configuration, never null.
     */
    public Configuration getConfiguration(@NonNull Project project) {
        return DefaultConfiguration.create(this, project, null);
    }

    /**
     * Report the given issue. This method will only be called if the configuration
     * provided by {@link #getConfiguration(Project)} has reported the corresponding
     * issue as enabled and has not filtered out the issue with its
     * {@link Configuration#ignore(Context, Issue, Location, String, Object)} method.
     * <p>
     *
     * @param context the context used by the detector when the issue was found
     * @param issue the issue that was found
     * @param severity the severity of the issue
     * @param location the location of the issue
     * @param message the associated user message
     * @param data optional extra data for a discovered issue, or null. The
     *            content depends on the specific issue. Detectors can pass
     *            extra info here which automatic fix tools etc can use to
     *            extract relevant information instead of relying on parsing the
     *            error message text. See each detector for details on which
     *            data if any is supplied for a given issue.
     */
    public abstract void report(
            @NonNull Context context,
            @NonNull Issue issue,
            @NonNull Severity severity,
            @Nullable Location location,
            @NonNull String message,
            @Nullable Object data);

    /**
     * Send an exception or error message (with warning severity) to the log
     *
     * @param exception the exception, possibly null
     * @param format the error message using {@link String#format} syntax, possibly null
     *    (though in that case the exception should not be null)
     * @param args any arguments for the format string
     */
    public void log(
            @Nullable Throwable exception,
            @Nullable String format,
            @Nullable Object... args) {
        log(Severity.WARNING, exception, format, args);
    }

    /**
     * Send an exception or error message to the log
     *
     * @param severity the severity of the warning
     * @param exception the exception, possibly null
     * @param format the error message using {@link String#format} syntax, possibly null
     *    (though in that case the exception should not be null)
     * @param args any arguments for the format string
     */
    public abstract void log(
            @NonNull Severity severity,
            @Nullable Throwable exception,
            @Nullable String format,
            @Nullable Object... args);

    /**
     * Returns a {@link IDomParser} to use to parse XML
     *
     * @return a new {@link IDomParser}, or null if this client does not support
     *         XML analysis
     */
    @Nullable
    public abstract IDomParser getDomParser();

    /**
     * Returns a {@link IJavaParser} to use to parse Java
     *
     * @return a new {@link IJavaParser}, or null if this client does not
     *         support Java analysis
     */
    @Nullable
    public abstract IJavaParser getJavaParser();

    /**
     * Returns an optimal detector, if applicable. By default, just returns the
     * original detector, but tools can replace detectors using this hook with a version
     * that takes advantage of native capabilities of the tool.
     *
     * @param detectorClass the class of the detector to be replaced
     * @return the new detector class, or just the original detector (not null)
     */
    @NonNull
    public Class<? extends Detector> replaceDetector(
            @NonNull Class<? extends Detector> detectorClass) {
        return detectorClass;
    }

    /**
     * Reads the given text file and returns the content as a string
     *
     * @param file the file to read
     * @return the string to return, never null (will be empty if there is an
     *         I/O error)
     */
    @NonNull
    public abstract String readFile(@NonNull File file);

    /**
     * Reads the given binary file and returns the content as a byte array.
     * By default this method will read the bytes from the file directly,
     * but this can be customized by a client if for example I/O could be
     * held in memory and not flushed to disk yet.
     *
     * @param file the file to read
     * @return the bytes in the file, never null
     * @throws IOException if the file does not exist, or if the file cannot be
     *             read for some reason
     */
    @NonNull
    public byte[] readBytes(@NonNull File file) throws IOException {
        return Files.toByteArray(file);
    }

    /**
     * Returns the list of source folders for Java source files
     *
     * @param project the project to look up Java source file locations for
     * @return a list of source folders to search for .java files
     */
    @NonNull
    public List<File> getJavaSourceFolders(@NonNull Project project) {
        return getClassPath(project).getSourceFolders();
    }

    /**
     * Returns the list of output folders for class files
     *
     * @param project the project to look up class file locations for
     * @return a list of output folders to search for .class files
     */
    @NonNull
    public List<File> getJavaClassFolders(@NonNull Project project) {
        return getClassPath(project).getClassFolders();

    }

    /**
     * Returns the list of Java libraries
     *
     * @param project the project to look up jar dependencies for
     * @return a list of jar dependencies containing .class files
     */
    @NonNull
    public List<File> getJavaLibraries(@NonNull Project project) {
        return getClassPath(project).getLibraries();
    }

    /**
     * Returns the {@link SdkInfo} to use for the given project.
     *
     * @param project the project to look up an {@link SdkInfo} for
     * @return an {@link SdkInfo} for the project
     */
    @NonNull
    public SdkInfo getSdkInfo(@NonNull Project project) {
        // By default no per-platform SDK info
        return new DefaultSdkInfo();
    }

    /**
     * Returns a suitable location for storing cache files. Note that the
     * directory may not exist.
     *
     * @param create if true, attempt to create the cache dir if it does not
     *            exist
     * @return a suitable location for storing cache files, which may be null if
     *         the create flag was false, or if for some reason the directory
     *         could not be created
     */
    @Nullable
    public File getCacheDir(boolean create) {
        String home = System.getProperty("user.home");
        String relative = ".android" + File.separator + "cache"; //$NON-NLS-1$ //$NON-NLS-2$
        File dir = new File(home, relative);
        if (create && !dir.exists()) {
            if (!dir.mkdirs()) {
                return null;
            }
        }
        return dir;
    }

    /**
     * Returns the File corresponding to the system property or the environment variable
     * for {@link #PROP_BIN_DIR}.
     * This property is typically set by the SDK/tools/lint[.bat] wrapper.
     * It denotes the path of the wrapper on disk.
     *
     * @return A new File corresponding to {@link LintClient#PROP_BIN_DIR} or null.
     */
    @Nullable
    private File getLintBinDir() {
        // First check the Java properties (e.g. set using "java -jar ... -Dname=value")
        String path = System.getProperty(PROP_BIN_DIR);
        if (path == null || path.length() == 0) {
            // If not found, check environment variables.
            path = System.getenv(PROP_BIN_DIR);
        }
        if (path != null && path.length() > 0) {
            return new File(path);
        }
        return null;
    }

    /**
     * Returns the File pointing to the user's SDK install area. This is generally
     * the root directory containing the lint tool (but also platforms/ etc).
     *
     * @return a file pointing to the user's install area
     */
    @Nullable
    public File getSdkHome() {
        File binDir = getLintBinDir();
        if (binDir != null) {
            assert binDir.getName().equals("tools");

            File root = binDir.getParentFile();
            if (root != null && root.isDirectory()) {
                return root;
            }
        }

        String home = System.getenv("ANDROID_HOME"); //$NON-NLS-1$
        if (home != null) {
            return new File(home);
        }

        return null;
    }

    /**
     * Locates an SDK resource (relative to the SDK root directory).
     * <p>
     * TODO: Consider switching to a {@link URL} return type instead.
     *
     * @param relativePath A relative path (using {@link File#separator} to
     *            separate path components) to the given resource
     * @return a {@link File} pointing to the resource, or null if it does not
     *         exist
     */
    @Nullable
    public File findResource(@NonNull String relativePath) {
        File dir = getLintBinDir();
        if (dir == null) {
            throw new IllegalArgumentException("Lint must be invoked with the System property "
                    + PROP_BIN_DIR + " pointing to the ANDROID_SDK tools directory");
        }

        File top = dir.getParentFile();
        File file = new File(top, relativePath);
        if (file.exists()) {
            return file;
        } else {
            return null;
        }
    }

    private Map<Project, ClassPathInfo> mProjectInfo;

    /**
     * Information about class paths (sources, class files and libraries)
     * usually associated with a project.
     */
    protected static class ClassPathInfo {
        private final List<File> mClassFolders;
        private final List<File> mSourceFolders;
        private final List<File> mLibraries;

        public ClassPathInfo(
                @NonNull List<File> sourceFolders,
                @NonNull List<File> classFolders,
                @NonNull List<File> libraries) {
            mSourceFolders = sourceFolders;
            mClassFolders = classFolders;
            mLibraries = libraries;
        }

        @NonNull
        public List<File> getSourceFolders() {
            return mSourceFolders;
        }

        @NonNull
        public List<File> getClassFolders() {
            return mClassFolders;
        }

        @NonNull
        public List<File> getLibraries() {
            return mLibraries;
        }
    }

    /**
     * Considers the given project as an Eclipse project and returns class path
     * information for the project - the source folder(s), the output folder and
     * any libraries.
     * <p>
     * Callers will not cache calls to this method, so if it's expensive to compute
     * the classpath info, this method should perform its own caching.
     *
     * @param project the project to look up class path info for
     * @return a class path info object, never null
     */
    @NonNull
    protected ClassPathInfo getClassPath(@NonNull Project project) {
        ClassPathInfo info;
        if (mProjectInfo == null) {
            mProjectInfo = Maps.newHashMap();
            info = null;
        } else {
            info = mProjectInfo.get(project);
        }

        if (info == null) {
            List<File> sources = new ArrayList<File>(2);
            List<File> classes = new ArrayList<File>(1);
            List<File> libraries = new ArrayList<File>();

            File projectDir = project.getDir();
            File classpathFile = new File(projectDir, ".classpath"); //$NON-NLS-1$
            if (classpathFile.exists()) {
                String classpathXml = readFile(classpathFile);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                InputSource is = new InputSource(new StringReader(classpathXml));
                factory.setNamespaceAware(false);
                factory.setValidating(false);
                try {
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document document = builder.parse(is);
                    NodeList tags = document.getElementsByTagName("classpathentry"); //$NON-NLS-1$
                    for (int i = 0, n = tags.getLength(); i < n; i++) {
                        Element element = (Element) tags.item(i);
                        String kind = element.getAttribute("kind"); //$NON-NLS-1$
                        List<File> addTo = null;
                        if (kind.equals("src")) {            //$NON-NLS-1$
                            addTo = sources;
                        } else if (kind.equals("output")) {  //$NON-NLS-1$
                            addTo = classes;
                        } else if (kind.equals("lib")) {     //$NON-NLS-1$
                            addTo = libraries;
                        }
                        if (addTo != null) {
                            String path = element.getAttribute("path"); //$NON-NLS-1$
                            File folder = new File(projectDir, path);
                            if (folder.exists()) {
                                addTo.add(folder);
                            }
                        }
                    }
                } catch (Exception e) {
                    log(null, null);
                }
            }

            // Add in libraries that aren't specified in the .classpath file
            File libs = new File(project.getDir(), LIBS_FOLDER);
            if (libs.isDirectory()) {
                File[] jars = libs.listFiles();
                if (jars != null) {
                    for (File jar : jars) {
                        if (LintUtils.endsWith(jar.getPath(), DOT_JAR)
                                && !libraries.contains(jar)) {
                            libraries.add(jar);
                        }
                    }
                }
            }

            if (classes.size() == 0) {
                File folder = new File(projectDir, CLASS_FOLDER);
                if (folder.exists()) {
                    classes.add(folder);
                } else {
                    // Maven checks
                    folder = new File(projectDir,
                            "target" + File.separator + "classes"); //$NON-NLS-1$ //$NON-NLS-2$
                    if (folder.exists()) {
                        classes.add(folder);

                        // If it's maven, also correct the source path, "src" works but
                        // it's in a more specific subfolder
                        if (sources.size() == 0) {
                            File src = new File(projectDir,
                                    "src" + File.separator     //$NON-NLS-1$
                                    + "main" + File.separator  //$NON-NLS-1$
                                    + "java");                 //$NON-NLS-1$
                            if (src.exists()) {
                                sources.add(src);
                            } else {
                                src = new File(projectDir, SRC_FOLDER);
                                if (src.exists()) {
                                    sources.add(src);
                                }
                            }

                            File gen = new File(projectDir,
                                    "target" + File.separator                  //$NON-NLS-1$
                                    + "generated-sources" + File.separator     //$NON-NLS-1$
                                    + "r");                                    //$NON-NLS-1$
                            if (gen.exists()) {
                                sources.add(gen);
                            }
                        }
                    }
                }
            }

            // Fallback, in case there is no Eclipse project metadata here
            if (sources.size() == 0) {
                File src = new File(projectDir, SRC_FOLDER);
                if (src.exists()) {
                    sources.add(src);
                }
                File gen = new File(projectDir, GEN_FOLDER);
                if (gen.exists()) {
                    sources.add(gen);
                }
            }

            info = new ClassPathInfo(sources, classes, libraries);
            mProjectInfo.put(project, info);
        }

        return info;
    }

    /**
     * A map from directory to existing projects, or null. Used to ensure that
     * projects are unique for a directory (in case we process a library project
     * before its including project for example)
     */
    private Map<File, Project> mDirToProject;

    /**
     * Returns a project for the given directory. This should return the same
     * project for the same directory if called repeatedly.
     *
     * @param dir the directory containing the project
     * @param referenceDir See {@link Project#getReferenceDir()}.
     * @return a project, never null
     */
    @NonNull
    public Project getProject(@NonNull File dir, @NonNull File referenceDir) {
        if (mDirToProject == null) {
            mDirToProject = new HashMap<File, Project>();
        }

        File canonicalDir = dir;
        try {
            // Attempt to use the canonical handle for the file, in case there
            // are symlinks etc present (since when handling library projects,
            // we also call getCanonicalFile to compute the result of appending
            // relative paths, which can then resolve symlinks and end up with
            // a different prefix)
            canonicalDir = dir.getCanonicalFile();
        } catch (IOException ioe) {
            // pass
        }

        Project project = mDirToProject.get(canonicalDir);
        if (project != null) {
            return project;
        }


        project = Project.create(this, dir, referenceDir);
        mDirToProject.put(canonicalDir, project);
        return project;
    }

    private IAndroidTarget[] mTargets;

    /**
     * Returns all the {@link IAndroidTarget} versions installed in the user's SDK install
     * area.
     *
     * @return all the installed targets
     */
    @NonNull
    public IAndroidTarget[] getTargets() {
        if (mTargets == null) {
            File sdkHome = getSdkHome();
            if (sdkHome != null) {
                StdLogger log = new StdLogger(Level.WARNING);
                SdkManager manager = SdkManager.createManager(sdkHome.getPath(), log);
                mTargets = manager.getTargets();
            } else {
                mTargets = new IAndroidTarget[0];
            }
        }

        return mTargets;
    }

    /**
     * Returns the highest known API level.
     *
     * @return the highest known API level
     */
    public int getHighestKnownApiLevel() {
        int max = SdkConstants.HIGHEST_KNOWN_API;

        for (IAndroidTarget target : getTargets()) {
            if (target.isPlatform()) {
                int api = target.getVersion().getApiLevel();
                if (api > max && !target.getVersion().isPreview()) {
                    max = api;
                }
            }
        }

        return max;
    }
}
