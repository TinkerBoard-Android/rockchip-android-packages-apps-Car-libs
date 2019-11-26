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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.car.encryptionrunner.Key
import android.os.ParcelUuid
import com.android.car.connecteddevice.AssociationCallback
import com.android.car.connecteddevice.model.AssociatedDevice
import com.android.car.connecteddevice.storage.CarCompanionDeviceStorage
import com.android.car.connecteddevice.util.logd
import com.android.car.connecteddevice.util.loge
import com.android.internal.annotations.VisibleForTesting
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val TAG = "CarBlePeripheralManager"

// Attribute protocol bytes attached to message. Available write size is MTU size minus att bytes.
private const val ATT_PROTOCOL_BYTES = 3
// Arbitrary delay time for a retry of association advertising if bluetooth adapter name change
// fails.
private const val ASSOCIATE_ADVERTISING_DELAY_MS = 10L
private val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

/**
 * Communication manager that allows for targeted connections to a specific device in the car.
 *
 * @param blePeripheralManager [BlePeripheralManager] for establishing connection.
 * @param carCompanionDeviceStorage Shared [CarCompanionDeviceStorage] for companion features.
 * @param associationServiceUuid [UUID] of association service
 * @param writeCharacteristicUuid [UUID] of characteristic the car will write to.
 * @param readCharacteristicUuid [UUID] of characteristic the device will write to.
 */
