/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.google.car.ui.sharedlibrary.toolbar;

import com.android.car.ui.sharedlibrary.oemapis.InsetsOEMV1;

import java.util.Objects;

class Insets implements InsetsOEMV1 {
    private final int mLeft;
    private final int mRight;
    private final int mTop;
    private final int mBottom;

    Insets() {
        mLeft = mRight = mTop = mBottom = 0;
    }

    Insets(int left, int top, int right, int bottom) {
        mLeft = left;
        mRight = right;
        mTop = top;
        mBottom = bottom;
    }

    @Override
    public int getLeft() {
        return mLeft;
    }

    @Override
    public int getRight() {
        return mRight;
    }

    @Override
    public int getTop() {
        return mTop;
    }

    @Override
    public int getBottom() {
        return mBottom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Insets insets = (Insets) o;
        return mLeft == insets.mLeft
                && mRight == insets.mRight
                && mTop == insets.mTop
                && mBottom == insets.mBottom;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mLeft, mRight, mTop, mBottom);
    }
}
