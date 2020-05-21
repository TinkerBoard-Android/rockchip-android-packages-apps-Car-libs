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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * A transparent {@link View} that can take focus. It's used by {@link
 * com.android.car.rotary.RotaryService} to support rotary controller navigation. A {@link
 * FocusParkingView} must be placed outside of all {@link FocusArea}s and its width and height must
 * not be zero. Each {@link android.view.Window} must have at least one FocusParkingView.
 * <p>
 * Android doesn't clear focus automatically when focus is set in another window. If we try to clear
 * focus in the previous window, Android will re-focus a view in that window, resulting in two
 * windows being focused simultaneously. Adding this view to each window can fix this issue. This
 * view is transparent and its default focus highlight is disabled, so it's invisible to the user no
 * matter whether it's focused or not. It can take focus so that RotaryService can "park" the focus
 * on it to remove the focus highlight.
 * <p>
 * If there is only one focus area in the current window, rotating the controller within the focus
 * area will cause RotaryService to move the focus around from the view on the right to the view on
 * the left or vice versa. Adding this view to each window can fix this issue. When RotaryService
 * finds out the focus target is a FocusParkingView, it will know a wrap-around is going to happen.
 * Then it will avoid the wrap-around by not moving focus.
 */
public class FocusParkingView extends View {

    public FocusParkingView(Context context) {
        super(context);
        init();
    }

    public FocusParkingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FocusParkingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public FocusParkingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        // This view is focusable, visible and enabled so it can take focus.
        setFocusable(View.FOCUSABLE);
        setVisibility(VISIBLE);
        setEnabled(true);

        // This view is not clickable so it won't affect the app's behavior when the user clicks on
        // it by accident.
        setClickable(false);

        // This view is always transparent.
        setAlpha(0f);

        // Prevent Android from drawing the default focus highlight for this view when it's focused.
        setDefaultFocusHighlightEnabled(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // This size of the view is always 1 x 1 pixel, no matter what value is set in the layout
        // file (match_parent, wrap_content, 100dp, 0dp, etc). Small size is to ensure it has little
        // impact on the layout, non-zero size is to ensure it can take focus.
        setMeasuredDimension(1, 1);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return FocusParkingView.class.getName();
    }
}
