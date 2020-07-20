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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.ui.R;
import com.android.car.ui.utils.CarUiUtils;

/**
 * A {@link RecyclerView.Adapter} that can limit its content based on a given length limit which
 * can change at run-time.
 *
 * @param <T> type of the {@link RecyclerView.ViewHolder} objects used by base classes.
 */
public abstract class ContentLimitingAdapter<T extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ContentLimiting {
    private static final String TAG = "ContentLimitingAdapter";

    private static final int SCROLLING_LIMITED_MESSAGE_VIEW_TYPE = Integer.MAX_VALUE;

    private int mMaxItems = ContentLimiting.UNLIMITED;
    private Integer mScrollingLimitedMessageResId;

    /**
     * Returns the viewType value to use for the scrolling limited message views.
     *
     * Override this method to provide your own alternative value if {@link Integer#MAX_VALUE} is
     * a viewType value already in-use by your adapter.
     */
    protected int getScrollingLimitedMessageViewType() {
        return SCROLLING_LIMITED_MESSAGE_VIEW_TYPE;
    }

    @Override
    @NonNull
    public final RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        if (viewType == getScrollingLimitedMessageViewType()) {
            View rootView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.car_ui_list_limiting_message, parent, false);
            return new ScrollingLimitedViewHolder(rootView);
        }

        return onCreateViewHolderImpl(parent, viewType);
    }

    /**
     * Returns a {@link androidx.recyclerview.widget.RecyclerView.ViewHolder} of type {@code T}.
     *
     * <p>It is delegated to by {@link #onCreateViewHolder(ViewGroup, int)} to handle any
     * {@code viewType}s other than the one corresponding to the "scrolling is limited" message.
     */
    protected abstract T onCreateViewHolderImpl(
            @NonNull ViewGroup parent, int viewType);

    @Override
    @SuppressWarnings("unchecked")
    public final void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (isContentLimited() && position == getScrollingLimitedMessagePosition()) {
            String message = holder.itemView.getContext()
                    .getString(R.string.car_ui_scrolling_limited_message);
            if (mScrollingLimitedMessageResId != null) {
                message = holder.itemView.getContext().getString(mScrollingLimitedMessageResId);
            }
            ((ScrollingLimitedViewHolder) holder).setMessage(message);
            return;
        }
        onBindViewHolderImpl((T) holder, position);
    }

    /**
     * Binds {@link androidx.recyclerview.widget.RecyclerView.ViewHolder}s of type {@code T}.
     *
     * <p>It is delegated to by {@link #onBindViewHolder(RecyclerView.ViewHolder, int)} to handle
     * holders that are not of type {@link ScrollingLimitedViewHolder}.
     */
    protected abstract void onBindViewHolderImpl(T holder, int position);

    @Override
    public final int getItemViewType(int position) {
        if (isContentLimited() && position == getScrollingLimitedMessagePosition()) {
            return getScrollingLimitedMessageViewType();
        }
        return getItemViewTypeImpl(position);
    }

    /**
     * Returns the view type of the item at {@code position}.
     *
     * <p>It is delegated to by {@link #getItemViewType(int)} for all positions other than the
     * {@link #getScrollingLimitedMessagePosition()}.
     */
    protected abstract int getItemViewTypeImpl(int position);

    /**
     * Returns the position where the "scrolling is limited" message should be placed.
     *
     * <p>The default implementation is to put this item at the very end of the limited list.
     * Subclasses can override to choose a different position to suit their needs.
     */
    protected int getScrollingLimitedMessagePosition() {
        return getItemCount() - 1;
    }

    @Override
    public final int getItemCount() {
        if (isContentLimited()) {
            // If there are more items than the content limit, return the limit plus 1 more row
            // for the special "scrolling limited message" item.
            return mMaxItems + 1;
        }
        return getUnrestrictedItemCount();
    }

    private boolean isContentLimited() {
        return mMaxItems > ContentLimiting.UNLIMITED && getUnrestrictedItemCount() > mMaxItems;
    }

    /**
     * Returns the number of items in the unrestricted list being displayed via this adapter.
     */
    protected abstract int getUnrestrictedItemCount();

    @Override
    @SuppressWarnings("unchecked")
    public final void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        if (!(holder instanceof ScrollingLimitedViewHolder)) {
            onViewRecycledImpl((T) holder);
        }
    }

    /**
     * Recycles {@link androidx.recyclerview.widget.RecyclerView.ViewHolder}s of type {@code T}.
     *
     * <p>It is delegated to by {@link #onViewRecycled(RecyclerView.ViewHolder)} to handle
     * holders that are not of type {@link ScrollingLimitedViewHolder}.
     */
    @SuppressWarnings("unused")
    protected void onViewRecycledImpl(@NonNull T holder) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public final boolean onFailedToRecycleView(@NonNull RecyclerView.ViewHolder holder) {
        if (!(holder instanceof ScrollingLimitedViewHolder)) {
            return onFailedToRecycleViewImpl((T) holder);
        }
        return super.onFailedToRecycleView(holder);
    }

    /**
     * Handles failed recycle attempts for
     * {@link androidx.recyclerview.widget.RecyclerView.ViewHolder}s of type {@code T}.
     *
     * <p>It is delegated to by {@link #onFailedToRecycleView(RecyclerView.ViewHolder)} for holders
     * that are not of type {@link ScrollingLimitedViewHolder}.
     */
    protected boolean onFailedToRecycleViewImpl(@NonNull T holder) {
        return super.onFailedToRecycleView(holder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (!(holder instanceof ScrollingLimitedViewHolder)) {
            onViewAttachedToWindowImpl((T) holder);
        }
    }

    /**
     * Handles attaching {@link androidx.recyclerview.widget.RecyclerView.ViewHolder}s of type
     * {@code T} to the application window.
     *
     * <p>It is delegated to by {@link #onViewAttachedToWindow(RecyclerView.ViewHolder)} for
     * holders that are not of type {@link ScrollingLimitedViewHolder}.
     */
    @SuppressWarnings("unused")
    protected void onViewAttachedToWindowImpl(@NonNull T holder) {
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (!(holder instanceof ScrollingLimitedViewHolder)) {
            onViewDetachedFromWindowImpl((T) holder);
        }
    }

    /**
     * Handles detaching {@link androidx.recyclerview.widget.RecyclerView.ViewHolder}s of type
     * {@code T} from the application window.
     *
     * <p>It is delegated to by {@link #onViewDetachedFromWindow(RecyclerView.ViewHolder)} for
     * holders that are not of type {@link ScrollingLimitedViewHolder}.
     */
    @SuppressWarnings("unused")
    protected void onViewDetachedFromWindowImpl(@NonNull T holder) {
    }

    @Override
    public void setMaxItems(int maxItems) {
        int originalCount = getItemCount();
        mMaxItems = maxItems;
        int newCount = getItemCount();
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "maxItems " + mMaxItems
                    + " unrestrictedItemCount " + getUnrestrictedItemCount()
                    + " originalCount " + originalCount
                    + " newCount " + newCount);
        }

        if (newCount == originalCount && newCount == 1) {
            // Need to update the single item to show scrolling limited message or the actual
            // item, depending on the state of the car.
            notifyItemChanged(1);
        }
        if (newCount < originalCount) {
            notifyItemRangeRemoved(newCount, originalCount - newCount);
        } else {
            notifyItemRangeInserted(originalCount, newCount - originalCount + 1);
        }
    }

    @Override
    public void setScrollingLimitedMessageResId(@StringRes int resId) {
        if (mScrollingLimitedMessageResId == null || mScrollingLimitedMessageResId != resId) {
            mScrollingLimitedMessageResId = resId;
            notifyItemChanged(getScrollingLimitedMessagePosition());
        }
    }

    /**
     * {@link RecyclerView.ViewHolder} for the last item in a scrolling limited list.
     */
    public static class ScrollingLimitedViewHolder extends RecyclerView.ViewHolder {

        private final TextView mMessage;

        ScrollingLimitedViewHolder(@NonNull View itemView) {
            super(itemView);
            mMessage = CarUiUtils.findViewByRefId(itemView, R.id.car_ui_list_limiting_message);
        }

        /**
         * Sets the display message.
         */
        public void setMessage(String message) {
            mMessage.setText(message);
        }
    }
}
