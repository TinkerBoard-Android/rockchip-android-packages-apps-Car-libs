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

package com.android.car.connecteddevice.ble;

import static com.android.car.connecteddevice.util.SafeLog.logd;
import static com.android.car.connecteddevice.util.SafeLog.loge;
import static com.android.car.connecteddevice.util.SafeLog.logw;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;

import com.android.internal.annotations.GuardedBy;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Class that manages BLE scanning operations.
 */
public class BleCentralManager {

    private static final String TAG = "BleCentralManager";

    private static final int RETRY_LIMIT = 5;

    private static final int RETRY_INTERVAL_MS = 1000;

    private final Context mContext;

    private final Handler mHandler;

    private List<ScanFilter> mScanFilters;

    private ScanSettings mScanSettings;

    private ScanCallback mScanCallback;

    private BluetoothLeScanner mScanner;

    private int mScannerStartCount = 0;

    private Lock mLock = new ReentrantLock();
    @GuardedBy("mLock")
    private volatile ScannerState mScannerState = ScannerState.STOPPED;

    private enum ScannerState {
        STOPPED,
        STARTED,
        SCANNING
    }

    public BleCentralManager(@NonNull Context context) {
        mContext = context;
        mHandler = new Handler(context.getMainLooper());
    }

    /**
     * Start the BLE scanning process.
     *
     * @param filters Optional list of {@link ScanFilter}s to apply to scan results.
     * @param settings {@link ScanSettings} to apply to scanner.
     * @param callback {@link ScanCallback} for scan events.
     */
    public void startScanning(@Nullable List<ScanFilter> filters, @NonNull ScanSettings settings,
            @NonNull ScanCallback callback) {
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            loge(TAG, "Attempted start scanning, but system does not support BLE. Ignoring");
            return;
        }
        logd(TAG, "Request received to start scanning.");
        mScannerStartCount = 0;
        mScanFilters = filters;
        mScanSettings = settings;
        mScanCallback = callback;
        updateScannerState(ScannerState.STARTED);
        startScanningInternally();
    }

    /** Stop the scanner */
    public void stopScanning() {
        logd(TAG, "Attempting to stop scanning");
        if (mScanner != null) {
            mScanner.stopScan(mInternalScanCallback);
        }
        mScanCallback = null;
        updateScannerState(ScannerState.STOPPED);
    }

    /** Returns `true` if currently scanning, `false` otherwise. */
    public boolean isScanning() {
        mLock.lock();
        try {
            return mScannerState == ScannerState.SCANNING;
        } finally {
            mLock.unlock();
        }
    }

    /** Clean up the scanning process. */
    public void cleanup() {
        if (isScanning()) {
            stopScanning();
        }
    }

    private void startScanningInternally() {
        logd(TAG, "Attempting to start scanning");
        if (mScanner == null && BluetoothAdapter.getDefaultAdapter() != null) {
            mScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        }
        if (mScanner != null) {
            mScanner.startScan(mScanFilters, mScanSettings, mInternalScanCallback);
            updateScannerState(ScannerState.SCANNING);
        } else {
            mHandler.postDelayed(() -> {
                // Keep trying
                logd(TAG, "Scanner unavailable. Trying again.");
                startScanningInternally();
            }, RETRY_INTERVAL_MS);
        }
    }

    private void updateScannerState(ScannerState newState) {
        mLock.lock();
        try {
            mScannerState = newState;
        } finally {
            mLock.unlock();
        }
    }

    private final ScanCallback mInternalScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (mScanCallback != null) {
                mScanCallback.onScanResult(callbackType, result);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            logd(TAG, "Batch scan found " + results.size() + " results.");
            if (mScanCallback != null) {
                mScanCallback.onBatchScanResults(results);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            if (mScannerStartCount >= RETRY_LIMIT) {
                loge(TAG, "Cannot start BLE Scanner. Scanning Retry count: "
                        + mScannerStartCount);
                if (mScanCallback != null) {
                    mScanCallback.onScanFailed(errorCode);
                }
                return;
            }

            mScannerStartCount++;
            logw(TAG, "BLE Scanner failed to start. Error: "
                    + errorCode
                    + " Retry: "
                    + mScannerStartCount);
            switch(errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    // Scanner already started. Do nothing.
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                case SCAN_FAILED_INTERNAL_ERROR:
                    mHandler.postDelayed(BleCentralManager.this::startScanningInternally,
                            RETRY_INTERVAL_MS);
                    break;
                default:
                    // Ignore other codes.
            }
        }
    };
}
