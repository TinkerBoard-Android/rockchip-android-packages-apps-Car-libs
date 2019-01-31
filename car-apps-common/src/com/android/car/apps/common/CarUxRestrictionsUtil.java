/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.car.apps.common;

import android.app.Application;
import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.Context;
import android.util.Log;

/**
 * Utility class to access Car Restriction Manager
 */
public class CarUxRestrictionsUtil {
    private static final String TAG = "CarUxRestrictionsUtil";

    private Car mCarApi;
    private CarUxRestrictionsManager mCarUxRestrictionsManager;
    private Context mContext;

    private static CarUxRestrictionsUtil sUtilInstance = null;

    private String mEllipses;

    private CarUxRestrictionsUtil(Application application) {
        mContext = application;
        mCarApi = Car.createCar(application);
        try {
            mCarUxRestrictionsManager = (CarUxRestrictionsManager) mCarApi
                    .getCarManager(Car.CAR_UX_RESTRICTION_SERVICE);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car not connected", e);
        }
        mEllipses = mContext.getResources().getString(R.string.ellipsis);
    }

    /**
     * Gets the singleton instance for this class
     */
    public static CarUxRestrictionsUtil getInstance(Application application) {
        if (sUtilInstance == null) {
            sUtilInstance = new CarUxRestrictionsUtil(application);
        }

        return sUtilInstance;
    }

    /**
     * Gets the DO defined maximum text length.
     */
    public int getMaxStringLength() {
        int maxLength;
        try {
            maxLength = mCarUxRestrictionsManager.getCurrentCarUxRestrictions()
                    .getMaxRestrictedStringLength();
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Car not connected", e);
            maxLength = mContext.getResources().getInteger(R.integer.default_max_string_length);
        }

        return maxLength;
    }

    /**
     * Ellipsizes string to fit DO specifications
     */
    public String restrictString(String str) {
        int maxStringLength = getMaxStringLength();

        if (str.length() > maxStringLength) {
            str = str.substring(0, maxStringLength) + mEllipses;
        }

        return str;
    }
}
