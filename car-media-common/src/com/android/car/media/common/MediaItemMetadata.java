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

import android.annotation.DrawableRes;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.VisibleForTesting;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract representation of a media item metadata.
 */
public class MediaItemMetadata implements Parcelable {
    private static final String TAG = "MediaItemMetadata";
    @NonNull
    private final MediaDescriptionCompat mMediaDescription;
    @Nullable
    private final Long mQueueId;
    private final boolean mIsBrowsable;
    private final boolean mIsPlayable;
    private final String mAlbumTitle;
    private final String mArtist;

    public MediaItemMetadata(@NonNull MediaDescriptionCompat description) {
        this(description, null, false, false, null, null);
    }

    /** Creates an instance based on a {@link MediaMetadataCompat} */
    public MediaItemMetadata(@NonNull MediaMetadataCompat metadata) {
        this(metadata.getDescription(), null, false, false,
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM),
                metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST));
    }

    /** Creates an instance based on a {@link MediaSessionCompat.QueueItem} */
    public MediaItemMetadata(@NonNull MediaSessionCompat.QueueItem queueItem) {
        this(queueItem.getDescription(), queueItem.getQueueId(), false, true, null, null);
    }

    /** Creates an instance based on a {@link MediaBrowserCompat.MediaItem} */
    public MediaItemMetadata(@NonNull MediaBrowserCompat.MediaItem item) {
        this(item.getDescription(), null, item.isBrowsable(), item.isPlayable(), null, null);
    }

    /** Creates an instance based on a {@link Parcel} */
    public MediaItemMetadata(@NonNull Parcel in) {
        mMediaDescription = (MediaDescriptionCompat) in.readValue(
                MediaDescriptionCompat.class.getClassLoader());
        mQueueId = in.readByte() == 0x00 ? null : in.readLong();
        mIsBrowsable = in.readByte() != 0x00;
        mIsPlayable = in.readByte() != 0x00;
        mAlbumTitle = in.readString();
        mArtist = in.readString();
    }

    /**
     * Creates a clone of this item
     *
     * @deprecated this method will be removed as part of b/79089344
     */
    @Deprecated
    public MediaItemMetadata(@NonNull MediaItemMetadata item) {
        mMediaDescription = item.mMediaDescription;
        mQueueId = item.mQueueId;
        mIsBrowsable = item.mIsBrowsable;
        mIsPlayable = item.mIsPlayable;
        mAlbumTitle = item.mAlbumTitle;
        mArtist = item.mArtist;
    }

    @VisibleForTesting
    public MediaItemMetadata(MediaDescriptionCompat description, Long queueId, boolean isBrowsable,
            boolean isPlayable, String albumTitle, String artist) {
        mMediaDescription = description;
        mQueueId = queueId;
        mIsPlayable = isPlayable;
        mIsBrowsable = isBrowsable;
        mAlbumTitle = albumTitle;
        mArtist = artist;
    }

    /** @return media item id */
    @Nullable
    public String getId() {
        return mMediaDescription.getMediaId();
    }

    /** @return media item title */
    @Nullable
    public CharSequence getTitle() {
        return mMediaDescription.getTitle();
    }

    /** @return media item subtitle */
    @Nullable
    public CharSequence getSubtitle() {
        return mMediaDescription.getSubtitle();
    }

    /** @return the album title for the media */
    @Nullable
    public String getAlbumTitle() {
        return mAlbumTitle;
    }

    /** @return the artist of the media */
    @Nullable
    public CharSequence getArtist() {
        return mArtist;
    }

    /**
     * @return the id of this item in the session queue, or NULL if this is not a session queue
     * item.
     */
    @Nullable
    public Long getQueueId() {
        return mQueueId;
    }

    /**
     * @return album art bitmap, or NULL if this item doesn't have a local album art. In this,
     * the {@link #getAlbumArtUri()} should be used to obtain a reference to a remote bitmap.
     */
    public Bitmap getAlbumArtBitmap() {
        return mMediaDescription.getIconBitmap();
    }

    /**
     * @return an {@link Uri} referencing the album art bitmap.
     */
    public Uri getAlbumArtUri() {
        return mMediaDescription.getIconUri();
    }

    /**
     * @return boolean that indicate if media is explicit.
     */
    public boolean isExplicit() {
        Bundle extras = mMediaDescription.getExtras();
        return extras != null && extras.getLong(MediaConstants.EXTRA_IS_EXPLICIT)
                == MediaConstants.EXTRA_METADATA_ENABLED_VALUE;
    }

    /**
     * @return boolean that indicate if media is downloaded.
     */
    public boolean isDownloaded() {
        Bundle extras = mMediaDescription.getExtras();
        return extras != null && extras.getLong(MediaConstants.EXTRA_DOWNLOAD_STATUS)
                == MediaDescriptionCompat.STATUS_DOWNLOADED;
    }

    /**
     * Updates the given {@link ImageView} with the album art of the given media item. This is an
     * asynchronous operation.
     * Note: If a view is set using this method, it should also be cleared using this same method.
     * Given that the loading is asynchronous, missing to use this method for clearing could cause
     * a delayed request to set an undesired image, or caching entries to be used for images not
     * longer necessary.
     *
     * @param context          {@link Context} used to load resources from
     * @param metadata         metadata to use, or NULL if the {@link ImageView} should be cleared.
     * @param imageView        loading target
     * @param loadingIndicator a drawable resource that would be set into the {@link ImageView}
     *                         while the image is being downloaded, or 0 if no loading indicator
     *                         is required.
     */
    public static void updateImageView(Context context, @Nullable MediaItemMetadata metadata,
            ImageView imageView, @DrawableRes int loadingIndicator) {
        Glide.with(context).clear(imageView);
        if (metadata == null) {
            imageView.setImageBitmap(null);
            imageView.setVisibility(View.GONE);
            return;
        }
        Bitmap image = metadata.getAlbumArtBitmap();
        if (image != null) {
            imageView.setImageBitmap(image);
            imageView.setVisibility(View.VISIBLE);
            return;
        }
        Uri imageUri = metadata.getAlbumArtUri();
        if (imageUri != null) {
            Glide.with(context)
                    .load(imageUri)
                    .apply(RequestOptions.placeholderOf(loadingIndicator))
                    .into(imageView);
            imageView.setVisibility(View.VISIBLE);
            return;
        }
        imageView.setImageBitmap(null);
        imageView.setVisibility(View.GONE);
    }

    /**
     * Loads the album art of this media item asynchronously. The loaded image will be scaled to
     * fit into the given view size.
     * Using {@link #updateImageView(Context, MediaItemMetadata, ImageView, int)} method is
     * preferred. Only use this method if you are going to apply transformations to the loaded
     * image.
     *
     * @param width  desired width (should be > 0)
     * @param height desired height (should be > 0)
     * @param fit    whether the image should be scaled to fit (fitCenter), or it should be cropped
     *               (centerCrop).
     * @return a {@link CompletableFuture} that will be completed once the image is loaded, or the
     * loading fails.
     */
    public CompletableFuture<Bitmap> getAlbumArt(Context context, int width, int height,
            boolean fit) {
        Bitmap image = getAlbumArtBitmap();
        if (image != null) {
            return CompletableFuture.completedFuture(image);
        }
        Uri imageUri = getAlbumArtUri();
        if (imageUri != null) {
            CompletableFuture<Bitmap> bitmapCompletableFuture = new CompletableFuture<>();
            RequestBuilder<Bitmap> builder = Glide.with(context)
                    .asBitmap()
                    .load(getAlbumArtUri());
            if (fit) {
                builder = builder.apply(RequestOptions.fitCenterTransform());
            } else {
                builder = builder.apply(RequestOptions.centerCropTransform());
            }
            Target<Bitmap> target = new SimpleTarget<Bitmap>(width, height) {
                @Override
                public void onResourceReady(@NonNull Bitmap bitmap,
                        @Nullable Transition<? super Bitmap> transition) {
                    bitmapCompletableFuture.complete(bitmap);
                }

                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    bitmapCompletableFuture.completeExceptionally(
                            new IllegalStateException("Unknown error"));
                }
            };
            builder.into(target);
            return bitmapCompletableFuture;
        }
        return CompletableFuture.completedFuture(null);
    }

    public boolean isBrowsable() {
        return mIsBrowsable;
    }

    /**
     * @return Content style hint for browsable items, if provided as an extra, or
     * 0 as default value if not provided.
     */
    public int getBrowsableContentStyleHint() {
        Bundle extras = mMediaDescription.getExtras();
        if (extras != null) {
            if (extras.containsKey(MediaConstants.CONTENT_STYLE_BROWSABLE_HINT)) {
                return extras.getInt(MediaConstants.CONTENT_STYLE_BROWSABLE_HINT, 0);
            } else if (extras.containsKey(MediaConstants.CONTENT_STYLE_BROWSABLE_HINT_PRERELEASE)) {
                return extras.getInt(MediaConstants.CONTENT_STYLE_BROWSABLE_HINT_PRERELEASE, 0);
            }
        }
        return 0;
    }

    public boolean isPlayable() {
        return mIsPlayable;
    }

    /**
     * @return Content style hint for playable items, if provided as an extra, or
     * 0 as default value if not provided.
     */
    public int getPlayableContentStyleHint() {
        Bundle extras = mMediaDescription.getExtras();
        if (extras != null) {

            if (extras.containsKey(MediaConstants.CONTENT_STYLE_PLAYABLE_HINT)) {
                return extras.getInt(MediaConstants.CONTENT_STYLE_PLAYABLE_HINT, 0);
            } else if (extras.containsKey(MediaConstants.CONTENT_STYLE_PLAYABLE_HINT_PRERELEASE)) {
                return extras.getInt(MediaConstants.CONTENT_STYLE_PLAYABLE_HINT_PRERELEASE, 0);
            }
        }
        return 0;
    }

    /**
     * @return Content style title group this item belongs to, or null if not provided
     */
    public String getTitleGrouping() {
        Bundle extras = mMediaDescription.getExtras();
        if (extras != null) {
            if (extras.containsKey(MediaConstants.CONTENT_STYLE_GROUP_TITLE_HINT)) {
                return extras.getString(MediaConstants.CONTENT_STYLE_GROUP_TITLE_HINT, null);
            } else if (extras.containsKey(
                    MediaConstants.CONTENT_STYLE_GROUP_TITLE_HINT_PRERELEASE)) {
                return extras.getString(MediaConstants.CONTENT_STYLE_GROUP_TITLE_HINT_PRERELEASE,
                        null);
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaItemMetadata that = (MediaItemMetadata) o;
        return mIsBrowsable == that.mIsBrowsable
                && mIsPlayable == that.mIsPlayable
                && Objects.equals(getId(), that.getId())
                && Objects.equals(getTitle(), that.getTitle())
                && Objects.equals(getSubtitle(), that.getSubtitle())
                && Objects.equals(getAlbumTitle(), that.getAlbumTitle())
                && Objects.equals(getArtist(), that.getArtist())
                && Objects.equals(getAlbumArtUri(), that.getAlbumArtUri())
                && Objects.equals(mQueueId, that.mQueueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mMediaDescription.getMediaId(), mQueueId, mIsBrowsable, mIsPlayable);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(mMediaDescription);
        if (mQueueId == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeLong(mQueueId);
        }
        dest.writeByte((byte) (mIsBrowsable ? 0x01 : 0x00));
        dest.writeByte((byte) (mIsPlayable ? 0x01 : 0x00));
        dest.writeString(mAlbumTitle);
        dest.writeString(mArtist);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<MediaItemMetadata> CREATOR =
            new Parcelable.Creator<MediaItemMetadata>() {
                @Override
                public MediaItemMetadata createFromParcel(Parcel in) {
                    return new MediaItemMetadata(in);
                }

                @Override
                public MediaItemMetadata[] newArray(int size) {
                    return new MediaItemMetadata[size];
                }
            };

    @Override
    public String toString() {
        return "[Id: "
                + (mMediaDescription != null ? mMediaDescription.getMediaId() : "-")
                + ", Queue Id: "
                + (mQueueId != null ? mQueueId : "-")
                + ", title: "
                + mMediaDescription != null ? mMediaDescription.getTitle().toString() : "-"
                + ", subtitle: "
                + mMediaDescription != null ? mMediaDescription.getSubtitle().toString() : "-"
                + ", album title: "
                + mAlbumTitle != null ? mAlbumTitle : "-"
                + ", artist: "
                + mArtist != null ? mArtist : "-"
                + ", album art URI: "
                + (mMediaDescription != null ? mMediaDescription.getIconUri() : "-")
                + "]";
    }
}
