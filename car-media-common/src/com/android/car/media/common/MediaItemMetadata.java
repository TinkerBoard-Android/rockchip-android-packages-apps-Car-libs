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

    /** Media item id */
    @Nullable
    public final String mId;

    /** Media item title */
    @Nullable
    public final CharSequence mTitle;

    /** Media item subtitle */
    @Nullable
    public final CharSequence mSubtitle;

    /** Media item description */
    @Nullable
    public final CharSequence mDescription;

    /** Queue item id. This can be used on {@link PlaybackModel#onSkipToQueueItem(long)} */
    public final long mQueueId;

    /** Creates an instance based on the individual pieces of data */
    public MediaItemMetadata(Context context, MediaMetadata metadata) {
        MediaDescription description = metadata.getDescription();
        mId = metadata.getDescription().getMediaId();
        mContext = context;
        mTitle = description.getTitle();
        mSubtitle = description.getSubtitle();
        mDescription = description.getDescription();
        mMediaMetadata = metadata;
        mMediaDescription = metadata.getDescription();
        mQueueId = 0;
    }

    /** Creates an instance based on a {@link MediaSession.QueueItem} */
    public MediaItemMetadata(Context context, MediaSession.QueueItem queueItem) {
        MediaDescription description = queueItem.getDescription();
        mId = description.getMediaId();
        mContext = context;
        mTitle = description.getTitle();
        mSubtitle = description.getSubtitle();
        mDescription = description.getDescription();
        mMediaMetadata = null;
        mMediaDescription = queueItem.getDescription();
        mQueueId = queueItem.getQueueId();
    }

    /**
     * @return a {@link Bitmap} corresponding to the album art of this item.
     */
    @Nullable
    public Bitmap getAlbumArt() {
        Bitmap bitmap = null;
        if (mMediaMetadata != null) {
            bitmap = getAlbumArtFromMetadata(mMediaMetadata);
        }
        if (bitmap == null && mMediaDescription != null) {
            bitmap = getAlbumArtFromDescription(mMediaDescription);
        }
        // TODO(b/76099191): Implement caching
        return bitmap;
    }

    private static Bitmap getAlbumArtFromMetadata(MediaMetadata metadata) {
        Bitmap icon = getMetadataBitmap(metadata);
        if (icon != null) {
            return icon;
        } else {
            // TODO(b/76099191): get icon from metadata URIs
        }
        return null;
    }

    private static Bitmap getAlbumArtFromDescription(MediaDescription description) {
        Bitmap icon = description.getIconBitmap();
        if (icon != null) {
            return icon;
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
