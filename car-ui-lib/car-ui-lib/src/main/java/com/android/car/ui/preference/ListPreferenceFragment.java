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

import static com.android.car.ui.core.CarUi.MIN_TARGET_API;
import static com.android.car.ui.preference.PreferenceDialogFragment.ARG_KEY;

import android.annotation.TargetApi;
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
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.FocusArea;
import com.android.car.ui.R;
import com.android.car.ui.baselayout.Insets;
import com.android.car.ui.baselayout.InsetsChangedListener;
import com.android.car.ui.recyclerview.CarUiContentListItem;
import com.android.car.ui.recyclerview.CarUiListItem;
import com.android.car.ui.recyclerview.CarUiListItemAdapter;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.toolbar.NavButtonMode;
import com.android.car.ui.toolbar.Toolbar;
import com.android.car.ui.toolbar.ToolbarController;
import com.android.car.ui.utils.CarUiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A fragment that provides a layout with a list of options associated with a {@link
 * ListPreference}.
 */
@TargetApi(MIN_TARGET_API)
public class ListPreferenceFragment extends Fragment implements InsetsChangedListener {

    private ListPreference mPreference;
    private CarUiContentListItem mSelectedItem;
    private int mSelectedIndex = -1;
    private boolean mUseInstantPreferenceChangeCallback;

    /**
     * Returns a new instance of {@link ListPreferenceFragment} for the {@link ListPreference} with
     * the given {@code key}.
     */
    @NonNull
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
        final CarUiRecyclerView carUiRecyclerView = CarUiUtils.requireViewByRefId(view, R.id.list);
        mUseInstantPreferenceChangeCallback =
                getResources().getBoolean(R.bool.car_ui_preference_list_instant_change_callback);
        ToolbarController toolbar = null;
        if (getTargetFragment() instanceof PreferenceFragment) {
            toolbar = ((PreferenceFragment) getTargetFragment()).getPreferenceToolbar(this);
        }

        carUiRecyclerView.setClipToPadding(false);
        mPreference = getListPreference();
        if (toolbar != null) {
            toolbar.setTitle(mPreference.getTitle());
            toolbar.setSubtitle("");
            if (toolbar.isStateSet()) {
                toolbar.setState(Toolbar.State.SUBPAGE);
            } else {
                toolbar.setNavButtonMode(NavButtonMode.BACK);
            }
            toolbar.setLogo(null);
            toolbar.setMenuItems(null);
            toolbar.setTabs(Collections.emptyList());
        }

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

        mSelectedIndex = mPreference.findIndexOfValue(mPreference.getValue());
        List<CarUiListItem> listItems = new ArrayList<>();
        CarUiListItemAdapter adapter = new CarUiListItemAdapter(listItems);

        for (int i = 0; i < entries.length; i++) {
            String entry = entries[i].toString();
            CarUiContentListItem item = new CarUiContentListItem(
                    CarUiContentListItem.Action.RADIO_BUTTON);
            item.setTitle(entry);

            if (i == mSelectedIndex) {
                item.setChecked(true);
                mSelectedItem = item;
            }

            item.setOnCheckedChangeListener((listItem, isChecked) -> {
                if (!isChecked) {
                    // Previously selected item is unchecked now, no further processing is needed.
                    return;
                }

                if (mSelectedItem != null) {
                    mSelectedItem.setChecked(false);
                    adapter.notifyItemChanged(listItems.indexOf(mSelectedItem));
                }
                mSelectedItem = listItem;
                mSelectedIndex = listItems.indexOf(mSelectedItem);

                if (mUseInstantPreferenceChangeCallback) {
                    updatePreference();
                }
            });

            listItems.add(item);
        }

        carUiRecyclerView.setAdapter(adapter);
        carUiRecyclerView.scrollToPosition(mSelectedIndex);
        carUiRecyclerView.post(
                () -> {
                    RecyclerView.ViewHolder viewHolder =
                            carUiRecyclerView.findViewHolderForAdapterPosition(mSelectedIndex);
                    if (viewHolder != null) {
                        viewHolder.itemView.requestFocus();
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getTargetFragment() instanceof PreferenceFragment) {
            Insets insets = ((PreferenceFragment) getTargetFragment()).getPreferenceInsets(this);
            if (insets != null) {
                onCarUiInsetsChanged(insets);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (!mUseInstantPreferenceChangeCallback) {
            updatePreference();
        }
    }

    private void updatePreference() {
        if (mSelectedIndex >= 0 && mPreference != null) {
            String entryValue = mPreference.getEntryValues()[mSelectedIndex].toString();

            if (mPreference.callChangeListener(entryValue)) {
                mPreference.setValue(entryValue);
            }
        }
    }

    private ListPreference getListPreference() {
        String key = requireArguments().getString(ARG_KEY);
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
    public void onCarUiInsetsChanged(@NonNull Insets insets) {
        View view = requireView();
        CarUiUtils.requireViewByRefId(view, R.id.list)
                .setPadding(0, insets.getTop(), 0, insets.getBottom());
        view.setPadding(insets.getLeft(), 0, insets.getRight(), 0);
        FocusArea focusArea = view.findViewById(R.id.car_ui_focus_area);
        if (focusArea != null) {
            focusArea.setHighlightPadding(0, insets.getTop(), 0, insets.getBottom());
            focusArea.setBoundsOffset(0, insets.getTop(), 0, insets.getBottom());
        }
    }
}
