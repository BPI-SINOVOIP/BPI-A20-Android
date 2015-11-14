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

import static com.android.SdkConstants.ANDROID_LIBRARY;
import static com.android.SdkConstants.ANDROID_LIBRARY_REFERENCE_FORMAT;
import static com.android.SdkConstants.ANDROID_MANIFEST_XML;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_MIN_SDK_VERSION;
import static com.android.SdkConstants.ATTR_PACKAGE;
import static com.android.SdkConstants.ATTR_TARGET_SDK_VERSION;
import static com.android.SdkConstants.PROGUARD_CONFIG;
import static com.android.SdkConstants.PROJECT_PROPERTIES;
import static com.android.SdkConstants.TAG_USES_SDK;
import static com.android.SdkConstants.VALUE_TRUE;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.SdkInfo;
import com.google.common.annotations.Beta;
import com.google.common.base.Charsets;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A project contains information about an Android project being scanned for
 * Lint errors.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class Project {
    private final LintClient mClient;
    private final File mDir;
    private final File mReferenceDir;
    private Configuration mConfiguration;
    private String mPackage;
    private int mMinSdk = 1;
    private int mTargetSdk = -1;
    private boolean mLibrary;
    private String mName;
    private String mProguardPath;
    private boolean mMergeManifests;

    /** The SDK info, if any */
    private SdkInfo mSdkInfo;

    /**
     * If non null, specifies a non-empty list of specific files under this
     * project which should be checked.
     */
    private List<File> mFiles;
    private List<File> mJavaSourceFolders;
    private List<File> mJavaClassFolders;
    private List<File> mJavaLibraries;
    private List<Project> mDirectLibraries;
    private List<Project> mAllLibraries;
    private boolean mReportIssues = true;

    /**
     * Creates a new {@link Project} for the given directory.
     *
     * @param client the tool running the lint check
     * @param dir the root directory of the project
     * @param referenceDir See {@link #getReferenceDir()}.
     * @return a new {@link Project}
     */
    @NonNull
    public static Project create(
            @NonNull LintClient client,
            @NonNull  File dir,
            @NonNull File referenceDir) {
        return new Project(client, dir, referenceDir);
    }

    /** Creates a new Project. Use one of the factory methods to create. */
    private Project(
            @NonNull LintClient client,
            @NonNull File dir,
            @NonNull File referenceDir) {
        mClient = client;
        mDir = dir;
        mReferenceDir = referenceDir;

        try {
            // Read properties file and initialize library state
            Properties properties = new Properties();
            File propFile = new File(dir, PROJECT_PROPERTIES);
            if (propFile.exists()) {
                @SuppressWarnings("resource") // Eclipse doesn't know about Closeables.closeQuietly
                BufferedInputStream is = new BufferedInputStream(new FileInputStream(propFile));
                try {
                    properties.load(is);
                    String value = properties.getProperty(ANDROID_LIBRARY);
                    mLibrary = VALUE_TRUE.equals(value);
                    mProguardPath = properties.getProperty(PROGUARD_CONFIG);
                    mMergeManifests = VALUE_TRUE.equals(properties.getProperty(
                            "manifestmerger.enabled")); //$NON-NLS-1$

                    for (int i = 1; i < 1000; i++) {
                        String key = String.format(ANDROID_LIBRARY_REFERENCE_FORMAT, i);
                        String library = properties.getProperty(key);
                        if (library == null || library.length() == 0) {
                            // No holes in the numbering sequence is allowed
                            break;
                        }

                        File libraryDir = new File(dir, library).getCanonicalFile();

                        if (mDirectLibraries == null) {
                            mDirectLibraries = new ArrayList<Project>();
                        }

                        // Adjust the reference dir to be a proper prefix path of the
                        // library dir
                        File libraryReferenceDir = referenceDir;
                        if (!libraryDir.getPath().startsWith(referenceDir.getPath())) {
                            // Symlinks etc might have been resolved, so do those to
                            // the reference dir as well
                            libraryReferenceDir = libraryReferenceDir.getCanonicalFile();
                            if (!libraryDir.getPath().startsWith(referenceDir.getPath())) {
                                File f = libraryReferenceDir;
                                while (f != null && f.getPath().length() > 0) {
                                    if (libraryDir.getPath().startsWith(f.getPath())) {
                                        libraryReferenceDir = f;
                                        break;
                                    }
                                    f = f.getParentFile();
                                }
                            }
                        }

                        Project libraryPrj = client.getProject(libraryDir, libraryReferenceDir);
                        mDirectLibraries.add(libraryPrj);
                        // By default, we don't report issues in inferred library projects.
                        // The driver will set report = true for those library explicitly
                        // requested.
                        libraryPrj.setReportIssues(false);
                    }
                } finally {
                    Closeables.closeQuietly(is);
                }
            }
        } catch (IOException ioe) {
            client.log(ioe, "Initializing project state");
        }

        if (mDirectLibraries != null) {
            mDirectLibraries = Collections.unmodifiableList(mDirectLibraries);
        } else {
            mDirectLibraries = Collections.emptyList();
        }
    }

    @Override
    public String toString() {
        return "Project [dir=" + mDir + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mDir == null) ? 0 : mDir.hashCode());
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Project other = (Project) obj;
        if (mDir == null) {
            if (other.mDir != null)
                return false;
        } else if (!mDir.equals(other.mDir))
            return false;
        return true;
    }

    /**
     * Adds the given file to the list of files which should be checked in this
     * project. If no files are added, the whole project will be checked.
     *
     * @param file the file to be checked
     */
    public void addFile(@NonNull File file) {
        if (mFiles == null) {
            mFiles = new ArrayList<File>();
        }
        mFiles.add(file);
    }

    /**
     * The list of files to be checked in this project. If null, the whole
     * project should be checked.
     *
     * @return the subset of files to be checked, or null for the whole project
     */
    @Nullable
    public List<File> getSubset() {
        return mFiles;
    }

    /**
     * Returns the list of source folders for Java source files
     *
     * @return a list of source folders to search for .java files
     */
    @NonNull
    public List<File> getJavaSourceFolders() {
        if (mJavaSourceFolders == null) {
            if (isAospBuildEnvironment()) {
                String top = getAospTop();
                if (mDir.getAbsolutePath().startsWith(top)) {
                    mJavaSourceFolders = getAospJavaSourcePath();
                    return mJavaSourceFolders;
                }
            }

            mJavaSourceFolders = mClient.getJavaSourceFolders(this);
        }

        return mJavaSourceFolders;
    }

    /**
     * Returns the list of output folders for class files
     * @return a list of output folders to search for .class files
     */
    @NonNull
    public List<File> getJavaClassFolders() {
        if (mJavaClassFolders == null) {
            if (isAospBuildEnvironment()) {
                String top = getAospTop();
                if (mDir.getAbsolutePath().startsWith(top)) {
                    mJavaClassFolders = getAospJavaClassPath();
                    return mJavaClassFolders;
                }
            }

            mJavaClassFolders = mClient.getJavaClassFolders(this);
        }
        return mJavaClassFolders;
    }

    /**
     * Returns the list of Java libraries (typically .jar files) that this
     * project depends on. Note that this refers to jar libraries, not Android
     * library projects which are processed in a separate pass with their own
     * source and class folders.
     *
     * @return a list of .jar files (or class folders) that this project depends
     *         on.
     */
    @NonNull
    public List<File> getJavaLibraries() {
        if (mJavaLibraries == null) {
            // AOSP builds already merge libraries and class folders into
            // the single classes.jar file, so these have already been processed
            // in getJavaClassFolders.

            mJavaLibraries = mClient.getJavaLibraries(this);
        }

        return mJavaLibraries;
    }

    /**
     * Returns the relative path of a given file relative to the user specified
     * directory (which is often the project directory but sometimes a higher up
     * directory when a directory tree is being scanned
     *
     * @param file the file under this project to check
     * @return the path relative to the reference directory (often the project directory)
     */
    @NonNull
    public String getDisplayPath(@NonNull File file) {
       String path = file.getPath();
       String referencePath = mReferenceDir.getPath();
       if (path.startsWith(referencePath)) {
           int length = referencePath.length();
           if (path.length() > length && path.charAt(length) == File.separatorChar) {
               length++;
           }

           return path.substring(length);
       }

       return path;
    }

    /**
     * Returns the relative path of a given file within the current project.
     *
     * @param file the file under this project to check
     * @return the path relative to the project
     */
    @NonNull
    public String getRelativePath(@NonNull File file) {
       String path = file.getPath();
       String referencePath = mDir.getPath();
       if (path.startsWith(referencePath)) {
           int length = referencePath.length();
           if (path.length() > length && path.charAt(length) == File.separatorChar) {
               length++;
           }

           return path.substring(length);
       }

       return path;
    }

    /**
     * Returns the project root directory
     *
     * @return the dir
     */
    @NonNull
    public File getDir() {
        return mDir;
    }

    /**
     * Returns the original user supplied directory where the lint search
     * started. For example, if you run lint against {@code /tmp/foo}, and it
     * finds a project to lint in {@code /tmp/foo/dev/src/project1}, then the
     * {@code dir} is {@code /tmp/foo/dev/src/project1} and the
     * {@code referenceDir} is {@code /tmp/foo/}.
     *
     * @return the reference directory, never null
     */
    @NonNull
    public File getReferenceDir() {
        return mReferenceDir;
    }

    /**
     * Gets the configuration associated with this project
     *
     * @return the configuration associated with this project
     */
    @NonNull
    public Configuration getConfiguration() {
        if (mConfiguration == null) {
            mConfiguration = mClient.getConfiguration(this);
        }
        return mConfiguration;
    }

    /**
     * Returns the application package specified by the manifest
     *
     * @return the application package, or null if unknown
     */
    @Nullable
    public String getPackage() {
        //assert !mLibrary; // Should call getPackage on the master project, not the library
        // Assertion disabled because you might be running lint on a standalone library project.

        return mPackage;
    }

    /**
     * Returns the minimum API level requested by the manifest, or -1 if not
     * specified
     *
     * @return the minimum API level or -1 if unknown
     */
    public int getMinSdk() {
        //assert !mLibrary; // Should call getMinSdk on the master project, not the library
        // Assertion disabled because you might be running lint on a standalone library project.

        return mMinSdk;
    }

    /**
     * Returns the target API level specified by the manifest, or -1 if not
     * specified
     *
     * @return the target API level or -1 if unknown
     */
    public int getTargetSdk() {
        //assert !mLibrary; // Should call getTargetSdk on the master project, not the library
        // Assertion disabled because you might be running lint on a standalone library project.

        return mTargetSdk;
    }

    /**
     * Initialized the manifest state from the given manifest model
     *
     * @param document the DOM document for the manifest XML document
     */
    public void readManifest(@NonNull Document document) {
        Element root = document.getDocumentElement();
        if (root == null) {
            return;
        }

        mPackage = root.getAttribute(ATTR_PACKAGE);

        // Initialize minSdk and targetSdk
        NodeList usesSdks = root.getElementsByTagName(TAG_USES_SDK);
        if (usesSdks.getLength() > 0) {
            Element element = (Element) usesSdks.item(0);

            String minSdk = null;
            if (element.hasAttributeNS(ANDROID_URI, ATTR_MIN_SDK_VERSION)) {
                minSdk = element.getAttributeNS(ANDROID_URI, ATTR_MIN_SDK_VERSION);
            }
            if (minSdk != null) {
                try {
                    mMinSdk = Integer.valueOf(minSdk);
                } catch (NumberFormatException e) {
                    mMinSdk = 1;
                }
            }

            String targetSdk = null;
            if (element.hasAttributeNS(ANDROID_URI, ATTR_TARGET_SDK_VERSION)) {
                targetSdk = element.getAttributeNS(ANDROID_URI, ATTR_TARGET_SDK_VERSION);
            } else if (minSdk != null) {
                targetSdk = minSdk;
            }
            if (targetSdk != null) {
                try {
                    mTargetSdk = Integer.valueOf(targetSdk);
                } catch (NumberFormatException e) {
                    // TODO: Handle codenames?
                    mTargetSdk = -1;
                }
            }
        } else if (isAospBuildEnvironment()) {
            extractAospMinSdkVersion();
        }
    }

    /**
     * Returns true if this project is an Android library project
     *
     * @return true if this project is an Android library project
     */
    public boolean isLibrary() {
        return mLibrary;
    }

    /**
     * Returns the list of library projects referenced by this project
     *
     * @return the list of library projects referenced by this project, never
     *         null
     */
    @NonNull
    public List<Project> getDirectLibraries() {
        return mDirectLibraries;
    }

    /**
     * Returns the transitive closure of the library projects for this project
     *
     * @return the transitive closure of the library projects for this project
     */
    @NonNull
    public List<Project> getAllLibraries() {
        if (mAllLibraries == null) {
            if (mDirectLibraries.size() == 0) {
                return mDirectLibraries;
            }

            List<Project> all = new ArrayList<Project>();
            addLibraryProjects(all);
            mAllLibraries = all;
        }

        return mAllLibraries;
    }

    /**
     * Adds this project's library project and their library projects
     * recursively into the given collection of projects
     *
     * @param collection the collection to add the projects into
     */
    private void addLibraryProjects(@NonNull Collection<Project> collection) {
        for (Project library : mDirectLibraries) {
            collection.add(library);
            // Recurse
            library.addLibraryProjects(collection);
        }
    }

    /**
     * Gets the SDK info for the current project.
     *
     * @return the SDK info for the current project, never null
     */
    @NonNull
    public SdkInfo getSdkInfo() {
        if (mSdkInfo == null) {
            mSdkInfo = mClient.getSdkInfo(this);
        }

        return mSdkInfo;
    }

    /**
     * Gets the path to the manifest file in this project, if it exists
     *
     * @return the path to the manifest file, or null if it does not exist
     */
    public File getManifestFile() {
        File manifestFile = new File(mDir, ANDROID_MANIFEST_XML);
        if (manifestFile.exists()) {
            return manifestFile;
        }

        return null;
    }

    /**
     * Returns the proguard path configured for this project, or null if ProGuard is
     * not configured.
     *
     * @return the proguard path, or null
     */
    @Nullable
    public String getProguardPath() {
        return mProguardPath;
    }

    /**
     * Returns the name of the project
     *
     * @return the name of the project, never null
     */
    @NonNull
    public String getName() {
        if (mName == null) {
            // TODO: Consider reading the name from .project (if it's an Eclipse project)
            mName = mDir.getName();
        }

        return mName;
    }

    /**
     * Sets whether lint should report issues in this project. See
     * {@link #getReportIssues()} for a full description of what that means.
     *
     * @param reportIssues whether lint should report issues in this project
     */
    public void setReportIssues(boolean reportIssues) {
        mReportIssues = reportIssues;
    }

    /**
     * Returns whether lint should report issues in this project.
     * <p>
     * If a user specifies a project and its library projects for analysis, then
     * those library projects are all "included", and all errors found in all
     * the projects are reported. But if the user is only running lint on the
     * main project, we shouldn't report errors in any of the library projects.
     * We still need to <b>consider</b> them for certain types of checks, such
     * as determining whether resources found in the main project are unused, so
     * the detectors must still get a chance to look at these projects. The
     * {@code #getReportIssues()} attribute is used for this purpose.
     *
     * @return whether lint should report issues in this project
     */
    public boolean getReportIssues() {
        return mReportIssues;
    }

    /**
     * Sets whether manifest merging is in effect.
     *
     * @param merging whether manifest merging is in effect
     */
    public void setMergingManifests(boolean merging) {
        mMergeManifests = merging;
    }

    /**
     * Returns whether manifest merging is in effect
     *
     * @return true if manifests in library projects should be merged into main projects
     */
    public boolean isMergingManifests() {
        return mMergeManifests;
    }


    // ---------------------------------------------------------------------------
    // Support for running lint on the AOSP source tree itself

    private static Boolean sAospBuild;

    /** Is lint running in an AOSP build environment */
    private static boolean isAospBuildEnvironment() {
        if (sAospBuild == null) {
            sAospBuild = getAospTop() != null;
        }

        return sAospBuild.booleanValue();
    }

    /** Get the root AOSP dir, if any */
    private static String getAospTop() {
        return System.getenv("ANDROID_BUILD_TOP");   //$NON-NLS-1$
    }

    /** Get the host out directory in AOSP, if any */
    private static String getAospHostOut() {
        return System.getenv("ANDROID_HOST_OUT");    //$NON-NLS-1$
    }

    /** Get the product out directory in AOSP, if any */
    private static String getAospProductOut() {
        return System.getenv("ANDROID_PRODUCT_OUT"); //$NON-NLS-1$
    }

    private List<File> getAospJavaSourcePath() {
        List<File> sources = new ArrayList<File>(2);
        // Normal sources
        File src = new File(mDir, "src"); //$NON-NLS-1$
        if (src.exists()) {
            sources.add(src);
        }

        // Generates sources
        for (File dir : getIntermediateDirs()) {
            File classes = new File(dir, "src"); //$NON-NLS-1$
            if (classes.exists()) {
                sources.add(classes);
            }
        }

        if (sources.size() == 0) {
            mClient.log(null,
                    "Warning: Could not find sources or generated sources for project %1$s",
                    getName());
        }

        return sources;
    }

    private List<File> getAospJavaClassPath() {
        List<File> classDirs = new ArrayList<File>(1);

        for (File dir : getIntermediateDirs()) {
            File classes = new File(dir, "classes"); //$NON-NLS-1$
            if (classes.exists()) {
                classDirs.add(classes);
            } else {
                classes = new File(dir, "classes.jar"); //$NON-NLS-1$
                if (classes.exists()) {
                    classDirs.add(classes);
                }
            }
        }

        if (classDirs.size() == 0) {
            mClient.log(null,
                    "No bytecode found: Has the project been built? (%1$s)", getName());
        }

        return classDirs;
    }

    /** Find the _intermediates directories for a given module name */
    private List<File> getIntermediateDirs() {
        // See build/core/definitions.mk and in particular the "intermediates-dir-for" definition
        List<File> intermediates = new ArrayList<File>();

        // TODO: Look up the module name, e.g. LOCAL_MODULE. However,
        // some Android.mk files do some complicated things with it - and most
        // projects use the same module name as the directory name.
        String moduleName = mDir.getName();

        String top = getAospTop();
        final String[] outFolders = new String[] {
            top + "/out/host/common/obj",             //$NON-NLS-1$
            top + "/out/target/common/obj",           //$NON-NLS-1$
            getAospHostOut() + "/obj",                //$NON-NLS-1$
            getAospProductOut() + "/obj"              //$NON-NLS-1$
        };
        final String[] moduleClasses = new String[] {
                "APPS",                //$NON-NLS-1$
                "JAVA_LIBRARIES",      //$NON-NLS-1$
        };

        for (String out : outFolders) {
            assert new File(out.replace('/', File.separatorChar)).exists() : out;
            for (String moduleClass : moduleClasses) {
                String path = out + '/' + moduleClass + '/' + moduleName
                        + "_intermediates"; //$NON-NLS-1$
                File file = new File(path.replace('/', File.separatorChar));
                if (file.exists()) {
                    intermediates.add(file);
                }
            }
        }

        return intermediates;
    }

    private void extractAospMinSdkVersion() {
        // Is the SDK level specified by a Makefile?
        boolean found = false;
        File makefile = new File(mDir, "Android.mk"); //$NON-NLS-1$
        if (makefile.exists()) {
            try {
                List<String> lines = Files.readLines(makefile, Charsets.UTF_8);
                Pattern p = Pattern.compile("LOCAL_SDK_VERSION\\s*:=\\s*(.*)"); //$NON-NLS-1$
                for (String line : lines) {
                    line = line.trim();
                    Matcher matcher = p.matcher(line);
                    if (matcher.matches()) {
                        found = true;
                        String version = matcher.group(1);
                        if (version.equals("current")) { //$NON-NLS-1$
                            mMinSdk = findCurrentAospVersion();
                        } else {
                            try {
                                mMinSdk = Integer.valueOf(version);
                            } catch (NumberFormatException e) {
                                // Codename - just use current
                                mMinSdk = findCurrentAospVersion();
                            }
                        }
                        break;
                    }
                }
            } catch (IOException ioe) {
                mClient.log(ioe, null);
            }
        }

        if (!found) {
            mMinSdk = findCurrentAospVersion();
        }
    }

    /** Cache for {@link #findCurrentAospVersion()} */
    private static int sCurrentVersion;

    /** In an AOSP build environment, identify the currently built image version, if available */
    private int findCurrentAospVersion() {
        if (sCurrentVersion < 1) {
            File apiDir = new File(getAospTop(), "frameworks/base/api" //$NON-NLS-1$
                    .replace('/', File.separatorChar));
            File[] apiFiles = apiDir.listFiles();
            int max = 1;
            for (File apiFile : apiFiles) {
                String name = apiFile.getName();
                int index = name.indexOf('.');
                if (index > 0) {
                    String base = name.substring(0, index);
                    if (Character.isDigit(base.charAt(0))) {
                        try {
                            int version = Integer.parseInt(base);
                            if (version > max) {
                                max = version;
                            }
                        } catch (NumberFormatException nufe) {
                            // pass
                        }
                    }
                }
            }
            sCurrentVersion = max;
        }

        return sCurrentVersion;
    }
}
