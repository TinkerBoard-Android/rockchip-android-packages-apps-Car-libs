/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.car.app;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.car.stream.ui.R;

/**
 * Re-usable ViewHolder that inflates car_list_item1.xml layout for use in the Drawer PagedListView
 * (see {@link CarDrawerActivity#getDrawerListView()}.
 * <p>
 * Clients should call {@link #create(ViewGroup, DrawerItemClickListener)} in their RecyclerView
 * Adapter's onCreateViewHolder.
 * <p>
 * A {@link DrawerItemClickListener} can be provided to handle clicks on specific items.
 */
public class DrawerItemViewHolder extends RecyclerView.ViewHolder {
    private final ImageView mIcon;
    private final TextView mText;
    private final ImageView mRightIcon;

    /**
     * Inflates car_list_item1.xml layout and wraps it in a DrawerItemViewHolder.
     *
     * @param parent Parent ViewGroup for created views.
     * @param listener Optional click listener to handle clicks.
     * @return DrawerItemViewHolder wrapping the inflated view.
     */
    public static DrawerItemViewHolder create(ViewGroup parent,
            @Nullable DrawerItemClickListener listener) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.car_list_item_1, parent, false);
        return new DrawerItemViewHolder(view, listener);
    }

    /**
     * @return Icon ImageView from inflated car_list_item1.xml layout.
     */
    public ImageView getIcon() {
        return mIcon;
    }

    /**
     * @return TextView from inflated car_list_item1.xml layout.
     */
    public TextView getText() {
        return mText;
    }

    /**
     * @return Right-Icon ImageView from inflated car_list_item1.xml layout.
     */
    public ImageView getRightIcon() {
        return mRightIcon;
    }

    private DrawerItemViewHolder(View view, @Nullable DrawerItemClickListener listener) {
        super(view);
        mIcon = (ImageView)view.findViewById(R.id.icon);
        mText = (TextView)view.findViewById(R.id.text);
        mRightIcon = (ImageView)view.findViewById(R.id.right_icon);
        if (listener != null) {
            view.setOnClickListener((unusedView) ->  {
                listener.onItemClick(getAdapterPosition());
            });
        }
    }
}
