package com.android.car.app;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.car.stream.ui.R;

/**
 * Concrete subclass of {@link CarDrawerAdapter} to that displays a single "empty list" indicator.
 */
public class CarDrawerEmptyAdapter extends CarDrawerAdapter {
    @StringRes
    private final int mTitleResId;
    private final Drawable mEmptyListDrawable;

    public CarDrawerEmptyAdapter(Context context, @StringRes int titleResId) {
        mTitleResId = titleResId;
        final int iconColor = context.getColor(R.color.car_tint);
        mEmptyListDrawable = context.getDrawable(R.drawable.ic_list_view_disable);
        mEmptyListDrawable.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
    }

    @Override
    public DrawerItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.car_list_item_empty, parent, false);
        return new DrawerItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DrawerItemViewHolder holder, int position) {
        holder.getTitle().setText(null);
        holder.getIcon().setImageDrawable(mEmptyListDrawable);
        holder.setItemClickListener(null);
    }

    @Override
    protected int getActualItemCount() {
        return 1;
    }

    @Override
    protected int getTitleResId() {
        return mTitleResId;
    }
}
