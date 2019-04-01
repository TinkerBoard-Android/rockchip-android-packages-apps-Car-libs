/*
 * Copyright 2019 The Android Open Source Project
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

package com.android.car.apps.common;

import android.view.ViewGroup;

import androidx.annotation.Nullable;

/**
 * Provides methods for interacting with a control bar that can be expanded or collapsed
 */
public interface ExpandableControlBar extends CarControlBar {

    /**
     * Callback for when the control bar is expanded or collapsed. This is called before the
     * ControlBar begins animating.
     */
    interface ExpandCollapseCallback {
        void onExpandCollapse(boolean expanding);
    }

    /**
     * Register a listener for expand/collapse. Callback can be unregistered by passing null.
     */
    void registerExpandCollapseCallback(@Nullable ExpandCollapseCallback callback);

    /**
     * Collapses the control bar.
     */
    void close();

    /**
     * Defines the root {@link ViewGroup} used to animate the expand/collapse layout transitions.
     * If this method is not used, only this view will be animated.
     * If other elements of the screen have a layout relative to this view, their container
     * layout should be passed to this method.
     */
    void setAnimationViewGroup(ViewGroup animationViewGroup);
}
