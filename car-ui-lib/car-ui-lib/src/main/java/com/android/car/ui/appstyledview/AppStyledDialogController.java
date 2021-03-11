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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.android.car.ui.appstyledview.AppStyledViewController.AppStyledDismissListener;
import com.android.car.ui.appstyledview.AppStyledViewController.AppStyledVCloseClickListener;
import com.android.car.ui.appstyledview.AppStyledViewController.AppStyledViewNavIcon;
import com.android.car.ui.sharedlibrarysupport.SharedLibraryFactorySingleton;

import java.util.Objects;

/**
 * Controller to interact with the app styled view UI.
 */
public final class AppStyledDialogController {

    @NonNull
    private final AppStyledViewController mAppStyledViewController;
    @NonNull
    private AppStyledDialogFragment mFragment;

    public AppStyledDialogController(@NonNull Context context) {
        Objects.requireNonNull(context);
        mAppStyledViewController = SharedLibraryFactorySingleton.get(context)
            .createAppStyledView();
    }

    /**
     * Sets the content view to de displayed in AppStyledView.
     */
    public void setContentView(@NonNull View contentView) {
        Objects.requireNonNull(contentView);

        mFragment = new AppStyledDialogFragment(mAppStyledViewController);

        mFragment.setContent(contentView);
        mAppStyledViewController.setOnCloseClickListener(mFragment::dismiss);
    }

    /**
     * Sets the nav icon to be used.
     */
    public void setNavIcon(@AppStyledViewNavIcon int navIcon) {
        mAppStyledViewController.setNavIcon(navIcon);
    }

    /**
     * Displays the dialog fragment to the user with the custom view provided by the app.
     */
    public void show(@NonNull FragmentManager fm) {
        mFragment.show(fm, "AppStyledFragment");
    }

    /**
     * Sets the {@link AppStyledVCloseClickListener}
     */
    public void setOnCloseClickListener(@NonNull AppStyledVCloseClickListener listener) {
        mAppStyledViewController.setOnCloseClickListener(() -> {
            mFragment.dismiss();
            listener.onClick();
        });
    }

    /**
     * Sets the {@link AppStyledDismissListener}
     */
    public void setOnDismissListener(@NonNull AppStyledDismissListener listener) {
        mFragment.setOnDismissListener(listener);
    }

    /**
     * Returns the width of the AppStyledView
     */
    public int getAppStyledViewDialogWidth() {
        return mAppStyledViewController.getAppStyledViewDialogWidth();
    }

    /**
     * Returns the height of the AppStyledView
     */
    public int getAppStyledViewDialogHeight() {
        return mAppStyledViewController.getAppStyledViewDialogHeight();
    }
}
