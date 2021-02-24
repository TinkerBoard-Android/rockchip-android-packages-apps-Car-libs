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

import android.view.View;

import androidx.fragment.app.FragmentManager;

/**
 * Controller to interact with the app styled Dialog Fragment.
 */
public class AppStyledViewController {

    /**
     * Callback to be invoked when the close icon is clicked.
     */
    public interface AppStyledVCloseClickListener {

        /**
         * Called when the close icon is clicked.
         */
        void onClick();
    }

    private View mContent;
    private FragmentManager mFragmentManager;
    private AppStyledDialogFragment mFragment;

    public AppStyledViewController(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
        mFragment = new AppStyledDialogFragment();
    }

    /**
     * Sets the content view to be displayed in the dialog fragment.
     */
    public void setContentView(View contentView) {
        mContent = contentView;
    }

    /**
     * Displays the dialog fragment to the user with the custom view provided by the app.
     */
    public void show() {
        if (mContent == null) {
            throw new RuntimeException("call setContentView(view) before calling show()");
        }
        mFragment.setContent(mContent);
        mFragment.show(mFragmentManager, "AppStyledFragment");
    }

    /**
     * Sets the AppStyledVCloseClickListener on the close icon.
     */
    public void setOnCloseClickListener(AppStyledVCloseClickListener listener) {
        mFragment.setOnCloseClickListener(listener);
    }
}
