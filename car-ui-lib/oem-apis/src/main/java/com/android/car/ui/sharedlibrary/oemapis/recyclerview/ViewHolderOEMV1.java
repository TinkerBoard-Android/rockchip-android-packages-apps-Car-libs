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

/** {@link androidx.recyclerview.widget.RecyclerView.ViewHolder} */
public interface ViewHolderOEMV1 {

    /** {@link androidx.recyclerview.widget.RecyclerView.ViewHolder#getAbsoluteAdapterPosition()} */
    int getAbsoluteAdapterPosition();

    /** {@link androidx.recyclerview.widget.RecyclerView.ViewHolder#getAdapterPosition()} */
    int getAdapterPosition();

    /** {@link androidx.recyclerview.widget.RecyclerView.ViewHolder#getBindingAdapter()} */
    AdapterOEMV1<? extends ViewHolderOEMV1> getBindingAdapter();

    /** {@link androidx.recyclerview.widget.RecyclerView.ViewHolder#getBindingAdapterPosition()} */
    int getBindingAdapterPosition();

    /** {@link androidx.recyclerview.widget.RecyclerView.ViewHolder#getItemId()} */
    long getItemId();

    /** {@link androidx.recyclerview.widget.RecyclerView.ViewHolder#getItemViewType()} */
    int getItemViewType();

    /** {@link androidx.recyclerview.widget.RecyclerView.ViewHolder#getLayoutPosition()} */
    int getLayoutPosition();

    /** {@link androidx.recyclerview.widget.RecyclerView.ViewHolder#getOldPosition()} */
    int getOldPosition();

    /** {@link androidx.recyclerview.widget.RecyclerView.ViewHolder#getPosition()} */
    int getPosition();

    /** {@link androidx.recyclerview.widget.RecyclerView.ViewHolder#isRecyclable()} */
    boolean isRecyclable();

    /** {@link androidx.recyclerview.widget.RecyclerView.ViewHolder#setIsRecyclable(boolean)} */
    void setIsRecyclable(boolean recyclable);

    /** {@link androidx.recyclerview.widget.RecyclerView.ViewHolder#toString()} */
    String toString();
}
