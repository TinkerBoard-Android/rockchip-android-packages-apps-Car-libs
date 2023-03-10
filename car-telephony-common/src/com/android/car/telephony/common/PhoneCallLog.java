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

package com.android.car.telephony.common;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;
import android.text.TextUtils;
import android.text.format.DateUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Entity class for call logs of a phone number. This call log may contains multiple call
 * records.
 */
public class PhoneCallLog {
    private static final String TAG = "CD.PhoneCallLog";

    @IntDef({TimeRange.TODAY, TimeRange.YESTERDAY, TimeRange.OLDER})
    public @interface TimeRange {
        int TODAY = 0;
        int YESTERDAY = 1;
        int OLDER = 2;
    }

    /** Call log record. */
    public static class Record implements Comparable<Record> {
        private final long mCallEndTimestamp;
        private final int mCallType;

        public Record(long callEndTimestamp, int callType) {
            mCallEndTimestamp = callEndTimestamp;
            mCallType = callType;
        }

        /** Returns the timestamp on when the call occured, in milliseconds since the epoch */
        public long getCallEndTimestamp() {
            return mCallEndTimestamp;
        }

        /**
         * Returns the type of this record. For example, missed call, outbound call. Allowed values
         * are defined in {@link CallLog.Calls#TYPE}.
         *
         * @see CallLog.Calls#TYPE
         */
        public int getCallType() {
            return mCallType;
        }

        /** Phone call records are sort in reverse chronological order. */
        @Override
        public int compareTo(Record otherRecord) {
            return (int) (otherRecord.mCallEndTimestamp - mCallEndTimestamp);
        }
    }

    private long mId;
    private String mPhoneNumberString;
    private I18nPhoneNumberWrapper mI18nPhoneNumberWrapper;
    private String mAccountName;
    private List<Record> mCallRecords = new ArrayList<>();
    private int mTimeRange;

    /**
     * Creates a {@link PhoneCallLog} from a {@link Cursor}.
     */
    public static PhoneCallLog fromCursor(Context context, Cursor cursor) {
        int idColumn = cursor.getColumnIndex(CallLog.Calls._ID);
        int numberColumn = cursor.getColumnIndex(CallLog.Calls.NUMBER);
        int dateColumn = cursor.getColumnIndex(CallLog.Calls.DATE);
        int callTypeColumn = cursor.getColumnIndex(CallLog.Calls.TYPE);
        int accountNameColumn = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID);

        PhoneCallLog phoneCallLog = new PhoneCallLog();
        phoneCallLog.mId = cursor.getLong(idColumn);
        phoneCallLog.mPhoneNumberString = cursor.getString(numberColumn);
        phoneCallLog.mI18nPhoneNumberWrapper = I18nPhoneNumberWrapper.Factory.INSTANCE.get(context,
                phoneCallLog.mPhoneNumberString);
        Record record = new Record(cursor.getLong(dateColumn), cursor.getInt(callTypeColumn));
        phoneCallLog.mCallRecords.add(record);
        phoneCallLog.mTimeRange = getTimeRange(record.getCallEndTimestamp());
        phoneCallLog.mAccountName = cursor.getString(accountNameColumn);
        return phoneCallLog;
    }

    /** Returns the phone number of this log. */
    public String getPhoneNumberString() {
        return mPhoneNumberString;
    }

    /**
     * Returns the account name that this call log belongs to. For call logs from Bluetooth device,
     * account name is the same as Bluetooth address.
     */
    public String getAccountName() {
        return mAccountName;
    }

    /** Returns the id of this log. */
    public long getPhoneLogId() {
        return mId;
    }

    /** Returns the last call end timestamp of this number. */
    public long getLastCallEndTimestamp() {
        Preconditions.checkState(!mCallRecords.isEmpty(), "Unexpected empty call records");
        return mCallRecords.get(0).getCallEndTimestamp();
    }

    /**
     * Returns a copy of records from the phone number. Logs are sorted from most recent to least
     * recent call end time.
     */
    public List<Record> getAllCallRecords() {
        return new ArrayList<>(mCallRecords);
    }

    /** Returns the time range when the phone call was made. */
    @TimeRange
    public int getTimeRange() {
        return mTimeRange;
    }

    /**
     * Merges all call records with this call log's call records if they are representing the same
     * phone number.
     *
     * @param checkTimeRange if true, only merge the call records if they are in the same time range
     */
    public boolean merge(@NonNull PhoneCallLog phoneCallLog, boolean checkTimeRange) {
        if (!equals(phoneCallLog)) {
            return false;
        }
        if (checkTimeRange && mTimeRange != phoneCallLog.getTimeRange()) {
            return false;
        }
        mCallRecords.addAll(phoneCallLog.mCallRecords);
        Collections.sort(mCallRecords);
        return true;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof PhoneCallLog) {
            // We compare the ids when the phone number string is empty.
            if (TextUtils.isEmpty(mPhoneNumberString)) {
                return mId == ((PhoneCallLog) object).mId;
            } else {
                return mI18nPhoneNumberWrapper.equals(
                        ((PhoneCallLog) object).mI18nPhoneNumberWrapper);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (TextUtils.isEmpty(mPhoneNumberString)) {
            return Long.hashCode(mId);
        } else {
            return Objects.hashCode(mI18nPhoneNumberWrapper);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PhoneNumber: ");
        sb.append(TelecomUtils.piiLog(mPhoneNumberString));
        sb.append(" CallLog: ");
        sb.append(mCallRecords.size());
        sb.append(" Account: ");
        sb.append(mAccountName);
        return sb.toString();
    }

    @TimeRange
    private static int getTimeRange(long callLogTime) {
        if (DateUtils.isToday(callLogTime)) {
            return TimeRange.TODAY;
        }

        if (DateUtils.isToday(callLogTime + TimeUnit.DAYS.toMillis(1))) {
            return TimeRange.YESTERDAY;
        }

        return TimeRange.OLDER;
    }
}
