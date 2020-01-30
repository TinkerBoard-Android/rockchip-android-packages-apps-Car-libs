/*
 * Copyright (C) 2020 The Android Open Source Project
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

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class implements {@link ListAdapter} using a {@link RecyclerView.Adapter} as its
 * backing data.
 *
 * @param <T> The ViewHolder type of the Adapter we're converting.
 */
public class RecyclerViewToListAdapter<T extends RecyclerView.ViewHolder> implements ListAdapter {

    private RecyclerView.Adapter<T> mRecyclerViewAdapter;
    private DataSetObservable mDataSetObservable = new DataSetObservable();
    private Map<View, T> mViewsToViewHolders = new HashMap<>();

    public RecyclerViewToListAdapter(RecyclerView.Adapter<T> adapter) {
        mRecyclerViewAdapter = adapter;
        mRecyclerViewAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                mDataSetObservable.notifyChanged();
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                mDataSetObservable.notifyChanged();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                mDataSetObservable.notifyChanged();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                mDataSetObservable.notifyChanged();
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                mDataSetObservable.notifyChanged();
            }
        });
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.registerObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mDataSetObservable.unregisterObserver(observer);
    }

    @Override
    public int getCount() {
        return mRecyclerViewAdapter.getItemCount();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // If convertView is null, we need to create a ViewHolder. But if it's not,
        // we can skip straight to bindViewHolder(), reusing the old viewHolder.
        T holder;
        if (convertView == null || !mViewsToViewHolders.containsKey(convertView)) {
            holder = mRecyclerViewAdapter.createViewHolder(parent, getItemViewType(position));
            mViewsToViewHolders.put(holder.itemView, holder);
        } else {
            holder = mViewsToViewHolders.get(convertView);
        }
        mRecyclerViewAdapter.bindViewHolder(holder, position);
        return holder.itemView;
    }

    @Override
    public int getItemViewType(int position) {
        return mRecyclerViewAdapter.getItemViewType(position);
    }

    @Override
    public int getViewTypeCount() {
        Set<Integer> types = new HashSet<>();
        for (int i = 0; i < mRecyclerViewAdapter.getItemCount(); i++) {
            types.add(mRecyclerViewAdapter.getItemViewType(i));
        }
        return types.size();
    }

    @Override
    public boolean isEmpty() {
        return getCount() == 0;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }
}
