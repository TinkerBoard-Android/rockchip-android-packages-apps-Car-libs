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
package com.google.car.ui.sharedlibrary;

import android.content.Context;
import android.view.View;

import com.android.car.ui.sharedlibrary.oemapis.InsetsOEMV1;
import com.android.car.ui.sharedlibrary.oemapis.SharedLibraryFactoryOEMV1;
import com.android.car.ui.sharedlibrary.oemapis.appstyledview.AppStyledViewControllerOEMV1;
import com.android.car.ui.sharedlibrary.oemapis.recyclerview.RecyclerViewAttributesOEMV1;
import com.android.car.ui.sharedlibrary.oemapis.recyclerview.RecyclerViewOEMV1;
import com.android.car.ui.sharedlibrary.oemapis.toolbar.ToolbarControllerOEMV1;

import com.google.car.ui.sharedlibrary.appstyleview.AppStyleViewControllerImpl;
import com.google.car.ui.sharedlibrary.toolbar.BaseLayoutInstaller;

import java.util.function.Consumer;

/**
 * An implementation of {@link SharedLibraryFactoryImpl} for creating the reference design
 * car-ui-lib components.
 */
@SuppressWarnings("AndroidJdkLibsChecker")
public class SharedLibraryFactoryImpl implements SharedLibraryFactoryOEMV1 {

    private final Context mSharedLibraryContext;

    public SharedLibraryFactoryImpl(Context sharedLibraryContext) {
        mSharedLibraryContext = sharedLibraryContext;
    }

    @Override
    public ToolbarControllerOEMV1 installBaseLayoutAround(View contentView,
            Consumer<InsetsOEMV1> insetsChangedListener, boolean toolbarEnabled,
            boolean fullscreen) {

        return BaseLayoutInstaller.installBaseLayoutAround(mSharedLibraryContext,
                contentView, insetsChangedListener, toolbarEnabled, fullscreen);
    }

    @Override
    public AppStyledViewControllerOEMV1 createAppStyledView() {
        return new AppStyleViewControllerImpl(mSharedLibraryContext);
    }

    @Override
    public RecyclerViewOEMV1 createRecyclerView(Context context,
            RecyclerViewAttributesOEMV1 attrs) {

        // TODO
        return null;
    }
}
