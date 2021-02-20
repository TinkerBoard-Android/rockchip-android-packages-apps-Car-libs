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
package com.android.car.ui.sharedlibrarysupport;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.ui.baselayout.InsetsChangedListener;
import com.android.car.ui.button.CarUiButton;
import com.android.car.ui.button.CarUiButtonAttributes;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.toolbar.ToolbarController;

/**
 * This interface contains methods to create customizable carui components.
 */
public interface SharedLibraryFactory {

    /**
     * Creates the base layout, and optionally the toolbar.
     *
     * @param contentView The view to install the base layout around.
     * @param insetsChangedListener A method to call when the insets change.
     * @param toolbarEnabled Whether or not to add a toolbar to the base layout.
     * @param fullscreen A hint specifying whether this view we're installing around takes up
     *                   the whole screen or not. Used to know if putting decorations around
     *                   the edges is appropriate.
     * @return A {@link ToolbarController} or null if {@code toolbarEnabled} was false.
     */
    @Nullable
    ToolbarController installBaseLayoutAround(
            View contentView,
            InsetsChangedListener insetsChangedListener,
            boolean toolbarEnabled,
            boolean fullscreen);

    /**
     * Creates a {@link CarUiButton}.
     *
     * @param context The visual context to create views with.
     * @param attrs An object containing initial attributes for the button.
     * @return A {@link CarUiButton}
     */
    @NonNull
    CarUiButton createButton(
            Context context,
            @Nullable CarUiButtonAttributes attrs);

    /**
     * Creates an instance of CarUiRecyclerView
     * @param context The visual context to create views with.
     * @param attrs An object containing initial attributes for the button.
     */
    CarUiRecyclerView createRecyclerView(Context context, AttributeSet attrs);
}
