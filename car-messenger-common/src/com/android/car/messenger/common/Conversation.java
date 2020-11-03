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

package com.android.car.messenger.common;

import android.app.PendingIntent;
import android.app.RemoteAction;
import android.app.RemoteInput;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.Person;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A generic conversation model that holds messaging conversation metadata.
 */
public final class Conversation implements Parcelable {
    @NonNull private final String mId;
    @NonNull private final Person mUser;
    @Nullable private final String mConversationTitle;
    @Nullable private final Icon mConversationIcon;
    @NonNull private final List<Message> mMessages;
    @NonNull private final List<Person> mParticipants;
    @Nullable private final List<ConversationAction> mActions;

    private final int mUnreadCount;
    private final boolean mIsMuted;
    @NonNull private final Bundle mExtras;

    public Conversation(
            @NonNull String id,
            @NonNull Person user,
            @Nullable String conversationTitle,
            @Nullable Icon conversationIcon,
            @NonNull List<Message> messages,
            @NonNull List<Person> participants,
            @Nullable List<ConversationAction> actions,
            int unreadCount,
            boolean isMuted,
            @Nullable Bundle extras
    ) {
        mId = id;
        mUser = user;
        mConversationTitle = conversationTitle;
        mConversationIcon = conversationIcon;
        mMessages = messages;
        mParticipants = participants;
        mActions = actions;
        mUnreadCount = unreadCount;
        mIsMuted = isMuted;
        mExtras = (extras == null) ? new Bundle() : extras;
    }

    public Conversation(Parcel in) {
        mId = in.readString();
        mUser = Person.fromBundle(in.readParcelable(Bundle.class.getClassLoader()));
        mConversationTitle = in.readString();
        mConversationIcon = in.readParcelable(Icon.class.getClassLoader());
        mMessages = in.createTypedArrayList(Message.CREATOR);
        mParticipants = in.createTypedArrayList(Bundle.CREATOR)
            .stream()
            .map(Person::fromBundle)
            .collect(Collectors.toList());
        mActions = in.createTypedArrayList(ConversationAction.CREATOR);
        mUnreadCount = in.readInt();
        mIsMuted = in.readByte() != 0;
        mExtras = in.readBundle();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeParcelable(mUser.toBundle(), flags);
        dest.writeString(mConversationTitle);
        dest.writeParcelable(mConversationIcon, flags);
        dest.writeTypedList(mMessages);
        dest.writeTypedList(mParticipants.stream()
                .map(Person::toBundle)
                .collect(Collectors.toList())
        );
        dest.writeTypedList(mActions);
        dest.writeInt(mUnreadCount);
        dest.writeByte((byte) (mIsMuted ? 1 : 0));
        dest.writeBundle(mExtras);
    }

    @Override
    public int describeContents() {
        return 0;
    }

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

    /**
     * Gets the unique identifier for this conversation
     */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Gets the current user for this conversation.
     *
     * The user receives inbound messages and sends outbound messages from the messaging
     * app/device.
     */
    @NonNull
    public Person getUser() {
        return mUser;
    }

    /**
     * Gets conversation title.
     *
     * @return null if conversation title is not set, or {@link String} if set
     */
    @Nullable
    public String getConversationTitle() {
        return mConversationTitle;
    }

    /**
     * Gets conversation icon.
     *
     * @return null if conversation icon is not set, or {@link Icon} if set
     */
    @Nullable
    public Icon getConversationIcon() {
        return mConversationIcon;
    }

    /**
     * Gets the list of messages conveyed by this conversation.
     *
     */
    @NonNull
    public List<Message> getMessages() {
        return mMessages;
    }

    /**
     * Gets the participants in the conversation, excluding the user
     *
     * @return list of participants or empty if not set
     */
    @NonNull
    public List<Person> getParticipants() {
        return mParticipants;
    }

    /**
     * Gets the actions in the conversation.
     */
    @Nullable
    public List<ConversationAction> getActions() {
        return mActions;
    }

    /**
     * Get the number of unread messages
     *
     * @return 0 if no unread messages, or a positive number when there are unread messages.
     */
    public int getUnreadCount() {
        return mUnreadCount;
    }

    /**
     * Gets if the conversation should be muted based on the user's preference.
     * This is useful when posting a notification.
     */
    public boolean isMuted() {
        return mIsMuted;
    }

    /**
     * Gets the extras for the conversation.
     * This can be used to hold additional data on the conversation.
     */
    @NonNull
    public Bundle getExtras() {
        return mExtras;
    }

    /** Creates and returns a new {@link Conversation.Builder}
     * initialized with this Conversation's data
     */
    @NonNull
    public Conversation.Builder toBuilder() {
        return new Conversation.Builder(this);
    }

