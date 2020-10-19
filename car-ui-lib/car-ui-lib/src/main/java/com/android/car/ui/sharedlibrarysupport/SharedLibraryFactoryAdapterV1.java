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

import android.view.View;

import androidx.annotation.Nullable;

import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.baselayout.InsetsChangedListener;
import com.android.car.ui.sharedlibrary.oemapis.InsetsOEMV1;
import com.android.car.ui.sharedlibrary.oemapis.SharedLibraryFactoryOEMV1;
import com.android.car.ui.sharedlibrary.oemapis.toolbar.ToolbarControllerOEMV1;
import com.android.car.ui.toolbar.ToolbarController;
import com.android.car.ui.toolbar.ToolbarControllerAdapterV1;

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

    @Override
    @Nullable
    public ToolbarController installBaseLayoutAround(
            View contentView,
            InsetsChangedListener insetsChangedListener,
            boolean toolbarEnabled) {
        ToolbarControllerOEMV1 toolbar = mOem.installBaseLayoutAround(contentView,
                insets -> insetsChangedListener.onCarUiInsetsChanged(adaptInsets(insets)),
                toolbarEnabled, true);

        if (toolbar == null) {
            return null;
        }
        return new ToolbarControllerAdapterV1(toolbar);
    }

    private Insets adaptInsets(InsetsOEMV1 insetsOEM) {
        return new Insets(insetsOEM.getLeft(), insetsOEM.getTop(),
                insetsOEM.getRight(), insetsOEM.getBottom());
    }
}
