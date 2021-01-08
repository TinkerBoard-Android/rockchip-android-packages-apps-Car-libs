/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.car.ui;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.app.AppComponentFactory;

import com.android.car.ui.sharedlibrarysupport.ModifiableClassLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link AppComponentFactory} that causes the app to be created with a custom classloader
 * that has the ability to load shared libraries. Used for providing custom OEM implementations
 * of CarUi components.
 */
public class CarUiAppComponentFactory extends AppComponentFactory {

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static ModifiableClassLoader sModifiableClassLoader;

    @NonNull
    @Override
    public ClassLoader instantiateClassLoader(@NonNull ClassLoader cl,
            @NonNull ApplicationInfo appInfo) {
        // All this apk loading code is copied from another Google app
        List<String> libraryPaths = new ArrayList<>(3);
        if (appInfo.nativeLibraryDir != null) {
            libraryPaths.add(appInfo.nativeLibraryDir);
        }
        if ((appInfo.flags & ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS) == 0) {
            for (String abi : getSupportedAbisForCurrentRuntime()) {
                libraryPaths.add(appInfo.sourceDir + "!/lib/" + abi);
            }
        }

        String flatLibraryPaths = (libraryPaths.size() == 0
                ? null : TextUtils.join(File.pathSeparator, libraryPaths));

        String apkPaths = appInfo.sourceDir;
        if (appInfo.sharedLibraryFiles != null && appInfo.sharedLibraryFiles.length > 0) {
            // Unless you pass PackageManager.GET_SHARED_LIBRARY_FILES this will always be null
            // HOWEVER, if you running on a device with F5 active, the module's dex files are
            // always listed in ApplicationInfo.sharedLibraryFiles and should be included in
            // the classpath.
            apkPaths +=
                    File.pathSeparator + TextUtils.join(File.pathSeparator,
                            appInfo.sharedLibraryFiles);
        }

        sModifiableClassLoader = new ModifiableClassLoader(apkPaths, flatLibraryPaths, cl);
        return sModifiableClassLoader;
    }

    private static List<String> getSupportedAbisForCurrentRuntime() {
        List<String> abis = new ArrayList<>();
        if (Process.is64Bit()) {
            Collections.addAll(abis, Build.SUPPORTED_64_BIT_ABIS);
        } else {
            Collections.addAll(abis, Build.SUPPORTED_32_BIT_ABIS);
        }
        return abis;
    }
}
