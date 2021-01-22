/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.car.ui.sharedlibrarysupport;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUiUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is a singleton that contains a {@link SharedLibraryFactory}. That SharedLibraryFactory
 * is used to create UI components that we want to be customizable by the OEM.
 */
public final class SharedLibraryFactorySingleton {

    private static final String TAG = "carui";
    private static final CharSequence OEMAPIS_PREFIX = "com.android.car.ui.sharedlibrary.oemapis.";
    private static SharedLibraryFactory sInstance;

    /**
     * Get the {@link SharedLibraryFactory}.
     *
     * If this is the first time the method is being called, it will initialize it using reflection
     * to check for the existence of a shared library, and resolving the appropriate version
     * of the shared library to use.
     */
    @SuppressLint("PrivateApi") // suppresses warning on Class.forName()
    public static SharedLibraryFactory get(Context context) {
        if (sInstance != null) {
            return sInstance;
        }

        String sharedLibPackageName = CarUiUtils.getSystemProperty(context.getResources(),
                R.string.car_ui_shared_library_package_system_property_name);

        if (TextUtils.isEmpty(sharedLibPackageName)) {
            sInstance = new SharedLibraryFactoryStub();
            return sInstance;
        }

        Context sharedLibraryContext;
        try {
            sharedLibraryContext = context.createPackageContext(
                    sharedLibPackageName,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not load CarUi shared library", e);
            sInstance = new SharedLibraryFactoryStub();
            return sInstance;
        }

        AdapterClassLoader adapterClassLoader =
                instantiateClassLoader(context.getApplicationInfo(),
                        SharedLibraryFactorySingleton.class.getClassLoader(),
                        sharedLibraryContext.getClassLoader());

        try {
            Class<?> oemApiUtilClass = adapterClassLoader
                    .loadClass("com.android.car.ui.sharedlibrarysupport.OemApiUtil");
            Method getSharedLibraryFactoryMethod =
                    oemApiUtilClass.getDeclaredMethod("getSharedLibraryFactory", Context.class);
            getSharedLibraryFactoryMethod.setAccessible(true);
            sInstance = (SharedLibraryFactory) getSharedLibraryFactoryMethod
                    .invoke(null, sharedLibraryContext);
        } catch (ReflectiveOperationException e) {
            Log.e(TAG, "Could not load CarUi shared library", e);
            sInstance = new SharedLibraryFactoryStub();
            return sInstance;
        }

        if (sInstance == null) {
            Log.e(TAG, "Could not load CarUi shared library");
            sInstance = new SharedLibraryFactoryStub();
        }

        return sInstance;
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private SharedLibraryFactorySingleton() {}

    @NonNull
    private static AdapterClassLoader instantiateClassLoader(@NonNull ApplicationInfo appInfo,
            @NonNull ClassLoader parent, @NonNull ClassLoader sharedlibraryClassLoader) {
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

        return new AdapterClassLoader(apkPaths, flatLibraryPaths, parent, sharedlibraryClassLoader);
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
