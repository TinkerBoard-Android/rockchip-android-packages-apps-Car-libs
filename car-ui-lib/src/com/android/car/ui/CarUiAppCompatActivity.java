/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.car.ui;

import android.content.res.TypedArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.car.ui.toolbar.ToolbarController;
import com.android.car.ui.toolbar.ToolbarControllerImpl;

/**
 * {@link CarUiAppCompatActivity} is a subclass of {@link AppCompatActivity} intended to provide
 * smoother access to the {@link com.android.car.ui.toolbar.Toolbar}. This is still very
 * experimental code, and should not yet be adopted by apps.
 *
 * For instance, you cannot currently have content underneath a toolbar provided this way.
 */
public class CarUiAppCompatActivity extends AppCompatActivity {

    private boolean mHasInitializedBaseLayout = false;
    private ToolbarController mToolbarController;

    @Override
    @CallSuper
    public void setContentView(View v) {
        setupBaseLayout();
        super.setContentView(v);
    }

    @Override
    @CallSuper
    public void setContentView(int resId) {
        setupBaseLayout();
        super.setContentView(resId);
    }

    @Override
    @CallSuper
    public void setContentView(View v, ViewGroup.LayoutParams lp) {
        setupBaseLayout();
        super.setContentView(v, lp);
    }

    @Override
    @CallSuper
    public void addContentView(View v, ViewGroup.LayoutParams lp) {
        setupBaseLayout();
        super.addContentView(v, lp);
    }

    @Override
    @CallSuper
    public void setTitle(CharSequence title) {
        setupBaseLayout();
        if (mToolbarController != null) {
            mToolbarController.setTitle(title);
        }
        super.setTitle(title);
    }

    @Override
    @CallSuper
    public void setTitle(int title) {
        setupBaseLayout();
        if (mToolbarController != null) {
            mToolbarController.setTitle(title);
        }
        super.setTitle(title);
    }

    private void setupBaseLayout() {
        if (mHasInitializedBaseLayout) {
            return;
        }

        TypedArray a = getTheme().obtainStyledAttributes(R.styleable.CarUi);

        try {
            if (!a.getBoolean(R.styleable.CarUi_carUiActionBar, false)) {
                mHasInitializedBaseLayout = true;
                return;
            }
        } finally {
            a.recycle();
        }

        // First call super.setContentView so that AppCompatActivity creates its own
        // subdecor. Then we can add ours under that one as opposed to the other way around.
        super.setContentView(new View(this));

        final ViewGroup windowContentView = getWindow().findViewById(android.R.id.content);

        View decor = LayoutInflater.from(this)
                .inflate(R.layout.car_ui_base_layout_toolbar, windowContentView, false);
        ViewGroup contentView = decor.requireViewById(
                R.id.content);

        mToolbarController = new ToolbarControllerImpl(decor);
        mToolbarController.setTitle(getTitle());

        windowContentView.removeAllViews();
        windowContentView.addView(decor);

        // Change our content FrameLayout to use the android.R.id.content id.
        // Useful for fragments.
        windowContentView.setId(View.NO_ID);
        contentView.setId(android.R.id.content);

        mHasInitializedBaseLayout = true;
    }

    /**
     * Gets the toolbar controller if the "carUiAppBar" theme attribute is true.
     *
     * If this is called before one of the setContentView() variants, or if that attribute is false,
     * we will return null.
     */
    @Nullable
    public ToolbarController getCarUiToolbar() {
        return mToolbarController;
    }
}
