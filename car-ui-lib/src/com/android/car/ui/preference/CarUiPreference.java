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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.view.ViewCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.car.ui.R;

/**
 * This class extends the base {@link Preference} class. Adds the support to add a drawable icon to
 * the preference if there is one of fragment, intent or onPreferenceClickListener set.
 */
public class CarUiPreference extends Preference {

    private Context mContext;
    private boolean mShowChevron;
    private String mMessageToShowWhenDisabledPreferenceClicked;

    private boolean mShouldShowRippleOnDisabledPreference;
    private boolean mEnabledAppearance = true;

    public CarUiPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public CarUiPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Preference_CarUi_Preference);
    }

    public CarUiPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.carUiPreferenceStyle);
    }

    public CarUiPreference(Context context) {
        this(context, null);
    }

    public void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mContext = context;

        TypedArray a = getContext().obtainStyledAttributes(
                attrs,
                R.styleable.CarUiPreference,
                defStyleAttr,
                defStyleRes);

        mShowChevron = a.getBoolean(R.styleable.CarUiPreference_showChevron, true);
        mShouldShowRippleOnDisabledPreference = a.getBoolean(
                R.styleable.CarUiPreference_showRippleOnDisabledPreference, false);
    }


    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        boolean viewEnabled = mEnabledAppearance;
        if (!viewEnabled) {
            enableView(holder.itemView, false, true);
        }
    }

    private void enableView(View view, boolean enabled, boolean isRootView) {
        if (!isRootView) {
            view.setEnabled(enabled);
        } else if (!mShouldShowRippleOnDisabledPreference) {
            ViewCompat.setBackground(view, null);
        }
        if (view instanceof ViewGroup) {
            ViewGroup grp = (ViewGroup) view;
            for (int index = 0; index < grp.getChildCount(); index++) {
                enableView(grp.getChildAt(index), enabled, false);
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        mEnabledAppearance = enabled;
        if (!enabled) {
            // Enabled state can change dependent preferences' states, so notify
            notifyDependencyChange(true);
            notifyChanged();
        }
    }

    @Override
    public void onAttached() {
        super.onAttached();

        boolean allowChevron = mContext.getResources().getBoolean(
                R.bool.car_ui_preference_show_chevron);

        if (!allowChevron || !mShowChevron) {
            return;
        }

        if (getOnPreferenceClickListener() != null || getIntent() != null
                || getFragment() != null) {
            setWidgetLayoutResource(R.layout.car_ui_preference_chevron);
        }
    }

    @Override
    protected void onClick() {
        if (mEnabledAppearance) {
            super.onClick();
        } else if (!mMessageToShowWhenDisabledPreferenceClicked.isEmpty()) {
            Toast toast = Toast.makeText(mContext, mMessageToShowWhenDisabledPreferenceClicked,
                    Toast.LENGTH_LONG);
            toast.show();
        }
    }

    public void setShowChevron(boolean showChevron) {
        mShowChevron = showChevron;
    }

    public void setShouldShowRippleOnDisabledPreference(boolean showRipple) {
        mShouldShowRippleOnDisabledPreference = showRipple;
    }

    public void setMessageToShowWhenDisabledPreferenceClicked(String message) {
        mMessageToShowWhenDisabledPreferenceClicked = message;
    }
}
