package com.android.car.app;

import android.support.annotation.StringRes;
import android.support.car.ui.PagedListView;
import android.support.v7.widget.RecyclerView;

/**
 * Base Adapter for displaying items in the CarDrawerActivity's Drawer which is a PagedListView.
 * <p>
 * Implementors must return the string resource for the title that will be displayed when displaying
 * the contents of this adapter (see {@link #getTitleResId()}.
 * <p>
 * This class also takes care of implementing the PageListView.ItemCamp contract and subclasses
 * should implement {@link #getActualItemCount()}.
 */
public abstract class CarDrawerAdapter extends RecyclerView.Adapter<DrawerItemViewHolder>
        implements PagedListView.ItemCap {

    private int mMaxItems = -1;

    @Override
    public final void setMaxItems(int maxItems) {
        mMaxItems = maxItems;
    }

    @Override
    public final int getItemCount() {
        return mMaxItems >= 0  ? Math.min(mMaxItems, getActualItemCount()) : getActualItemCount();
    }

    /**
     * @return Actual number of items in this adapter.
     */
    protected abstract int getActualItemCount();

    /**
     * @return String resource to display in the toolbar title when displaying this adapter's
     * contents.
     */
    @StringRes
    protected abstract int getTitleResId();
}
