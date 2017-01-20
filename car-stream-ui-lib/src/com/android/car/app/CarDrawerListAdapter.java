package com.android.car.app;

import android.support.annotation.LayoutRes;
import android.support.car.ui.PagedListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.car.stream.ui.R;

/**
 * Variant of {@link CarDrawerAdapter} for displaying a list of items that support clicks.
 * <p>
 * Subclasses should implement:
 * <ul>
 *     <li>{@link #populateViewHolder(DrawerItemViewHolder, int)} to actually populate the
 *     drawer-item.</li>
 *     <li>{@link #getTitleResId()} to set the string to display when the Drawer is displaying this
 *     adapter's contents.</li>
 *     <li>{@link #getActualItemCount()} to return the actual number of items in the adapter (since
 *     {@link #getItemCount()} needs to honor {@link PagedListView.ItemCap}.</li>
 *     <li>{@link #onItemClick(int)} to handle clicks on items. To load a sub-level of items, the
 *     handler may call {@link CarDrawerActivity#switchToAdapter(CarDrawerAdapter)} to load
 *     the next level of items.</li>
 * </ul>
 */
public abstract class CarDrawerListAdapter extends CarDrawerAdapter
        implements DrawerItemClickListener {
    @LayoutRes
    private final int mItemLayoutResId;

    protected CarDrawerListAdapter(boolean useNormalLayout) {
        mItemLayoutResId = useNormalLayout ?
                R.layout.car_menu_list_item_normal : R.layout.car_menu_list_item_small;
    }

    @Override
    public final DrawerItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(mItemLayoutResId, parent, false);
        return new DrawerItemViewHolder(view);
    }

    @Override
    public final void onBindViewHolder(DrawerItemViewHolder holder, int position) {
        holder.setItemClickListener(this);
        populateViewHolder(holder, position);
    }

    /**
     * Subclasses should set all elements in {@code holder} to populate the drawer-item.
     * If some element is not used, it should be nulled out since these ViewHolder/View's are
     * recycled.
     */
    protected abstract void populateViewHolder(DrawerItemViewHolder holder, int position);
}