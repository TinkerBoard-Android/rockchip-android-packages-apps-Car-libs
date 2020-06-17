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

package com.android.car.connecteddevice.oob;

import static com.android.car.connecteddevice.util.SafeLog.loge;

import android.annotation.NonNull;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.android.car.connecteddevice.model.AssociatedDevice;

import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.UUID;

/**
 * Handles out of band data exchange over a secure RFCOMM channel.
 */
public class BluetoothRfcommChannel implements OobChannel {
    private static final String TAG = "BluetoothRfcommChannel";
    private static final UUID RFCOMM_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket mBluetoothSocket;
    @VisibleForTesting
    Callback mCallback;

    @Override
    public void completeOobDataExchange(@NonNull AssociatedDevice device,
            @NonNull Callback callback) {
        completeOobDataExchange(device, callback, BluetoothAdapter.getDefaultAdapter());
    }

    @VisibleForTesting
    void completeOobDataExchange(AssociatedDevice device, Callback callback,
            BluetoothAdapter bluetoothAdapter) {
        mCallback = callback;

        BluetoothDevice remoteDevice = bluetoothAdapter.getRemoteDevice(device.getDeviceAddress());

        try {
            mBluetoothSocket = remoteDevice.createRfcommSocketToServiceRecord(RFCOMM_UUID);
        } catch (IOException e) {
            loge(TAG, "Rfcomm socket creation with " + remoteDevice.getName() + " failed", e);
            mCallback.onOobExchangeFailure();
            return;
        }

        bluetoothAdapter.cancelDiscovery();

        try {
            mBluetoothSocket.connect();
        } catch (IOException e) {
            loge(TAG, "Socket connection failed", e);
            mCallback.onOobExchangeFailure();
            return;
        }

        callback.onOobExchangeSuccess(OobConnectionManager.forClient(this));
    }

    @Override
    public void sendOobData(byte[] oobData) {
        if (mBluetoothSocket == null) {
            loge(TAG, "Bluetooth socket is null, oob data cannot be sent");
            mCallback.onOobExchangeFailure();
            return;
        }
        try {
            mBluetoothSocket.getOutputStream().write(oobData);
        } catch (IOException e) {
            loge(TAG, "Sending oob data failed", e);
            if (mCallback != null) {
                mCallback.onOobExchangeFailure();
            }
        }
    }
}
