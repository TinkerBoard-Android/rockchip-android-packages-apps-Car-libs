/*
 * Copyright (c) 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.car.stream;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;

/**
 * A parcelable stream card that is used to send data from the StreamManager to the Overview.
 */
public final class StreamCard implements Parcelable {
    public static final int STANDARD_STREAM_CARD = 1 << 0;
    public static final int ONGOING_STREAM_CARD = 1 << 1;

    public static final int CONTENT_FLAG_HAS_BACKGROUND_BITMAP = 1;
    public static final int CONTENT_FLAG_HAS_BACKGROUND_COLOR = 2;

    public static final int PRIORITY_NORMAL = 0;

    public static final int PRIORITY_ONGOING = 100;
    public static final int PRIORITY_ONGOING_MEDIA = PRIORITY_ONGOING + 1;
    public static final int PRIORITY_ONGOING_CALL = PRIORITY_ONGOING + 2;
    public static final int PRIORITY_ONGOING_NAVIGATION = PRIORITY_ONGOING + 3;

    @IntDef({STANDARD_STREAM_CARD, ONGOING_STREAM_CARD})
    public @interface StreamCardCategory {}

    private String mDescription;
    private int mType;
    private RemoteViews mRemoteView;
    private RemoteViews mSmallRemoteView;
    private long mId;
    private int mPriority;
    private int mCategory;
    private Bitmap mBackgroundBitmap;
    private int mBackgroundColor;

    private int mFlags;

    public static final Parcelable.Creator<StreamCard> CREATOR = new
            Parcelable.Creator<StreamCard>() {
                public StreamCard createFromParcel(Parcel in) {
                    return new StreamCard(in);
                }

                public StreamCard[] newArray(int size) {
                    return new StreamCard[size];
                }
            };

    /**
     * @param category One of the {@link StreamCardCategory} values. This category is not intended
     *                 to be a combination of of sizes
     *                 (e.g. not STANDARD_STREAM_CARD | ONGOING_STREAM_CARD).
     */
    public StreamCard(@NonNull RemoteViews remoteViews, @NonNull RemoteViews smallRemoteView,
            @NonNull String description, long id, int priority, int type, @StreamCardCategory
            int category) {
        mDescription = description;
        mType = type;
        mRemoteView = remoteViews;
        mSmallRemoteView = smallRemoteView;
        mId = id;
        mPriority = priority;
        mCategory = category;
    }

    private StreamCard(Parcel in) {
        readFromParcel(in);
    }

    private void readFromParcel(Parcel in) {
        mFlags = in.readInt();
        mType = in.readInt();
        mDescription = in.readString();
        mRemoteView = RemoteViews.CREATOR.createFromParcel(in);
        mSmallRemoteView = RemoteViews.CREATOR.createFromParcel(in);
        mId = in.readLong();
        mPriority = in.readInt();
        mCategory = in.readInt();

        if ((mFlags & CONTENT_FLAG_HAS_BACKGROUND_BITMAP) > 0) {
            mBackgroundBitmap = Bitmap.CREATOR.createFromParcel(in);
        }

        if ((mFlags & CONTENT_FLAG_HAS_BACKGROUND_COLOR) > 0) {
            mBackgroundColor = in.readInt();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mFlags);
        out.writeInt(mType);
        out.writeString(mDescription);
        mRemoteView.writeToParcel(out, 0);
        mSmallRemoteView.writeToParcel(out, 0);
        out.writeLong(mId);
        out.writeInt(mPriority);
        out.writeInt(mCategory);

        if ((mFlags & CONTENT_FLAG_HAS_BACKGROUND_BITMAP) > 0) {
            mBackgroundBitmap.writeToParcel(out, 0);
        }

        if((mFlags & CONTENT_FLAG_HAS_BACKGROUND_COLOR) > 0) {
            out.writeInt(mBackgroundColor);
        }
    }

    /**
     * @return A description of the StreamCard, used when it is not possible to render the
     * {@link RemoteViews}
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * @return The type of this {@link StreamCard}.
     */
    public int getType() {
        return mType;
    }

    /**
     * @return The rendering category of this {@link StreamCard}.
     */
    @StreamCardCategory
    public int getCategory() {
        return mCategory;
    }

    /**
     * @return A {@link RemoteViews} that can be used to render this StreamCard.
     */
    public RemoteViews getRemoteView() {
        return mRemoteView;
    }

    /**
     * @return A smaller {@link RemoteViews} than the one returned by {@link #getRemoteView()}.
     */
    public RemoteViews getSmallRemoteView() {
        return mSmallRemoteView;
    }

    /**
     * Set the priority of this StreamCard.
     */
    public void setPriority(int priority) {
        mPriority = priority;
    }

    /**
     * @return Get the priority value of this StreamCard.
     */
    public int getPriority() {
        return mPriority;
    }

    /**
     * @return Get the unique ID of this StreamCard.
     */
    public long getId() {
        return mId;
    }

    /**
     * @return Get the background bitmap of this StreamCard if available.
     */
    public Bitmap getBackgroundBitmap() {
        return mBackgroundBitmap;
    }

    /**
     * Set the background bitmap of this StreamCard if available.
     */
    public void setBackgroundBitmap(Bitmap bitmap) {
        mBackgroundBitmap = bitmap;
        if (bitmap != null) {
            mFlags |= CONTENT_FLAG_HAS_BACKGROUND_BITMAP;
        } else {
            mFlags &= ~CONTENT_FLAG_HAS_BACKGROUND_BITMAP;
        }
    }

    /**
     * @return {@code true} if a background color was set for this StreamCard.
     */
    public boolean hasBackgroundColor() {
        return (mFlags &= CONTENT_FLAG_HAS_BACKGROUND_COLOR) > 0;
    }

    /**
     * @return Get the background colors specified for this card.
     */
    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    /**
     * Set the background color of this stream card.
     */
    public void setBackgroundColor(@ColorInt int color) {
        mBackgroundColor = color;
        mFlags |= CONTENT_FLAG_HAS_BACKGROUND_COLOR;
    }
}