    /**
     * Builder class for {@link Conversation} objects.
     */
    public static class Builder {
        @NonNull private final String mId;
        @NonNull private final Person mUser;
        @Nullable private String mConversationTitle;
        @Nullable private Icon mConversationIcon;
        @NonNull private List<Message> mMessages;
        @NonNull private List<Person> mParticipants;
        @Nullable private List<ConversationAction> mActions;
        private int mUnreadCount;
        private boolean mIsMuted;
        @NonNull private Bundle mExtras;

        /**
         * Constructs a new builder from a {@link Conversation}?
         * @param conversation the conversation containing the data to initialize builder with
         */
        private Builder(@NonNull Conversation conversation) {
            mUser = conversation.mUser;
            mId = conversation.mId;
            mMessages = conversation.mMessages;
            mConversationTitle = conversation.getConversationTitle();
            mConversationIcon = conversation.getConversationIcon();
            mParticipants = conversation.getParticipants();
            mActions = conversation.getActions();
            mUnreadCount = conversation.getUnreadCount();
            mIsMuted = conversation.isMuted();
            mExtras = conversation.getExtras();
        }

        /**
         * Constructs a new builder for {@link Conversation}.
         *
         * @param conversationId the unique identifier of this conversation.
         * @param user           The name of the other participant in the conversation.
         */
        public Builder(
                @NonNull Person user,
                @NonNull String conversationId
        ) {
            mUser = user;
            mId = conversationId;
            mMessages = new ArrayList<>();
            mParticipants = new ArrayList<>();
            mExtras = new Bundle();
        }

        /**
         * Sets list of messages for this conversation.
         *
         * @param messages List of messages
         * @return This object for method chaining.
         */
        @NonNull
        public Builder setMessages(@NonNull List<Message> messages) {
            mMessages = messages;
            return this;
        }

        /**
         * Sets list of participants for this conversation
         *
         * @return This object for method chaining.
         */
        @NonNull
        public Builder setParticipants(@NonNull List<Person> participants) {
            mParticipants = participants;
            return this;
        }

        /**
         * Sets list of messages for this conversation.
         *
         * @param actions list of actions
         * @return This object for method chaining.
         */
        @NonNull
        public Builder setActions(@NonNull List<ConversationAction> actions) {
            mActions = actions;
            return this;
        }

        /**
         * Sets conversation title which would be used when displaying the name of the conversation.
         *
         * This could be the sender name for a 1-1 message, or an appended string of participants
         * name for a group message, or a nickname for the group.
         *
         * @param conversationTitle the title to refer to the conversation
         * @return This object for method chaining
         */
        @NonNull
        public Builder setConversationTitle(@Nullable String conversationTitle) {
            mConversationTitle = conversationTitle;
            return this;
        }

        /**
         * Sets conversation icon for display purposes
         *
         * @param conversationIcon This could be the sender icon for a 1-1 message or a collage
         *                         of participants' icons for a group message, or an icon the user
         *                         uploaded as the
         *                         conversation icon.
         * @return This object for method chaining.
         */
        @NonNull
        public Builder setConversationIcon(@Nullable Icon conversationIcon) {
            mConversationIcon = conversationIcon;
            return this;
        }

        /**
         * Sets the number of unread message, default is 0
         *
         * @return This object for method chaining.
         */
        @NonNull
        public Builder setUnreadCount(int unreadCount) {
            mUnreadCount = unreadCount;
            return this;
        }

        /**
         * Sets if this conversation should be muted per user's request
         * A muted conversation may not post notifications for instance.
         *
         * @return This object for method chaining.
         */
        @NonNull
        public Builder setMuted(boolean muted) {
            mIsMuted = muted;
            return this;
        }

        /**
         * Sets bundle extra for the conversation
         *
         * @param extras this could contain additional data on the conversation
         * @return This object for method chaining.
         */
        @NonNull
        public Builder setExtras(@NonNull Bundle extras) {
            mExtras = extras;
            return this;
        }

        /**
         * Builds a new unread conversation object.
         *
         * @return The new unread conversation object.
         */
        @NonNull
        public Conversation build() {
            return new Conversation(
                mId,
                mUser,
                mConversationTitle,
                mConversationIcon,
                mMessages,
                mParticipants,
                mActions,
                mUnreadCount,
                mIsMuted,
                mExtras
            );
        }
    }

    /**
     * A class representing the metadata for a conversation message.
     */
    public static final class Message implements Parcelable {
        @NonNull private final String mText;
        private final long mTimestamp;
        @Nullable private final Person mPerson;
        @MessageStatus private int mMessageStatus;
        @MessageType private int mMessageType;

        @NonNull private Bundle mExtras = new Bundle();
        @Nullable private String mDataMimeType;
        @Nullable private Uri mDataUri;

