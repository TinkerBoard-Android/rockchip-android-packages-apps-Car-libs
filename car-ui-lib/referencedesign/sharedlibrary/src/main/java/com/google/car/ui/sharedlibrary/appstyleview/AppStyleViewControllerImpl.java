/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.google.car.ui.sharedlibrary.appstyleview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ScrollView;

import com.android.car.ui.sharedlibrary.oemapis.appstyledview.AppStyledViewControllerOEMV1;

import com.google.car.ui.sharedlibrary.R;

/** The OEM implementation for {@link AppStyledViewControllerOEMV1} for a AppStyledView. */
public class AppStyleViewControllerImpl implements AppStyledViewControllerOEMV1 {

    private final Context mContext;
    private View mAppStyleView;
    private int mNavIcon;
    private Runnable mCloseListener = null;

    public AppStyleViewControllerImpl(Context context) {
        mContext = context;
    }

    @Override
    public View getAppStyledView(ViewGroup container, View content) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        mAppStyleView = inflater.inflate(R.layout.app_styled_view, null, false);

        ScrollView scrollview = mAppStyleView.findViewById(R.id.app_styled_content);
        scrollview.addView(content);

        ImageView navIcon = mAppStyleView.findViewById(R.id.app_styled_view_icon_close);
        if (mNavIcon == 0) {
            navIcon.setImageResource(R.drawable.icon_back);
        } else if (mNavIcon == 1) {
            navIcon.setImageResource(R.drawable.icon_close);
        } else {
            navIcon.setImageResource(R.drawable.icon_close);
        }

        if (mCloseListener != null) {
            navIcon.setOnClickListener((v) -> {
                mCloseListener.run();
            });
        }

        return mAppStyleView;
    }

    @Override
    public void setOnCloseClickListener(Runnable listener) {
        mCloseListener = listener;
    }

    @Override
    public int getAppStyledViewDialogWidth() {
        return 1000;
    }

    @Override
    public int getAppStyledViewDialogHeight() {
        return 400;
    }

    @Override
    public void setNavIcon(int navIcon) {
        mNavIcon = navIcon;
    }
}
