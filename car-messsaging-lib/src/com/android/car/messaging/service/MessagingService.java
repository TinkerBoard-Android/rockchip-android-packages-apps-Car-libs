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

package com.android.car.messaging.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;

/**
 * Base class for messaging apps which enables messaging apps to provide their data to authorized
 * clients.
 */
public abstract class MessagingService extends Service {
    private static final String TAG = "MessagingService";

    private MessagingServiceProviderImpl mMessagingServiceProviderImpl;
    private final Handler mMainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        mMessagingServiceProviderImpl = new MessagingServiceProviderImpl();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessagingServiceProviderImpl;
    }

    private class MessagingServiceProviderImpl extends IMessagingServiceProvider.Stub {
        private MessagingServiceImpl mMessagingService = new MessagingServiceImpl();

        @Override
        public SupportedVersionInfo getSupportedApiVersionInfo() {
            return new SupportedVersionInfo(/* minVersion = */ 1, /* maxVersion = */ 1);
        }

        @Override
        public void connect(ConnectionConfig connectionConfig,
                IConnectionCallback callback) throws RemoteException {
            int uid = Binder.getCallingUid();
            if (isValidPackage(connectionConfig.getPackageName(), uid)) {
                mMainThreadHandler.post(() -> {
                    try {
                        if (onAuthenticate(connectionConfig.getPackageName())) {
                            callback.onConnect(mMessagingService);
                        } else {
                            callback.onConnectFailed();
                        }
                    } catch (RemoteException e) {
                        Log.w(TAG, "Calling IConnectionCallback failed. Ignoring. "
                                + "pkg=" + connectionConfig.getPackageName());
                    }
                });
            } else {
                callback.onConnectFailed();
            }
        }

        /**
         * Returns true if the provided package name belongs to the given uid.
         */
        private boolean isValidPackage(String pkg, int uid) {
            if (pkg == null) {
                return false;
            }

            final PackageManager pm = getPackageManager();
            final String[] packages = pm.getPackagesForUid(uid);
            if (packages == null) {
                return false;
            }

            for (int i = 0; i < packages.length; i++) {
                if (packages[i].equals(pkg)) {
                    return true;
                }
            }
            return false;
        }
    }

    private class MessagingServiceImpl extends IMessagingService.Stub {
    }

    /**
     * Called when a client tries to connect to this {@link MessagingService}. Returns true if
     * the client is authorized to access
     *
     * @param packageName The calling client's package name.
     */
    protected abstract boolean onAuthenticate(String packageName);
}
