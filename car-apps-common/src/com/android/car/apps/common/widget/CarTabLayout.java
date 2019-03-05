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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.apps.common.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Custom tab layout which supports adding tabs dynamically
 *
 * <p>It supports two layout modes:
 * <ul><li>Flexible layout which will fill the width
 * <li>Non-flexible layout which wraps content with a minimum tab width. By setting tab gravity,
 * it can left aligned, right aligned or center aligned.
 *
 * <p>Scrolling function is not supported. If a tab item runs out of the tab layout bound, there
 * is no way to access it. It's better to set the layout mode to flexible in this case.
 *
 * <p>Default tab item inflates from R.layout.car_tab_item, but it also supports custom layout id.
 * By doing this, appearance of tab item view can be customized.
 *
 * <p>Touch feedback is using @android:attr/selectableItemBackground.
 */
public class CarTabLayout extends LinearLayout {

    /** Listener that listens the car tab selection change. */
    public interface OnCarTabSelectedListener {
        /** Callback triggered when a car tab is selected. */
        void onCarTabSelected(CarTab carTab);

        /** Callback triggered when a car tab is unselected. */
        void onCarTabUnselected(CarTab carTab);

        /** Callback triggered when a car tab is reselected. */
        void onCarTabReselected(CarTab carTab);
    }

    /** No-op implementation of {@link OnCarTabSelectedListener}. */
    public static class SimpleOnCarTabSelectedListener implements OnCarTabSelectedListener {

        @Override
        public void onCarTabSelected(CarTab carTab) {
            // No-op
        }

        @Override
        public void onCarTabUnselected(CarTab carTab) {
            // No-op
        }

        @Override
        public void onCarTabReselected(CarTab carTab) {
            // No-op
        }
    }

    // View attributes
    private final int mTabMinWidth;
    private final boolean mTabFlexibleLayout;
    private final int mTabSpacing;

    private final Set<OnCarTabSelectedListener> mOnCarTabSelectedListeners;

    private final CarTabAdapter mCarTabAdapter;

    public CarTabLayout(@NonNull Context context) {
        this(context, null);
    }

    public CarTabLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarTabLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mOnCarTabSelectedListeners = new ArraySet<>();

        TypedArray ta = context.obtainStyledAttributes(
                attrs, R.styleable.CarTabLayout, defStyle, 0);
        mTabSpacing = ta.getDimensionPixelSize(R.styleable.CarTabLayout_tabSpacing,
                context.getResources().getDimensionPixelSize(R.dimen.car_tab_padding_x));
        mTabMinWidth = ta.getDimensionPixelSize(R.styleable.CarTabLayout_tabMinWidth,
                context.getResources().getDimensionPixelSize(R.dimen.car_tab_min_width));
        mTabFlexibleLayout = ta.getBoolean(R.styleable.CarTabLayout_tabFlexibleLayout,
                context.getResources().getBoolean(R.bool.car_tab_flexible_layout));
        int tabItemLayout = ta.getResourceId(R.styleable.CarTabLayout_tabItemLayout,
                R.layout.car_tab_item);
        ta.recycle();

