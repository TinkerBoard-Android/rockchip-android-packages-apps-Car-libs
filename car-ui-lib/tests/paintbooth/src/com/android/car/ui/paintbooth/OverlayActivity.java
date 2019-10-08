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

package com.android.car.ui.paintbooth;

import android.car.userlib.CarUserManagerHelper;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Activity representing lists of RROs ppackage name as title and the corresponding target package
 * name as the summary with a toggle switch to enable/disable the overlays.
 */
public class OverlayActivity extends AppCompatActivity {

    private static final String TAG = OverlayActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(android.R.id.content, new OverlayFragment())
                    .commit();
        }
    }

    /** PreferenceFragmentCompat that sets the preference hierarchy from XML */
    public static class OverlayFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load the preferences from an XML resource
            setPreferencesFromResource(R.xml.preference_overlays, rootKey);

            CarUserManagerHelper carUserManagerHelper = new CarUserManagerHelper(getContext());

            final IOverlayManager overlayManager = IOverlayManager.Stub.asInterface(
                    ServiceManager.getService(Context.OVERLAY_SERVICE));

            Map<String, List<OverlayInfo>> overlays = Collections.emptyMap();
            try {
                overlays = overlayManager.getAllOverlays(
                        carUserManagerHelper.getCurrentForegroundUserId());
            } catch (RemoteException e) {
                Toast.makeText(getContext(), "Something went wrong internally.",
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, "can't apply overlay: ", e);
            }

            for (String targetPackage : overlays.keySet()) {

                for (OverlayInfo overlayPackage : overlays.get(targetPackage)) {
                    SwitchPreference switchPreference = new SwitchPreference(getContext());
                    switchPreference.setKey(overlayPackage.packageName);
                    switchPreference.setTitle(overlayPackage.packageName);
                    switchPreference.setSummary(targetPackage);
                    switchPreference.setChecked(overlayPackage.state == OverlayInfo.STATE_ENABLED);

                    switchPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                        applyOverlay(overlayPackage.packageName, (boolean) newValue, overlayManager,
                                carUserManagerHelper);
                        return true;
                    });

                    getPreferenceScreen().addPreference(switchPreference);
                }
            }
        }

        private void applyOverlay(String overlayPackage, boolean enableOverlay,
                IOverlayManager overlayManager, CarUserManagerHelper carUserManagerHelper) {
            try {
                overlayManager.setEnabled(overlayPackage, enableOverlay,
                        carUserManagerHelper.getCurrentForegroundUserId());
            } catch (RemoteException e) {
                Toast.makeText(getContext(), "Something went wrong internally.",
                        Toast.LENGTH_LONG).show();
                Log.w(TAG, "Can't change theme", e);
            }
        }
    }
}
