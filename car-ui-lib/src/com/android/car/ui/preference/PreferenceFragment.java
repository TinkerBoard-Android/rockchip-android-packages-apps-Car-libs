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

package com.android.car.ui.preference;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;
import com.android.car.ui.toolbar.Toolbar;

/**
 * A PreferenceFragmentCompat is the entry point to using the Preference library.
 *
 * <p>Note: this is borrowed as-is from androidx.preference.PreferenceFragmentCompat with updates to
 * launch Car UI library {@link DialogFragment}. Automotive applications should use children of
 * this fragment in order to launch the system themed {@link DialogFragment}.
 */
public abstract class PreferenceFragment extends PreferenceFragmentCompat {

    private static final String DIALOG_FRAGMENT_TAG =
            "com.android.car.ui.PreferenceFragment.DIALOG";

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        final Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (recyclerView == null || toolbar == null) {
            return;
        }

        recyclerView.setPadding(0, toolbar.getHeight(), 0, 0);
        toolbar.registerToolbarHeightChangeListener(height -> {
            recyclerView.setPadding(0, height, 0, 0);
        });
    }

    /**
     * Called when a preference in the tree requests to display a dialog. Subclasses should override
     * this method to display custom dialogs or to handle dialogs for custom preference classes.
     *
     * <p>Note: this is borrowed as-is from androidx.preference.PreferenceFragmentCompat with
     * updates to launch Car UI library {@link DialogFragment} instead of the ones in the
     * support library.
     *
     * @param preference The {@link Preference} object requesting the dialog
     */
    @Override
    public void onDisplayPreferenceDialog(Preference preference) {

        if (getActivity() instanceof OnPreferenceDisplayDialogCallback
                && ((OnPreferenceDisplayDialogCallback) getActivity())
                .onPreferenceDisplayDialog(this, preference)) {
            return;
        }

        // check if dialog is already showing
        if (getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }

        final DialogFragment f;
        if (preference instanceof EditTextPreference) {
            f = EditTextPreferenceDialogFragment.newInstance(preference.getKey());
        } else if (preference instanceof ListPreference) {
            f = ListPreferenceDialogFragment.newInstance(preference.getKey());
        } else if (preference instanceof MultiSelectListPreference) {
            f = MultiSelectListPreferenceDialogFragment.newInstance(preference.getKey());
        } else {
            throw new IllegalArgumentException(
                    "Cannot display dialog for an unknown Preference type: "
                            + preference.getClass().getSimpleName()
                            + ". Make sure to implement onPreferenceDisplayDialog() to handle "
                            + "displaying a custom dialog for this Preference.");
        }
        f.setTargetFragment(this, 0);
        f.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
    }
}
