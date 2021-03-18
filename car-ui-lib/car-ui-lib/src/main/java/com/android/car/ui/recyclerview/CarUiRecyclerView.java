/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.sharedlibrarysupport.SharedLibraryFactorySingleton;

import java.lang.annotation.Retention;

/**
 * A class to access the {@link CarUiRecyclerView} methods. The appearance and layout is
 * customizable by OEM.
 * <p>
 * This is the base class for CarUiRecyclerView implementation.
 */
public abstract class CarUiRecyclerView extends RecyclerView {

    /**
     * Use this method to create an instance of CarUiRecyclerView at runtime.
     */
    public static CarUiRecyclerView create(Context context) {
        return SharedLibraryFactorySingleton.get(context)
                .createRecyclerView(context, null);
    }

    /**
     * Use this method to create an instance of CarUiRecyclerView at runtime.
     */
    public static CarUiRecyclerView create(Context context, AttributeSet attributeSet) {
        return SharedLibraryFactorySingleton.get(context)
                .createRecyclerView(context, attributeSet);
    }

    /**
     * The possible values for setScrollBarPosition. The default value is {@link
     * CarUiRecyclerViewLayout#LINEAR}.
     */
    @IntDef({
            CarUiRecyclerViewLayout.LINEAR,
            CarUiRecyclerViewLayout.GRID,
    })
    @Retention(SOURCE)
    public @interface CarUiRecyclerViewLayout {
        /**
         * Arranges items either horizontally in a single row or vertically in a single column. This
         * is default.
         */
        int LINEAR = 0;

        /**
         * Arranges items in a Grid.
         */
        int GRID = 1;
    }

    /**
     * Interface for a {@link RecyclerView.Adapter} to cap the number of items.
     *
     * <p>NOTE: it is still up to the adapter to use maxItems in {@link
     * RecyclerView.Adapter#getItemCount()}.
     *
     * <p>the recommended way would be with:
     *
     * <pre>{@code
     * {@literal@}Override
     * public int getItemCount() {
     *   return Math.min(super.getItemCount(), mMaxItems);
     * }
     * }</pre>
     */
    public interface ItemCap {

        /**
         * A value to pass to {@link #setMaxItems(int)} that indicates there should be no limit.
         */
        int UNLIMITED = -1;

        /**
         * Sets the maximum number of items available in the adapter. A value less than '0' means
         * the list should not be capped.
         */
        void setMaxItems(int maxItems);
    }

    public CarUiRecyclerView(Context context) {
        super(context);
    }

    public CarUiRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CarUiRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Sets the number of columns in which grid needs to be divided.
     */
    public abstract void setNumOfColumns(int numberOfColumns);

    /**
     * Sets the scrollbar's padding top and bottom. This padding is applied in addition to the
     * padding of the RecyclerView.
     */
    public abstract void setScrollBarPadding(int paddingTop, int paddingBottom);

    /**
     * Sets divider item decoration for linear layout. This should be called after the call to
     * {@link #setLayoutManager(LayoutManager)}  has been completed. If called before that could
     * result in unexpected behavior.
     */
    public abstract void setLinearDividerItemDecoration(boolean enableDividers);

    /**
     * Sets divider item decoration for grid layout. This should be called after the call to {@link
     * #setLayoutManager(LayoutManager)}  has been completed. If called before that could result in
     * unexpected behavior.
     */
    public abstract void setGridDividerItemDecoration(boolean enableDividers);

    /**
     * Set the {@link LayoutManager} that this RecyclerView will use.
     *
     * <p>In contrast to other adapter-backed views such as {@link android.widget.ListView}
     * or {@link android.widget.GridView}, RecyclerView allows client code to provide custom layout
     * arrangements for child views. These arrangements are controlled by the {@link LayoutManager}.
     * A LayoutManager must be provided for RecyclerView to function.</p>
     *
     * <p>Several default strategies are provided for common uses such as lists and grids.</p>
     *
     * @param layoutManager LayoutManager to use
     * @deprecated to be implemented by OEM
     */
    @Deprecated
    @Override
    public void setLayoutManager(@Nullable LayoutManager layoutManager) {
        super.setLayoutManager(layoutManager);
    }

    /**
     * Add an {@link ItemDecoration} to this RecyclerView. Item decorations can affect both
     * measurement and drawing of individual item views.
     *
     * <p>Item decorations are ordered. Decorations placed earlier in the list will
     * be run/queried/drawn first for their effects on item views. Padding added to views will be
     * nested; a padding added by an earlier decoration will mean further item decorations in the
     * list will be asked to draw/pad within the previous decoration's given area.</p>
     *
     * @param decor Decoration to add
     * @deprecated to be implemented by OEMs
     */
    @Deprecated
    @Override
    public void addItemDecoration(@NonNull ItemDecoration decor) {
        super.addItemDecoration(decor);
    }

    /**
     * Add an {@link ItemDecoration} to this RecyclerView. Item decorations can affect both
     * measurement and drawing of individual item views.
     *
     * <p>Item decorations are ordered. Decorations placed earlier in the list will
     * be run/queried/drawn first for their effects on item views. Padding added to views will be
     * nested; a padding added by an earlier decoration will mean further item decorations in the
     * list will be asked to draw/pad within the previous decoration's given area.</p>
     *
     * @param decor Decoration to add
     * @param index Position in the decoration chain to insert this decoration at. If this value is
     *              negative the decoration will be added at the end.
     * @deprecated to be implemented by OEM
     */
    @Deprecated
    @Override
    public void addItemDecoration(@NonNull ItemDecoration decor, int index) {
        super.addItemDecoration(decor, index);
    }

    /**
     * Returns an {@link ItemDecoration} previously added to this RecyclerView.
     *
     * @param index The index position of the desired ItemDecoration.
     * @return the ItemDecoration at index position
     * @throws IndexOutOfBoundsException on invalid index
     * @deprecated to be handled by OEM
     */
    @Deprecated
    @Override
    @NonNull
    public ItemDecoration getItemDecorationAt(int index) {
        return super.getItemDecorationAt(index);
    }

    /**
     * Returns the number of {@link ItemDecoration} currently added to this RecyclerView.
     *
     * @return number of ItemDecorations currently added added to this RecyclerView.
     * @deprecated to be handled by OEM
     */
    @Deprecated
    @Override
    public int getItemDecorationCount() {
        return super.getItemDecorationCount();
    }

    /**
     * Removes the {@link ItemDecoration} associated with the supplied index position.
     *
     * @param index The index position of the ItemDecoration to be removed.
     * @deprecated to be handled by OEM
     */
    @Deprecated
    @Override
    public void removeItemDecorationAt(int index) {
        super.removeItemDecorationAt(index);
    }

    /**
     * Remove an {@link ItemDecoration} from this RecyclerView.
     *
     * <p>The given decoration will no longer impact the measurement and drawing of
     * item views.</p>
     *
     * @param decor Decoration to remove
     * @see #addItemDecoration(ItemDecoration)
     * @deprecated to be handled by OEM
     */
    @Deprecated
    @Override
    public void removeItemDecoration(@NonNull ItemDecoration decor) {
        super.removeItemDecoration(decor);
    }
}
