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
import androidx.annotation.NonNull;
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

    private static final String LEFT_INSET_TAG = "car_ui_left_inset";
    private static final String RIGHT_INSET_TAG = "car_ui_right_inset";
    private static final String TOP_INSET_TAG = "car_ui_top_inset";
    private static final String BOTTOM_INSET_TAG = "car_ui_bottom_inset";

    private boolean mHasInitializedBaseLayout = false;
    private ToolbarController mToolbarController;

    @Nullable private View mLeftInsetView;
    @Nullable private View mRightInsetView;
    @Nullable private View mTopInsetView;
    @Nullable private View mBottomInsetView;
    private boolean mInsetsDirty = true;

    @NonNull
    private Insets mInsets = new Insets();

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

        View.OnLayoutChangeListener layoutChangeListener =
                (View v, int left, int top, int right, int bottom,
                 int oldLeft, int oldTop, int oldRight, int oldBottom) -> {
                    if (left != oldLeft || top != oldTop
                            || right != oldRight || bottom != oldBottom) {
                        mInsetsDirty = true;
                    }
                };

        mLeftInsetView = decor.findViewWithTag(LEFT_INSET_TAG);
        mRightInsetView = decor.findViewWithTag(RIGHT_INSET_TAG);
        mTopInsetView = decor.findViewWithTag(TOP_INSET_TAG);
        mBottomInsetView = decor.findViewWithTag(BOTTOM_INSET_TAG);

        if (mLeftInsetView != null) {
            mLeftInsetView.addOnLayoutChangeListener(layoutChangeListener);
        }
        if (mRightInsetView != null) {
            mRightInsetView.addOnLayoutChangeListener(layoutChangeListener);
        }
        if (mTopInsetView != null) {
            mTopInsetView.addOnLayoutChangeListener(layoutChangeListener);
        }
        if (mBottomInsetView != null) {
            mBottomInsetView.addOnLayoutChangeListener(layoutChangeListener);
        }
        contentView.addOnLayoutChangeListener(layoutChangeListener);

        // The global layout listener will run after all the individual layout change listeners
        // so that we only updateInsets once per layout, even if multiple inset views changed
        contentView.getViewTreeObserver().addOnGlobalLayoutListener(this::updateInsets);

        mHasInitializedBaseLayout = true;
    }

    private void updateInsets() {
        if (!mInsetsDirty) {
            return;
        }

        View content = requireViewById(android.R.id.content);

        // Calculate how much each inset view overlays the content view
        Insets insets = new Insets();
        if (mTopInsetView != null) {
            insets.mTop = Math.max(0, getBottomOfView(mTopInsetView) - getTopOfView(content));
        }
        if (mBottomInsetView != null) {
            insets.mBottom = Math.max(0, getBottomOfView(content) - getTopOfView(mBottomInsetView));
        }
        if (mLeftInsetView != null) {
            insets.mLeft = Math.max(0, getRightOfView(mLeftInsetView) - getLeftOfView(content));
        }
        if (mRightInsetView != null) {
            insets.mRight = Math.max(0, getRightOfView(content) - getLeftOfView(mRightInsetView));
        }

        mInsetsDirty = false;
        if (!insets.equals(mInsets)) {
            mInsets = insets;
            onCarUiInsetsChanged(insets);
        }
    }

    /**
     * This method will be called whenever the insets change.
     *
     * The insets represent how much your content should be indented from each edge of the screen.
     * You can still draw up to the edge of the screen, and should do so for background content
     * like a scrolling list, a map, or background image, but this content may be partially
     * or completely covered.
     *
     * The default implementation, if this is not overridden, is to apply the insets as padding
     * to your content view, forcing all of your content to be within the insets.
     */
    protected void onCarUiInsetsChanged(Insets insets) {
        findViewById(android.R.id.content).setPadding(insets.getLeft(), insets.getTop(),
                insets.getRight(), insets.getBottom());
    }

    @NonNull
    public Insets getCarUiInsets() {
        return mInsets;
    }

    private int getLeftOfView(View v) {
        int[] position = new int[2];
        v.getLocationOnScreen(position);
        return position[0];
    }

    private int getRightOfView(View v) {
        int[] position = new int[2];
        v.getLocationOnScreen(position);
        return position[0] + v.getWidth();
    }

    private int getTopOfView(View v) {
        int[] position = new int[2];
        v.getLocationOnScreen(position);
        return position[1];
    }

    private int getBottomOfView(View v) {
        int[] position = new int[2];
        v.getLocationOnScreen(position);
        return position[1] + v.getHeight();
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
