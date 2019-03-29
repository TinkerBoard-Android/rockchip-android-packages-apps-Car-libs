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

package com.android.car.apps.common.util;

import android.view.View;

import androidx.annotation.Nullable;

/** Static helper methods for {@link View}. */
public class ViewHelper {

    /** Sets the visibility of the (optional) view to {@link View#VISIBLE} or {@link View#GONE}. */
    public static void setVisible(@Nullable View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Sets the visibility of the (optional) view to {@link View#INVISIBLE} or {@link View#VISIBLE}.
     */
    public static void setInvisible(@Nullable View view, boolean invisible) {
        if (view != null) {
            view.setVisibility(invisible ? View.INVISIBLE : View.VISIBLE);
        }
    }
}
