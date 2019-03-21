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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * A {@link ImageView} that scales in a similar way as {@link ScaleType#CENTER_CROP} but aligning
 * the image to the specified edge of the view.
 */
public class CropAlignedImageView extends ImageView {

    private static final int ALIGN_HORIZONTAL_CENTER = 0;
    private static final int ALIGN_HORIZONTAL_LEFT = 1;
    private static final int ALIGN_HORIZONTAL_RIGHT = 2;

    private int mAlignHorizontal;

    public CropAlignedImageView(Context context) {
        this(context, null);
    }

    public CropAlignedImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CropAlignedImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CropAlignedImageView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        TypedArray ta = context.obtainStyledAttributes(
                attrs, R.styleable.CrossfadeImageView, defStyleAttr, defStyleRes);
        mAlignHorizontal = ta.getInt(R.styleable.CrossfadeImageView_align_horizontal,
                ALIGN_HORIZONTAL_CENTER);
        ta.recycle();
        setScaleType(ScaleType.MATRIX);
    }

    @Override
    protected boolean setFrame(int frameLeft, int frameTop, int frameRight, int frameBottom) {
        if (getDrawable() != null) {
            setMatrix(frameRight - frameLeft, frameBottom - frameTop);
        }

        return super.setFrame(frameLeft, frameTop, frameRight, frameBottom);
    }

    private void setMatrix(int frameWidth, int frameHeight) {
        float originalImageWidth = (float) getDrawable().getIntrinsicWidth();
        float originalImageHeight = (float) getDrawable().getIntrinsicHeight();
        float fitHorizontallyScaleFactor = frameWidth / originalImageWidth;
        float fitVerticallyScaleFactor = frameHeight / originalImageHeight;
        float usedScaleFactor = Math.max(fitHorizontallyScaleFactor, fitVerticallyScaleFactor);
        float newImageWidth = originalImageWidth * usedScaleFactor;
        float newImageHeight = originalImageHeight * usedScaleFactor;
        Matrix matrix = getImageMatrix();
        matrix.setScale(usedScaleFactor, usedScaleFactor, 0, 0);
        float dx = 0;
        switch (mAlignHorizontal) {
            case ALIGN_HORIZONTAL_CENTER:
                dx = (frameWidth - newImageWidth) / 2;
                break;
            case ALIGN_HORIZONTAL_LEFT:
                dx = 0;
                break;
            case ALIGN_HORIZONTAL_RIGHT:
                dx = (frameWidth - newImageWidth);
                break;
        }
        matrix.postTranslate(dx, (frameHeight - newImageHeight) / 2);
        setImageMatrix(matrix);
    }
}
