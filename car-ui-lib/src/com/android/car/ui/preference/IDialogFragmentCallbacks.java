/*
 * Copyright 2020 The Android Open Source Project
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
package com.android.car.ui.preference;

import android.app.AlertDialog;
import android.view.View;

import androidx.preference.DialogPreference;

/**
 * Interface for preferences to handle their own dialogs.
 *
 * A {@link androidx.preference.Preference} should implement this, and its {@link DialogPreference}
 * will call these methods on the Preference.
 */
public interface IDialogFragmentCallbacks {

    /**
     * @see CarUiDialogFragment#onPrepareDialogBuilder(AlertDialog.Builder)
     */
    default void onPrepareDialogBuilder(AlertDialog.Builder builder) {
    }

    /**
     * @see CarUiDialogFragment#onDialogClosed(boolean)
     */
    default void onDialogClosed(boolean positiveResult) {
    }

    /**
     * @see CarUiDialogFragment#onBindDialogView(View)
     */
    default void onBindDialogView(View view) {
    }
}
