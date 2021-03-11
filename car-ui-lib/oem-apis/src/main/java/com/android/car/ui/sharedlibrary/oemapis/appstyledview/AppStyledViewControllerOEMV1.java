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

package com.android.car.ui.sharedlibrary.oemapis.appstyledview;

import android.view.View;
import android.view.ViewGroup;

/** The OEM interface for a AppStyledView. */
public interface AppStyledViewControllerOEMV1 {

    /**
     * Creates a app styled view.
     *
     * @param container If non-null, this is the parent view that the view
     * should be attached to.
     * @param content app content view.
     * @return the view used for app styled view.
     */
    View getAppStyledView(ViewGroup container,
            View content);

    /**
     * Sets a {@link Runnable} to be called whenever the close icon is clicked.
     */
    void setOnCloseClickListener(Runnable listener);

    /**
     * Returns the width of the AppStyledView
     */
    int getAppStyledViewDialogWidth();

    /**
     * Returns the height of the AppStyledView
     */
    int getAppStyledViewDialogHeight();

    /**
     * Sets the nav icon to be used.
     */
    void setNavIcon(int navIcon);
}
