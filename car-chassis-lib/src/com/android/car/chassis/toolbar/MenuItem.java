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
package com.android.car.chassis.toolbar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;

import com.android.car.chassis.R;

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

    private Context mContext;
    private Listener mListener;
    private CharSequence mTitle;
    private Drawable mIcon;
    private OnClickListener mOnClickListener;
    private DisplayBehavior mDisplayBehavior;
    private boolean mIsEnabled;
    private boolean mIsCheckable;
    private boolean mIsChecked;
    private int mId;
    private View mView;

    private MenuItem(Builder builder) {
        mContext = builder.mContext;
        mTitle = builder.mTitle;
        mIcon = builder.mIcon;
        mOnClickListener = builder.mOnClickListener;
        mDisplayBehavior = builder.mDisplayBehavior;
        mIsEnabled = builder.mIsEnabled;
        mIsCheckable = builder.mIsCheckable;
        mIsChecked = builder.mIsChecked;
        mId = builder.mId;
    }

    /** Returns whether the MenuItem is enabled */
    public boolean isEnabled() {
        return mIsEnabled;
    }

    /** Sets whether the MenuItem is enabled */
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;

        if (mView != null) {
            recursiveSetEnabled(mView, mIsEnabled);
        }
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

        if (mView != null) {
            Switch s = mView.requireViewById(R.id.chassis_toolbar_menu_item_switch);
            s.setChecked(mIsChecked);
        }

        if (isEnabled() && getOnClickListener() != null) {
            getOnClickListener().onClick(this);
        }
    }

    /** Gets the title of this MenuItem. */
    public CharSequence getTitle() {
        return mTitle;
    }

    /** Sets the title of this MenuItem. */
    public void setTitle(CharSequence title) {
        mTitle = title;

        if (mView != null) {
            Button button = mView.findViewById(R.id.chassis_toolbar_menu_item_text);
            if (button != null) {
                button.setText(mTitle);
            }
        }

        if (mListener != null) {
            mListener.onMenuItemTitleChanged(this, mTitle);
        }
    }

    /** Sets the title of this MenuItem to a string resource. */
    public void setTitle(int resId) {
        setTitle(mContext.getString(resId));
    }

    /** Gets the current {@link OnClickListener} */
    public OnClickListener getOnClickListener() {
        return mOnClickListener;
    }

    /** Sets the {@link OnClickListener} */
    public void setOnClickListener(OnClickListener listener) {
        mOnClickListener = listener;
    }

    /** Gets the current {@link DisplayBehavior} */
    public DisplayBehavior getDisplayBehavior() {
        return mDisplayBehavior;
    }

    /** Gets the current Icon */
    public Drawable getIcon() {
        return mIcon;
    }

    int getId() {
        return mId;
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
        private boolean mIsEnabled = true;
        private boolean mIsCheckable = false;
        private boolean mIsChecked = false;
        private int mId;

        public Builder(Context c) {
            mContext = c;
        }

        /** Builds a {@link MenuItem} from the current state of the Builder */
        public MenuItem build() {
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

        /** Sets the icon to a drawable resource id */
        public Builder setIcon(int resId) {
            mIcon = mContext.getDrawable(resId);
            return this;
        }

        /** Sets the {@link OnClickListener} */
        public Builder setOnClickListener(OnClickListener listener) {
            mOnClickListener = listener;
            return this;
        }

        /**
         * Sets the {@link DisplayBehavior}.
         *
         * <p>If the DisplayBehavior is {@link DisplayBehavior#NEVER}, the MenuItem must not be
         * {@link #setCheckable() checkable}.
         */
        public Builder setDisplayBehavior(DisplayBehavior behavior) {
            if (behavior == DisplayBehavior.NEVER && mIsCheckable) {
                throw new IllegalStateException(
                        "Currently we don't support a checkable overflow item");
            }
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
            if (mDisplayBehavior == DisplayBehavior.NEVER) {
                throw new IllegalStateException(
                        "Currently we don't support a checkable overflow item");
            }
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

        private Builder setId(int id) {
            mId = id;
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
                    .setTitle(R.string.chassis_toolbar_menu_item_search_title)
                    .setIcon(R.drawable.chassis_icon_search)
                    .setOnClickListener(listener)
                    .setId(R.id.search)
                    .build();
        }

        /**
         * Creates a settings MenuItem.
         *
         * <p>The advantage of this over creating your own is getting an OEM-styled settings icon.
         */
        public static MenuItem createSettings(Context c, OnClickListener listener) {
            return new Builder(c)
                    .setTitle(R.string.chassis_toolbar_menu_item_settings_title)
                    .setIcon(R.drawable.chassis_icon_settings)
                    .setOnClickListener(listener)
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
        /** Called when the MenuItem's title is changed. For use only by {@link Toolbar} */
        void onMenuItemTitleChanged(MenuItem item, CharSequence title);
    }

    void setListener(Listener listener) {
        mListener = listener;
    }

    View createView(ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        if (isCheckable()) {
            mView = inflater.inflate(
                    R.layout.chassis_toolbar_menu_item_switch, parent, false);
            Switch s = mView.requireViewById(R.id.chassis_toolbar_menu_item_switch);
            s.setChecked(isChecked());
        } else if (getIcon() != null) {
            mView = inflater.inflate(
                    R.layout.chassis_toolbar_menu_item_icon, parent, false);
            ImageView imageView = mView.requireViewById(R.id.chassis_toolbar_menu_item_icon);
            imageView.setImageDrawable(getIcon());
        } else {
            mView = (Button) inflater.inflate(
                    R.layout.chassis_toolbar_menu_item_text, parent, false);
            Button button = mView.requireViewById(R.id.chassis_toolbar_menu_item_text);
            button.setText(getTitle());
        }

        if (getId() != 0) {
            mView.setId(getId());
        }

        recursiveSetEnabled(mView, isEnabled());
        mView.setOnClickListener(v -> {
            Switch s = v.findViewById(R.id.chassis_toolbar_menu_item_switch);
            if (s != null) {
                s.toggle();
                setChecked(s.isChecked());
            }

            if (getOnClickListener() != null) {
                getOnClickListener().onClick(this);
            }
        });

        return mView;
    }

    private void recursiveSetEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = ((ViewGroup) view);
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                recursiveSetEnabled(viewGroup.getChildAt(i), enabled);
            }
        }
    }
}
