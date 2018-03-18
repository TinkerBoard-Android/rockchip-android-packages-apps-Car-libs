/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.list;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A line item which presents an action button at the end of the line.
 */
public abstract class ActionIconButtonLineItem
        extends TypedPagedListAdapter.LineItem<ActionIconButtonLineItem.ViewHolder> {
    private final CharSequence mTitle;

    /**
     * Constructs an ActionIconButtonLineItem with just the title set.
     */
    public ActionIconButtonLineItem(CharSequence title) {
        mTitle = title;
    }

    @Override
    public int getType() {
        return ACTION_BUTTON_TYPE;
    }

    @Override
    public void bindViewHolder(ActionIconButtonLineItem.ViewHolder viewHolder) {
        super.bindViewHolder(viewHolder);
        viewHolder.mTitleView.setText(mTitle);
        setIcon(viewHolder.mIconView);
        setActionButtonIcon(viewHolder.mEndIconView);
        CharSequence desc = getDesc();
        if (TextUtils.isEmpty(desc)) {
            viewHolder.mDescView.setVisibility(View.GONE);
        } else {
            viewHolder.mDescView.setVisibility(View.VISIBLE);
            viewHolder.mDescView.setText(desc);
        }
        viewHolder.mEndIconContainer.setOnClickListener(
                v -> onActionButtonClick(viewHolder.getAdapterPosition()));
        viewHolder.mDividerLine.setVisibility(
                isClickable() && isEnabled() ? View.VISIBLE : View.GONE);
    }

    /**
     * ViewHolder that contains the elements that make up an ActionIconButtonLineItem,
     * including the title, description, icon, end action button, and divider.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView mTitleView;
        final TextView mDescView;
        final ImageView mIconView;
        final ImageView mEndIconView;
        final View mEndIconContainer;
        final View mDividerLine;

        public ViewHolder(View view) {
            super(view);
            mIconView = (ImageView) view.findViewById(R.id.icon);
            mEndIconView = (ImageView) view.findViewById(R.id.end_icon);
            mTitleView = (TextView) view.findViewById(R.id.title);
            mDescView = (TextView) view.findViewById(R.id.desc);
            mEndIconContainer = view.findViewById(R.id.end_icon_container);
            mDividerLine = view.findViewById(R.id.line_item_divider);
        }
    }

    /**
     * Creates a ViewHolder with the elements of an ActionIconButtonLineItem
     */
    public static RecyclerView.ViewHolder createViewHolder(ViewGroup parent) {
        return new ActionIconButtonLineItem.ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.action_icon_button_line_item, parent, false));
    }

    /**
     * Provides a {@link ImageView} so that the start icon can be set. Derived class should override
     * to set the icon.
     */
    public abstract void setIcon(ImageView iconView);

    /**
     * Provides a {@link ImageView} so that the action button icon can be set. Derived class should
     * override to set the icon.
     */
    public abstract void setActionButtonIcon(ImageView iconView);

    /**
     * Invoked when the action button's onClickListener is invoked.
     */
    public abstract void onActionButtonClick(int adapterPosition);
}
