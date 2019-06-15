/**
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

package com.android.car.messaging.service;

import com.android.car.messaging.service.ConnectionConfig;
import com.android.car.messaging.service.IConnectionCallback;
import com.android.car.messaging.service.IMessagingService;
import com.android.car.messaging.service.SupportedVersionInfo;

/**
 * An interface through which clients are authenticated, api version handshaked and connected
 * to a MessagingService.
 */
interface IMessagingServiceProvider {

    /* @return The versions supported by the API. */
    SupportedVersionInfo getSupportedApiVersionInfo() = 1;

    /**
     * Requests to connect to a MessagingService.
     *
     * Returns a non-null MessagingService if the client is authenticated and connection is
     * successful.
     */
    oneway void connect(in ConnectionConfig config, IConnectionCallback callback) = 2;
}