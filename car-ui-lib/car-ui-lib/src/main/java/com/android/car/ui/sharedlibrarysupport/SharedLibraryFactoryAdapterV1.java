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

import com.android.car.ui.sharedlibrary.oemapis.SharedLibraryFactoryOEMV1;

/**
 * This class is an wrapper around {@link SharedLibraryFactoryOEMV1} that implements
 * {@link SharedLibraryFactory}, to provide a version-agnostic way of interfacing with
 * the OEM's SharedLibraryFactory.
 */
public final class SharedLibraryFactoryAdapterV1 implements SharedLibraryFactory {

    SharedLibraryFactoryOEMV1 mOem;

    public SharedLibraryFactoryAdapterV1(SharedLibraryFactoryOEMV1 oem) {
        mOem = oem;
    }

    // This will have methods delegating from SharedLibraryFactory to SharedLibraryFactoryOEMV1
    // once they have methods.
}
