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

import android.app.Activity;
import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.XmlRes;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUiUtils;
import com.android.car.ui.utils.CarUxRestrictionsUtil;
import com.android.car.ui.uxr.DrawableStateView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MenuItemRenderer implements MenuItem.Listener {

    private static final int[] RESTRICTED_STATE = new int[] {R.attr.state_ux_restricted};

    private Toolbar.State mToolbarState;
    private CarUxRestrictions mUxRestrictions;

    private final MenuItem mMenuItem;
    private final ViewGroup mParentView;
    private View mView;
    private boolean mPreviouslySetOnClickListener = false;

    MenuItemRenderer(MenuItem item, ViewGroup parentView) {
        mMenuItem = item;
        mParentView = parentView;
        mUxRestrictions = CarUxRestrictionsUtil.getInstance(parentView.getContext())
                .getCurrentRestrictions();
        mMenuItem.setListener(this);
    }

    void setToolbarState(Toolbar.State state) {
        mToolbarState = state;

        if (mMenuItem.isSearch()) {
            updateView();
        }
    }

    void setUxRestrictions(CarUxRestrictions restrictions) {
        mUxRestrictions = restrictions;

        if (mMenuItem.getUxRestrictions() != CarUxRestrictions.UX_RESTRICTIONS_BASELINE) {
            updateView();
        }
    }

    @Override
    public void performClick() {
        if (!isRestricted() && mView != null) {
            mView.performClick();
        }
    }

    @Override
    public void onMenuItemChanged() {
        updateView();
    }

    View createView() {
        LayoutInflater inflater = (LayoutInflater) mParentView.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        if (mMenuItem.isCheckable()) {
            mView = inflater.inflate(
                    R.layout.car_ui_toolbar_menu_item_switch, mParentView, false);
        } else if (mMenuItem.isShowingIconAndTitle()) {
            mView = inflater.inflate(
                    R.layout.car_ui_toolbar_menu_item_icon_and_text, mParentView, false);
        } else if (mMenuItem.getIcon() != null) {
            mView = inflater.inflate(
                    R.layout.car_ui_toolbar_menu_item_icon, mParentView, false);
        } else {
            mView = inflater.inflate(
                    R.layout.car_ui_toolbar_menu_item_text, mParentView, false);
        }

        updateView();
        return mView;
    }

    private void updateView() {
        if (mView == null) {
            return;
        }

        if (!mMenuItem.isVisible()
                || (mMenuItem.isSearch() && mToolbarState == Toolbar.State.SEARCH)) {
            mView.setVisibility(View.GONE);
            return;
        }

        mView.setVisibility(View.VISIBLE);

        ImageView imageView = mView.findViewById(R.id.car_ui_toolbar_menu_item_icon);
        if (imageView != null) {
            imageView.setImageDrawable(mMenuItem.getIcon());
        }

        TextView textView = mView.findViewById(R.id.car_ui_toolbar_menu_item_text);
        if (textView != null) {
            textView.setText(mMenuItem.getTitle());

            if (mMenuItem.isShowingIconAndTitle() && imageView == null) {
                int menuItemIconSize = mView.getContext().getResources()
                        .getDimensionPixelSize(R.dimen.car_ui_toolbar_menu_item_icon_size);

                mMenuItem.getIcon().setBounds(0, 0, menuItemIconSize, menuItemIconSize);

                textView.setCompoundDrawables(mMenuItem.getIcon(), null, null, null);
            }
        }

        Switch s = mView.findViewById(R.id.car_ui_toolbar_menu_item_switch);
        if (s != null) {
            s.setChecked(mMenuItem.isChecked());
        }

        if (!mMenuItem.isTinted()) {
            mMenuItem.getIcon().setTintList(null);
        }

        recursiveSetEnabledAndDrawableState(mView);
        mView.setActivated(mMenuItem.isActivated());

        MenuItem.OnClickListener onClickListener = mMenuItem.getOnClickListener();
        if (onClickListener != null || mMenuItem.isCheckable()) {
            if (isRestricted()) {
                mView.setOnClickListener(v -> Toast.makeText(mView.getContext(),
                        R.string.car_ui_restricted_while_driving, Toast.LENGTH_LONG).show());
            } else {
                mView.setOnClickListener(v -> {
                    if (mMenuItem.isActivatable()) {
                        mMenuItem.setActivated(!mMenuItem.isActivated());
                    }

                    if (mMenuItem.isCheckable()) {
                        mMenuItem.setChecked(!mMenuItem.isChecked());
                    }

                    if (onClickListener != null) {
                        onClickListener.onClick(mMenuItem);
                    }
                });
            }

            mPreviouslySetOnClickListener = true;
        } else if (mPreviouslySetOnClickListener) {
            // We should only set this stuff to null if we had previously set our own listener
            // to avoid overwriting a custom view's onClickListener
            mView.setOnClickListener(null);
            mView.setClickable(false);
            mPreviouslySetOnClickListener = false;
        }
    }

    private void recursiveSetEnabledAndDrawableState(View view) {
        view.setEnabled(mMenuItem.isEnabled());

        if (view instanceof ImageView) {
            ((ImageView) view).setImageState(isRestricted() ? RESTRICTED_STATE : null, true);
        } else if (view instanceof DrawableStateView) {
            ((DrawableStateView) view).setDrawableState(isRestricted() ? RESTRICTED_STATE : null);
        }

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = ((ViewGroup) view);
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                recursiveSetEnabledAndDrawableState(viewGroup.getChildAt(i));
            }
        }
    }

    private boolean isRestricted() {
        return CarUxRestrictionsUtil.isRestricted(mMenuItem.getUxRestrictions(), mUxRestrictions);
    }

    static List<MenuItem> readMenuItemList(Context c, @XmlRes int resId) {
        if (resId == 0) {
            return Collections.emptyList();
        }

        try (XmlResourceParser parser = c.getResources().getXml(resId)) {
            AttributeSet attrs = Xml.asAttributeSet(parser);
            List<MenuItem> menuItems = new ArrayList<>();

            parser.next();
            parser.next();
            parser.require(XmlPullParser.START_TAG, null, "MenuItems");
            while (parser.next() != XmlPullParser.END_TAG) {
                menuItems.add(readMenuItem(c, parser, attrs));
            }

            return menuItems;
        } catch (XmlPullParserException | IOException e) {
            throw new RuntimeException("Unable to parse Menu Items", e);
        }
    }

    private static MenuItem readMenuItem(Context c, XmlResourceParser parser, AttributeSet attrs)
            throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, null, "MenuItem");

        TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.CarUiToolbarMenuItem);
        try {
            String title = a.getString(R.styleable.CarUiToolbarMenuItem_title);
            Drawable icon = a.getDrawable(R.styleable.CarUiToolbarMenuItem_icon);
            boolean tinted = a.getBoolean(R.styleable.CarUiToolbarMenuItem_tinted, true);
            boolean visible = a.getBoolean(R.styleable.CarUiToolbarMenuItem_visible, true);
            boolean showIconAndTitle = a.getBoolean(
                    R.styleable.CarUiToolbarMenuItem_showIconAndTitle, false);
            boolean checkable = a.getBoolean(R.styleable.CarUiToolbarMenuItem_checkable, false);
            boolean checked = a.getBoolean(R.styleable.CarUiToolbarMenuItem_checked, false);
            boolean checkedExists = a.hasValue(R.styleable.CarUiToolbarMenuItem_checked);
            boolean activatable = a.getBoolean(R.styleable.CarUiToolbarMenuItem_activatable, false);
            boolean activated = a.getBoolean(R.styleable.CarUiToolbarMenuItem_activated, false);
            boolean activatedExists = a.hasValue(R.styleable.CarUiToolbarMenuItem_activated);
            int displayBehaviorInt = a.getInt(R.styleable.CarUiToolbarMenuItem_displayBehavior, 0);
            int uxRestrictions = a.getInt(R.styleable.CarUiToolbarMenuItem_uxRestrictions, 0);
            String onClickMethod = a.getString(R.styleable.CarUiToolbarMenuItem_onClick);
            MenuItem.OnClickListener onClickListener = null;

            if (onClickMethod != null) {
                Activity activity = CarUiUtils.getActivity(c);
                if (activity == null) {
                    throw new RuntimeException("Couldn't find an activity for the MenuItem");
                }

                try {
                    Method m = activity.getClass().getMethod(onClickMethod, MenuItem.class);
                    onClickListener = i -> {
                        try {
                            m.invoke(activity, i);
                        } catch (InvocationTargetException | IllegalAccessException e) {
                            throw new RuntimeException("Couldn't call the MenuItem's listener", e);
                        }
                    };
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("OnClick method "
                            + onClickMethod + "(MenuItem) not found in your activity", e);
                }
            }

            MenuItem.DisplayBehavior displayBehavior = displayBehaviorInt == 0
                    ? MenuItem.DisplayBehavior.ALWAYS
                    : MenuItem.DisplayBehavior.NEVER;

            parser.next();
            parser.require(XmlPullParser.END_TAG, null, "MenuItem");

            MenuItem.Builder builder = new MenuItem.Builder(c)
                    .setTitle(title)
                    .setIcon(icon)
                    .setOnClickListener(onClickListener)
                    .setUxRestrictions(uxRestrictions)
                    .setTinted(tinted)
                    .setVisible(visible)
                    .setShowIconAndTitle(showIconAndTitle)
                    .setDisplayBehavior(displayBehavior);

            if (checkable || checkedExists) {
                builder.setChecked(checked);
            }

            if (activatable || activatedExists) {
                builder.setActivated(activated);
            }

            return builder.build();
        } finally {
            a.recycle();
        }
    }
}
