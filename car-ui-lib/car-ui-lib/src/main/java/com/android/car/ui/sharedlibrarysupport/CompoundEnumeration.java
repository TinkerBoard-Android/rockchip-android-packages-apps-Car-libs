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
package com.android.car.ui.sharedlibrarysupport;

import androidx.annotation.RestrictTo;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * A copy of {@code libcore/ojluni/src/main/java/sun/misc/CompoundEnumeration.java}.
 *
 * @param <E> The type of element to enumerate.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class CompoundEnumeration<E> implements Enumeration<E> {
    private final Enumeration<E>[] mEnums;
    private int mIndex = 0;

    public CompoundEnumeration(Enumeration<E>[] enums) {
        this.mEnums = enums;
    }

    private boolean next() {
        while (mIndex < mEnums.length) {
            if (mEnums[mIndex] != null && mEnums[mIndex].hasMoreElements()) {
                return true;
            }
            mIndex++;
        }
        return false;
    }

    @Override
    public boolean hasMoreElements() {
        return next();
    }

    @Override
    public E nextElement() {
        if (!next()) {
            throw new NoSuchElementException();
        }
        return mEnums[mIndex].nextElement();
    }
}
