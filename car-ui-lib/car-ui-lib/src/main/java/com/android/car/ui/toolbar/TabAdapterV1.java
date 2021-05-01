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

package com.android.car.ui.toolbar;

import android.graphics.drawable.Drawable;

import com.android.car.ui.sharedlibrary.oemapis.toolbar.TabOEMV1;

import java.util.function.Consumer;

@SuppressWarnings("AndroidJdkLibsChecker")
class TabAdapterV1 implements TabOEMV1 {

    private final Tab mClientTab;

    TabAdapterV1(Tab clientTab) {
        mClientTab = clientTab;
    }

    public Tab getClientTab() {
        return mClientTab;
    }

    @Override
    public String getTitle() {
        return mClientTab.getText();
    }

    @Override
    public Drawable getIcon() {
        return mClientTab.getIcon();
    }


    @Override
    public Runnable getOnClickListener() {
        Consumer<Tab> selectedListener = mClientTab.getSelectedListener();
        if (selectedListener == null) {
            return null;
        } else {
            return () -> selectedListener.accept(mClientTab);
        }
    }

    @Override
    public boolean shouldTint() {
        return true;
    }
}
