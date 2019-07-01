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

import java.util.ArrayList;
import java.util.List;

/**
 * Contains a list of conversations.
 */
public class ConversationContainer extends BaseEntity {

    private String mTitle;
    private List<Conversation> mConversations = new ArrayList<>();

    public static final Creator<ConversationContainer> CREATOR =
            new Creator<ConversationContainer>() {
                @Override
                public ConversationContainer createFromParcel(Parcel in) {
                    return new ConversationContainer(in);
                }

                @Override
                public ConversationContainer[] newArray(int size) {
                    return new ConversationContainer[size];
                }
            };

    protected ConversationContainer(Parcel in) {
        super(in);
        mTitle = in.readString();
        in.readList(mConversations, Conversation.class.getClassLoader());
    }

    protected ConversationContainer() {}

    public String getTitle() {
        return mTitle;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mTitle);
        dest.writeList(mConversations);
    }

    /**
     * Builder for {@link ConversationContainer}.
     */
    public static class Builder {
        private String mTitle;
        private List<Conversation> mConversations = new ArrayList<>();


        public void setTitle(String title) {
            mTitle = title;
        }

        /**
         * Adds a conversation to a conversation container.
         */
        public void addConversation(Conversation conversation) {
            mConversations.add(conversation);
        }

        /**
         * Builds {@link ConversationContainer}.
         */
        public ConversationContainer build() {
            ConversationContainer conversationContainer = new ConversationContainer();
            conversationContainer.mTitle = mTitle;
            conversationContainer.mConversations.addAll(mConversations);
            return conversationContainer;
        }
    }

}
