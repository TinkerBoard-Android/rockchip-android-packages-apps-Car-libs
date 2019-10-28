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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUxRestrictionsUtil;
import com.android.car.ui.uxr.DrawableStateView;

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

    @Override
    public View getView() {
        if (mMenuItem.mCustomLayoutId != 0) {
            return mView;
        }
        return null;
    }

    View createView() {
        LayoutInflater inflater = (LayoutInflater) mParentView.getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        if (mMenuItem.mCustomLayoutId != 0) {
            mView = inflater.inflate(mMenuItem.mCustomLayoutId, mParentView, false);
        } else if (mMenuItem.isCheckable()) {
            mView = inflater.inflate(
                    R.layout.car_ui_toolbar_menu_item_switch, mParentView, false);
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
        }

        Switch s = mView.findViewById(R.id.car_ui_toolbar_menu_item_switch);
        if (s != null) {
            s.setChecked(mMenuItem.isChecked());
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
}
