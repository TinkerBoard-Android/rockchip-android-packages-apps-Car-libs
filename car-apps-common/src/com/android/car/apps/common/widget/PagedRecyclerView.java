/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.apps.common.widget;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.NonNull;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.apps.common.R;
import com.android.car.apps.common.util.ScrollBarUI;

import java.lang.annotation.Retention;

/**
 * View that extends a {@link RecyclerView} and creates a nested {@code RecyclerView} with an option
 * to render a custom scroll bar that has page up and down arrows. Interaction with this view is
 * similar to a {@code RecyclerView} as it takes the same adapter and the layout manager.
 */
public final class PagedRecyclerView extends RecyclerView {

    private static final boolean DEBUG = false;
    private static final String TAG = "PagedRecyclerView";

    private Context mContext;
    private AttributeSet mAttrs;

    private ScrollBarUI mScrollBar;
    private boolean mScrollBarEnabled;
    private int mScrollBarContainerWidth;
    private @ScrollBarPosition int mScrollBarPosition;
    private boolean mScrollBarAboveRecyclerView;

    @Gutter
    private int mGutter;
    private int mGutterSize;
    private Adapter mNestedAdapter;
    private LayoutManager mNestedLayoutManager;
    private RecyclerView mNestedRecyclerView;

    private Boolean mVerticalFadingEdgeEnabled;
    private Integer mFadingEdgeLength;

    /**
     * The possible values for @{link #setGutter}. The default value is actually
     * {@link PagedRecyclerView.Gutter#BOTH}.
     */
    @IntDef({
            Gutter.NONE,
            Gutter.START,
            Gutter.END,
            Gutter.BOTH,
    })

    @Retention(SOURCE)
    public @interface Gutter {
        /**
         * No gutter on either side of the list items. The items will span the full width of the
         * RecyclerView
         */
        int NONE = 0;

        /**
         * Include a gutter only on the start side (that is, the same side as the scroll bar).
         */
        int START = 1;

        /**
         * Include a gutter only on the end side (that is, the opposite side of the scroll bar).
         */
        int END = 2;

        /**
         * Include a gutter on both sides of the list items. This is the default behaviour.
         */
        int BOTH = 3;
    }

    /**
     * The possible values for setScrollbarPosition. The default value is actually
     * {@link PagedRecyclerView.ScrollBarPosition#START}.
     */
    @IntDef({
            ScrollBarPosition.START,
            ScrollBarPosition.END,
    })

    @Retention(SOURCE)
    public @interface ScrollBarPosition {
        /**
         * Position the scrollbar to the left of the screen. This is default.
         */
        int START = 0;

        /**
         * Position scrollbar to the right of the screen.
         */
        int END = 2;
    }

    public PagedRecyclerView(@NonNull Context context) {
        this(context, null, 0);
    }