internal class CarBlePeripheralManager(
    private val blePeripheralManager: BlePeripheralManager,
    carCompanionDeviceStorage: CarCompanionDeviceStorage,
    private val associationServiceUuid: UUID,
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

    // TODO(b/144506791): Move the callback definitions to the end of the class.
    private val reconnectPeripheralCallback by lazy(mode = LazyThreadSafetyMode.NONE) {
        object : BlePeripheralManager.Callback {
            override fun onDeviceNameRetrieved(deviceName: String?) {
                // Ignored
            }

            override fun onMtuSizeChanged(size: Int) {
                setMtuSize(size)
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
                    carCompanionDeviceStorage
                ).apply {
                    channelCallback = secureChannelCallback
                }
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
                reset()
            }
        }
    }

    private val associatePeripheralCallback by lazy(mode = LazyThreadSafetyMode.NONE) {
        object : BlePeripheralManager.Callback {
            override fun onDeviceNameRetrieved(deviceName: String?) {
                clientDeviceName = deviceName
            }

            override fun onMtuSizeChanged(size: Int) {
                setMtuSize(size)
            }

            override fun onRemoteDeviceConnected(device: BluetoothDevice) {
                resetBluetoothAdapterName()
                blePeripheralManager.stopAdvertising(advertiseCallback)
                clientDeviceAddress = device.address
                clientDeviceName = device.name
                if (clientDeviceName == null) {
                    logd(TAG, "Device connected, but name is null; issuing request to " +
                        "retrieve device name.")
                    blePeripheralManager.retrieveDeviceName(device)
                }
                val secureStream = BleDeviceMessageStream(
                    blePeripheralManager, device,
                    writeCharacteristic, readCharacteristic
                ).apply {
                    maxWriteSize = writeSize
                }
                val secureChannel = SecureBleChannel(
                    secureStream,
                    carCompanionDeviceStorage,
                    isReconnect = false
                )
                secureChannel.channelCallback = secureChannelCallback
                secureChannel.showVerificationCodeListener =
                    object : SecureBleChannel.ShowVerificationCodeListener {
                        override fun showVerificationCode(code: String) {
                            if (associationCallback == null) {
                                loge(TAG, "No valid callback for association.")
                            }
                            associationCallback?.onVerificationCodeAvailable(code)
                        }
                    }
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
                reset()
            }
        }
    }

    private val secureChannelCallback by lazy(mode = LazyThreadSafetyMode.NONE) {
        object : SecureBleChannel.Callback {
            override fun onSecureChannelEstablished(encryptionKey: Key) {
                val deviceId = connectedDevice?.deviceId
                if (deviceId == null) {
                    disconnectWithError("Null device id found when secure channel established.")
                    return
                }
                val address = clientDeviceAddress
                if (address == null) {
                    disconnectWithError(
                        "Null device address found when secure channel established.")
                    return
                }
                callbacks.invoke { it.onSecureChannelEstablished(deviceId) }
                if (!isAssociating) {
                    return
                }
                logd(TAG, "Secure channel established for un-associated device. " +
                    "Saving association of that device for current user.")
                carCompanionDeviceStorage.addAssociatedDeviceForActiveUser(
                    AssociatedDevice(deviceId, address, clientDeviceName)
                )
                associationCallback?.onAssociationCompleted()
            }

            override fun onMessageReceived(deviceMessage: DeviceMessage) {
                val deviceId = connectedDevice?.deviceId
                if (deviceId != null) {
                    callbacks.invoke { it.onMessageReceived(deviceId, deviceMessage) }
                } else {
                    disconnectWithError("Null device id found when message received.")
                }
            }

            override fun onMessageReceivedError(exception: Exception?) {
                // TODO(b/143879960) Extend the message error from here to continue up the chain.
            }

            // TODO(b/143879960): Extend the channel error from here to continue up the chain.
            override fun onEstablishSecureChannelFailure(
                @SecureBleChannel.ChannelError error: Int
            ) {
                val deviceId = connectedDevice?.deviceId
                if (deviceId != null) {
                    callbacks.invoke { it.onSecureChannelError(deviceId) }
                } else {
                    disconnectWithError(
                        "Null device id found when secure channel failed to establish.")
                }
                if (isAssociating) {
                    associationCallback?.onAssociationError(error)
                    disconnectWithError("Error while establishing secure connection.")
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

    private var originalBluetoothName: String? = null
    private var associationCallback: AssociationCallback? = null
    private var clientDeviceName: String? = null
    private var clientDeviceAddress: String? = null
    private val isAssociating get() = associationCallback != null
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    init {
        readCharacteristic.addDescriptor(descriptor)
    }

    override fun stop() {
        super.stop()
        reset()
        blePeripheralManager.cleanup()
    }

    /** Clear saved global values and callbacks of previous session. */
    fun reset() {
        resetBluetoothAdapterName()
        clientDeviceAddress = null
        clientDeviceName = null
        associationCallback = null
        connectedDevices.clear()
    }

    /** Connect to device with id of [deviceId]. */
    fun connectToDevice(deviceId: UUID) {
        val isAlreadyConnected = connectedDevices.any {
            try {
                UUID.fromString(it.deviceId) == deviceId
            } catch (e: Exception) {
                false
            }
        }
        if (isAlreadyConnected) {
            // Already connected to this device. Ignore requests to connect again.
            return
        }

        // Clear any previous session before starting a new one.
        reset()
        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                logd(TAG, "Successfully started advertising for device $deviceId.")
            }
        }
        blePeripheralManager.unregisterCallback(associatePeripheralCallback)
        blePeripheralManager.registerCallback(reconnectPeripheralCallback)
        startAdvertising(deviceId, advertiseCallback)
    }

    /** Start the association with a new device */
    fun startAssociation(deviceName: String, callback: AssociationCallback) {
        // Clear the previous session before starting a new one.
        reset()
        associationCallback = callback
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (originalBluetoothName == null) {
            originalBluetoothName = adapter.name
        }
        adapter.name = deviceName
        logd(TAG, "Changing bluetooth adapter name from $originalBluetoothName to $deviceName.")
        blePeripheralManager.unregisterCallback(reconnectPeripheralCallback)
        blePeripheralManager.registerCallback(associatePeripheralCallback)
        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                associationCallback?.onAssociationStartSuccess(deviceName)
                logd(TAG, "Successfully started advertising for association.")
            }
            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                callback.onAssociationStartFailure()
                logd(TAG, "Failed to start advertising for association. Error code: $errorCode")
            }
        }
        attemptAssociationAdvertising(callback, deviceName)
    }

    /** Stop the association with any device */
    fun stopAssociation(callback: AssociationCallback) {
        if (!isAssociating || callback != associationCallback) {
            return
        }
        reset()
    }

    private fun attemptAssociationAdvertising(callback: AssociationCallback, deviceName: String) {
        if (originalBluetoothName != null &&
            BluetoothAdapter.getDefaultAdapter().name == deviceName
        ) {
            startAdvertising(associationServiceUuid, advertiseCallback, includeDeviceName = true)
            return
        }
        scheduler.schedule(
            { attemptAssociationAdvertising(callback, deviceName) },
            ASSOCIATE_ADVERTISING_DELAY_MS,
            TimeUnit.MILLISECONDS
        )
        logd(TAG, "Adapter name change has not taken affect prior to advertising attempt." +
            " Trying again in $ASSOCIATE_ADVERTISING_DELAY_MS milliseconds.")
    }

    private fun startAdvertising(
        serviceUUID: UUID,
        callback: AdvertiseCallback,
        includeDeviceName: Boolean = false
    ) {
        val gattService = BluetoothGattService(
            serviceUUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        ).apply {
            addCharacteristic(readCharacteristic)
            addCharacteristic(writeCharacteristic)
        }
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(includeDeviceName)
            .addServiceUuid(ParcelUuid(serviceUUID))
            .build()
        blePeripheralManager.startAdvertising(gattService, advertiseData, callback)
    }

    /** Notify that the user has accepted a pairing code or any out-of-band confirmation*/
    fun notifyOutOfBandAccepted() {
        if (connectedDevice == null) {
            disconnectWithError("Null connected device found when out-of-band confirmation " +
                "received.")
            return
        }
        val secureChannel = connectedDevice?.secureChannel
        if (secureChannel == null) {
            disconnectWithError("Null SecureBleChannel found for the current connected device " +
                "when out-of-band confirmation received.")
            return
        }
        secureChannel.notifyOutOfBandAccepted()
    }

    @VisibleForTesting
    fun getConnectedDeviceChannel() = connectedDevice?.secureChannel

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
        reset()
    }

    private fun resetBluetoothAdapterName() {
        val originalName = originalBluetoothName ?: return
        logd(TAG, "Changing bluetooth adapter name back to $originalName")
        BluetoothAdapter.getDefaultAdapter().name = originalName
        originalBluetoothName = null
    }

    private fun setMtuSize(size: Int) {
        writeSize = size - ATT_PROTOCOL_BYTES
        connectedDevice?.secureChannel?.stream?.maxWriteSize = writeSize
    }
}
