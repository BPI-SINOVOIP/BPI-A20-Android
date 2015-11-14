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
package com.android.builder;

import com.android.annotations.Nullable;
import com.google.common.collect.Maps;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Class able to generate a BuildConfig class in Android project.
 * The BuildConfig class contains constants related to the build target.
 */
class BuildConfigGenerator {

    private final static String TEMPLATE = "BuildConfig.template";
    private final static String PH_PACKAGE = "#PACKAGE#";
    private final static String PH_DEBUG = "#DEBUG#";
    private final static String PH_LINES = "#ADDITIONAL_LINES#";

    private final String mGenFolder;
    private final String mAppPackage;
    private final boolean mDebug;

    public final static String BUILD_CONFIG_NAME = "BuildConfig.java";

    /**
     * Creates a generator
     * @param genFolder the gen folder of the project
     * @param appPackage the application package
     * @param debug whether it's a debug build
     */
    public BuildConfigGenerator(String genFolder, String appPackage, boolean debug) {
        mGenFolder = genFolder;
        mAppPackage = appPackage;
        mDebug = debug;
    }

    /**
     * Returns a File representing where the BuildConfig class will be.
     */
    public File getFolderPath() {
        File genFolder = new File(mGenFolder);
        return new File(genFolder, mAppPackage.replace('.', File.separatorChar));
    }

    public File getBuildConfigFile() {
        File folder = getFolderPath();
        return new File(folder, BUILD_CONFIG_NAME);
    }

    /**
     * Generates the BuildConfig class.
     * @param additionalLines a list of additional lines to be added to the class.
     */
    public void generate(@Nullable List<String> additionalLines) throws IOException {
        Map<String, String> map = Maps.newHashMap();
        map.put(PH_PACKAGE, mAppPackage);
        map.put(PH_DEBUG, Boolean.toString(mDebug));

        if (additionalLines != null) {
            StringBuilder sb = new StringBuilder();
            for (String line : additionalLines) {
                sb.append("    ").append(line).append('\n');
            }
            map.put(PH_LINES, sb.toString());

        } else {
            map.put(PH_LINES, "");
        }

        File pkgFolder = getFolderPath();
        if (pkgFolder.isDirectory() == false) {
            pkgFolder.mkdirs();
        }

        File buildConfigJava = new File(pkgFolder, BUILD_CONFIG_NAME);

        TemplateProcessor processor = new TemplateProcessor(
                BuildConfigGenerator.class.getResourceAsStream(TEMPLATE),
                map);

        processor.generate(buildConfigJava);
    }
}
