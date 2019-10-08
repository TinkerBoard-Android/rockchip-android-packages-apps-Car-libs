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

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Switch;

import com.android.car.ui.R;

/**
 * Switch preference widget. This widget is exactly similar to switch widget just that it calls
 * {@link Switch#jumpDrawablesToCurrentState} on each click.
 */
public class PreferenceSwitchWidget extends Switch {

    private boolean mEnableAnimation = true;

    public PreferenceSwitchWidget(Context context) {
        super(context);
        init(context);
    }

    public PreferenceSwitchWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public PreferenceSwitchWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public PreferenceSwitchWidget(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mEnableAnimation = context.getResources().getBoolean(
                R.bool.car_ui_preference_switch_toggle_show_animation);
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);

        if (!mEnableAnimation) {
            jumpDrawablesToCurrentState();
        }
    }
}
