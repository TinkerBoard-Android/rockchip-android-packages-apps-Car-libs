/*
 * Copyright (C) 2018 Google Inc.
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
package com.android.car.settingslib.robolectric;

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
 * Custom test runner for the testing of CarSettingsLib. This is needed because the
 * default behavior for robolectric is just to grab the resource directory in the target package.
 * We want to override this to add several spanning different projects.
 */
public class CarSettingsLibRobolectricTestRunner extends RobolectricTestRunner {
    private static final Map<String, String> AAR_VERSIONS;
    private static final String SUPPORT_RESOURCE_PATH_TEMPLATE =
            "jar:file:prebuilts/sdk/current/androidx/m2repository/androidx/"
                    + "%1$s/%1$s/%2$s/%1$s-%2$s.aar!/res";

    static {
        AAR_VERSIONS = new HashMap<>();
        AAR_VERSIONS.put("car", "1.0.0-alpha6");
        AAR_VERSIONS.put("appcompat", "1.1.0-alpha01");
    }

    /**
     * We don't actually want to change this behavior, so we just call super.
     */
    public CarSettingsLibRobolectricTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    private static ResourcePath createResourcePath(@NonNull String filePath) {
        try {
            return new ResourcePath(null, Fs.fromURL(new URL(filePath)), null);
        } catch (MalformedURLException e) {
            throw new RuntimeException("CarSettingsLibRobolectricTestRunner failure", e);
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
        return String.format(SUPPORT_RESOURCE_PATH_TEMPLATE, componentId,
                AAR_VERSIONS.get(componentId));
    }

    /**
     * We are going to create our own custom manifest so that we can add multiple resource
     * paths to it. This lets us access resources in both Settings and SettingsLib in our tests.
     */
    @Override
    protected AndroidManifest getAppManifest(Config config) {
        // Using the manifest file's relative path, we can figure out the application directory.
        final String appRoot = "packages/apps/Car/libs/car-settings-lib/";
        final String manifestPath = appRoot + "/AndroidManifest.xml";
        final String resDir = appRoot + "/res";
        final String assetsDir = appRoot + config.assetDir();

        // By adding any resources from libraries we need to the AndroidManifest, we can access
        // them from within the parallel universe's resource loader.
        final AndroidManifest manifest = new AndroidManifest(Fs.fileFromPath(manifestPath),
                Fs.fileFromPath(resDir), Fs.fileFromPath(assetsDir)) {
            @Override
            public List<ResourcePath> getIncludedResourcePaths() {
                List<ResourcePath> paths = super.getIncludedResourcePaths();
                paths.add(createResourcePath("file:packages/apps/Car/libs/car-settings-lib/res"));

                // Support library resources. These need to point to the prebuilts of support
                // library and not the source.
                paths.add(createResourcePath(createSupportResourcePathFromJar("appcompat")));
                paths.add(createResourcePath(createSupportResourcePathFromJar("car")));
                return paths;
            }
        };

        return manifest;
    }
}
