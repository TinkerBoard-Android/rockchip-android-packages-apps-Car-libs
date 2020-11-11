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

import com.android.car.ui.imewidescreen.CarUiImeSearchListItem;
import com.android.car.ui.recyclerview.CarUiContentListItem;
import com.android.car.ui.sharedlibrary.oemapis.toolbar.SearchItemOEMV1;

import java.util.function.Consumer;

class SearchItemAdapterV1 implements SearchItemOEMV1 {
    private final CarUiImeSearchListItem mClientItem;
    SearchItemAdapterV1(CarUiImeSearchListItem item) {
        mClientItem = item;
    }

    @Override
    public int getIconResId() {
        return mClientItem.getIconResId();
    }

    @Override
    public int getSupplementalIconResId() {
        return mClientItem.getSupplementalIconResId();
    }

    @Override
    public CharSequence getTitle() {
        return mClientItem.getTitle();
    }

    @Override
    public CharSequence getBody() {
        return mClientItem.getBody();
    }

    @Override
    public Consumer<SearchItemOEMV1> getOnClickListener() {
        CarUiContentListItem.OnClickListener listener = mClientItem.getOnClickListener();
        if (listener == null) {
            return null;
        }

        return item -> listener.onClick(mClientItem);
    }

    @Override
    public Consumer<SearchItemOEMV1> getSupplementalIconOnClickListener() {
        CarUiContentListItem.OnClickListener listener =
                mClientItem.getSupplementalIconOnClickListener();
        if (listener == null) {
            return null;
        }

        return item -> listener.onClick(mClientItem);
    }
}
