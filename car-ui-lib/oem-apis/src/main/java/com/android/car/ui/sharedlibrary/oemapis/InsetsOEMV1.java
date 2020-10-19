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
package com.android.car.ui.sharedlibrary.oemapis;

/**
 * Represents insets in the base layout. {@link com.android.car.ui.baselayout.Insets} for more
 * information.
 */
public interface InsetsOEMV1 {
    /** Gets the left inset */
    int getLeft();
    /** Gets the right inset */
    int getRight();
    /** Gets the top inset */
    int getTop();
    /** Gets the bottom inset */
    int getBottom();
}
