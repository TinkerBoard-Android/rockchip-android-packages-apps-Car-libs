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

package com.android.car.ui.utils;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Constants for the rotary controller. */
public final class RotaryConstants {
    /**
     * Content description indicating that the rotary controller should scroll this view
     * horizontally.
     */
    public static final String ROTARY_HORIZONTALLY_SCROLLABLE =
            "com.android.car.ui.utils.HORIZONTALLY_SCROLLABLE";

    /**
     * Content description indicating that the rotary controller should scroll this view
     * vertically.
     */
    public static final String ROTARY_VERTICALLY_SCROLLABLE =
            "com.android.car.ui.utils.VERTICALLY_SCROLLABLE";

    /** The key to store a FocusActionType in the Bundle. */
    public static final String FOCUS_ACTION_TYPE = "com.android.car.ui.utils.FOCUS_ACTION_TYPE";

    /** Value indicating that the ACTION_FOCUS hasn't specified what to focus. */
    public static final int FOCUS_INVALID = 0;

    /**
     * Value indicating that the ACTION_FOCUS is meant to focus on the default focus in the
     * FocusArea.
     */
    public static final int FOCUS_DEFAULT = 1;

    /**
     * Value indicating that the ACTION_FOCUS is meant to focus on the first focusable view in the
     * FocusArea.
     */
    public static final int FOCUS_FIRST = 2;

    @IntDef(flag = true, value = {FOCUS_DEFAULT, FOCUS_FIRST})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusActionType {
    }

    /** Prevent instantiation. */
    private RotaryConstants() {}
}