    public PagedRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.PagedRecyclerView, defStyleAttr,
                R.style.PagedRecyclerView);

        mScrollBarEnabled = a.getBoolean(R.styleable.PagedRecyclerView_scrollBarEnabled, true);

        if (!mScrollBarEnabled) {
            a.recycle();
            return;
        }

        mContext = context;
        mAttrs = attrs;

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        linearLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        super.setLayoutManager(linearLayoutManager);

        PagedRecyclerViewAdapter adapter = new PagedRecyclerViewAdapter();
        super.setAdapter(adapter);

        super.setNestedScrollingEnabled(false);
        super.setClipToPadding(false);

        // Gutter
        int defaultGutterSize = getResources().getDimensionPixelSize(R.dimen.car_scroll_bar_margin);
        mGutter = a.getInt(R.styleable.PagedRecyclerView_gutter, Gutter.BOTH);
        mGutterSize = defaultGutterSize;

        int carMargin = mContext.getResources().getDimensionPixelSize(
                R.dimen.car_scroll_bar_margin);
        mScrollBarContainerWidth = a.getDimensionPixelSize(
                R.styleable.PagedRecyclerView_scrollBarContainerWidth, carMargin);

        mScrollBarPosition = a.getInt(R.styleable.PagedRecyclerView_scrollBarPosition,
                    ScrollBarPosition.START);

        mScrollBarAboveRecyclerView = a.getBoolean(
                R.styleable.PagedRecyclerView_scrollBarAboveRecyclerView, true /* defValue */);

        // Apply layout changes after the layout has been calculated the this view.
        this.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                PagedRecyclerView.this.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                initNestedRecyclerView();
                setNestedViewLayout();

                if (mScrollBarEnabled) {
                    createScrollBarFromConfig();
                }
            }
        });

        a.recycle();
    }

    @Override
    public void setClipToPadding(boolean clipToPadding) {
        if (!mScrollBarEnabled) {
            super.setClipToPadding(clipToPadding);
            return;
        }

        if (mNestedRecyclerView == null) return;
        mNestedRecyclerView.setClipToPadding(clipToPadding);
    }

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        if (!mScrollBarEnabled) {
            super.setAdapter(adapter);
            return;
        }

        mNestedAdapter = adapter;
        if (mNestedRecyclerView != null) {
            mNestedRecyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void setLayoutManager(@Nullable LayoutManager layout) {
        if (!mScrollBarEnabled) {
            super.setLayoutManager(layout);
            return;
        }

        mNestedLayoutManager = layout;
        if (mNestedRecyclerView != null) {
            mNestedRecyclerView.setLayoutManager(layout);
        }
    }

    @Override
    public void setVerticalFadingEdgeEnabled(boolean verticalFadingEdgeEnabled) {
        if (!mScrollBarEnabled) {
            super.setVerticalFadingEdgeEnabled(verticalFadingEdgeEnabled);
            return;
        }

        mVerticalFadingEdgeEnabled = verticalFadingEdgeEnabled;
        if (mNestedRecyclerView == null) return;
        mNestedRecyclerView.setVerticalFadingEdgeEnabled(verticalFadingEdgeEnabled);
    }

    @Override
    public void setFadingEdgeLength(int length) {
        if (!mScrollBarEnabled) {
            super.setFadingEdgeLength(length);
            return;
        }

        mFadingEdgeLength = length;
        if (mNestedRecyclerView == null) return;
        mNestedRecyclerView.setFadingEdgeLength(length);
    }

    private void initNestedRecyclerView() {
        PagedRecyclerViewAdapter.NestedRowViewHolder vh =
                (PagedRecyclerViewAdapter.NestedRowViewHolder)
                        this.findViewHolderForAdapterPosition(0);
        if (vh == null) {
            throw new Error("Outer RecyclerView failed to initialize.");
        }

        mNestedRecyclerView = new RecyclerView(mContext, mAttrs,
                R.style.PagedRecyclerView_NestedRecyclerView);
        vh.mFrameLayout.addView(mNestedRecyclerView);

        mNestedRecyclerView.setAdapter(mNestedAdapter);
        mNestedRecyclerView.setLayoutManager(mNestedLayoutManager);

        if (mVerticalFadingEdgeEnabled != null) {
            mNestedRecyclerView.setVerticalFadingEdgeEnabled(mVerticalFadingEdgeEnabled);
        }
        if (mFadingEdgeLength != null) {
            mNestedRecyclerView.setFadingEdgeLength(mFadingEdgeLength);
        }
    }

    private void createScrollBarFromConfig() {
        if (DEBUG) Log.d(TAG, "createScrollBarFromConfig");
        final String clsName = mContext.getString(R.string.config_scrollBarComponent);
        if (clsName == null || clsName.length() == 0) {
            throw andLog("No scroll bar component configured", null);
        }

        Class<?> cls;
        try {
            cls = mContext.getClassLoader().loadClass(clsName);
        } catch (Throwable t) {
            throw andLog("Error loading scroll bar component: " + clsName, t);
        }
        try {
            mScrollBar = (ScrollBarUI) cls.newInstance();
        } catch (Throwable t) {
            throw andLog("Error creating scroll bar component: " + clsName, t);
        }

        mScrollBar.initialize(mContext, mNestedRecyclerView, mScrollBarContainerWidth,
                mScrollBarPosition, mScrollBarAboveRecyclerView);

        if (DEBUG) Log.d(TAG, "started " + mScrollBar.getClass().getSimpleName());
    }

    /**
     * Set the nested view's layout to the specified value.
     *
     * <p>The gutter is the space to the start/end of the list view items and will be equal in size
     * to the scroll bars. By default, there is a gutter to both the left and right of the list
     * view items, to account for the scroll bar.
     *
     * @param gutter A {@link Gutter} value that identifies which sides to apply the gutter to.
     */
    private void setNestedViewLayout() {
        int startMargin = 0;
        int endMargin = 0;
        if ((mGutter & Gutter.START) != 0) {
            startMargin = mGutterSize;
        }
        if ((mGutter & Gutter.END) != 0) {
            endMargin = mGutterSize;
        }

        MarginLayoutParams layoutParams =
                (MarginLayoutParams) mNestedRecyclerView.getLayoutParams();

        layoutParams.setMarginStart(startMargin);
        layoutParams.setMarginEnd(endMargin);

        layoutParams.height = LayoutParams.MATCH_PARENT;
        layoutParams.width = super.getLayoutManager().getWidth() - startMargin - endMargin;
        // requestLayout() isn't sufficient because we also need to resolveLayoutParams().
        mNestedRecyclerView.setLayoutParams(layoutParams);

        // If there's a gutter, set ClipToPadding to false so that CardView's shadow will still
        // appear outside of the padding.
        mNestedRecyclerView.setClipToPadding(startMargin == 0 && endMargin == 0);
    }

    private RuntimeException andLog(String msg, Throwable t) {
        Log.e(TAG, msg, t);
        throw new RuntimeException(msg, t);
    }
}
