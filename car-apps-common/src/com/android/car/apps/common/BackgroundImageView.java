/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.car.apps.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

/**
 * A View to place a large, blurred image in the background.
 * Intended for Car's Dialer and Media apps.
 */
public class BackgroundImageView extends ConstraintLayout {

    private CrossfadeImageView mImageView;

    /** Configuration (controlled from resources) */
    private float mBackgroundBlurRadius;
    private float mBackgroundBlurScale;

    private float mBackgroundScale = 0;
    private int mBackgroundImageSize = 0;

    private Bitmap mBitmap;
    private View mDarkeningScrim;

    public BackgroundImageView(Context context) {
        this(context, null);
    }

    public BackgroundImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BackgroundImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        inflate(getContext(), R.layout.background_image, this);

        mImageView = findViewById(R.id.background_image_image);
        mDarkeningScrim = findViewById(R.id.background_image_darkening_scrim);

        mBackgroundScale = getResources().getFloat(R.dimen.background_image_scale);

        mBackgroundBlurRadius = getResources().getFloat(R.dimen.background_image_blur_radius);
        mBackgroundBlurScale = getResources().getFloat(R.dimen.background_image_blur_scale);

        addOnLayoutChangeListener(
                (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    int newBackgroundImageSize = Math.round(getHeight() * mBackgroundScale);

                    if (newBackgroundImageSize != mBackgroundImageSize) {
                        mBackgroundImageSize = newBackgroundImageSize;
                        setBackgroundImage(mBitmap, false);
                    }
                });
    }

    /**
     * Sets the image to display to a bitmap
     * @param bitmap The image to show. It will be scaled to the correct size and blurred.
     * @param showAnimation Whether or not to cross fade to the new image
     */
    public void setBackgroundImage(@Nullable Bitmap bitmap, boolean showAnimation) {
        // Save the bitmap so that we can reset it when we resize
        mBitmap = bitmap;

        if (bitmap == null) {
            mImageView.setImageBitmap(null, false);
            return;
        }

        if (mBackgroundImageSize == 0) {
            // We're not set up yet, wait for the OnLayoutChangeListener to set the correct size
            return;
        }

        // STOPSHIP(b130576879) Rework this to not be so wasteful
        // We need to scale it to a reasonable size, because if the image was small
        // our blur radius would be way to large, comparably.
        bitmap = Bitmap.createScaledBitmap(bitmap,
                mBackgroundImageSize,
                mBackgroundImageSize,
                false);

        if (bitmap != null) {
            bitmap = ImageUtils.blur(getContext(), bitmap, mBackgroundBlurScale,
                    mBackgroundBlurRadius);
        }

        if (bitmap == null) {
            showAnimation = false;
        }

        mImageView.setImageBitmap(bitmap, showAnimation);

        invalidate();
        requestLayout();
    }

    /**
     * Sets the image to display to a bitmap
     * @param bitmap The image to show. It will be scaled to the correct size and blurred.
     */
    public void setBackgroundImage(@Nullable Bitmap bitmap) {
        setBackgroundImage(bitmap, true);
    }

    /** Sets the background to a color */
    public void setBackgroundColor(int color) {
        mImageView.setBackgroundColor(color);
    }

    /**
     * Gets the desired size for an image to pass to {@link #setBackgroundImage(Bitmap, boolean)}
     * If the image doesn't match this size, it will be scaled to it.
     */
    public int getDesiredBackgroundSize() {
        return mBackgroundImageSize;
    }

    /** Dims/undims the background image by 30% */
    public void setDimmed(boolean dim) {
        mDarkeningScrim.setVisibility(dim ?  View.VISIBLE : View.GONE);
    }
}
