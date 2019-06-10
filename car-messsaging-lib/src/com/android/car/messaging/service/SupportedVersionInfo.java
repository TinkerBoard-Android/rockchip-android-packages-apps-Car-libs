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

package com.android.car.messaging.service;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Contains the max and min supported api version.
 */
public final class SupportedVersionInfo implements Parcelable {
    private int mMinVersion;
    private int mMaxVersion;

    public SupportedVersionInfo(int minVersion, int maxVersion) {
        mMinVersion = minVersion;
        mMaxVersion = maxVersion;
    }

    protected SupportedVersionInfo(Parcel in) {
        mMinVersion = in.readInt();
        mMaxVersion = in.readInt();
    }

    public static final Creator<SupportedVersionInfo> CREATOR =
            new Creator<SupportedVersionInfo>() {
                @Override
                public SupportedVersionInfo createFromParcel(Parcel in) {
                    return new SupportedVersionInfo(in);
                }

                @Override
                public SupportedVersionInfo[] newArray(int size) {
                    return new SupportedVersionInfo[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mMinVersion);
        dest.writeInt(mMaxVersion);
    }

    public int getMinVersion() {
        return mMinVersion;
    }

    public int getMaxVersion() {
        return mMaxVersion;
    }
}
