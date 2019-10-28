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

package com.android.car.connecteddevice.util

import com.android.internal.annotations.GuardedBy

/** Class for managing unique numeric ids. */
class IdManager {

    private val lock = object
    @Volatile
    @GuardedBy("lock")
    private var nextVal = 0

    private var openVals = 0

    /**
     * Returns the next available id from the pool and reserves it from future use until released.
     */
    fun reserve(): Int {
        synchronized(lock) {
            return nextVal++
        }
    }

    /** Release the [value] back to id pool. */
    fun releaseReservation(value: Int) {
        synchronized(lock) {
            if (value == nextVal - 1) {
                nextVal = value
            } else {
                openVals++
            }
            if (nextVal == openVals) {
                nextVal = 0
                openVals = 0
            }
        }
    }
}