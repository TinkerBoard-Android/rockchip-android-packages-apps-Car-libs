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

package com.android.car.ui.appstyledview;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;

/**
 * Controller to interact with the app styled view.
 */
public class AppStyledViewControllerImpl implements AppStyledViewController {

    private final Context mContext;
    private @AppStyledViewNavIcon int mAppStyleViewNavIcon;
    private AppStyledVCloseClickListener mAppStyledVCloseClickListener = null;

    public AppStyledViewControllerImpl(Context context) {
        mContext = context;
    }

    @Override
    public void setNavIcon(@AppStyledViewNavIcon int navIcon) {
        mAppStyleViewNavIcon = navIcon;
    }

    /**
     * Sets the AppStyledVCloseClickListener on the close icon.
     */
    @Override
    public void setOnCloseClickListener(AppStyledVCloseClickListener listener) {
        mAppStyledVCloseClickListener = listener;
    }

    @Override
    public View getAppStyledView(@Nullable ViewGroup container, View contentView) {
        // create ContextThemeWrapper from the original Activity Context with the custom theme
        final Context contextThemeWrapper = new ContextThemeWrapper(mContext, R.style.Theme_CarUi);
        LayoutInflater inflater = LayoutInflater.from(mContext);
        // clone the inflater using the ContextThemeWrapper
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

        View appStyleView = localInflater.inflate(R.layout.car_ui_app_styled_view, container,
                false);

        RecyclerView rv = appStyleView.findViewById(R.id.car_ui_app_styled_content);

        AppStyledRecyclerViewAdapter adapter = new AppStyledRecyclerViewAdapter(contentView);
        rv.setLayoutManager(new LinearLayoutManager(mContext));
        rv.setAdapter(adapter);

        ImageView close = appStyleView.findViewById(R.id.car_ui_app_styled_view_icon_close);
        if (mAppStyleViewNavIcon == AppStyledViewNavIcon.BACK) {
            close.setImageResource(R.drawable.car_ui_icon_arrow_back);
        } else if (mAppStyleViewNavIcon == AppStyledViewNavIcon.CLOSE) {
            close.setImageResource(R.drawable.car_ui_icon_close);
        } else {
            close.setImageResource(R.drawable.car_ui_icon_close);
        }

        if (mAppStyledVCloseClickListener != null) {
            close.setOnClickListener((v) -> {
                mAppStyledVCloseClickListener.onClick();
            });
        }

        return appStyleView;
    }

    @Override
    public int getAppStyledViewDialogWidth() {
        return mContext.getResources().getDimensionPixelSize(
                R.dimen.car_ui_app_styled_dialog_width);
    }

    @Override
    public int getAppStyledViewDialogHeight() {
        return mContext.getResources().getDimensionPixelSize(
                R.dimen.car_ui_app_styled_dialog_height);
    }
}