        mCarTabAdapter = new CarTabAdapter(context, tabItemLayout, this);
    }

    /**
     * Add a tab to this layout. The tab will be added at the end of the list. If this is the first
     * tab to be added it will become the selected tab.
     */
    public void addCarTab(CarTab carTab) {
        mCarTabAdapter.add(carTab);
        // If there is only one tab in the group, set it to be selected.
        if (mCarTabAdapter.getCount() == 1) {
            mCarTabAdapter.selectCarTab(0);
        }
    }

    /** Set the tab as the current selected tab. */
    public void selectCarTab(CarTab carTab) {
        mCarTabAdapter.selectCarTab(carTab);
    }

    /** Set the tab at given position as the current selected tab. */
    public void selectCarTab(int position) {
        mCarTabAdapter.selectCarTab(position);
    }

    /** Returns how tab items it has. */
    public int getCarTabCount() {
        return mCarTabAdapter.getCount();
    }

    /** Returns the position of the given car tab. */
    public int getCarTabPosition(CarTab carTab) {
        return mCarTabAdapter.getPosition(carTab);
    }

    /** Return the car tab at the given position. */
    public CarTab get(int position) {
        return mCarTabAdapter.getItem(position);
    }

    /** Clear all car tabs. */
    public void clearAllCarTabs() {
        mCarTabAdapter.clear();
    }

    /** Register a {@link OnCarTabSelectedListener}. Same listener will only be registered once. */
    public void addOnCarTabSelectedListener(
            @NonNull OnCarTabSelectedListener onCarTabSelectedListener) {
        mOnCarTabSelectedListeners.add(onCarTabSelectedListener);
    }

    /** Unregister a {@link OnCarTabSelectedListener} */
    public void removeOnCarTabSelectedListener(
            @NonNull OnCarTabSelectedListener onCarTabSelectedListener) {
        mOnCarTabSelectedListeners.remove(onCarTabSelectedListener);
    }

    private void dispatchOnCarTabSelected(CarTab carTab) {
        for (OnCarTabSelectedListener onCarTabSelectedListener : mOnCarTabSelectedListeners) {
            onCarTabSelectedListener.onCarTabSelected(carTab);
        }
    }

    private void dispatchOnCarTabUnselected(CarTab carTab) {
        for (OnCarTabSelectedListener onCarTabSelectedListener : mOnCarTabSelectedListeners) {
            onCarTabSelectedListener.onCarTabUnselected(carTab);
        }
    }

    private void dispatchOnCarTabReselected(CarTab carTab) {
        for (OnCarTabSelectedListener onCarTabSelectedListener : mOnCarTabSelectedListeners) {
            onCarTabSelectedListener.onCarTabReselected(carTab);
        }
    }

    private void addCarTabView(View carTabView, int position) {
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        if (mTabFlexibleLayout) {
            layoutParams.weight = 1;
        }
        addView(carTabView, position, layoutParams);
    }

    private ViewGroup createCarTabItemView() {
        LinearLayout carTabItemView = new LinearLayout(mContext);
        carTabItemView.setOrientation(LinearLayout.VERTICAL);
        carTabItemView.setGravity(Gravity.CENTER);
        carTabItemView.setPadding(mTabSpacing, 0, mTabSpacing, 0);
        carTabItemView.setMinimumWidth(mTabSpacing * 2 + mTabMinWidth);
        // TODO: add indirection to allow customization
        Drawable backgroundDrawable = getAttrDrawable(getContext(),
                android.R.attr.selectableItemBackground);
        carTabItemView.setBackground(backgroundDrawable);
        return carTabItemView;
    }

    private static class CarTabAdapter extends BaseAdapter {
        private static final int MEDIUM_WEIGHT = 500;
        private final Context mContext;
        private final CarTabLayout mCarTabLayout;
        @LayoutRes
        private final int mCarTabItemLayoutRes;
        private final Typeface mUnselectedTypeface;
        private final Typeface mSelectedTypeface;
        private final List<CarTab> mCarTabList;

        private CarTabAdapter(Context context, @LayoutRes int res, CarTabLayout carTabLayout) {
            mCarTabList = new ArrayList<>();
            mContext = context;
            mCarTabItemLayoutRes = res;
            mCarTabLayout = carTabLayout;
            mUnselectedTypeface = Typeface.defaultFromStyle(Typeface.NORMAL);
            // TODO: add indirection to allow customization.
            mSelectedTypeface = Typeface.create(mUnselectedTypeface, MEDIUM_WEIGHT, false);
        }

        private void add(@NonNull CarTab carTab) {
            mCarTabList.add(carTab);
            notifyItemInserted(mCarTabList.size() - 1);
        }

        private void clear() {
            mCarTabList.clear();
            mCarTabLayout.removeAllViews();
        }

        private int getPosition(CarTab carTab) {
            return mCarTabList.indexOf(carTab);
        }

        @Override
        public int getCount() {
            return mCarTabList.size();
        }

        @Override
        public CarTab getItem(int position) {
            return mCarTabList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        @NonNull
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewGroup carTabItemView = mCarTabLayout.createCarTabItemView();
            LayoutInflater.from(mContext).inflate(mCarTabItemLayoutRes, carTabItemView, true);

            presentCarTabItemView(position, carTabItemView);
            return carTabItemView;
        }

        private void selectCarTab(CarTab carTab) {
            int position = mCarTabList.indexOf(carTab);
            selectCarTab(position);
        }

        private void selectCarTab(int position) {
            if (position < 0 || position >= getCount()) {
                throw new IndexOutOfBoundsException("Invalid position");
            }

            for (int i = 0; i < getCount(); i++) {
                CarTab carTabItem = mCarTabList.get(i);
                boolean isTabSelected = position == i;
                if (carTabItem.mIsSelected != isTabSelected) {
                    carTabItem.mIsSelected = isTabSelected;
                    notifyItemChanged(i);
                    if (carTabItem.mIsSelected) {
                        mCarTabLayout.dispatchOnCarTabSelected(carTabItem);
                    } else {
                        mCarTabLayout.dispatchOnCarTabUnselected(carTabItem);
                    }
                } else if (carTabItem.mIsSelected) {
                    mCarTabLayout.dispatchOnCarTabReselected(carTabItem);
                }
            }
        }

        /** Represent the car tab item at given position without destroying and recreating UI. */
        private void notifyItemChanged(int position) {
            View carTabItemView = mCarTabLayout.getChildAt(position);
            presentCarTabItemView(position, carTabItemView);
        }

        private void notifyItemInserted(int position) {
            View insertedView = getView(position, null, mCarTabLayout);
            mCarTabLayout.addCarTabView(insertedView, position);
        }

        private void presentCarTabItemView(int position, @NonNull View carTabItemView) {
            CarTab carTab = mCarTabList.get(position);

            ImageView iconView = carTabItemView.findViewById(R.id.car_tab_item_icon);
            TextView textView = carTabItemView.findViewById(R.id.car_tab_item_text);

            carTabItemView.setOnClickListener(view -> selectCarTab(carTab));
            carTab.bindText(textView);
            carTab.bindIcon(iconView);

            carTabItemView.setSelected(carTab.mIsSelected);
            iconView.setSelected(carTab.mIsSelected);
            textView.setSelected(carTab.mIsSelected);

            maybeAdjustTextViewWidth(textView);
            textView.setTypeface(carTab.mIsSelected ? mSelectedTypeface : mUnselectedTypeface);
        }

        /** We don't want the car tab item view change width due to the font weight change. */
        private void maybeAdjustTextViewWidth(@NonNull TextView textView) {
            CharSequence text = textView.getText();
            if (mCarTabLayout.mTabFlexibleLayout || TextUtils.isEmpty(text)) {
                return;
            }

            int wrapContentSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

            textView.setTypeface(mSelectedTypeface);
            textView.measure(wrapContentSpec, wrapContentSpec);
            int selectedWidth = textView.getMeasuredWidth();

            textView.setTypeface(mUnselectedTypeface);
            textView.measure(wrapContentSpec, wrapContentSpec);
            int unselectedWidth = textView.getMeasuredWidth();

            textView.setWidth(Math.max(selectedWidth, unselectedWidth));
        }
    }

    /** Car tab entity. */
    public static class CarTab {
        private final Drawable mIcon;
        private final CharSequence mText;
        private boolean mIsSelected;

        public CarTab(@Nullable Drawable icon, @Nullable CharSequence text) {
            mIcon = icon;
            mText = text;
        }

        /** Set tab text. */
        protected void bindText(TextView textView) {
            textView.setText(mText);
        }

        /** Set icon drawable. */
        protected void bindIcon(ImageView imageView) {
            imageView.setImageDrawable(mIcon);
        }
    }

    // TODO: use Themes.getAttrDrawable once refactor change is merged.
    private static Drawable getAttrDrawable(Context context, int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[]{attr});
        Drawable value = ta.getDrawable(0);
        ta.recycle();
        return value;
    }
}
