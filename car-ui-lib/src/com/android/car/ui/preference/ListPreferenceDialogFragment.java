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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;

import com.android.car.ui.R;
import com.android.car.ui.recyclerview.CarUiRecyclerView;
import com.android.car.ui.toolbar.Toolbar;

import java.util.ArrayList;

/**
 * Presents a dialog with a list of options associated with a {@link ListPreference}.
 *
 * <p>Note: this is borrowed as-is from androidx.preference.ListPreferenceDialogFragmentCompat
 * with updates to formatting to match the project style. Automotive applications should use this
 * implementations in order to launch the system themed platform {@link AlertDialog} instead of the
 * one in the support library. In addition this list preference can be shown in a full screen
 * dialog. Full screen dialog will have a custom layout returned by  {@link #onCreateDialogView} and
 * the window size is changes in {@link #onStart()}.
 */
public class ListPreferenceDialogFragment extends PreferenceDialogFragment implements
        Toolbar.OnBackListener, CarUiRecyclerViewRadioButtonAdapter.OnRadioButtonClickedListener {

    private static final String SAVE_STATE_INDEX = "ListPreferenceDialogFragment.index";
    private static final String SAVE_STATE_ENTRIES = "ListPreferenceDialogFragment.entries";
    private static final String SAVE_STATE_ENTRY_VALUES =
            "ListPreferenceDialogFragment.entryValues";

    private int mClickedDialogEntryIndex;
    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private View mDialogView;
    private CarUiRecyclerView mCarUiRecyclerView;
    private boolean mShowFullScreen;

    /**
     * Returns a new instance of {@link ListPreferenceDialogFragment} for the {@link
     * ListPreference} with the given {@code key}.
     */
    public static ListPreferenceDialogFragment newInstance(String key) {
        ListPreferenceDialogFragment fragment = new ListPreferenceDialogFragment();
        Bundle b = new Bundle(/* capacity= */ 1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public View onCreateDialogView(Context context) {
        return mShowFullScreen ? mDialogView : super.onCreateDialogView(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            ListPreference preference = getListPreference();

            if (preference.getEntries() == null || preference.getEntryValues() == null) {
                throw new IllegalStateException(
                        "ListPreference requires an entries array and an entryValues array.");
            }

            mClickedDialogEntryIndex = preference.findIndexOfValue(preference.getValue());
            mEntries = preference.getEntries();
            mEntryValues = preference.getEntryValues();
        } else {
            mClickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0);
            mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES);
            mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES);
        }

        mShowFullScreen = getContext().getResources().getBoolean(
                R.bool.car_ui_preference_list_show_full_screen);
        if (!mShowFullScreen) {
            return;
        }

        setStyle(DialogFragment.STYLE_NORMAL, R.style.Preference_CarUi_ListPreference);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        mDialogView = inflater.inflate(R.layout.car_ui_list_preference_dialog, null);

        Toolbar toolbar = mDialogView.findViewById(R.id.toolbar);
        toolbar.registerOnBackListener(this);

        ArrayList<String> entries = new ArrayList<>();
        for (int i = 0; i < mEntries.length; i++) {
            entries.add(mEntries[i].toString());
        }

        mCarUiRecyclerView = mDialogView.findViewById(R.id.radio_list);
        CarUiRecyclerViewRadioButtonAdapter adapter = new CarUiRecyclerViewRadioButtonAdapter(
                entries, mClickedDialogEntryIndex);
        mCarUiRecyclerView.setAdapter(adapter);
        adapter.registerListener(this);

        mDialogView.getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                mDialogView.getViewTreeObserver()
                                        .removeOnGlobalLayoutListener(this);
                                int recyclerViewHeight =
                                        getDialog().getWindow().getDecorView().getHeight();

                                mCarUiRecyclerView.setPadding(mCarUiRecyclerView.getPaddingLeft(),
                                        toolbar.getHeight(),
                                        mCarUiRecyclerView.getPaddingRight(),
                                        mCarUiRecyclerView.getPaddingBottom());

                                ViewGroup.LayoutParams params =
                                        mCarUiRecyclerView.getLayoutParams();
                                params.height = recyclerViewHeight;
                                mCarUiRecyclerView.setLayoutParams(params);
                            }
                        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_INDEX, mClickedDialogEntryIndex);
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries);
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues);
    }

    private ListPreference getListPreference() {
        return (ListPreference) getPreference();
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);

        if (mShowFullScreen) {
            builder.setPositiveButton(null, null);
            builder.setNegativeButton(null, null);
            builder.setTitle(null);
            return;
        } else {
            builder.setSingleChoiceItems(mEntries, mClickedDialogEntryIndex,
                    (dialog, which) -> onClick(which));

            // The typical interaction for list-based dialogs is to have click-on-an-item dismiss
            // the
            // dialog instead of the user having to press 'Ok'.
            builder.setPositiveButton(null, null);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && mShowFullScreen) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setLayout(width, height);
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult && mClickedDialogEntryIndex >= 0) {
            String value = mEntryValues[mClickedDialogEntryIndex].toString();
            ListPreference preference = getListPreference();
            if (preference.callChangeListener(value)) {
                preference.setValue(value);
            }
        }
    }

    @Override
    public boolean onBack() {
        onClick(getDialog(), DialogInterface.BUTTON_NEGATIVE);
        getDialog().dismiss();
        return true;
    }

    @Override
    public void onClick(int position) {
        mClickedDialogEntryIndex = position;
        onClick(getDialog(), DialogInterface.BUTTON_POSITIVE);
        getDialog().dismiss();
    }
}
