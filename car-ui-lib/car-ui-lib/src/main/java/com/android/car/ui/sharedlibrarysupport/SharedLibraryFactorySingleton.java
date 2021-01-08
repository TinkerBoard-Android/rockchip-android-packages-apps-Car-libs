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
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.car.ui.CarUiAppComponentFactory;
import com.android.car.ui.R;
import com.android.car.ui.sharedlibrary.oemapis.SharedLibraryFactoryOEMV1;
import com.android.car.ui.sharedlibrary.oemapis.SharedLibraryVersionProviderOEMV1;
import com.android.car.ui.utils.CarUiUtils;

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

        if (CarUiAppComponentFactory.sModifiableClassLoader == null) {
            Log.w(TAG, "CarUiAppComponentFactory not initialized! "
                    + "Did you add it to your AndroidManifest.xml? " + context.getPackageName());
            sInstance = new SharedLibraryFactoryStub();
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

        CarUiAppComponentFactory.sModifiableClassLoader
                .addAdditionalClassLoader(sharedLibraryContext.getClassLoader());

        Object oemVersionProvider;
        try {
            oemVersionProvider = Class
                    .forName("com.android.car.ui.sharedlibrary.SharedLibraryVersionProviderImpl")
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ReflectiveOperationException e) {
            if (e instanceof ClassNotFoundException) {
                Log.i(TAG, "SharedLibraryVersionProviderImpl not found.");
            } else {
                Log.e(TAG, "SharedLibraryVersionProviderImpl could not be instantiated!", e);
            }
            sInstance = new SharedLibraryFactoryStub();
            return sInstance;
        }

        // Add new version providers in an if-else chain here, in descending version order so that
        // higher versions are preferred.
        SharedLibraryVersionProvider versionProvider = null;
        if (classExists(OEMAPIS_PREFIX + "SharedLibraryVersionProviderOEMV1")
                && oemVersionProvider instanceof SharedLibraryVersionProviderOEMV1) {
            versionProvider = new SharedLibraryVersionProviderAdapterV1(
                    (SharedLibraryVersionProviderOEMV1) oemVersionProvider);
        } else {
            Log.e(TAG, "SharedLibraryVersionProviderImpl was not instanceof any known "
                    + "versions of SharedLibraryVersionProviderOEMV#.");

            sInstance = new SharedLibraryFactoryStub();
            return sInstance;
        }

        Object factory =
                versionProvider.getSharedLibraryFactory(1, sharedLibraryContext);
        // Add new factories in an if-else chain here, in descending version order so that
        // higher versions are preferred.
        if (classExists(OEMAPIS_PREFIX + "SharedLibraryFactoryOEMV1")
                && factory instanceof SharedLibraryFactoryOEMV1) {
            sInstance = new SharedLibraryFactoryAdapterV1((SharedLibraryFactoryOEMV1) factory);
        } else {
            Log.e(TAG, "SharedLibraryVersionProvider found, but did not provide a"
                    + " factory implementing any known interfaces!");
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
}
