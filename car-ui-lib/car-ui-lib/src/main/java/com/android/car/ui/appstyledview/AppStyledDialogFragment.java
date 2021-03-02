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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.car.ui.R;
import com.android.car.ui.appstyledview.AppStyledViewController.AppStyledVCloseClickListener;
import com.android.car.ui.recyclerview.CarUiRecyclerView;

/**
 * App styled dialog Fragment used to display a view that cannot be customized via OEM. Dialog
 * Fragment
 * will inflate a layout and add the view provided by the application into the layout. Everything
 * other than the view within the layout can be customized by OEM.
 */
public class AppStyledDialogFragment extends DialogFragment {

    private View mContent;
    private ImageView mCloseView;
    private AppStyledVCloseClickListener mAppStyledVCloseClickListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // create ContextThemeWrapper from the original Activity Context with the custom theme
        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(),
                R.style.Theme_CarUi);

        // clone the inflater using the ContextThemeWrapper
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

        View appStyledView = localInflater.inflate(R.layout.car_ui_app_styled_view, container,
                false);

        CarUiRecyclerView rv = appStyledView.findViewById(R.id.car_ui_app_styled_content);

        AppStyledRecyclerViewAdapter adapter = new AppStyledRecyclerViewAdapter(mContent);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        mCloseView = appStyledView.findViewById(R.id.car_ui_app_styled_view_icon_close);
        mCloseView.setOnClickListener(v -> {
            dismiss();
            if (mAppStyledVCloseClickListener != null) {
                mAppStyledVCloseClickListener.onClick();
            }
        });

        return appStyledView;
    }

    @Override
    public void onResume() {
        super.onResume();

        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = getContext().getResources().getDimensionPixelSize(
                R.dimen.car_ui_app_styled_dialog_width);
        params.height = getContext().getResources().getDimensionPixelSize(
                R.dimen.car_ui_app_styled_dialog_height);
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
    }


    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    void setContent(View view) {
        mContent = view;
    }

    void setOnCloseClickListener(AppStyledVCloseClickListener listener) {
        mAppStyledVCloseClickListener = listener;
    }
}
