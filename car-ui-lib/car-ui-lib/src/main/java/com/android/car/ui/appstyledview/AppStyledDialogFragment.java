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
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.android.car.ui.appstyledview.AppStyledViewController.AppStyledDismissListener;

/**
 * App styled dialog Fragment used to display a view that cannot be customized via OEM. Dialog
 * Fragment will inflate a layout and add the view provided by the application into the layout.
 * Everything other than the view within the layout can be customized by OEM.
 *
 * Apps should not use this directly. App's should use {@link AppStyledDialogController}.
 */
public final class AppStyledDialogFragment extends DialogFragment {

    private final AppStyledViewController mController;
    private AppStyledDismissListener mOnDismissListener;
    private View mContent;

    public AppStyledDialogFragment(AppStyledViewController controller) {
        mController = controller;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return mController.getAppStyledView(container, mContent);
    }

    @Override
    public void onResume() {
        super.onResume();

        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = mController.getAppStyledViewDialogWidth();
        params.height = mController.getAppStyledViewDialogHeight();
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mOnDismissListener != null) {
            mOnDismissListener.onDismiss();
        }
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        return dialog;
    }

    void setContent(View contentView) {
        mContent = contentView;
    }

    void setOnDismissListener(AppStyledDismissListener listener) {
        mOnDismissListener = listener;
    }
}
