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

package com.android.car.connecteddevice.connection.spp;

import static com.android.car.connecteddevice.util.SafeLog.logd;
import static com.android.car.connecteddevice.util.SafeLog.loge;

import android.annotation.Nullable;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;

import com.android.car.connecteddevice.R;

import java.io.IOException;
import java.util.UUID;

/**
 * This task runs while listening for incoming connections. It behaves like a server-side client. It
 * runs until a connection is accepted (or until cancelled).
 */
public class AcceptTask implements Runnable {
    private static final String TAG = "AcceptTask";
    // Service names and UUIDs of SDP(Service Discovery Protocol) record, need to keep it consistent
    // among client and server.
    private static final String SERVICE_NAME_SECURE = "NAME_SECURE";
    private static final String SERVICE_NAME_INSECURE = "NAME_INSECURE";
    private final UUID mServiceUuidSecure;
    private final UUID mServiceUuidInsecure;
    private final boolean mIsSecure;
    private final OnTaskCompletedListener mListener;
    private final BluetoothAdapter mAdapter;
    private BluetoothServerSocket mServerSocket;

    AcceptTask(Context context, BluetoothAdapter adapter, boolean isSecure,
            OnTaskCompletedListener listener) {
        mIsSecure = isSecure;
        mListener = listener;
        mAdapter = adapter;
        mServiceUuidSecure = UUID.fromString(
                context.getString(R.string.car_spp_service_uuid_secure));
        mServiceUuidInsecure = UUID.fromString(
                context.getString(R.string.car_spp_service_uuid_insecure));
    }

    /**
     * Start the socket to listen to any incoming connection request.
     *
     * @return {@code true} if listening is started successfully.
     */
    boolean startListening() {
        // Create a new listening server socket
        try {
            if (mIsSecure) {
                mServerSocket = mAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME_SECURE,
                        mServiceUuidSecure);
            } else {
                mServerSocket = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        SERVICE_NAME_INSECURE, mServiceUuidInsecure);
            }
        } catch (IOException e) {
            loge(TAG, "Socket listen() failed", e);
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        logd(TAG, "BEGIN AcceptTask: " + this);
        BluetoothSocket socket = null;

        // Listen to the server socket if we're not connected
        while (true) {
            try {
                socket = mServerSocket.accept();
            } catch (IOException e) {
                loge(TAG, "accept() failed", e);
                break;
            }
            if (socket != null) {
                break;
            }
        }

        mListener.onTaskCompleted(socket, mIsSecure);
    }

    void cancel() {
        logd(TAG, "CANCEL AcceptTask: " + this);
        try {
            if (mServerSocket != null) {
                mServerSocket.close();
            }
        } catch (IOException e) {
            loge(TAG, "close() of server failed", e);
        }
    }

    interface OnTaskCompletedListener {
        /**
         * Will be called when the accept task is completed.
         *
         * @param socket   will be {@code null} if the task failed.
         * @param isSecure is {@code true} when it is listening to a secure RFCOMM channel.
         */
        void onTaskCompleted(@Nullable BluetoothSocket socket, boolean isSecure);
    }
}
