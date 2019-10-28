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
package com.android.car.ui.drawable;

import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.ui.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * A drawable to renders a given text. It can be used as part of a compound
 * drawable as follows:
 *
 * <pre>
 * {@code
 * <layer-list xmlns:android="http://schemas.android.com/apk/res/android">
 *     ...
 *     <item
 *         <drawable
 *             class="com.android.car.ui.drawable.CarUiTextDrawable"
 *             app:text="Some text"
 *             app:textSize="30sp">
 *         </drawable>
 *     </item>
 *     ...
 * </layer-list>
 * }
 * </pre>
 *
 * <b>Important: This class is referenced by reflection from overlays. DO NOT
 * RENAME, REMOVE OR MOVE TO ANOTHER PACKAGE, otherwise the overlays would
 * cause the target application to crash.</b>
 */
public class CarUiTextDrawable extends Drawable {
    private final TextPaint mPaint = new TextPaint();
    private final Rect mTextBounds = new Rect(); // Minimum bounds of the text only.

    // Attributes initialized during inflation
    @Nullable
    private String mText;
    private Typeface mTypeface = Typeface.DEFAULT;
    private float mTextSize = 10;
    private int mTextColor = Color.WHITE;

    /** Text sample used to measure font height */
    private static final String TEXT_HEIGHT_SAMPLE = "Ag";

    /** Constructor available to include this drawable by code */
    public CarUiTextDrawable(@Nullable String text,
                             Typeface typeface,
                             float textSize,
                             int textColor) {
        mText = text;
        mTypeface = typeface;
        mTextSize = textSize;
        mTextColor = textColor;
        refreshTextBoundsAndInvalidate();
    }

    /** Constructor invoked by XML inflator */
    public CarUiTextDrawable() {
        // To be used during inflation.
    }

    /** Updates the text rendered by this drawable */
    public void setText(@Nullable String text) {
        this.mText = text;
        refreshTextBoundsAndInvalidate();
    }

    @Override
    public int getIntrinsicHeight() {
        return mTextBounds.height();
    }

    @Override
    public int getIntrinsicWidth() {
        return mTextBounds.width();
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty() || mPaint.getAlpha() == 0) {
            return;
        }

        // Draw text.
        if (mText != null) {
            canvas.drawText(mText,
                    bounds.centerX(),
                    bounds.centerY() + (mTextBounds.height() / 2.0f),
                    mPaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        final int old = mPaint.getAlpha();
        if (alpha != old) {
            mPaint.setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return mPaint.bgColor != 0 ? PixelFormat.OPAQUE : PixelFormat.TRANSPARENT;
    }

    /**
     * Call this to re-measure the text when properties that may affect the
     * textBounds are changed.
     */
    private void refreshTextBoundsAndInvalidate() {
        mPaint.setTypeface(mTypeface);
        mPaint.setTextSize(mTextSize);
        mPaint.setColor(mTextColor);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        calculateTextBounds(mPaint, mText, mTextBounds);
        invalidateSelf();
    }

    /**
     * Calculate the bounds of the given text.
     *
     * The textBounds are different from the drawable bounds: the textBounds is
     * is strictly a subset, and measures the minimum bounding box necessary to
     * draw the text. The left and right of the textBounds are used to calculate
     * the intrinsic width and intrinsic height.
     *
     * Text height is calculated based on a fixed sample text (to avoid height
     * changing every time a different text is rendered)
     */
    private static void calculateTextBounds(TextPaint paint, @Nullable String text,
                                            Rect outBounds) {
        outBounds.setEmpty();
        paint.getTextBounds(TEXT_HEIGHT_SAMPLE, 0, TEXT_HEIGHT_SAMPLE.length(), outBounds);

        if (text != null) {
            // Save the top and bottom bounds of the sample text.
            int top = outBounds.top;
            int bottom = outBounds.bottom;

            // Get the actual text bounds.
            paint.getTextBounds(text, 0, text.length(), outBounds);

            // Replace top and bottom with sample text bounds. We only care about left
            // and right.
            outBounds.top = top;
            outBounds.bottom = bottom;
        } else {
            outBounds.left = 0;
            outBounds.right = 0;
        }
    }

    @NonNull
    private static TypedArray themedObtainAttributes(@NonNull Resources res,
                                              @Nullable Resources.Theme theme,
                                              @NonNull AttributeSet set,
                                              @NonNull int[] attrs) {
        if (theme == null) {
            return res.obtainAttributes(set, attrs);
        }
        return theme.obtainStyledAttributes(set, attrs, 0, 0);
    }

    @Override
    public void inflate(@NonNull Resources r, @NonNull XmlPullParser parser,
                        @NonNull AttributeSet attrs, @Nullable Resources.Theme theme)
            throws XmlPullParserException, IOException {
        final TypedArray a = themedObtainAttributes(r, theme, attrs, R.styleable.CarUiTextDrawable);
        mText = a.getString(R.styleable.CarUiTextDrawable_renderedText);
        mTypeface = Typeface.create(a.getString(R.styleable.CarUiTextDrawable_typeface),
                Typeface.NORMAL);
        mTextSize = a.getDimension(R.styleable.CarUiTextDrawable_textSize, 10);
        mTextColor = a.getColor(R.styleable.CarUiTextDrawable_textColor,
                r.getColor(R.color.car_ui_primary_text_color, theme));
        a.recycle();
        refreshTextBoundsAndInvalidate();
        super.inflate(r, parser, attrs, theme);
    }
}
