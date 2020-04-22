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
package com.android.car.ui.recyclerview;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;
import com.android.car.ui.recyclerview.decorations.grid.GridDividerItemDecoration;
import com.android.car.ui.recyclerview.decorations.grid.GridOffsetItemDecoration;
import com.android.car.ui.recyclerview.decorations.linear.LinearDividerItemDecoration;
import com.android.car.ui.recyclerview.decorations.linear.LinearOffsetItemDecoration;
import com.android.car.ui.recyclerview.decorations.linear.LinearOffsetItemDecoration.OffsetPosition;
import com.android.car.ui.toolbar.Toolbar;
import com.android.car.ui.utils.CarUxRestrictionsUtil;

import java.lang.annotation.Retention;

/**
 * View that extends a {@link RecyclerView} and wraps itself into a {@link LinearLayout} which
 * could potentially include a scrollbar that has page up and down arrows. Interaction with this
 * view is similar to a {@code RecyclerView} as it takes the same adapter and the layout manager.
 */
public final class CarUiRecyclerView extends RecyclerView implements
        Toolbar.OnHeightChangedListener {

    private static final String TAG = "CarUiRecyclerView";

    private final CarUxRestrictionsUtil.OnUxRestrictionsChangedListener mListener =
            new UxRestrictionChangedListener();

    private CarUxRestrictionsUtil mCarUxRestrictionsUtil;
    private boolean mScrollBarEnabled;
    private String mScrollBarClass;
    private int mScrollBarPaddingTop;
    private int mScrollBarPaddingBottom;
    private boolean mHasScrolledToTop = false;

    private ScrollBar mScrollBar;
    private int mInitialTopPadding;

    private GridOffsetItemDecoration mOffsetItemDecoration;
    private GridDividerItemDecoration mDividerItemDecoration;
    private int mNumOfColumns;
    private boolean mInstallingExtScrollBar = false;
    private int mContainerVisibility = View.VISIBLE;
    private Rect mContainerPadding;
    private Rect mContainerPaddingRelative;
    private LinearLayout mContainer;

    /**
     * The possible values for setScrollBarPosition. The default value is actually {@link
     * CarUiRecyclerViewLayout#LINEAR}.
     */
    @IntDef({
            CarUiRecyclerViewLayout.LINEAR,
            CarUiRecyclerViewLayout.GRID,
    })
    @Retention(SOURCE)
    public @interface CarUiRecyclerViewLayout {
        /**
         * Arranges items either horizontally in a single row or vertically in a single column.
         * This is default.
         */
        int LINEAR = 0;

        /** Arranges items in a Grid. */
        int GRID = 2;
    }

    /**
     * Interface for a {@link RecyclerView.Adapter} to cap the number of items.
     *
     * <p>NOTE: it is still up to the adapter to use maxItems in {@link
     * RecyclerView.Adapter#getItemCount()}.
     *
     * <p>the recommended way would be with:
     *
     * <pre>{@code
     * {@literal@}Override
     * public int getItemCount() {
     *   return Math.min(super.getItemCount(), mMaxItems);
     * }
     * }</pre>
     */
    public interface ItemCap {

        /**
         * A value to pass to {@link #setMaxItems(int)} that indicates there should be no limit.
         */
        int UNLIMITED = -1;

        /**
         * Sets the maximum number of items available in the adapter. A value less than '0' means
         * the
         * list should not be capped.
         */
        void setMaxItems(int maxItems);
    }

    public CarUiRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public CarUiRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.carUiRecyclerViewStyle);
    }

    public CarUiRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        setClipToPadding(false);
        mCarUxRestrictionsUtil = CarUxRestrictionsUtil.getInstance(context);
        TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.CarUiRecyclerView,
                defStyleAttr,
                R.style.Widget_CarUi_CarUiRecyclerView);

        mScrollBarEnabled = context.getResources().getBoolean(R.bool.car_ui_scrollbar_enable);

        mScrollBarPaddingTop = context.getResources()
                .getDimensionPixelSize(R.dimen.car_ui_scrollbar_padding_top);
        mScrollBarPaddingBottom = context.getResources()
                .getDimensionPixelSize(R.dimen.car_ui_scrollbar_padding_bottom);

        @CarUiRecyclerViewLayout int carUiRecyclerViewLayout =
                a.getInt(R.styleable.CarUiRecyclerView_layoutStyle, CarUiRecyclerViewLayout.LINEAR);
        mNumOfColumns = a.getInt(R.styleable.CarUiRecyclerView_numOfColumns, /* defValue= */ 2);
        boolean enableDivider =
                a.getBoolean(R.styleable.CarUiRecyclerView_enableDivider, /* defValue= */ false);

        if (carUiRecyclerViewLayout == CarUiRecyclerViewLayout.LINEAR) {

            int linearTopOffset =
                    a.getInteger(R.styleable.CarUiRecyclerView_topOffset, /* defValue= */ 0);
            int linearBottomOffset =
                    a.getInteger(R.styleable.CarUiRecyclerView_bottomOffset, /* defValue= */ 0);

            if (enableDivider) {
                RecyclerView.ItemDecoration dividerItemDecoration =
                        new LinearDividerItemDecoration(
                                context.getDrawable(R.drawable.car_ui_recyclerview_divider));
                addItemDecoration(dividerItemDecoration);
            }
            RecyclerView.ItemDecoration topOffsetItemDecoration =
                    new LinearOffsetItemDecoration(linearTopOffset, OffsetPosition.START);

            RecyclerView.ItemDecoration bottomOffsetItemDecoration =
                    new LinearOffsetItemDecoration(linearBottomOffset, OffsetPosition.END);

            addItemDecoration(topOffsetItemDecoration);
            addItemDecoration(bottomOffsetItemDecoration);
            setLayoutManager(new LinearLayoutManager(getContext()));
        } else {
            int gridTopOffset =
                    a.getInteger(R.styleable.CarUiRecyclerView_topOffset, /* defValue= */ 0);
            int gridBottomOffset =
                    a.getInteger(R.styleable.CarUiRecyclerView_bottomOffset, /* defValue= */ 0);

            if (enableDivider) {
                mDividerItemDecoration =
                        new GridDividerItemDecoration(
                                context.getDrawable(R.drawable.car_ui_divider),
                                context.getDrawable(R.drawable.car_ui_divider),
                                mNumOfColumns);
                addItemDecoration(mDividerItemDecoration);
            }

            mOffsetItemDecoration =
                    new GridOffsetItemDecoration(gridTopOffset, mNumOfColumns,
                            OffsetPosition.START);

            GridOffsetItemDecoration bottomOffsetItemDecoration =
                    new GridOffsetItemDecoration(gridBottomOffset, mNumOfColumns,
                            OffsetPosition.END);

            addItemDecoration(mOffsetItemDecoration);
            addItemDecoration(bottomOffsetItemDecoration);
            setLayoutManager(new GridLayoutManager(getContext(), mNumOfColumns));
            setNumOfColumns(mNumOfColumns);
        }

        a.recycle();
        if (!mScrollBarEnabled) {
            return;
        }

        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);

        mScrollBarClass = context.getResources().getString(R.string.car_ui_scrollbar_component);
        this.getViewTreeObserver()
                .addOnGlobalLayoutListener(() -> {
                    if (!mHasScrolledToTop && getLayoutManager() != null) {
                        // Scroll to the top after the first global layout, so that
                        // we can set padding for the insets and still have the
                        // recyclerview start at the top.
                        new Handler(Looper.myLooper()).post(() ->
                                getLayoutManager().scrollToPosition(0));
                        mHasScrolledToTop = true;
                    }

                    if (mInitialTopPadding == 0) {
                        mInitialTopPadding = getPaddingTop();
                    }
                });
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);

        // If we're restoring an existing RecyclerView, we don't want
        // to do the initial scroll to top
        mHasScrolledToTop = true;
    }

    @Override
    public void onHeightChanged(int height) {
        setPaddingRelative(getPaddingStart(), mInitialTopPadding + height,
                getPaddingEnd(), getPaddingBottom());
    }

    /**
     * Sets the number of columns in which grid needs to be divided.
     */
    public void setNumOfColumns(int numberOfColumns) {
        mNumOfColumns = numberOfColumns;
        if (mOffsetItemDecoration != null) {
            mOffsetItemDecoration.setNumOfColumns(mNumOfColumns);
        }
        if (mDividerItemDecoration != null) {
            mDividerItemDecoration.setNumOfColumns(mNumOfColumns);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        mContainerVisibility = visibility;
        if (mContainer != null) {
            mContainer.setVisibility(visibility);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mCarUxRestrictionsUtil.register(mListener);
        if (mInstallingExtScrollBar || !mScrollBarEnabled) {
            return;
        }
        // When CarUiRV is detached from the current parent and attached to the container with
        // the scrollBar, onAttachedToWindow() will get called immediately when attaching the
        // CarUiRV to the container. This flag will help us keep track of this state and avoid
        // recursion. We also want to reset the state of this flag as soon as the container is
        // successfully attached to the CarUiRV's original parent.
        mInstallingExtScrollBar = true;
        installExternalScrollBar();
        mInstallingExtScrollBar = false;
    }

    /**
     * This method will detach the current recycler view from its parent and attach it to the
     * container which is a LinearLayout. Later the entire container is attached to the
     * parent where the recycler view was set with the same layout params.
     */
    private void installExternalScrollBar() {
        mContainer = new LinearLayout(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.car_ui_recycler_view, mContainer, true);
        mContainer.setVisibility(mContainerVisibility);

        if (mContainerPadding != null) {
            mContainer.setPadding(mContainerPadding.left, mContainerPadding.top,
                    mContainerPadding.right, mContainerPadding.bottom);
        } else if (mContainerPaddingRelative != null) {
            mContainer.setPaddingRelative(mContainerPaddingRelative.left,
                    mContainerPaddingRelative.top, mContainerPaddingRelative.right,
                    mContainerPaddingRelative.bottom);
        } else {
            mContainer.setPadding(getPaddingLeft(), /* top= */ 0,
                    getPaddingRight(), /* bottom= */ 0);
            setPadding(/* left= */ 0, getPaddingTop(),
                    /* right= */ 0, getPaddingBottom());
        }

        mContainer.setLayoutParams(getLayoutParams());
        ViewGroup parent = (ViewGroup) getParent();
        int index = parent.indexOfChild(this);
        parent.removeViewInLayout(this);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        ((CarUiRecyclerViewContainer) mContainer.findViewById(R.id.car_ui_recycler_view))
                .addRecyclerView(this, params);
        parent.addView(mContainer, index);

        createScrollBarFromConfig(mContainer.findViewById(R.id.car_ui_scroll_bar));
    }

    private void createScrollBarFromConfig(View scrollView) {
        Class<?> cls;
        try {
            cls = !TextUtils.isEmpty(mScrollBarClass)
                    ? getContext().getClassLoader().loadClass(mScrollBarClass)
                    : DefaultScrollBar.class;
        } catch (Throwable t) {
            throw andLog("Error loading scroll bar component: " + mScrollBarClass, t);
        }
        try {
            mScrollBar = (ScrollBar) cls.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            throw andLog("Error creating scroll bar component: " + mScrollBarClass, t);
        }

        mScrollBar.initialize(this, scrollView);

        setScrollBarPadding(mScrollBarPaddingTop, mScrollBarPaddingBottom);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mCarUxRestrictionsUtil.unregister(mListener);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(0, top, 0, bottom);
        mContainerPaddingRelative = null;
        mContainerPadding = new Rect(left, 0, right, 0);
        if (mContainer != null) {
            mContainer.setPadding(left, 0, right, 0);
        }
        setScrollBarPadding(mScrollBarPaddingTop, mScrollBarPaddingBottom);
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        super.setPaddingRelative(0, top, 0, bottom);
        mContainerPadding = null;
        mContainerPaddingRelative = new Rect(start, 0, end, 0);
        if (mContainer != null) {
            mContainer.setPaddingRelative(start, 0, end, 0);
        }
        setScrollBarPadding(mScrollBarPaddingTop, mScrollBarPaddingBottom);
    }

    /**
     * Sets the scrollbar's padding top and bottom.
     * This padding is applied in addition to the padding of the RecyclerView.
     */
    public void setScrollBarPadding(int paddingTop, int paddingBottom) {
        if (mScrollBarEnabled) {
            mScrollBarPaddingTop = paddingTop;
            mScrollBarPaddingBottom = paddingBottom;

            if (mScrollBar != null) {
                mScrollBar.setPadding(paddingTop + getPaddingTop(),
                        paddingBottom + getPaddingBottom());
            }
        }
    }

    private static RuntimeException andLog(String msg, Throwable t) {
        Log.e(TAG, msg, t);
        throw new RuntimeException(msg, t);
    }

    private class UxRestrictionChangedListener implements
            CarUxRestrictionsUtil.OnUxRestrictionsChangedListener {

        @Override
        public void onRestrictionsChanged(@NonNull CarUxRestrictions carUxRestrictions) {
            Adapter<?> adapter = getAdapter();
            // If the adapter does not implement ItemCap, then the max items on it cannot be
            // updated.
            if (!(adapter instanceof ItemCap)) {
                return;
            }

            int maxItems = ItemCap.UNLIMITED;
            if ((carUxRestrictions.getActiveRestrictions()
                    & CarUxRestrictions.UX_RESTRICTIONS_LIMIT_CONTENT)
                    != 0) {
                maxItems = carUxRestrictions.getMaxCumulativeContentItems();
            }

            int originalCount = adapter.getItemCount();
            ((ItemCap) adapter).setMaxItems(maxItems);
            int newCount = adapter.getItemCount();

            if (newCount == originalCount) {
                return;
            }

            if (newCount < originalCount) {
                adapter.notifyItemRangeRemoved(newCount, originalCount - newCount);
            } else {
                adapter.notifyItemRangeInserted(originalCount, newCount - originalCount);
            }
        }
    }
}
