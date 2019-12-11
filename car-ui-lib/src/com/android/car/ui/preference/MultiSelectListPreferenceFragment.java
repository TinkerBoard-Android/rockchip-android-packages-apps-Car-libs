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

import static java.util.stream.Collectors.toList;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;

import com.android.car.ui.R;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.toolbar.Toolbar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A fragment that provides a layout with a list of options associated with a {@link
 * CarUiMultiSelectListPreference}.
 */
public class MultiSelectListPreferenceFragment extends Fragment implements
        CarUiMultiSelectListItemAdapter.OnCheckedChangeListener {

    private CarUiMultiSelectListPreference mPreference;
    private Set<String> mNewValues;

    /**
     * Returns a new instance of {@link MultiSelectListPreferenceFragment} for the {@link
     * CarUiMultiSelectListPreference} with the given {@code key}.
     */
    static MultiSelectListPreferenceFragment newInstance(String key) {
        MultiSelectListPreferenceFragment fragment = new MultiSelectListPreferenceFragment();
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
        return inflater.inflate(R.layout.car_ui_multi_select_list_preference, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final CarUiRecyclerView recyclerView = view.requireViewById(R.id.list);
        final Toolbar toolbar = view.requireViewById(R.id.toolbar);

        recyclerView.setPadding(0, toolbar.getHeight(), 0, 0);
        toolbar.registerToolbarHeightChangeListener(newHeight -> {
            if (recyclerView.getPaddingTop() == newHeight) {
                return;
            }

            int oldHeight = recyclerView.getPaddingTop();
            recyclerView.setPadding(0, newHeight, 0, 0);
            recyclerView.scrollBy(0, oldHeight - newHeight);
        });

        mPreference = getPreference();

        recyclerView.setClipToPadding(false);
        toolbar.setTitle(mPreference.getTitle());

        mNewValues = new HashSet<>(mPreference.getValues());
        CharSequence[] entries = mPreference.getEntries();
        CharSequence[] entryValues = mPreference.getEntryValues();

        if (entries == null || entryValues == null) {
            throw new IllegalStateException(
                    "MultiSelectListPreference requires an entries array and an entryValues array"
                            + ".");
        }

        if (entries.length != entryValues.length) {
            throw new IllegalStateException(
                    "MultiSelectListPreference entries array length does not match entryValues "
                            + "array length.");
        }

        List<String> entryStrings = Arrays.stream(entries).map(CharSequence::toString).collect(
                toList());

        CarUiMultiSelectListItemAdapter adapter = new CarUiMultiSelectListItemAdapter(
                entryStrings, mPreference.getSelectedItems());
        recyclerView.setAdapter(adapter);
        adapter.setOnCheckedChangedListener(this);

        Button positiveButton = view.requireViewById(R.id.positive_button);
        Button negativeButton = view.requireViewById(R.id.negative_button);

        positiveButton.setOnClickListener(v -> {
            if (mPreference.callChangeListener(mNewValues)) {
                mPreference.setValues(mNewValues);
            }

            dismissFragment();
        });

        negativeButton.setOnClickListener(v -> {
            dismissFragment();
        });
    }

    private CarUiMultiSelectListPreference getPreference() {
        if (getArguments() == null) {
            throw new IllegalStateException("Preference arguments cannot be null");
        }

        String key = getArguments().getString(ARG_KEY);
        DialogPreference.TargetFragment fragment =
                (DialogPreference.TargetFragment) getTargetFragment();

        if (key == null) {
            throw new IllegalStateException(
                    "MultiSelectListPreference key not found in Fragment arguments");
        }

        if (fragment == null) {
            throw new IllegalStateException(
                    "Target fragment must be registered before displaying "
                            + "MultiSelectListPreference screen.");
        }

        Preference preference = fragment.findPreference(key);

        if (!(preference instanceof CarUiMultiSelectListPreference)) {
            throw new IllegalStateException(
                    "Cannot use MultiSelectListPreferenceFragment with a preference that is "
                            + "not of type CarUiMultiSelectListPreference");
        }

        return (CarUiMultiSelectListPreference) preference;
    }

    private void dismissFragment() {
        if (getActivity() == null) {
            throw new IllegalStateException(
                    "MultiSelectListPreference fragment is not attached to an Activity.");
        }

        getActivity().getSupportFragmentManager().popBackStack();
    }

    @Override
    public void onCheckChanged(int position, boolean isChecked) {
        CharSequence[] entryValues = mPreference.getEntryValues();

        if (position < 0 || position > entryValues.length - 1) {
            throw new IllegalStateException(
                    "Clicked preference has invalid index.");
        }

        String value = entryValues[position].toString();

        if (isChecked) {
            mNewValues.add(value);
        } else {
            mNewValues.remove(value);
        }
    }
}
