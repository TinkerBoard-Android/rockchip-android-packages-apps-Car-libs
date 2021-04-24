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

package com.android.car.ui.sharedlibrary.oemapis.toolbar;

import android.graphics.drawable.Drawable;
import android.view.View;

import java.util.List;

/** The OEM interface of SearchInfo */
public interface SearchConfigOEMV1 {

    /**
     * Returns the view set by an app to be displayed within the widescreen IME's content area.
     */
    View getSearchResultsView();

    /**
     * Returns the icon set by an app to be displayed in the input field fo widescreen IME.
     */
    Drawable getSearchResultsInputViewIcon();

    /**
     * Returns the search results set by an app to be displayed in the widescreen IME template.
     */
    List<? extends SearchItemOEMV1> getSearchResultItems();
}
