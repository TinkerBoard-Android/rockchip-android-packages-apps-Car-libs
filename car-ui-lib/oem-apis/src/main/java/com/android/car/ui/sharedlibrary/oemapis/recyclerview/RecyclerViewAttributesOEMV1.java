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
package com.android.car.ui.sharedlibrary.oemapis.recyclerview;

/**
 * Set of attributes passed from UI to the oem implementation
 */
public interface RecyclerViewAttributesOEMV1 {

    /** @return Id of the view set in the layout */
    int getId();

    /** @return if dividers should be enabled or not */
    boolean enableDivider();

    /** @return top offset */
    int getTopOffset();

    /** @return bottom offset */
    int getBottomOffset();

    /** @return left offset */
    int getLeftOffset();

    /** @return right offset */
    int getRightOffset();

    /** @return if rotary scroll is enabled */
    boolean isRotaryScrollEnabled();

    /** @return layout style set in xml */
    LayoutStyleOEMV1 getLayoutStyle();
}
