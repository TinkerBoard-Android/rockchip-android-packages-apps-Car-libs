/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.ui.paintbooth.preferences;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.car.ui.paintbooth.R;
import com.android.car.ui.preference.CarUiPreference;
import com.android.car.ui.preference.CarUiTwoActionBasePreference;
import com.android.car.ui.preference.CarUiTwoActionIconPreference;
import com.android.car.ui.preference.CarUiTwoActionSwitchPreference;
import com.android.car.ui.preference.CarUiTwoActionTextPreference;
import com.android.car.ui.preference.PreferenceFragment;

import java.util.Objects;

/**
 * Fragment to load preferences
 */
public class PreferenceDemoFragment extends PreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preference_samples, rootKey);
        CarUiPreference preferenceDisabledWithoutRipple = requirePreference(
                "preference_disabled_without_ripple");
        preferenceDisabledWithoutRipple.setEnabled(false);
        preferenceDisabledWithoutRipple.setMessageToShowWhenDisabledPreferenceClicked(
                "I am disabled because...");
        preferenceDisabledWithoutRipple.setShouldShowRippleOnDisabledPreference(false);

        CarUiPreference preferenceDisabledWithRipple = requirePreference(
                "preference_disabled_with_ripple");
        preferenceDisabledWithRipple.setEnabled(false);
        preferenceDisabledWithRipple.setMessageToShowWhenDisabledPreferenceClicked(
                "I am disabled because...");
        preferenceDisabledWithRipple.setShouldShowRippleOnDisabledPreference(true);

        setupTwoActionPreferenceClickListeners(requirePreference("twoactiontext"));
        setupTwoActionPreferenceClickListeners(requirePreference("twoactiontextborderless"));
        setupTwoActionPreferenceClickListeners(requirePreference("twoactionicon"));
        setupTwoActionPreferenceClickListeners(requirePreference("twoactionswitch"));
    }

    private void setupTwoActionPreferenceClickListeners(CarUiTwoActionBasePreference preference) {
        if (preference instanceof CarUiTwoActionSwitchPreference) {
            ((CarUiTwoActionSwitchPreference) preference).setOnSecondaryActionClickListener(
                    (selected) -> preference.setSecondaryActionEnabled(false));
        } else if (preference instanceof CarUiTwoActionTextPreference) {
            ((CarUiTwoActionTextPreference) preference).setOnSecondaryActionClickListener(
                    () -> preference.setSecondaryActionEnabled(false));
        } else {
            ((CarUiTwoActionIconPreference) preference).setOnSecondaryActionClickListener(
                    () -> preference.setSecondaryActionEnabled(false));
        }

        preference.setOnPreferenceClickListener((pref) -> {
            if (!preference.isSecondaryActionEnabled()) {
                preference.setSecondaryActionEnabled(true);
            } else {
                preference.setSecondaryActionVisible(
                        !preference.isSecondaryActionVisible());
            }
            return true;
        });
    }

    @NonNull
    private <T extends Preference> T requirePreference(CharSequence key) {
        T pref = findPreference(key);
        return Objects.requireNonNull(pref);
    }
}
