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
package com.android.car.ui.core;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.toolbar.ToolbarController;

/**
 * Public interface for general CarUi static functions.
 */
public class CarUi {

    /**
     * Gets the {@link ToolbarController} for an activity. Requires that the Activity uses
     * Theme.CarUi.WithToolbar, or otherwise sets carUiBaseLayout and carUiToolbar to true.
     *
     * See also: {@link #requireToolbar(Activity)}
     */
    @Nullable
    public static ToolbarController getToolbar(Activity activity) {
        BaseLayoutController controller = BaseLayoutController.getBaseLayout(activity);
        if (controller != null) {
            return controller.getToolbarController();
        }
        return null;
    }

    /**
     * Gets the {@link ToolbarController} for an activity. Requires that the Activity uses
     * Theme.CarUi.WithToolbar, or otherwise sets carUiBaseLayout and carUiToolbar to true.
     *
     * <p>See also: {@link #getToolbar(Activity)}
     *
     * @throws IllegalArgumentException When the CarUi Toolbar cannot be found.
     */
    @NonNull
    public static ToolbarController requireToolbar(Activity activity) {
        ToolbarController result = getToolbar(activity);
        if (result == null) {
            throw new IllegalArgumentException("Activity does not have a CarUi Toolbar! "
                    + "Are you using Theme.CarUi.WithToolbar?");
        }

        return result;
    }

    /**
     * Gets the current {@link Insets} of the given {@link Activity}. Only applies to Activities
     * using the base layout, ie have the theme attribute "carUiBaseLayout" set to true.
     *
     * <p>Note that you likely don't want to use this without also using
     * {@link com.android.car.ui.baselayout.InsetsChangedListener}, as without it the Insets
     * will automatically be applied to your Activity's content view.
     */
    @Nullable
    public static Insets getInsets(Activity activity) {
        BaseLayoutController controller = BaseLayoutController.getBaseLayout(activity);
        if (controller != null) {
            return controller.getInsets();
        }
        return null;
    }

    /**
     * Gets the current {@link Insets} of the given {@link Activity}. Only applies to Activities
     * using the base layout, ie have the theme attribute "carUiBaseLayout" set to true.
     *
     * <p>Note that you likely don't want to use this without also using
     * {@link com.android.car.ui.baselayout.InsetsChangedListener}, as without it the Insets
     * will automatically be applied to your Activity's content view.
     *
     * @throws IllegalArgumentException When the activity is not using base layouts.
     */
    @NonNull
    public static Insets requireInsets(Activity activity) {
        Insets result = getInsets(activity);
        if (result == null) {
            throw new IllegalArgumentException("Activity does not have a base layout! "
                    + "Are you using Theme.CarUi.WithToolbar or Theme.CarUi.NoToolbar?");
        }

        return result;
    }
}
