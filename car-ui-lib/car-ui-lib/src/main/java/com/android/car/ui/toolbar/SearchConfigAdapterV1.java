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

package com.android.car.ui.toolbar;

import static com.android.car.ui.utils.CarUiUtils.convertList;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;

import com.android.car.ui.sharedlibrary.oemapis.toolbar.SearchConfigOEMV1;
import com.android.car.ui.sharedlibrary.oemapis.toolbar.SearchItemOEMV1;

import java.util.List;

/**
 * Adapts a {@link SearchConfig} into a {@link SearchConfigOEMV1}
 */
public final class SearchConfigAdapterV1 implements SearchConfigOEMV1 {

    private final SearchConfig mSearchConfig;

    public SearchConfigAdapterV1(@NonNull SearchConfig searchConfig) {
        mSearchConfig = searchConfig;
    }

    @Override
    public View getSearchResultsView() {
        return mSearchConfig.getSearchResultsView();
    }

    @Override
    public Drawable getSearchResultsInputViewIcon() {
        return mSearchConfig.getSearchResultsInputViewIcon();
    }

    @Override
    public List<? extends SearchItemOEMV1> getSearchResultItems() {
        return convertList(mSearchConfig.getSearchResultItems(), SearchItemAdapterV1::new);
    }
}
