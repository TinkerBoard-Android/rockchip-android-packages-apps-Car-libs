/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.telephony.common;

import androidx.annotation.NonNull;

import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.Fs;
import org.robolectric.res.ResourcePath;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom test runner for CarDialer. This is needed because the default behavior for
 * robolectric is just to grab the resource directory in the target package.
 * We want to override this to add several spanning different projects.
 */
public class CarTelephonyCommonRobolectricTestRunner extends RobolectricTestRunner {
    private static final Map<String, String> AAR_VERSIONS;
    private static final String SUPPORT_RESOURCE_PATH_TEMPLATE =
            "jar:file:prebuilts/sdk/current/androidx/m2repository/androidx/"
                    + "%1$s/%1$s/%2$s/%1$s-%2$s.aar!/res";
    // contraint-layout aar lives in separate path.
    // Note its path contains a hyphen.
    private static final String CONSTRAINT_LAYOUT_RESOURCE_PATH_TEMPLATE =
            "jar:file:prebuilts/sdk/current/extras/constraint-layout-x/"
                    + "%1$s/%2$s/%1$s-%2$s.aar!/res";

    static {
        AAR_VERSIONS = new HashMap<>();
        AAR_VERSIONS.put("car", "1.0.0-alpha6");
        AAR_VERSIONS.put("appcompat", "1.1.0-alpha01");
        AAR_VERSIONS.put("constraintlayout", "1.1.2");
    }

    /**
     * We don't actually want to change this behavior, so we just call super.
     */
    public CarTelephonyCommonRobolectricTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    private static ResourcePath createResourcePath(@NonNull String filePath) {
        try {
            return new ResourcePath(null, Fs.fromURL(new URL(filePath)), null);
        } catch (MalformedURLException e) {
            throw new RuntimeException("CarTelephonyRobolectricTestRunner failure", e);
        }
    }

    /**
     * Create the resource path for a support library component's JAR.
     */
    private static String createSupportResourcePathFromJar(@NonNull String componentId) {
        if (!AAR_VERSIONS.containsKey(componentId)) {
            throw new IllegalArgumentException("Unknown component " + componentId
                    + ". Update test with appropriate component name and version.");
        }
        if (componentId.equals("constraintlayout")) {
            return String.format(CONSTRAINT_LAYOUT_RESOURCE_PATH_TEMPLATE, componentId,
                    AAR_VERSIONS.get(componentId));
        }
        return String.format(SUPPORT_RESOURCE_PATH_TEMPLATE, componentId,
                AAR_VERSIONS.get(componentId));
    }

    /**
     * We are going to create our own custom manifest so that we can add multiple resource
     * paths to it. This lets us access resources in both Settings and SettingsLib in our tests.
     */
    @Override
    protected AndroidManifest getAppManifest(Config config) {
        try {
            // Using the manifest file's relative path, we can figure out the application directory.
            URL appRoot = new URL("file:packages/apps/Car/libs/car-telephony-common/");
            URL manifestPath = new URL(appRoot, "AndroidManifest.xml");
            URL resDir = new URL(appRoot, "res");
            URL assetsDir = new URL(appRoot, config.assetDir());

            // By adding any resources from libraries we need to the AndroidManifest, we can access
            // them from within the parallel universe's resource loader.
            return new AndroidManifest(Fs.fromURL(manifestPath), Fs.fromURL(resDir),
                    Fs.fromURL(assetsDir)) {
                @Override
                public List<ResourcePath> getIncludedResourcePaths() {
                    List<ResourcePath> paths = super.getIncludedResourcePaths();
                    paths.add(createResourcePath(
                            "file:packages/apps/Car/libs/car-telephony-common/tests/robotests/"
                                    + "res"));

                    // Support library resources. These need to point to the prebuilts of support
                    // library and not the source.
                    paths.add(createResourcePath(createSupportResourcePathFromJar("appcompat")));
                    paths.add(createResourcePath(createSupportResourcePathFromJar("car")));
                    paths.add(createResourcePath(
                            createSupportResourcePathFromJar("constraintlayout")));
                    paths.add(
                            createResourcePath("file:packages/apps/Car/libs/car-apps-common/res"));
                    return paths;
                }
            };
        } catch (MalformedURLException e) {
            throw new RuntimeException("CarTelephonyRobolectricTestRunner failure", e);
        }
    }
}