        /**
         * Creates a new {@link Message} with the given text, timestamp, and sender.
         *
         * @param text      A {@link String} to be displayed as the message content
         * @param timestamp Time at which the message arrived in ms since Unix epoch
         * @param person    A {@link Person} whose {@link Person#getName()} value is used as the
         *                  display name for the sender. For messages by
         *                  the current user, this should be identical to
         *                  {@link Conversation#getUser()}
         */
        public Message(@NonNull String text, long timestamp, @Nullable Person person) {
            mText = text;
            mTimestamp = timestamp;
            mPerson = person;
        }

        public Message(Parcel in) {
            mText = in.readString();
            mTimestamp = in.readLong();
            Bundle mPersonBundle = in.readParcelable(Bundle.class.getClassLoader());
            mPerson = mPersonBundle == null ? null : Person.fromBundle(mPersonBundle);
            mMessageStatus = in.readInt();
            mMessageType = in.readInt();
            mExtras = in.readParcelable(Bundle.class.getClassLoader());
            mDataMimeType = in.readString();
            mDataUri = in.readParcelable(Uri.class.getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mText);
            dest.writeLong(mTimestamp);
            dest.writeParcelable(mPerson == null ? null : mPerson.toBundle(), flags);
            dest.writeInt(mMessageStatus);
            dest.writeInt(mMessageType);
            dest.writeBundle(mExtras);
            dest.writeString(mDataMimeType);
            dest.writeParcelable(mDataUri, flags);
        }

        @Override
        public int describeContents() {
            return 0;
        }

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

        /**
         * Sets a binary blob of data and an associated MIME type for a message. In the case
         * where the platform doesn't support the MIME type, the original text provided in the
         * constructor will be used.
         *
         * @param dataMimeType The MIME type of the content.
         * @param dataUri      The uri containing the content whose type is given by the MIME type.
         * @return this object for method chaining
         */
        @NonNull
        public Message setData(@NonNull String dataMimeType, @NonNull Uri dataUri) {
            mDataMimeType = dataMimeType;
            mDataUri = dataUri;
            return this;
        }

        /** Sets the message status for a message. */
        @NonNull
        public Message setMessageStatus(@MessageStatus int messageStatus) {
            mMessageStatus = messageStatus;
            return this;
        }

        /**
         * Sets the message type for a message.
         *
         * @return this object for method chaining
         */
        @NonNull
        public Message setMessageType(@MessageType int messageType) {
            mMessageType = messageType;
            return this;
        }

        /**
         * Get the text to be used for this message, or the fallback text if a type and content
         * Uri have been set
         */
        @NonNull
        public String getText() {
            return mText;
        }

        /** Get the time at which this message arrived in ms since Unix epoch. */
        public long getTimestamp() {
            return mTimestamp;
        }

        /** Gets the message status for this message */
        @MessageStatus
        public int getMessageStatus() {
            return mMessageStatus;
        }

        /** Get the message type for this message */
        @MessageType
        public int getMessageType() {
            return mMessageType;
        }

        /** Get the extras Bundle for this message. */
        @NonNull
        public Bundle getExtras() {
            return mExtras;
        }

        /** Returns the {@link Person} sender of this message. */
        @Nullable
        public Person getPerson() {
            return mPerson;
        }

        /** Get the MIME type of the data pointed to by the URI. */
        @Nullable
        public String getDataMimeType() {
            return mDataMimeType;
        }

        /**
         * Get the Uri pointing to the content of the message. Can be null, in which case
         * {@see #getText()} is used.
         */
        @Nullable
        public Uri getDataUri() {
            return mDataUri;
        }

