/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.car.media.common;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaSession;

/**
 * Abstract representation of a media item metadata.
 */
public class MediaItemMetadata {
    private final MediaMetadata mMediaMetadata;
    private final MediaDescription mMediaDescription;
    private final Context mContext;

    /** Media item title */
    @Nullable
    public final CharSequence mTitle;

    /** Media item subtitle */
    @Nullable
    public final CharSequence mSubtitle;

    /** Media item description */
    @Nullable
    public final CharSequence mDescription;

    /** Creates an instance based on the individual pieces of data */
    public MediaItemMetadata(Context context, MediaMetadata metadata) {
        MediaDescription description = metadata.getDescription();
        mContext = context;
        mTitle = description.getTitle();
        mSubtitle = description.getSubtitle();
        mDescription = description.getDescription();
        mMediaMetadata = metadata;
        mMediaDescription = metadata.getDescription();
    }

    /** Creates an instance based on a {@link MediaSession.QueueItem} */
    public MediaItemMetadata(Context context, MediaSession.QueueItem queueItem) {
        MediaDescription description = queueItem.getDescription();
        mContext = context;
        mTitle = description.getTitle();
        mSubtitle = description.getSubtitle();
        mDescription = description.getDescription();
        mMediaMetadata = null;
        mMediaDescription = queueItem.getDescription();
    }

    /**
     * @return a {@link Drawable} corresponding to the album art of this item.
     */
    @Nullable
    public Drawable getAlbumArt() {
        Drawable drawable = null;
        if (mMediaMetadata != null) {
            drawable = getAlbumArtFromMetadata(mContext, mMediaMetadata);
        }
        if (drawable == null && mMediaDescription != null) {
            drawable = getAlbumArtFromDescription(mContext, mMediaDescription);
        }
        // TODO(b/76099191): Implement caching
        return drawable;
    }

    private static Drawable getAlbumArtFromMetadata(Context context, MediaMetadata metadata) {
        Bitmap icon = getMetadataBitmap(metadata);
        if (icon != null) {
            return new BitmapDrawable(context.getResources(), icon);
        } else {
            // TODO(b/76099191): get icon from metadata URIs
        }
        return null;
    }

    private static Drawable getAlbumArtFromDescription(Context context,
            MediaDescription description) {
        Bitmap icon = description.getIconBitmap();
        if (icon != null) {
            return new BitmapDrawable(context.getResources(), icon);
        } else {
            // TODO(b/76099191) get icon from description icon URI
        }
        return null;
    }

    private static final String[] PREFERRED_BITMAP_ORDER = {
            MediaMetadata.METADATA_KEY_ALBUM_ART,
            MediaMetadata.METADATA_KEY_ART,
            MediaMetadata.METADATA_KEY_DISPLAY_ICON
    };

    @Nullable
    private static Bitmap getMetadataBitmap(@NonNull MediaMetadata metadata) {
        // Get the best art bitmap we can find
        for (String bitmapKey : PREFERRED_BITMAP_ORDER) {
            Bitmap bitmap = metadata.getBitmap(bitmapKey);
            if (bitmap != null) {
                return bitmap;
            }
        }
        return null;
    }
}
