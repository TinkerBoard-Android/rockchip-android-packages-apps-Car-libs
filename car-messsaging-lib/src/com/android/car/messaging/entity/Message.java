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

/**
 * Represents an individual message.
 */
public class Message extends BaseEntity {

    private String mMessageBody;

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    protected Message(Parcel in) {
        super(in);
        mMessageBody = in.readString();
    }

    protected Message() {}

    /**
     * Returns the message body.
     */
    public String getMessageBody() {
        return mMessageBody;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mMessageBody);
    }

    /**
     * Builder for {@link Message}.
     */
    public static class Builder {
        private String mMessage;

        public void setMessage(String message) {
            mMessage = message;
        }

        /**
         * Builds a {@link Message}.
         */
        public Message build() {
            Message message = new Message();
            message.mMessageBody = mMessage;
            return message;
        }
    }
}
