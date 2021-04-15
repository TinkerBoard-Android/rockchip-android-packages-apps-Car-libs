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

package com.android.car.ui.sharedlibrary.oemapis.toolbar;

import android.graphics.drawable.Drawable;

/** Interface representing a toolbar tab */
public interface TabOEMV1 {
    /** Gets the title of the tab */
    String getTitle();
    /** Gets the icon of the tab. The icon may be tinted to match the theme of the toolbar */
    Drawable getIcon();
    /** Gets the function to call when the tab is selected */
    Runnable getOnClickListener();
    /**
     * Returns if the icon should be tinted to match the style of the toolbar.
     * Most of the time this will be true. If not, then the original colors of the drawable
     * should be shown.
     */
    boolean shouldTint();
}
