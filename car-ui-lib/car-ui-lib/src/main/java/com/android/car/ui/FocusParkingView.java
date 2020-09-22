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

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS;

import static com.android.car.ui.utils.RotaryConstants.ACTION_HIDE_IME;
import static com.android.car.ui.utils.RotaryConstants.ACTION_RESTORE_DEFAULT_FOCUS;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;

import com.android.car.ui.utils.ViewUtils;

/**
 * A transparent {@link View} that can take focus. It's used by {@link
 * com.android.car.rotary.RotaryService} to support rotary controller navigation. Each {@link
 * android.view.Window} should have one FocusParkingView as the first focusable view in the view
 * tree, and outside of all {@link FocusArea}s. If multiple FocusParkingView are added in the
 * window, only the first one will be focusable.
 * <p>
 * Android doesn't clear focus automatically when focus is set in another window. If we try to clear
 * focus in the previous window, Android will re-focus a view in that window, resulting in two
 * windows being focused simultaneously. Adding this view to each window can fix this issue. This
 * view is transparent and its default focus highlight is disabled, so it's invisible to the user no
 * matter whether it's focused or not. It can take focus so that RotaryService can "park" the focus
 * on it to remove the focus highlight.
 * <p>
 * If the focused view is scrolled off the screen, Android will refocus the first focusable view in
 * the window. The FocusParkingView should be the first view so that it gets focus. The
 * RotaryService detects this and moves focus to the scrolling container.
 * <p>
 * If there is only one focus area in the current window, rotating the controller within the focus
 * area will cause RotaryService to move the focus around from the view on the right to the view on
 * the left or vice versa. Adding this view to each window can fix this issue. When RotaryService
 * finds out the focus target is a FocusParkingView, it will know a wrap-around is going to happen.
 * Then it will avoid the wrap-around by not moving focus.
 */
public class FocusParkingView extends View {
    private static final String TAG = "FocusParkingView";

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
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) {
            // We need to clear the focus (by parking the focus on the FocusParkingView) once the
            // current window goes to background. This can't be done by RotaryService because
            // RotaryService sees the window as removed, thus can't perform any action (such as
            // focus, clear focus) on the nodes in the window. So FocusParkingView has to grab the
            // focus proactively.
            requestFocus();
        }
        super.onWindowFocusChanged(hasWindowFocus);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return FocusParkingView.class.getName();
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        switch (action) {
            case ACTION_RESTORE_DEFAULT_FOCUS:
                View root = getRootView();

                // If there is a view focused by default and it can take focus, move focus to it.
                View defaultFocus = ViewUtils.findFocusedByDefaultView(root);
                if (defaultFocus != null) {
                    return defaultFocus.requestFocus();
                }

                // If there is a primary focus view, move focus to it.
                View primaryFocus = ViewUtils.findPrimaryFocusView(root);
                if (primaryFocus != null) {
                    return primaryFocus.requestFocus();
                }

                return false;
            case ACTION_HIDE_IME:
                InputMethodManager inputMethodManager =
                        getContext().getSystemService(InputMethodManager.class);
                return inputMethodManager.hideSoftInputFromWindow(getWindowToken(),
                        /* flags= */ 0);
            case ACTION_FOCUS:
                // Don't leave this to View to handle as it will exit touch mode.
                if (!hasFocus()) {
                    return requestFocus();
                }
                break;
        }
        return super.performAccessibilityAction(action, arguments);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // If there is a FocusParkingView already, make the one after in the view tree
        // non-focusable.
        boolean []isBefore = new boolean[1];
        View anotherFpv = ViewUtils.depthFirstSearch(getRootView(), v -> {
            if (this == v) {
                isBefore[0] = true;
            }
            return v != this && v instanceof FocusParkingView && v.isFocusable();
        });
        if (anotherFpv != null) {
            Log.w(TAG, "There should be only one FocusParkingView in the window");
            if (isBefore[0]) {
                anotherFpv.setFocusable(false);
            } else {
                setFocusable(false);
            }
        }
    }
}
