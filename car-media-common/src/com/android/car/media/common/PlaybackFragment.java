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

import android.car.Car;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.request.target.Target;

/**
 * {@link Fragment} that can be used to display and control the currently playing media item.
 * Its requires the android.Manifest.permission.MEDIA_CONTENT_CONTROL permission be held by the
 * hosting application.
 */
public class PlaybackFragment extends Fragment {
    private PlaybackModel mModel;
    private CrossfadeImageView mAlbumBackground;
    private PlaybackControls mPlaybackControls;
    private ImageView mAppIcon;
    private TextView mAppName;
    private TextView mTitle;
    private TextView mSubtitle;

    private PlaybackModel.PlaybackObserver mObserver = new PlaybackModel.PlaybackObserver() {
        @Override
        public void onSourceChanged() {
            updateMetadata();
        }

        @Override
        public void onMetadataChanged() {
            updateMetadata();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.car_playback_fragment, container, false);
        mModel = new PlaybackModel(getContext());
        mAlbumBackground = view.findViewById(R.id.album_background);
        mPlaybackControls = view.findViewById(R.id.playback_controls);
        mPlaybackControls.setModel(mModel);
        mAppIcon = view.findViewById(R.id.app_icon);
        mAppName = view.findViewById(R.id.app_name);
        mTitle = view.findViewById(R.id.title);
        mSubtitle = view.findViewById(R.id.subtitle);

        mAlbumBackground.setOnClickListener(v -> {
            Intent intent = new Intent(Car.CAR_INTENT_ACTION_MEDIA_TEMPLATE);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mModel.registerObserver(mObserver);
    }

    @Override
    public void onStop() {
        super.onStop();
        mModel.unregisterObserver(mObserver);
    }

    private void updateMetadata() {
        MediaSource mediaSource = mModel.getMediaSource();

        if (mediaSource == null) {
            mTitle.setText(null);
            mSubtitle.setText(null);
            mAppName.setText(null);
            mAlbumBackground.setImageBitmap(null, true);
            return;
        }

        MediaItemMetadata metadata = mModel.getMetadata();
        mTitle.setText(metadata != null ? metadata.getTitle() : null);
        mSubtitle.setText(metadata != null ? metadata.getSubtitle() : null);
        if (metadata != null) {
            metadata.getAlbumArt(getContext(),
                    Target.SIZE_ORIGINAL,
                    Target.SIZE_ORIGINAL,
                    false)
                    .thenAccept(bitmap -> {
                        //bitmap = ImageUtils.blur(getContext(), bitmap, 1f, 10f);
                        mAlbumBackground.setImageBitmap(bitmap, true);
                    });
        } else {
            mAlbumBackground.setImageBitmap(null, true);
        }
        mAppName.setText(mediaSource.getName());
        Bitmap cropped = getCircleCroppedBitmap(drawableToBitmap(mediaSource.getPackageIcon()));
        mAppIcon.setImageBitmap(cropped);
    }

    /**
     * Converts the given {@link Drawable} into a {@link Bitmap}
     */
    private Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * Crops the given bitmap into a circle with the same dimensions as the original one.
     */
    private Bitmap getCircleCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }
}
