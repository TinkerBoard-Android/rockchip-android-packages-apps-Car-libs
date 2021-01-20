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

import android.view.View;

import com.android.car.ui.sharedlibrary.oemapis.InsetsOEMV1;

/**
 * {@link androidx.recyclerview.widget.RecyclerView}
 */
public interface RecyclerViewOEMV1 {

    /** {@link androidx.recyclerview.widget.RecyclerView#setAdapter(Adapter)} */
    void setAdapter(AdapterOEMV1 adapter);

    /**
     * Sets the number of columns in which grid needs to be divided.
     */
    void setNumOfColumns(int numberOfColumns);

    /**
     * Sets the scrollbar's insets. This padding is applied in addition to the
     * insets of the RecyclerView.
     */
    void setScrollBarInsets(InsetsOEMV1 insets);

    /**
     * Sets divider item decoration for linear layout.
     */
    void setLinearDividerItemDecoration(boolean enableDividers);

    /**
     * Sets divider item decoration for grid layout.
     */
    void setGridDividerItemDecoration(boolean enableDividers);

    /** {@link androidx.recyclerview.widget.RecyclerView#addOnScrollListener} */
    void addOnScrollListener(OnScrollListenerOEMV1 listener);

    /** {@link androidx.recyclerview.widget.RecyclerView#removeOnScrollListener} */
    void removeOnScrollListener(OnScrollListenerOEMV1 listener);

    /** {@link androidx.recyclerview.widget.RecyclerView#clearOnScrollListeners()} */
    void clearOnScrollListeners();

    /** {@link androidx.recyclerview.widget.RecyclerView#scrollToPosition(int)} */
    void scrollToPosition(int position);

    /** {@link androidx.recyclerview.widget.RecyclerView#smoothScrollBy(int, int)} */
    void smoothScrollBy(int dx, int dy);

    /** {@link androidx.recyclerview.widget.RecyclerView#smoothScrollToPosition(int)} */
    void smoothScrollToPosition(int position);

    /** {@link androidx.recyclerview.widget.RecyclerView#findViewHolderForAdapterPosition(int)} */
    ViewHolderOEMV1 findViewHolderForAdapterPosition(int position);

    /** {@link androidx.recyclerview.widget.RecyclerView#computeVerticalScrollRange()} */
    int computeVerticalScrollRange();

    /** {@link androidx.recyclerview.widget.RecyclerView#computeVerticalScrollOffset()} */
    int computeVerticalScrollOffset();

    /** {@link androidx.recyclerview.widget.RecyclerView#computeVerticalScrollExtent()} */
    int computeVerticalScrollExtent();

    /** {@link androidx.recyclerview.widget.RecyclerView#computeHorizontalScrollRange()} */
    int computeHorizontalScrollRange();

    /** {@link androidx.recyclerview.widget.RecyclerView#computeHorizontalScrollOffset()} */
    int computeHorizontalScrollOffset();

    /** {@link androidx.recyclerview.widget.RecyclerView#computeHorizontalScrollExtent()} */
    int computeHorizontalScrollExtent();

    /** {@link androidx.recyclerview.widget.RecyclerView#setHasFixedSize(boolean)} */
    void setHasFixedSize(boolean hasFixedSize);

    /** {@link androidx.recyclerview.widget.RecyclerView#hasFixedSize()} */
    boolean hasFixedSize();

    /**
     * Returns the view that will be displayed on the screen.
     */
    View getView();
}
