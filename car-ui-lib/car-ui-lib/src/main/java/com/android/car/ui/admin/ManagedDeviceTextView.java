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
package com.android.car.ui.admin;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.android.car.ui.R;

/**
 * Custom view used to display a disclaimer when a device is managed by a device owner.
 */
public final class ManagedDeviceTextView extends TextView {

    public ManagedDeviceTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ManagedDeviceTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ManagedDeviceTextView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        if (isManagedDevice(context)) {
            setText(R.string.car_ui_managed_device_message);
        } else {
            setVisibility(View.GONE);
        }
    }

    private static boolean isManagedDevice(Context context) {
        PackageManager pm = context.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)) return false;

        DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        return dpm != null && dpm.isDeviceManaged();
    }
}
