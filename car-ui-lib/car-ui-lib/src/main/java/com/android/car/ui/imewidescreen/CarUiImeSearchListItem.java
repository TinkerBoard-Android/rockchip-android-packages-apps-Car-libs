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

package com.android.car.ui.imewidescreen;

import androidx.annotation.Nullable;

import com.android.car.ui.recyclerview.CarUiContentListItem;
import com.android.car.ui.recyclerview.CarUiListItemAdapter;

/**
 * Definition of list items that can be inserted into {@link CarUiListItemAdapter}. This class is
 * used to display the search items in the template for wide screen mode.
 *
 * The class is used to pass application icon resources ids to the IME for rendering in its
 * process. Applications can also pass a unique id for each item and supplemental icon that will be
 * used by the IME to notify the application when a click action is taken on them.
 */
public class CarUiImeSearchListItem extends CarUiContentListItem {

    private CharSequence mItemId;
    private int mIconResId;
    private CharSequence mSupplementalIconId;
    private int mSupplementalIconResId;

    public CarUiImeSearchListItem(Action action) {
        super(action);
    }

    /**
     * Returns the id of the item.
     */
    @Nullable
    public CharSequence getItemId() {
        return mItemId;
    }

    /**
     * Sets the id of the item.
     *
     * @param itemId text to display as title.
     */
    public void setItemId(CharSequence itemId) {
        mItemId = itemId;
    }

    /**
     * Returns the icons resource of the item.
     */
    public int getIconResId() {
        return mIconResId;
    }

    /**
     * Sets the icons resource of the item.
     */
    public void setIconResId(int iconResId) {
        mIconResId = iconResId;
    }

    /**
     * Returns the supplemental icon resource id of the item.
     */
    public int getSupplementalIconResId() {
        return mSupplementalIconResId;
    }

    /**
     * Sets supplemental icon resource id.
     */
    public void setSupplementalIconResId(int supplementalIconResId) {
        mSupplementalIconResId = supplementalIconResId;
    }

    /**
     * Returns the supplemental icon id of the item.
     */
    @Nullable
    public CharSequence getSupplementalIconId() {
        return mSupplementalIconId;
    }

    /**
     * Sets supplemental icon id.
     */
    public void setSupplementalIconId(CharSequence supplementalIconId) {
        mSupplementalIconId = supplementalIconId;
    }
}
