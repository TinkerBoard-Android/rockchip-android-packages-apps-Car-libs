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

package com.android.car.connecteddevice.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.car.encryptionrunner.Key
import android.os.ParcelUuid
import com.android.car.connecteddevice.storage.CarCompanionDeviceStorage
import com.android.car.connecteddevice.util.logd
import com.android.car.connecteddevice.util.loge
import java.util.UUID

private const val TAG = "CarBlePeripheralManager"

// Attribute protocol bytes attached to message. Available write size is MTU size minus att bytes.
private const val ATT_PROTOCOL_BYTES = 3
private val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

/**
 * Communication manager that allows for targeted connections to a specific device in the car.
 *
 * @param blePeripheralManager [BlePeripheralManager] for establishing connection.
 * @param carCompanionDeviceStorage Shared [CarCompanionDeviceStorage] for companion features.
 * @param writeCharacteristicUuid [UUID] of characteristic the car will write to.
 * @param readCharacteristicUuid [UUID] of characteristic the device will write to.
 */
internal class CarBlePeripheralManager(
    private val blePeripheralManager: BlePeripheralManager,
    carCompanionDeviceStorage: CarCompanionDeviceStorage,
    writeCharacteristicUuid: UUID,
    readCharacteristicUuid: UUID
) : CarBleManager(carCompanionDeviceStorage) {

    private val connectedDevice get() = connectedDevices.firstOrNull()

    private val writeCharacteristic by lazy(mode = LazyThreadSafetyMode.NONE) {
        BluetoothGattCharacteristic(
            writeCharacteristicUuid,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
    }

    private val readCharacteristic by lazy(mode = LazyThreadSafetyMode.NONE) {
        BluetoothGattCharacteristic(
            readCharacteristicUuid,
            BluetoothGattCharacteristic.PROPERTY_WRITE
                or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
    }

    private val descriptor by lazy(mode = LazyThreadSafetyMode.NONE) {
        BluetoothGattDescriptor(
            CLIENT_CHARACTERISTIC_CONFIG,
            BluetoothGattDescriptor.PERMISSION_READ or
                BluetoothGattDescriptor.PERMISSION_WRITE
        ).apply {
            value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        }
    }

    private val peripheralCallback by lazy(mode = LazyThreadSafetyMode.NONE) {
        object : BlePeripheralManager.Callback {
            override fun onDeviceNameRetrieved(deviceName: String?) {
                // Ignored
            }

            override fun onMtuSizeChanged(size: Int) {
                writeSize = size - ATT_PROTOCOL_BYTES
                connectedDevice?.secureChannel?.stream?.maxWriteSize = writeSize
            }

            override fun onRemoteDeviceConnected(device: BluetoothDevice) {
                blePeripheralManager.stopAdvertising(advertiseCallback)
                val secureStream = BleDeviceMessageStream(
                    blePeripheralManager, device,
                    writeCharacteristic, readCharacteristic
                ).apply {
                    maxWriteSize = writeSize
                }
                val secureChannel = SecureBleChannel(
                    secureStream,
                    carCompanionDeviceStorage,
                    secureChannelCallback
                )
                val bleDevice = BleDevice(device, gatt = null).apply {
                    this.secureChannel = secureChannel
                }
                addConnectedDevice(bleDevice)
            }

            override fun onRemoteDeviceDisconnected(device: BluetoothDevice) {
                val deviceId = getConnectedDevice(device)?.deviceId
                if (deviceId != null) {
                    callbacks.invoke { it.onDeviceDisconnected(deviceId) }
                }
            }
        }
    }

    private val secureChannelCallback by lazy(mode = LazyThreadSafetyMode.NONE) {
        object : SecureBleChannel.Callback {
            override fun onSecureChannelEstablished(encryptionKey: Key) {
                val deviceId = connectedDevice?.deviceId
                if (deviceId != null) {
                    callbacks.invoke { it.onSecureChannelEstablished(deviceId) }
                } else {
                    disconnectWithError("Null device id found when secure channel established.")
                }
            }

            override fun onMessageReceived(deviceMessage: DeviceMessage) {
                val deviceId = connectedDevice?.deviceId
                if (deviceId != null) {
                    callbacks.invoke { it.onMessageReceived(deviceId, deviceMessage) }
                } else {
                    disconnectWithError("Null device id found when message received.")
                }
            }

            override fun onMessageReceivedError(exception: Exception) {
                stop()
            }

            // TODO(b/143879960): Extend the callbacks from here to continue up the chain
            override fun onEstablishSecureChannelFailure(
                @SecureBleChannel.ChannelError error: Int
            ) {
                val deviceId = connectedDevice?.deviceId
                if (deviceId != null) {
                    callbacks.invoke { it.onSecureChannelError(deviceId) }
                } else {
                    disconnectWithError("Null device id found when secure channel failed to " +
                        "establish.")
                }
            }

            override fun onDeviceIdReceived(deviceId: String) {
                setDeviceId(deviceId)
            }
        }
    }

    // BLE default is 23, minus 3 bytes for ATT_PROTOCOL.
    private var writeSize = 20

    private lateinit var advertiseCallback: AdvertiseCallback

    override fun start() {
        super.start()
        blePeripheralManager.registerCallback(peripheralCallback)
    }

    override fun stop() {
        super.stop()
        blePeripheralManager.cleanup()
    }

    /** Connect to device with id of [deviceId]. */
    fun connectToDevice(deviceId: UUID) {
        val gattService = BluetoothGattService(deviceId, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        readCharacteristic.addDescriptor(descriptor)
        gattService.addCharacteristic(readCharacteristic)
        gattService.addCharacteristic(writeCharacteristic)
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(deviceId))
            .build()
        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                logd(TAG, "Successfully started advertising for device $deviceId.")
            }
        }
        blePeripheralManager.startAdvertising(gattService, advertiseData, advertiseCallback)
    }

    private fun setDeviceId(deviceId: String) {
        logd(TAG, "Setting device id: $deviceId")
        val device = connectedDevice
        if (device != null) {
            device.deviceId = deviceId
            callbacks.invoke { callback -> callback.onDeviceConnected(deviceId) }
        } else {
            disconnectWithError("Null connected device found when device id received.")
        }
    }

    private fun disconnectWithError(error: String) {
        loge(TAG, error)
        blePeripheralManager.cleanup()
    }
}
