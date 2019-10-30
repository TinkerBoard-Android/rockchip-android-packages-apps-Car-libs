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
package com.android.car.ui.toolbar;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.android.car.ui.R;

/**
 * Represents a button to display in the {@link Toolbar}.
 *
 * <p>There are currently 3 types of buttons: icon, text, and switch. Using
 * {@link Builder#setCheckable()} will ensure that you get a switch, after that
 * {@link Builder#setIcon(int)} will ensure an icon, and anything left just requires
 * {@link Builder#setTitle(int)}.
 *
 * <p>Each MenuItem has a {@link DisplayBehavior} that controls if it appears on the {@link Toolbar}
 * itself, or it's overflow menu.
 *
 * <p>If you require a search or settings button, you should use
 * {@link Builder#createSearch(Context, OnClickListener)} or
 * {@link Builder#createSettings(Context, OnClickListener)}.
 *
 * <p>Some properties can be changed after the creating a MenuItem, but others require being set
 * with a {@link Builder}.
 */
public class MenuItem {

    private final Context mContext;
    private final boolean mIsCheckable;
    private final boolean mIsActivatable;
    private final boolean mIsSearch;
    private final boolean mShowIconAndTitle;
    private final boolean mIsTinted;
    @CarUxRestrictions.CarUxRestrictionsInfo
    private final int mUxRestrictions;

    private Listener mListener;
    private CharSequence mTitle;
    private Drawable mIcon;
    private OnClickListener mOnClickListener;
    private DisplayBehavior mDisplayBehavior;
    private boolean mIsEnabled;
    private boolean mIsChecked;
    private boolean mIsVisible;
    private boolean mIsActivated;

    private MenuItem(Builder builder) {
        mContext = builder.mContext;
        mIsCheckable = builder.mIsCheckable;
        mIsActivatable = builder.mIsActivatable;
        mTitle = builder.mTitle;
        mIcon = builder.mIcon;
        mOnClickListener = builder.mOnClickListener;
        mDisplayBehavior = builder.mDisplayBehavior;
        mIsEnabled = builder.mIsEnabled;
        mIsChecked = builder.mIsChecked;
        mIsVisible = builder.mIsVisible;
        mIsActivated = builder.mIsActivated;
        mIsSearch = builder.mIsSearch;
        mShowIconAndTitle = builder.mShowIconAndTitle;
        mIsTinted = builder.mIsTinted;
        mUxRestrictions = builder.mUxRestrictions;
    }

    private void update() {
        if (mListener != null) {
            mListener.onMenuItemChanged();
        }
    }

    /** Returns whether the MenuItem is enabled */
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /** Sets whether the MenuItem is enabled */
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;