        /**
         * Indicates the message status of the message.
         */
        @IntDef(value = {
            MessageStatus.MESSAGE_STATUS_NONE,
            MessageStatus.MESSAGE_STATUS_UNREAD,
            MessageStatus.MESSAGE_STATUS_SEEN,
            MessageStatus.MESSAGE_STATUS_READ,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface MessageStatus {
            /**
             * {@code MessageStatus}: No message status defined.
             */
            int MESSAGE_STATUS_NONE = 0;

            /**
             * {@code MessageStatus}: Message is unread
             */
            int MESSAGE_STATUS_UNREAD = 1;

            /**
             * {@code MessageStatus}: Message is seen; The "seen" flag determines
             * whether we need to show a notification.
             */
            int MESSAGE_STATUS_SEEN = 2;

            /**
             * {@code MessageStatus}: Message is read
             */
            int MESSAGE_STATUS_READ = 3;

        }

        /**
         * Indicates the message status of the message.
         */
        @IntDef(value = {
            MessageType.MESSAGE_TYPE_ALL,
            MessageType.MESSAGE_TYPE_INBOX,
            MessageType.MESSAGE_TYPE_SENT,
            MessageType.MESSAGE_TYPE_DRAFT,
            MessageType.MESSAGE_TYPE_FAILED,
            MessageType.MESSAGE_TYPE_OUTBOX,
            MessageType.MESSAGE_TYPE_QUEUED
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface MessageType {

            /** Message type: all messages. */
            int MESSAGE_TYPE_ALL = 0;

            /** Message type: inbox. */
            int MESSAGE_TYPE_INBOX = 1;

            /** Message type: sent messages. */
            int MESSAGE_TYPE_SENT = 2;

            /** Message type: drafts. */
            int MESSAGE_TYPE_DRAFT = 3;

            /** Message type: outbox. */
            int MESSAGE_TYPE_OUTBOX = 4;

            /** Message type: failed outgoing message. */
            int MESSAGE_TYPE_FAILED = 5;

            /** Message type: queued to send later. */
            int MESSAGE_TYPE_QUEUED = 6;
        }
    }

    /**
     * {@link ConversationAction} provides the actions for this conversation.
     * The semantic action indicates the type of action represented.
     * The remote action holds the pending intent to be fired
     * The remote input holds a key by which responses can be filled into.
     */
    public static class ConversationAction implements Parcelable {
        @ActionType private final int mActionType;
        @NonNull private final RemoteAction mRemoteAction;
        @Nullable private final RemoteInput mRemoteInput;
        @NonNull private final Bundle mExtras = new Bundle();

        public ConversationAction(
                @ActionType int actionType,
                @NonNull RemoteAction remoteAction,
                @Nullable RemoteInput remoteInput) {
            mActionType = actionType;
            mRemoteAction = remoteAction;
            mRemoteInput = remoteInput;
        }

        protected ConversationAction(Parcel in) {
            mActionType = in.readInt();
            mRemoteAction = in.readParcelable(RemoteAction.class.getClassLoader());
            mRemoteInput = in.readParcelable(RemoteInput.class.getClassLoader());
            mExtras.putAll(in.readBundle());
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mActionType);
            dest.writeParcelable(mRemoteAction, flags);
            dest.writeParcelable(mRemoteInput, flags);
            dest.writeBundle(mExtras);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<ConversationAction> CREATOR =
                new Creator<ConversationAction>() {
                    @Override
                    public ConversationAction createFromParcel(Parcel in) {
                        return new ConversationAction(in);
                    }

                    @Override
                    public ConversationAction[] newArray(int size) {
                        return new ConversationAction[size];
                    }
                };

        @ActionType
        public int getActionType() {
            return mActionType;
        }

        @NonNull
        public RemoteAction getRemoteAction() {
            return mRemoteAction;
        }

        @Nullable
        public RemoteInput getRemoteInput() {
            return mRemoteInput;
        }

        @NonNull
        public Bundle getExtras() {
            return mExtras;
        }

        /**
         * Provides meaning to an {@link ConversationAction} that hints at what the associated
         * {@link PendingIntent} will do. For example, an {@link ConversationAction} with a
         * {@link PendingIntent} that replies to a text message may have the
         * {@link #ACTION_TYPE_REPLY} {@code ActionType} set within it.
         */
        @IntDef(value = {
            ActionType.ACTION_TYPE_NONE,
            ActionType.ACTION_TYPE_REPLY,
            ActionType.ACTION_TYPE_MARK_AS_READ,
            ActionType.ACTION_TYPE_MARK_AS_UNREAD,
            ActionType.ACTION_TYPE_DELETE,
            ActionType.ACTION_TYPE_ARCHIVE,
            ActionType.ACTION_TYPE_MUTE,
            ActionType.ACTION_TYPE_UNMUTE
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface ActionType {

            /**
             * {@code ActionType}: No semantic action defined.
             */
            int ACTION_TYPE_NONE = 0;

            /**
             * {@code ActionType}: Reply to a conversation, chat, group, or wherever replies
             * may be appropriate.
             */
            int ACTION_TYPE_REPLY = 1;

            /**
             * {@code ActionType}: Mark content as read.
             */
            int ACTION_TYPE_MARK_AS_READ = 2;

            /**
             * {@code ActionType}: Mark content as unread.
             */
            int ACTION_TYPE_MARK_AS_UNREAD = 3;

            /**
             * {@code ActionType}: Delete the content associated with the conversation.
             */
            int ACTION_TYPE_DELETE = 4;

            /**
             * {@code ActionType}: Archive the content associated with the conversation. This
             * could mean hiding a conversation, or placing it in an archived list.
             */
            int ACTION_TYPE_ARCHIVE = 5;

            /**
             * {@code ActionType}: Mute the content associated with the conversation. This could
             * mean silencing a conversation or currently playing media.
             */
            int ACTION_TYPE_MUTE = 6;

            /**
             * {@code ActionType}: Unmute the content associated with the conversation.
             * This could mean un-silencing a conversation or currently playing media.
             */
            int ACTION_TYPE_UNMUTE = 7;
        }
    }
}
