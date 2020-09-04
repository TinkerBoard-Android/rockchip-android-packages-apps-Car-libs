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

import static com.android.car.ui.utils.RotaryConstants.ACTION_NUDGE_SHORTCUT;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_ACTION_TYPE;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_BOTTOM_BOUND_OFFSET;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_LEFT_BOUND_OFFSET;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_RIGHT_BOUND_OFFSET;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_AREA_TOP_BOUND_OFFSET;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_DEFAULT;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_FIRST;
import static com.android.car.ui.utils.RotaryConstants.FOCUS_INVALID;
import static com.android.car.ui.utils.RotaryConstants.NUDGE_SHORTCUT_DIRECTION;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

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

    private static final int INVALID_DIMEN = -1;

    private static final int INVALID_DIRECTION = -1;

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

    /** The offset (in pixels) of the FocusArea's bounds. */
    private int mLeftOffset;
    private int mRightOffset;
    private int mTopOffset;
    private int mBottomOffset;

    /** Whether the layout direction is {@link View#LAYOUT_DIRECTION_RTL}. */
    private boolean mRtl;

    /** The ID of the view specified in {@code app:defaultFocus}. */
    private int mDefaultFocusId;
    /** The view specified in {@code app:defaultFocus}. */
    @Nullable
    private View mDefaultFocusView;

    /** The ID of the view specified in {@code app:nudgeShortcut}. */
    private int mNudgeShortcutId;
    /** The view specified in {@code app:nudgeShortcut}. */
    @Nullable
    private View mNudgeShortcutView;

    /** The direction specified in {@code app:nudgeShortcutDirection}. */
    private int mNudgeShortcutDirection;

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

        // Ensure that an AccessibilityNodeInfo is created for this view.
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        // By default all ViewGroup subclasses do not call their draw() and onDraw() methods. We
        // should enable it since we override these methods.
        setWillNotDraw(false);

        if (mEnableBackgroundHighlight || mEnableForegroundHighlight) {
            // Update highlight of the FocusArea when the focus of its descendants has changed.
            registerFocusChangeListener();
        }

        initAttrs(context, attrs);
    }

    @VisibleForTesting
    void registerFocusChangeListener() {
        getViewTreeObserver().addOnGlobalFocusChangeListener(
                (oldFocus, newFocus) -> {
                    boolean hasFocus = hasFocus();
                    if (mHasFocus != hasFocus) {
                        mHasFocus = hasFocus;
                        invalidate();
                    }
                });
    }

    private void initAttrs(Context context, @Nullable AttributeSet attrs) {
        if (attrs == null) {
            return;
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FocusArea);
        try {
            mDefaultFocusId = a.getResourceId(R.styleable.FocusArea_defaultFocus, View.NO_ID);

            // Initialize the highlight padding. The padding, for example, left padding, is set in
            // the following order:
            // 1. if highlightPaddingStart (or highlightPaddingEnd in RTL layout) specified, use it
            // 2. otherwise, if highlightPaddingHorizontal is specified, use it
            // 3. otherwise use 0

            int paddingStart = a.getDimensionPixelSize(
                    R.styleable.FocusArea_highlightPaddingStart, INVALID_DIMEN);
            if (paddingStart == INVALID_DIMEN) {
                paddingStart = a.getDimensionPixelSize(
                        R.styleable.FocusArea_highlightPaddingHorizontal, 0);
            }

            int paddingEnd = a.getDimensionPixelSize(
                    R.styleable.FocusArea_highlightPaddingEnd, INVALID_DIMEN);
            if (paddingEnd == INVALID_DIMEN) {
                paddingEnd = a.getDimensionPixelSize(
                        R.styleable.FocusArea_highlightPaddingHorizontal, 0);
            }

            mRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
            mPaddingLeft = mRtl ? paddingEnd : paddingStart;
            mPaddingRight = mRtl ? paddingStart : paddingEnd;

            mPaddingTop = a.getDimensionPixelSize(
                    R.styleable.FocusArea_highlightPaddingTop, INVALID_DIMEN);
            if (mPaddingTop == INVALID_DIMEN) {
                mPaddingTop = a.getDimensionPixelSize(
                        R.styleable.FocusArea_highlightPaddingVertical, 0);
            }

            mPaddingBottom = a.getDimensionPixelSize(
                    R.styleable.FocusArea_highlightPaddingBottom, INVALID_DIMEN);
            if (mPaddingBottom == INVALID_DIMEN) {
                mPaddingBottom = a.getDimensionPixelSize(
                        R.styleable.FocusArea_highlightPaddingVertical, 0);
            }

            // Initialize the offset of the FocusArea's bounds. The offset, for example, left
            // offset, is set in the following order:
            // 1. if startBoundOffset (or endBoundOffset in RTL layout) specified, use it
            // 2. otherwise, if horizontalBoundOffset is specified, use it
            // 3. otherwise use mPaddingLeft

            int startOffset = a.getDimensionPixelSize(
                    R.styleable.FocusArea_startBoundOffset, INVALID_DIMEN);
            if (startOffset == INVALID_DIMEN) {
                startOffset = a.getDimensionPixelSize(
                        R.styleable.FocusArea_horizontalBoundOffset, paddingStart);
            }

            int endOffset = a.getDimensionPixelSize(
                    R.styleable.FocusArea_endBoundOffset, INVALID_DIMEN);
            if (endOffset == INVALID_DIMEN) {
                endOffset = a.getDimensionPixelSize(
                        R.styleable.FocusArea_horizontalBoundOffset, paddingEnd);
            }

            mLeftOffset = mRtl ? endOffset : startOffset;
            mRightOffset = mRtl ? startOffset : endOffset;

            mTopOffset = a.getDimensionPixelSize(
                    R.styleable.FocusArea_topBoundOffset, INVALID_DIMEN);
            if (mTopOffset == INVALID_DIMEN) {
                mTopOffset = a.getDimensionPixelSize(
                        R.styleable.FocusArea_verticalBoundOffset, mPaddingTop);
            }

            mBottomOffset = a.getDimensionPixelSize(
                    R.styleable.FocusArea_bottomBoundOffset, INVALID_DIMEN);
            if (mBottomOffset == INVALID_DIMEN) {
                mBottomOffset = a.getDimensionPixelSize(
                        R.styleable.FocusArea_verticalBoundOffset, mPaddingBottom);
            }

            mNudgeShortcutId = a.getResourceId(R.styleable.FocusArea_nudgeShortcut, View.NO_ID);
            mNudgeShortcutDirection = a.getInt(
                    R.styleable.FocusArea_nudgeShortcutDirection, INVALID_DIRECTION);
            if ((mNudgeShortcutId == View.NO_ID) ^ (mNudgeShortcutDirection == INVALID_DIRECTION)) {
                throw new IllegalStateException("nudgeShortcut and nudgeShortcutDirection must "
                        + "be specified together");
            }
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
        if (mNudgeShortcutId != View.NO_ID) {
            mNudgeShortcutView = CarUiUtils.requireViewByRefId(this, mNudgeShortcutId);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        if (mRtl != rtl) {
            mRtl = rtl;

            int temp = mPaddingLeft;
            mPaddingLeft = mPaddingRight;
            mPaddingRight = temp;

            temp = mLeftOffset;
            mLeftOffset = mRightOffset;
            mRightOffset = temp;
        }
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        switch (action) {
            case ACTION_FOCUS: {
                // FocusArea is not focusable, so we focus on its descendant when handling
                // ACTION_FOCUS.
                if (arguments == null) {
                    Log.e(TAG,
                            "Must specify action type when performing ACTION_FOCUS on FocusArea");
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
            case ACTION_NUDGE_SHORTCUT: {
                if (mNudgeShortcutDirection == INVALID_DIRECTION) {
                    // No nudge shortcut configured for this FocusArea.
                    return false;
                }
                if (arguments == null
                        || arguments.getInt(NUDGE_SHORTCUT_DIRECTION, INVALID_DIRECTION)
                            != mNudgeShortcutDirection) {
                    // The user is not nudging to the nudge shortcut direction.
                    return false;
                }
                if (mNudgeShortcutView.isFocused()) {
                    // The nudge shortcut view is already focused; return false so that the user can
                    // nudge to another FocusArea.
                    return false;
                }
                return mNudgeShortcutView.requestFocus(mNudgeShortcutDirection);
            }
            default:
                return super.performAccessibilityAction(action, arguments);
        }
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

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        Bundle bundle = info.getExtras();
        bundle.putInt(FOCUS_AREA_LEFT_BOUND_OFFSET, mLeftOffset);
        bundle.putInt(FOCUS_AREA_RIGHT_BOUND_OFFSET, mRightOffset);
        bundle.putInt(FOCUS_AREA_TOP_BOUND_OFFSET, mTopOffset);
        bundle.putInt(FOCUS_AREA_BOTTOM_BOUND_OFFSET, mBottomOffset);
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

    /** Sets the offset (in pixels) of the FocusArea's bounds. */
    public void setBoundsOffset(int left, int top, int right, int bottom) {
        mLeftOffset = left;
        mTopOffset = top;
        mRightOffset = right;
        mBottomOffset = bottom;
    }
}
