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

package com.android.car.ui;

import java.util.Objects;

/**
 * A representation of the insets into the content view that the user-accessible
 * content should have.
 *
 * See {@link CarUiAppCompatActivity#onCarUiInsetsChanged(Insets)} for more information.
 */
public final class Insets {
    /* package */ int mLeft = 0;
    /* package */ int mRight = 0;
    /* package */ int mTop = 0;
    /* package */ int mBottom = 0;

    /* package */ Insets() {}

    public int getLeft() {
        return mLeft;
    }

    public int getRight() {
        return mRight;
    }

    public int getTop() {
        return mTop;
    }

    public int getBottom() {
        return mBottom;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (other.getClass() != Insets.class) {
            return false;
        }

        Insets otherInsets = (Insets) other;

        return mLeft == otherInsets.getLeft()
                && mRight == otherInsets.getRight()
                && mTop == otherInsets.getTop()
                && mBottom == otherInsets.getBottom();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLeft(), getRight(), getTop(), getBottom());
    }

    @Override
    public String toString() {
        return "{ left: " + mLeft + ", right: " + mRight
                + ", top: " + mTop + ", bottom: " + mBottom + " }";
    }
}
