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
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.car.stream.ui.R;

/**
 * Re-usable ViewHolder that inflates car_menu_list_item.xml layout for use in the Drawer
 * PagedListView (see {@link CarDrawerActivity#getDrawerListView()}.
 * <p>
 * Clients should call {@link #create(ViewGroup, DrawerItemClickListener)} in their RecyclerView
 * Adapter's onCreateViewHolder.
 * <p>
 * A {@link DrawerItemClickListener} can be provided to handle clicks on specific items.
 */
public class DrawerItemViewHolder extends RecyclerView.ViewHolder {
    private final ImageView mIcon;
    private final TextView mTitle;
    private final TextView mText;
    private final ViewStub mRightItem;

    /**
     * Inflates car_menu_list_item.xml layout and wraps it in a DrawerItemViewHolder.
     *
     * @param parent Parent ViewGroup for created views.
     * @param listener Optional click listener to handle clicks.
     * @return DrawerItemViewHolder wrapping the inflated view.
     */
    public static DrawerItemViewHolder create(ViewGroup parent,
            @Nullable DrawerItemClickListener listener) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.car_menu_list_item, parent, false);
        return new DrawerItemViewHolder(view, listener);
    }

    /**
     * @return Icon ImageView from inflated car_menu_list_item.xml layout.
     */
    public ImageView getIcon() {
        return mIcon;
    }

    /**
     * @return Main title TextView inflated from car_menu_list_item.xml layout.
     */
    public TextView getTitle() {
        return mTitle;
    }

    /**
     * @return Main text TextView from inflated car_menu_list_item.xml layout.
     */
    public TextView getText() {
        return mText;
    }

    /**
     * @return Right-Item ViewStub from inflated car_menu_list_item.xml layout.
     */
    public ViewStub getRightItem() {
        return mRightItem;
    }

    private DrawerItemViewHolder(View view, @Nullable DrawerItemClickListener listener) {
        super(view);
        mIcon = (ImageView)view.findViewById(R.id.icon);
        mTitle = (TextView)view.findViewById(R.id.title);
        mText = (TextView)view.findViewById(R.id.text);
        mRightItem = (ViewStub)view.findViewById(R.id.right_item);
        if (listener != null) {
            view.setOnClickListener((unusedView) ->  {
                listener.onItemClick(getAdapterPosition());
            });
        }
    }
}
