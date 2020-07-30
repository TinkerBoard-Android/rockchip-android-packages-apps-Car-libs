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

package com.android.car.connecteddevice.connection;

import android.annotation.NonNull;
import android.annotation.Nullable;

import com.android.car.connecteddevice.StreamProtos.OperationProto;

/**
 * Abstract class which includes common logic of different types of {@link DeviceMessageStream}.
 */
public abstract class DeviceMessageStream {

    /**
     * Listener which will be notified when there is new {@link DeviceMessage} received.
     */
    private MessageReceivedListener mMessageReceivedListener;

    /**
     * Listener which will be notified when there is error parsing the received message.
     */
    private MessageReceivedErrorListener mMessageReceivedErrorListener;

    /**
     * Set the given listener to be notified when a new message was received from the client. If
     * listener is {@code null}, clear.
     */
    public final void setMessageReceivedListener(@Nullable MessageReceivedListener listener) {
        mMessageReceivedListener = listener;
    }

    /**
     * Set the given listener to be notified when there was an error during receiving message from
     * the client. If listener is {@code null}, clear.
     */
    public final void setMessageReceivedErrorListener(
            @Nullable MessageReceivedErrorListener listener) {
        mMessageReceivedErrorListener = listener;
    }

    /**
     * Notify the {@code mMessageReceivedListener} about the message received if it is  not {@code
     * null}.
     *
     * @param deviceMessage The message received.
     * @param operationType The operation type of the message.
     */
    protected final void notifyMessageReceivedListener(@NonNull DeviceMessage deviceMessage,
            OperationProto.OperationType operationType) {
        if (mMessageReceivedListener != null) {
            mMessageReceivedListener.onMessageReceived(deviceMessage, operationType);
        }
    }

    /**
     * Notify the {@code mMessageReceivedErrorListener} about the message received if it is  not
     * {@code null}.
     *
     * @param e The exception happened when parsing the received message.
     */
    protected final void notifyMessageReceivedErrorListener(Exception e) {
        if (mMessageReceivedErrorListener != null) {
            mMessageReceivedErrorListener.onMessageReceivedError(e);
        }
    }

    /**
     * Send {@link DeviceMessage} to remote connected devices.
     *
     * @param deviceMessage The message which need to be sent
     * @param operationType The operation type of current message
     */
    public abstract void writeMessage(@NonNull DeviceMessage deviceMessage,
            OperationProto.OperationType operationType);

    /**
     * Listener to be invoked when a complete message is received from the client.
     */
    public interface MessageReceivedListener {

        /**
         * Called when a complete message is received from the client.
         *
         * @param deviceMessage The message received from the client.
         * @param operationType The {@link OperationProto.OperationType} of the received message.
         */
        void onMessageReceived(@NonNull DeviceMessage deviceMessage,
                OperationProto.OperationType operationType);
    }

    /**
     * Listener to be invoked when there was an error during receiving message from the client.
     */
    public interface MessageReceivedErrorListener {
        /**
         * Called when there was an error during receiving message from the client.
         *
         * @param exception The error.
         */
        void onMessageReceivedError(@NonNull Exception exception);
    }
}
