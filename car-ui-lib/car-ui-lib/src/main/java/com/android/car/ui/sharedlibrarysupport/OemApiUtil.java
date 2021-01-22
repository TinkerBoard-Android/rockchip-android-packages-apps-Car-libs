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

import android.content.Context;
import android.util.Log;

import com.android.car.ui.sharedlibrary.oemapis.SharedLibraryFactoryOEMV1;
import com.android.car.ui.sharedlibrary.oemapis.SharedLibraryVersionProviderOEMV1;

/**
 * Helper class for accessing oem-apis without reflection.
 * Load this class only via AdapterClassLoader.
 */
final class OemApiUtil {

    private static final String TAG = "carui";

    static SharedLibraryFactory getSharedLibraryFactory(Context sharedLibraryContext) {
        SharedLibraryFactory oemSharedLibraryFactory = null;

        Object oemVersionProvider = null;
        try {
            oemVersionProvider = Class
                    .forName("com.android.car.ui.sharedlibrary.SharedLibraryVersionProviderImpl")
                    .getDeclaredConstructor()
                    .newInstance();
        } catch (ClassNotFoundException e) {
            Log.i(TAG, "SharedLibraryVersionProviderImpl not found.", e);
        } catch (ReflectiveOperationException e) {
            Log.e(TAG, "SharedLibraryVersionProviderImpl could not be instantiated!", e);
        }

        // Add new version providers in an if-else chain here, in descending version order so
        // that higher versions are preferred.
        SharedLibraryVersionProvider versionProvider = null;
        if (oemVersionProvider instanceof SharedLibraryVersionProviderOEMV1) {
            versionProvider = new SharedLibraryVersionProviderAdapterV1(
                        (SharedLibraryVersionProviderOEMV1) oemVersionProvider);
        } else {
            Log.e(TAG, "SharedLibraryVersionProviderImpl was not instanceof any known "
                    + "versions of SharedLibraryVersionProviderOEMV#.");
        }

        if (versionProvider != null) {
            Object factory =
                    versionProvider.getSharedLibraryFactory(1, sharedLibraryContext);
            if (factory instanceof SharedLibraryFactoryOEMV1) {
                oemSharedLibraryFactory = new SharedLibraryFactoryAdapterV1(
                            (SharedLibraryFactoryOEMV1) factory);
            } else {
                Log.e(TAG, "SharedLibraryVersionProvider found, but did not provide a"
                        + " factory implementing any known interfaces!");
            }
        }

        return oemSharedLibraryFactory;
    }
}
