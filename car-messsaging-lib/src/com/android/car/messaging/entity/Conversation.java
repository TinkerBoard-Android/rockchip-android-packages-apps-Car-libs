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
 * Represents a conversation.
 */
public class Conversation extends BaseEntity {
    private ConversationMetaData mConversationMetaData;
    private List<Action> mActions = new ArrayList<>();

    public static final Creator<Conversation> CREATOR = new Creator<Conversation>() {
        @Override
        public Conversation createFromParcel(Parcel in) {
            return new Conversation(in);
        }

        @Override
        public Conversation[] newArray(int size) {
            return new Conversation[size];
        }
    };

    protected Conversation(Parcel in) {
        super(in);
        mConversationMetaData = in.readParcelable(ConversationMetaData.class.getClassLoader());
        in.readList(mActions, /* classloader = */ Action.class.getClassLoader());
    }

    protected Conversation() {
        super();
    }

    public ConversationMetaData getConversationMetaData() {
        return mConversationMetaData;
    }

    public List<Action> getActions() {
        return mActions;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(mConversationMetaData);
        dest.writeList(mActions);
    }

    /**
     * Builder for {@link Conversation}.
     */
    public static class Builder {
        private ConversationMetaData mConversationMetaData;
        private List<Action> mActions = new ArrayList<>();

        /**
         * Add an Action to the Conversation.
         */
        public void addActions(Action action) {
            mActions.add(action);
        }

        /**
         * Builds {@link Conversation}.
         */
        public Conversation build() {
            Conversation conversation = new Conversation();
            conversation.mConversationMetaData = mConversationMetaData;
            conversation.mActions.addAll(mActions);
            return conversation;
        }

        public void setConversationMetaData(ConversationMetaData conversationMetaData) {
            mConversationMetaData = conversationMetaData;
        }
    }
}
