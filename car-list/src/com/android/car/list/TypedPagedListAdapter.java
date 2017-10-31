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
 * limitations under the License
 */

package com.android.car.list;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.annotation.CallSuper;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.content.Context;
import android.support.car.widget.PagedListView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.Retention;
import java.util.ArrayList;

/**
 * Renders all types of LineItem to a view to be displayed as a row in a list.
 */
public class TypedPagedListAdapter extends RecyclerView.Adapter<ViewHolder>
        implements PagedListView.ItemCap {

    private final Context mContext;
    private ArrayList<LineItem> mContentList;

    /**
     * Constructor with an empty content list
     *
     * @param context program context
     */
    public TypedPagedListAdapter(@NonNull Context context) {
        this(context, new ArrayList<>());
    }

    /**
     * Constructor with a context and content list
     *
     * @param context program context
     * @param contentList what is contained in the adapter
     */
    public TypedPagedListAdapter(@NonNull Context context, ArrayList<LineItem> contentList) {
        mContext = context;
        mContentList = contentList;
    }

    /**
     * Change current content list to new content list
     *
     * @param contentList New content list
     */
    public void updateList(ArrayList<LineItem> contentList) {
        mContentList = contentList;
        notifyDataSetChanged();
    }

    /**
     * Return true if empty, false if not.
     */
    public boolean isEmpty() {
        return mContentList.isEmpty();
    }

    /**
     * Definition for items that are able to be inserted into the TypedPagedListAdapter
     */
    public static abstract class LineItem<VH extends ViewHolder> {
        @Retention(SOURCE)
        @IntDef({TEXT_TYPE,
                TOGGLE_TYPE,
                ICON_TEXT_TYPE,
                SEEKBAR_TYPE,
                ICON_TOGGLE_TYPE,
                CHECKBOX_TYPE,
                EDIT_TEXT_TYPE,
                SINGLE_TEXT_TYPE,
                SPINNER_TYPE,
                PASSWORD_TYPE})
        public @interface LineItemType {}

        // with one title and one description
        static final int TEXT_TYPE = 1;

        // with one tile, one description, and a toggle on the right.
        static final int TOGGLE_TYPE = 2;

        // with one icon, one tile and one description.
        static final int ICON_TEXT_TYPE = 3;

        // with one tile and one seekbar.
        static final int SEEKBAR_TYPE = 4;

        // with one icon, title, description and a toggle.
        static final int ICON_TOGGLE_TYPE = 5;

        // with one icon, title and a checkbox.
        static final int CHECKBOX_TYPE = 6;

        // with one title and a EditText.
        static final int EDIT_TEXT_TYPE = 7;

        // with one title.
        static final int SINGLE_TEXT_TYPE = 8;

        // with a spinner.
        static final int SPINNER_TYPE = 9;

        // with a password input window and a checkbox for show password or not.
        static final int PASSWORD_TYPE = 10;

        /**
         * Returns the LineItemType of this LineItem
         *
         * @return LineItem's type
         */
        @LineItemType
        abstract int getType();

        /**
         * Called when binding the LineItem to its ViewHolder, sets up onClick to be called
         * when the item is clicked.
         *
         * @param holder The holder for the LineItem
         */
        @CallSuper
        void bindViewHolder(VH holder) {
            holder.itemView.setOnClickListener(this::onClick);
        }

        /**
         * Returns the description of this LineItem
         */
        public abstract CharSequence getDesc();

        /**
         * Returns true if this item is ready to receive touch. If it's set to false,
         * this item maybe not clickable or temporarily not functional.
         */
        public boolean isEnabled() {
            return true;
        }

        /**
         * Returns true if some component on this item can be clicked.
         */
        public boolean isClickable() {
            return isExpandable();
        }

        /**
         * Returns true if this item can launch another activity or fragment.
         */
        public abstract boolean isExpandable();

        /**
         * Called when the LineItem is clicked, default behavior is nothing.
         *
         * @param view Passed in by the view's onClick listener
         */
        public void onClick(View view) {
            // do nothing
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case LineItem.TEXT_TYPE:
                return TextLineItem.createViewHolder(parent);
            case LineItem.TOGGLE_TYPE:
                return ToggleLineItem.createViewHolder(parent);
            case LineItem.ICON_TEXT_TYPE:
                return IconTextLineItem.createViewHolder(parent);
            case LineItem.SEEKBAR_TYPE:
                return SeekbarLineItem.createViewHolder(parent);
            case LineItem.ICON_TOGGLE_TYPE:
                return IconToggleLineItem.createViewHolder(parent);
            case LineItem.CHECKBOX_TYPE:
                return CheckBoxLineItem.createViewHolder(parent);
            case LineItem.EDIT_TEXT_TYPE:
                return EditTextLineItem.createViewHolder(parent);
            case LineItem.SINGLE_TEXT_TYPE:
                return SingleTextLineItem.createViewHolder(parent);
            case LineItem.SPINNER_TYPE:
                return SpinnerLineItem.createViewHolder(parent);
            case LineItem.PASSWORD_TYPE:
                return PasswordLineItem.createViewHolder(parent);
            default:
                throw new IllegalStateException("ViewType not supported: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        mContentList.get(position).bindViewHolder(holder);
    }

    @Override
    public int getItemViewType(int position) {
        return mContentList.get(position).getType();
    }

    @Override
    public int getItemCount() {
        return mContentList.size();
    }

    @Override
    public void setMaxItems(int maxItems) {
        // no limit in this list.
    }
}
