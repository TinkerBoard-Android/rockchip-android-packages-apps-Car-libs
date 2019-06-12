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

package com.android.car.messaging.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Contains meta data of a conversation such as conversation title.
 */
public class ConversationMetaData implements Parcelable {
    private String mTitle;

    public static final Creator<ConversationMetaData> CREATOR =
            new Creator<ConversationMetaData>() {
                @Override
                public ConversationMetaData createFromParcel(Parcel in) {
                    return new ConversationMetaData(in);
                }

                @Override
                public ConversationMetaData[] newArray(int size) {
                    return new ConversationMetaData[size];
                }
            };

    protected ConversationMetaData(Parcel in) {
        mTitle = in.readString();
    }

    protected ConversationMetaData() {}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTitle);
    }

    public String getTitle() {
        return mTitle;
    }

    /**
     * Builder for {@link ConversationMetaData}.
     */
    public static class Builder {
        private String mTitle;

        public void setTitle(String title) {
            mTitle = title;
        }

        /**
         * Builds {@link ConversationContainer}.
         */
        public ConversationMetaData build() {
            ConversationMetaData conversationMetaData = new ConversationMetaData();
            conversationMetaData.mTitle = mTitle;
            return conversationMetaData;
        }
    }
}
