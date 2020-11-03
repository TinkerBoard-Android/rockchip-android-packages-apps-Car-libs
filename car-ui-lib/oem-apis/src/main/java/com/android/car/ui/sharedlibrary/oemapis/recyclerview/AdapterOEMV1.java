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
package com.android.car.ui.sharedlibrary.oemapis.recyclerview;

import android.view.ViewGroup;

import java.util.List;

/**
 * See {@link androidx.recyclerview.widget.RecyclerView.Adapter}
 *
 * @param <VH> A class that extends ViewHolder that will be used by the adapter.
 */
public interface AdapterOEMV1<VH extends ViewHolderOEMV1> {

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#bindViewHolder} */
    void bindViewHolder(VH holder, int position);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#createViewHolder(ViewGroup, int)} */
    VH createViewHolder(ViewGroup parent, int viewType);

    /** See
     * {@link androidx.recyclerview.widget.RecyclerView.Adapter#findRelativeAdapterPositionIn}
     */
    int findRelativeAdapterPositionIn(AdapterOEMV1<? extends ViewHolderOEMV1> adapter,
            ViewHolderOEMV1 viewHolder, int localPosition);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#getItemCount()} */
    int getItemCount();

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#getItemId(int)} */
    long getItemId(int position);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#getItemViewType(int)} */
    int getItemViewType(int position);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#getStateRestorationPolicy()} */
    int getStateRestorationPolicy();

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#hasObservers()} */
    boolean hasObservers();

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#hasStableIds()} */
    boolean hasStableIds();

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#notifyDataSetChanged()} */
    void notifyDataSetChanged();

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#notifyItemChanged(int, Object)} */
    void notifyItemChanged(int position, Object payload);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#notifyItemChanged(int)} */
    void notifyItemChanged(int position);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#notifyItemInserted(int)} */
    void notifyItemInserted(int position);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#notifyItemMoved(int, int)} */
    void notifyItemMoved(int fromPosition, int toPosition);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#notifyItemRangeChanged(int, int, Object)} */
    void notifyItemRangeChanged(int positionStart, int itemCount, Object payload);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#notifyItemRangeChanged(int, int)} */
    void notifyItemRangeChanged(int positionStart, int itemCount);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#notifyItemRangeInserted(int, int)} */
    void notifyItemRangeInserted(int positionStart, int itemCount);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#notifyItemRangeRemoved(int, int)} */
    void notifyItemRangeRemoved(int positionStart, int itemCount);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#notifyItemRemoved(int)} */
    void notifyItemRemoved(int position);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#onAttachedToRecyclerView} */
    void onAttachedToRecyclerView(RecyclerViewOEMV1 recyclerView);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#onBindViewHolder} */
    void onBindViewHolder(VH holder, int position);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#onBindViewHolder} */
    void onBindViewHolder(VH holder, int position, List<Object> payloads);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#onCreateViewHolder(ViewGroup, int)} */
    VH onCreateViewHolder(ViewGroup parent, int viewType);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#onDetachedFromRecyclerView} */
    void onDetachedFromRecyclerView(RecyclerViewOEMV1 recyclerView);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#onFailedToRecycleView} */
    boolean onFailedToRecycleView(VH holder);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#onViewAttachedToWindow} */
    void onViewAttachedToWindow(VH holder);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#onViewDetachedFromWindow} */
    void onViewDetachedFromWindow(VH holder);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#onViewRecycled} */
    void onViewRecycled(VH holder);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#registerAdapterDataObserver} */
    void registerAdapterDataObserver(AdapterDataObserverOEMV1 observer);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#setHasStableIds(boolean)} */
    void setHasStableIds(boolean hasStableIds);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#setStateRestorationPolicy} */
    void setStateRestorationPolicy(int strategy);

    /** See {@link androidx.recyclerview.widget.RecyclerView.Adapter#unregisterAdapterDataObserver} */
    void unregisterAdapterDataObserver(AdapterDataObserverOEMV1 observer);
}
