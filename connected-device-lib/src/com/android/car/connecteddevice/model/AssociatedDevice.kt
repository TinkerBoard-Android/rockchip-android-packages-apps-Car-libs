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

package com.android.car.connecteddevice.model

private const val DELIMITER = ","
private const val NUM_OF_FIELDS = 3
private const val DEFAULT_DEVICE_NAME = ""

// When serialized, the id, address and name of this class are stored in the following
// order. These index values are used for deserialization.
private const val ID_INDEX = 0
private const val ADDRESS_INDEX = 1
private const val NAME_INDEX = 2

/**
 * Contains basic info of an associated device.
 *
 * @property deviceId Id of the associated device.
 * @property deviceName Name of the associated device. `null` if not known.
 * @property deviceAddress Address of the associated device.
 */
data class AssociatedDevice(
    val deviceId: String,
    val deviceAddress: String,
    val deviceName: String?
) {
    /**
     * Returns a string representation of this [AssociatedDevice] that can be saved to
     * [SharedPreferences].
     */
    fun serialize(): String {
        return listOf(
            deviceId,
            deviceAddress,
            deviceName ?: DEFAULT_DEVICE_NAME
        ).joinToString(separator = DELIMITER)
    }
    companion object {
        /**
         * Create an [AssociatedDevice] from the given serialized string. The string passed to this
         * method should be one created by [serialize].
         */
        @JvmStatic
        fun deserialize(associatedDevice: String): AssociatedDevice {
            val res = associatedDevice.split(DELIMITER)
            require(res.size == NUM_OF_FIELDS) {
                "Failed to deserialize, invalid number of fields."
            }
            return AssociatedDevice(res[ID_INDEX], res[ADDRESS_INDEX], res[NAME_INDEX])
        }
    }
}