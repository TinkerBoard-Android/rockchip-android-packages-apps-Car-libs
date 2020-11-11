/*
 * Copyright (C) 2019 The Android Open Source Project
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

import java.util.function.Consumer;

/** Interface representing a search list item */
public interface SearchItemOEMV1 {

    /**
     * Returns the resource id of a drawable. This drawable should be the primary icon
     * of the list item.
     */
    int getIconResId();

    /**
     * Returns the resource id of a drawable. This drawable should be the secondary icon
     * of the list item.
     */
    int getSupplementalIconResId();

    /** Returns the title of the item. */
    CharSequence getTitle();

    /** Returns the body text of the item */
    CharSequence getBody();

    /** Gets the on-click listener for the whole search item */
    Consumer<SearchItemOEMV1> getOnClickListener();

    /** Gets the on-click listener for just the supplemental icon */
    Consumer<SearchItemOEMV1> getSupplementalIconOnClickListener();
}