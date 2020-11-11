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

import android.view.View;

import com.android.car.ui.sharedlibrary.oemapis.toolbar.ToolbarControllerOEMV1;

import java.util.function.Consumer;

/**
 * This interface contains methods to create customizable carui components.
 *
 * It returns them as their OEM-versioned interfaces (i.e. ToolbarControllerOEMV1) and is versioned
 * itself so that no additional reflection or casting is necessary once the SharedLibraryFactory
 * has been created.
 *
 * Multiple of these can be provided via {@link SharedLibraryVersionProviderOEMV1} to allow
 * shared libraries to provide an old implementation for old apps, and a newer implementation
 * for newer apps.
 */
public interface SharedLibraryFactoryOEMV1 {

    /**
     * Creates the base layout, and optionally the toolbar.
     *
     * @param contentView The view to install the base layout around.
     * @param insetsChangedListener A method to call when the insets change.
     * @param toolbarEnabled Whether or not to add a toolbar to the base layout.
     * @param fullscreen Whether or not this base layout / toolbar is taking up the whole screen.
     *                   This can be used to decide whether or not to add decorations around the
     *                   edge of it.
     * @return A {@link ToolbarControllerOEMV1} or null if {@code toolbarEnabled} was false.
     */
    ToolbarControllerOEMV1 installBaseLayoutAround(
            View contentView,
            Consumer<InsetsOEMV1> insetsChangedListener,
            boolean toolbarEnabled,
            boolean fullscreen);
}