        update();
    }

    /** Returns whether the MenuItem is checkable. If it is, it will be displayed as a switch. */
    public boolean isCheckable() {
        return mIsCheckable;
    }

    /**
     * Returns whether the MenuItem is currently checked. Only valid if {@link #isCheckable()}
     * is true.
     */
    public boolean isChecked() {
        return mIsChecked;
    }

    /**
     * Sets whether or not the MenuItem is checked.
     * @throws IllegalStateException When {@link #isCheckable()} is false.
     */
    public void setChecked(boolean checked) {
        if (!isCheckable()) {
            throw new IllegalStateException("Cannot call setChecked() on a non-checkable MenuItem");
        }

        mIsChecked = checked;

        update();
    }

    public boolean isTinted() {
        return mIsTinted;
    }

    /** Returns whether or not the MenuItem is visible */
    public boolean isVisible() {
        return mIsVisible;
    }

    /** Sets whether or not the MenuItem is visible */
    public void setVisible(boolean visible) {
        mIsVisible = visible;

        update();
    }

    /**
     * Returns whether the MenuItem is activatable. If it is, it's every click will toggle
     * the MenuItem's View to appear activated or not.
     */
    public boolean isActivatable() {
        return mIsActivatable;
    }

    /** Returns whether or not this view is selected. Toggles after every click */
    public boolean isActivated() {
        return mIsActivated;
    }

    /** Sets the MenuItem as activated and updates it's View to the activated state */
    public void setActivated(boolean activated) {
        if (!isActivatable()) {
            throw new IllegalStateException(
                    "Cannot call setActivated() on a non-activatable MenuItem");
        }

        mIsActivated = activated;

        update();
    }

    /** Gets the title of this MenuItem. */
    public CharSequence getTitle() {
        return mTitle;
    }

    /** Sets the title of this MenuItem. */
    public void setTitle(CharSequence title) {
        mTitle = title;

        update();
    }

    /** Sets the title of this MenuItem to a string resource. */
    public void setTitle(int resId) {
        setTitle(mContext.getString(resId));
    }

    @CarUxRestrictions.CarUxRestrictionsInfo
    public int getUxRestrictions() {
        return mUxRestrictions;
    }

    /** Gets the current {@link OnClickListener} */
    public OnClickListener getOnClickListener() {
        return mOnClickListener;
    }

    public boolean isShowingIconAndTitle() {
        return mShowIconAndTitle;
    }

    /** Sets the {@link OnClickListener} */
    public void setOnClickListener(OnClickListener listener) {
        mOnClickListener = listener;

        update();
    }

    /** Calls the {@link OnClickListener}. */
    public void performClick() {
        if (mListener != null) {
            mListener.performClick();
        }
    }

    /** Gets the current {@link DisplayBehavior} */
    public DisplayBehavior getDisplayBehavior() {
        return mDisplayBehavior;
    }

    /** Gets the current Icon */
    public Drawable getIcon() {
        return mIcon;
    }

    /** Sets the Icon of this MenuItem. */
    public void setIcon(Drawable icon) {
        mIcon = icon;

        update();
    }

    /** Sets the Icon of this MenuItem to a drawable resource. */
    public void setIcon(int resId) {
        setIcon(mContext.getDrawable(resId));
    }

    /** Returns if this is the search MenuItem, which has special behavior when searching */
    boolean isSearch() {
        return mIsSearch;
    }

    /**
     * Builder class.
     *
     * <p>Use the static {@link #createSearch(Context, OnClickListener)} or
     * {@link #createSettings(Context, OnClickListener)} if you want one of those specialized
     * buttons.
     */
    public static final class Builder {
        private Context mContext;

        private CharSequence mTitle;
        private Drawable mIcon;
        private OnClickListener mOnClickListener;
        private DisplayBehavior mDisplayBehavior = DisplayBehavior.ALWAYS;
        private boolean mIsTinted = true;
        private boolean mShowIconAndTitle = false;
        private boolean mIsEnabled = true;
        private boolean mIsCheckable = false;
        private boolean mIsChecked = false;
        private boolean mIsVisible = true;
        private boolean mIsActivatable = false;
        private boolean mIsActivated = false;
        private boolean mIsSearch = false;
        @CarUxRestrictions.CarUxRestrictionsInfo
        private int mUxRestrictions = CarUxRestrictions.UX_RESTRICTIONS_BASELINE;

        public Builder(Context c) {
            mContext = c;
        }

        /** Builds a {@link MenuItem} from the current state of the Builder */
        public MenuItem build() {
            if (mIsActivatable && (mShowIconAndTitle || mIcon == null)) {
                throw new IllegalStateException("Only simple icons can be activatable");
            }
            if (mIsCheckable
                    && (mDisplayBehavior == DisplayBehavior.NEVER
                    || mShowIconAndTitle
                    || mIsActivatable)) {
                throw new IllegalStateException("Unsupported options for a checkable MenuItem");
            }

            return new MenuItem(this);
        }

        /** Sets the title to a string resource id */
        public Builder setTitle(int resId) {
            setTitle(mContext.getString(resId));
            return this;
        }

        /** Sets the title */
        public Builder setTitle(CharSequence title) {
            mTitle = title;
            return this;
        }

        /**
         * Sets the icon to a drawable resource id.
         *
         * <p>The icon's color and size will be changed to match the other MenuItems.
         */
        public Builder setIcon(int resId) {
            mIcon = mContext.getDrawable(resId);
            return this;
        }

        /**
         * Sets whether to tint the icon, true by default.
         *
         * <p>Try not to use this, it should only be used if the MenuItem is displaying some
         * kind of logo or avatar and should be colored.
         */
        public Builder setTinted(boolean tinted) {
            mIsTinted = tinted;
            return this;
        }

        /** Sets whether the MenuItem is visible or not. Default true. */
        public Builder setVisible(boolean visible) {
            mIsVisible = visible;
            return this;
        }

        /**
         * Makes the MenuItem activatable, which means it will toggle it's visual state after
         * every click.
         */
        public Builder setActivatable() {
            mIsActivatable = true;
            return this;
        }

        /**
         * Sets whether or not the MenuItem is selected. If it is,
         * {@link View#setSelected(boolean)} will be called on its View.
         */
        public Builder setActivated(boolean activated) {
            setActivatable();
            mIsActivated = activated;
            return this;
        }

        /** Sets the {@link OnClickListener} */
        public Builder setOnClickListener(OnClickListener listener) {
            mOnClickListener = listener;
            return this;
        }

        /**
         * Used to show both the icon and title when displayed on the toolbar. If this
         * is false, only the icon while be displayed when the MenuItem is in the toolbar
         * and only the title will be displayed when the MenuItem is in the overflow menu.
         *
         * <p>Defaults to false.
         */
        public Builder setShowIconAndTitle(boolean showIconAndTitle) {
            mShowIconAndTitle = showIconAndTitle;
            return this;
        }

        /**
         * Sets the {@link DisplayBehavior}.
         *
         * <p>If the DisplayBehavior is {@link DisplayBehavior#NEVER}, the MenuItem must not be
         * {@link #setCheckable() checkable}.
         */
        public Builder setDisplayBehavior(DisplayBehavior behavior) {
            mDisplayBehavior = behavior;
            return this;
        }

        /** Sets whether the MenuItem is enabled or not. Default true. */
        public Builder setEnabled(boolean enabled) {
            mIsEnabled = enabled;
            return this;
        }

        /**
         * Makes the MenuItem checkable, meaning it will be displayed as a
         * switch. Currently a checkable MenuItem cannot have a {@link DisplayBehavior} of NEVER.
         *
         * <p>The MenuItem is not checkable by default.
         */
        public Builder setCheckable() {
            mIsCheckable = true;
            return this;
        }

        /**
         * Sets whether the MenuItem is checked or not. This will imply {@link #setCheckable()}.
         */
        public Builder setChecked(boolean checked) {
            setCheckable();
            mIsChecked = checked;
            return this;
        }

        /**
         * Sets under what {@link android.car.drivingstate.CarUxRestrictions.CarUxRestrictionsInfo}
         * the MenuItem should be restricted.
         */
        public Builder setUxRestrictions(
                @CarUxRestrictions.CarUxRestrictionsInfo int restrictions) {
            mUxRestrictions = restrictions;
            return this;
        }

        /** Sets that this is the search MenuItem, which has special behavior while searching */
        private Builder setSearch() {
            mIsSearch = true;
            return this;
        }

        /**
         * Creates a search MenuItem.
         *
         * <p>The advantage of using this over creating your own is getting an OEM-styled search
         * icon, and this button will always disappear while searching, even when the
         * {@link Toolbar Toolbar's} showMenuItemsWhileSearching is true.
         */
        public static MenuItem createSearch(Context c, OnClickListener listener) {
            return new Builder(c)
                    .setTitle(R.string.car_ui_toolbar_menu_item_search_title)
                    .setIcon(R.drawable.car_ui_icon_search)
                    .setOnClickListener(listener)
                    .setSearch()
                    .build();
        }

        /**
         * Creates a settings MenuItem.
         *
         * <p>The advantage of this over creating your own is getting an OEM-styled settings icon,
         * and that the MenuItem will be restricted based on
         * {@link CarUxRestrictions#UX_RESTRICTIONS_NO_SETUP}
         */
        public static MenuItem createSettings(Context c, OnClickListener listener) {
            return new Builder(c)
                    .setTitle(R.string.car_ui_toolbar_menu_item_settings_title)
                    .setIcon(R.drawable.car_ui_icon_settings)
                    .setOnClickListener(listener)
                    .setUxRestrictions(CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP)
                    .build();
        }
    }

    /**
     * OnClickListener for a MenuItem.
     */
    public interface OnClickListener {
        /** Called when the MenuItem is clicked */
        void onClick(MenuItem item);
    }

    /**
     * DisplayBehavior controls how the MenuItem is presented in the Toolbar
     */
    public enum DisplayBehavior {
        /** Always show the MenuItem on the toolbar instead of the overflow menu */
        ALWAYS,
        /** Never show the MenuItem in the toolbar, always put it in the overflow menu */
        NEVER
    }

    /** Listener for {@link Toolbar} to update when this MenuItem changes */
    interface Listener {
        /** Called when the MenuItem is changed. For use only by {@link Toolbar} */
        void onMenuItemChanged();

        /** Called when {@link MenuItem#performClick()} is called */
        void performClick();
    }

    void setListener(Listener listener) {
        mListener = listener;
    }
}
