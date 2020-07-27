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

import static com.android.car.ui.utils.RotaryConstants.FOCUS_ACTION_TYPE;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_DEFAULT;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_FIRST;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_INVALID;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.android.car.ui.utils.CarUiUtils;
import com.android.car.ui.utils.RotaryConstants;

/**
 * A {@link LinearLayout} used as a navigation block for the rotary controller.
 * <p>
 * The {@link com.android.car.rotary.RotaryService} looks for instances of {@link FocusArea} in the
 * view hierarchy when handling rotate and nudge actions. When receiving a rotation event ({@link
 * android.car.input.RotaryEvent}), RotaryService will move the focus to another {@link View} that
 * can take focus within the same FocusArea. When receiving a nudge event ({@link
 * KeyEvent#KEYCODE_SYSTEM_NAVIGATION_UP}, {@link KeyEvent#KEYCODE_SYSTEM_NAVIGATION_DOWN}, {@link
 * KeyEvent#KEYCODE_SYSTEM_NAVIGATION_LEFT}, or {@link KeyEvent#KEYCODE_SYSTEM_NAVIGATION_RIGHT}),
 * RotaryService will move the focus to another view that can take focus in another (typically
 * adjacent) FocusArea.
 * <p>
 * If enabled, FocusArea can draw highlights when one of its descendants has focus and it's not in
 * touch mode.
 * <p>
 * When creating a navigation block in the layout file, if you intend to use a LinearLayout as a
 * container for that block, just use a FocusArea instead; otherwise wrap the block in a FocusArea.
 * <p>
 * DO NOT nest a FocusArea inside another FocusArea because it will result in undefined navigation
 * behavior.
 */
public class FocusArea extends LinearLayout {

    private static final String TAG = "FocusArea";

    /** Whether the FocusArea's descendant has focus (the FocusArea itself is not focusable). */
    private boolean mHasFocus;

    /**
     * Whether to draw {@link #mForegroundHighlight} when one of the FocusArea's descendants has
     * focus and it's not in touch mode.
     */
    private boolean mEnableForegroundHighlight;

    /**
     * Whether to draw {@link #mBackgroundHighlight} when one of the FocusArea's descendants has
     * focus and it's not in touch mode.
     */
    private boolean mEnableBackgroundHighlight;

    /**
     * Highlight (typically outline of the FocusArea) drawn on top of the FocusArea and its
     * descendants.
     */
    private Drawable mForegroundHighlight;

    /**
     * Highlight (typically a solid or gradient shape) drawn on top of the FocusArea but behind its
     * descendants.
     */
    private Drawable mBackgroundHighlight;

    /** The padding (in pixels) of the FocusArea highlight. */
    private int mPaddingLeft;
    private int mPaddingRight;
    private int mPaddingTop;
    private int mPaddingBottom;

    /** The ID of the view specified in {@code app:defaultFocus}. */
    private int mDefaultFocusId = View.NO_ID;
    /** The view specified in {@code app:defaultFocus}. */
    @Nullable
    private View mDefaultFocusView;

    public FocusArea(Context context) {
        super(context);
        init(context, null);
    }

    public FocusArea(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FocusArea(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public FocusArea(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        mEnableForegroundHighlight = getContext().getResources().getBoolean(
                R.bool.car_ui_enable_focus_area_foreground_highlight);
        mEnableBackgroundHighlight = getContext().getResources().getBoolean(
                R.bool.car_ui_enable_focus_area_background_highlight);
        mForegroundHighlight = getContext().getResources().getDrawable(
                R.drawable.car_ui_focus_area_foreground_highlight, getContext().getTheme());
        mBackgroundHighlight = getContext().getResources().getDrawable(
                R.drawable.car_ui_focus_area_background_highlight, getContext().getTheme());

        // By default all ViewGroup subclasses do not call their draw() and onDraw() methods. We
        // should enable it since we override these methods.
        setWillNotDraw(false);

        // Update highlight of the FocusArea when the focus of its descendants has changed.
        getViewTreeObserver().addOnGlobalFocusChangeListener(
                (oldFocus, newFocus) -> {
                    boolean hasFocus = hasFocus();
                    if (mHasFocus != hasFocus) {
                        mHasFocus = hasFocus;
                        invalidate();
                    }
                });

        initAttrs(context, attrs);
    }

    private void initAttrs(Context context, @Nullable AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FocusArea);
        try {
            mDefaultFocusId = a.getResourceId(R.styleable.FocusArea_defaultFocus, View.NO_ID);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mDefaultFocusId != View.NO_ID) {
            mDefaultFocusView = CarUiUtils.requireViewByRefId(this, mDefaultFocusId);
        }
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        // FocusArea is not focusable, so we focus on its descendant when handling ACTION_FOCUS.
        if (action == ACTION_FOCUS) {
            if (arguments == null) {
                Log.e(TAG, "Must specify action type when performing ACTION_FOCUS on FocusArea");
                return false;
            }
            @RotaryConstants.FocusActionType
            int type = arguments.getInt(FOCUS_ACTION_TYPE, FOCUS_INVALID);
            switch (type) {
                case FOCUS_DEFAULT:
                    // Move focus to the default focus (mDefaultFocusView), if any.
                    if (mDefaultFocusView != null) {
                        if (mDefaultFocusView.requestFocus()) {
                            return true;
                        }
                        Log.e(TAG, "The default focus of the FocusArea can't take focus");
                    }
                    return false;
                case FOCUS_FIRST:
                    // Focus on the first focusable view in the FocusArea.
                    return requestFocus();
                default:
                    Log.e(TAG, "Invalid action type " + type);
                    return false;
            }
        }
        return super.performAccessibilityAction(action, arguments);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw highlight on top of this FocusArea (including its background and content) but
        // behind its children.
        if (mEnableBackgroundHighlight && mHasFocus && !isInTouchMode()) {
            mBackgroundHighlight.setBounds(
                    mPaddingLeft + getScrollX(),
                    mPaddingTop + getScrollY(),
                    getScrollX() + getWidth() - mPaddingRight,
                    getScrollY() + getHeight() - mPaddingBottom);
            mBackgroundHighlight.draw(canvas);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        // Draw highlight on top of this FocusArea (including its background and content) and its
        // children (including background, content, focus highlight, etc).
        if (mEnableForegroundHighlight && mHasFocus && !isInTouchMode()) {
            mForegroundHighlight.setBounds(
                    mPaddingLeft + getScrollX(),
                    mPaddingTop + getScrollY(),
                    getScrollX() + getWidth() - mPaddingRight,
                    getScrollY() + getHeight() - mPaddingBottom);
            mForegroundHighlight.draw(canvas);
        }
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return FocusArea.class.getName();
    }

    /** Sets the padding (in pixels) of the FocusArea highlight. */
    public void setHighlightPadding(int left, int top, int right, int bottom) {
        if (mPaddingLeft == left && mPaddingTop == top && mPaddingRight == right
                && mPaddingBottom == bottom) {
            return;
        }
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;
        invalidate();
    }
}
