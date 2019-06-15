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

import android.content.pm.PackageManager;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Contains configuration of {@link MessagingService} connection.
 */
public class ConnectionConfig implements Parcelable {
    private int mApiVersion;
    private String mPackageName;

    private ConnectionConfig() {}

    protected ConnectionConfig(Parcel in) {
        mApiVersion = in.readInt();
        mPackageName = in.readString();
    }

    public static final Creator<ConnectionConfig> CREATOR = new Creator<ConnectionConfig>() {
        @Override
        public ConnectionConfig createFromParcel(Parcel in) {
            return new ConnectionConfig(in);
        }

        @Override
        public ConnectionConfig[] newArray(int size) {
            return new ConnectionConfig[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mApiVersion);
        dest.writeString(mPackageName);
    }

    /**
     * Returns the Api version for the current connection.
     */
    public int getApiVersion() {
        return mApiVersion;
    }

    /**
     * Returns the package name of the caller.
     */
    public String getPackageName() {
        return mPackageName;
    }

    /**
     * Builds a {@link ConnectionConfig}.
     */
    public static class Builder {
        private int mApiVersion;
        private String mPackageName;

        /**
         * Sets the requested api version for the connection.
         */
        public void setApiVersion(int apiVersion) {
            mApiVersion = apiVersion;
        }

        /**
         * Sets the package name of the caller. Must be one of the return value of
         * {@link PackageManager#getPackagesForUid} for the caller's uid.
         */
        public void setPackageName(String packageName) {
            mPackageName = packageName;
        }

        /**
         * Builds a {#link ConnectionConfig}.
         */
        public ConnectionConfig build() {
            ConnectionConfig connectionConfig = new ConnectionConfig();
            connectionConfig.mApiVersion = mApiVersion;
            connectionConfig.mPackageName = mPackageName;
            return connectionConfig;
        }
    }
}
