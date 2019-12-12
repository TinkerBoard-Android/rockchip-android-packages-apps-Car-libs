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

import static com.android.car.ui.preference.PreferenceDialogFragment.ARG_KEY;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.DialogPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.car.ui.R;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.toolbar.Toolbar;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment that provides a layout with a list of options associated with a {@link
 * ListPreference}.
 */
public class ListPreferenceFragment extends Fragment implements
        CarUiRecyclerViewRadioButtonAdapter.OnRadioButtonClickedListener {

    private ListPreference mPreference;
    private int mClickedDialogEntryIndex;

    /**
     * Returns a new instance of {@link ListPreferenceFragment} for the {@link ListPreference} with
     * the given {@code key}.
     */
    static ListPreferenceFragment newInstance(String key) {
        ListPreferenceFragment fragment = new ListPreferenceFragment();
        Bundle b = new Bundle(/* capacity= */ 1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.car_ui_list_preference, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final CarUiRecyclerView mCarUiRecyclerView = view.requireViewById(R.id.radio_list);
        final Toolbar toolbar = view.requireViewById(R.id.toolbar);

        mCarUiRecyclerView.setPadding(0, toolbar.getHeight(), 0, 0);
        toolbar.registerToolbarHeightChangeListener(newHeight -> {
            if (mCarUiRecyclerView.getPaddingTop() == newHeight) {
                return;
            }

            int oldHeight = mCarUiRecyclerView.getPaddingTop();
            mCarUiRecyclerView.setPadding(0, newHeight, 0, 0);
            mCarUiRecyclerView.scrollBy(0, oldHeight - newHeight);
        });

        mCarUiRecyclerView.setClipToPadding(false);
        mPreference = getListPreference();
        toolbar.setTitle(mPreference.getTitle());

        CharSequence[] entries = mPreference.getEntries();
        CharSequence[] entryValues = mPreference.getEntryValues();

        if (entries == null || entryValues == null) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array.");
        }

        if (entries.length != entryValues.length) {
            throw new IllegalStateException(
                    "ListPreference entries array length does not match entryValues array length.");
        }

        mClickedDialogEntryIndex = mPreference.findIndexOfValue(mPreference.getValue());
        List<String> entryStrings = new ArrayList<>(entries.length);
        for (CharSequence entry : entries) {
            entryStrings.add(entry.toString());
        }

        CarUiRecyclerViewRadioButtonAdapter adapter = new CarUiRecyclerViewRadioButtonAdapter(
                entryStrings, mClickedDialogEntryIndex);
        mCarUiRecyclerView.setAdapter(adapter);
        adapter.registerListener(this);
    }

    private ListPreference getListPreference() {
        if (getArguments() == null) {
            throw new IllegalStateException("Preference arguments cannot be null");
        }

        String key = getArguments().getString(ARG_KEY);
        DialogPreference.TargetFragment fragment =
                (DialogPreference.TargetFragment) getTargetFragment();

        if (key == null) {
            throw new IllegalStateException(
                    "ListPreference key not found in Fragment arguments");
        }

        if (fragment == null) {
            throw new IllegalStateException(
                    "Target fragment must be registered before displaying ListPreference "
                            + "screen.");
        }

        Preference preference = fragment.findPreference(key);

        if (!(preference instanceof ListPreference)) {
            throw new IllegalStateException(
                    "Cannot use ListPreferenceFragment with a preference that is not of type "
                            + "ListPreference");
        }

        return (ListPreference) preference;
    }

    @Override
    public void onClick(int position) {
        CharSequence[] entryValues = mPreference.getEntryValues();

        if (position < 0 || position > entryValues.length - 1) {
            throw new IllegalStateException(
                    "Clicked preference has invalid index.");
        }

        mClickedDialogEntryIndex = position;
        String value = entryValues[mClickedDialogEntryIndex].toString();
        if (mPreference.callChangeListener(value)) {
            mPreference.setValue(value);
        }

        if (getActivity() == null) {
            throw new IllegalStateException(
                    "ListPreference fragment is not attached to an Activity.");
        }

        getActivity().getSupportFragmentManager().popBackStack();
    }
}