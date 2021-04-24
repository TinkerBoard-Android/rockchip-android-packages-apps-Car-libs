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

/** The OEM interface of SearchCapabilities */
public interface SearchCapabilitiesOEMV1 {

    /**
     * Returns true if the toolbar can display search result items. One example of this is when the
     * system is configured to display search items in the IME instead of in the app.
     */
    boolean canShowSearchResultItems();

    /**
     * Returns true if the app is allowed to set search results view.
     */
    boolean canShowSearchResultsView();
}
