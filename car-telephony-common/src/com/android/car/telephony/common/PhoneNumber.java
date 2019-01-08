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
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;

import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Contact phone number and its meta data.
 */
public class PhoneNumber implements Parcelable {

    private final int mType;
    private final I18nPhoneNumberWrapper mI18nPhoneNumber;
    @Nullable
    private final String mLabel;

    private int mId;
    private int mDataVersion;

    /**
     * Creates a new {@link PhoneNumber}.
     *
     * @param rawNumber   A potential phone number.
     * @param type        The phone number type. See more at
     *                    {@link ContactsContract.CommonDataKinds.CommonColumns#TYPE}
     * @param label       The user defined label. See more at
     *                    {@link ContactsContract.CommonDataKinds.CommonColumns#LABEL}
     * @param id          The unique key for raw contact entry containing the phone number entity.
     * @param dataVersion The dataVersion of the raw contact entry record. See more at {@link
     *                    android.provider.ContactsContract.Data#DATA_VERSION}
     */
    public static PhoneNumber newInstance(Context context, String rawNumber, int type,
                                          @Nullable String label, int id, int dataVersion) {
        I18nPhoneNumberWrapper i18nPhoneNumber = I18nPhoneNumberWrapper.newInstance(context,
                rawNumber);
        return new PhoneNumber(i18nPhoneNumber, type, label, id, dataVersion);
    }

    private PhoneNumber(I18nPhoneNumberWrapper i18nNumber, int type, @Nullable String label, int id,
                        int dataVersion) {
        mType = type;
        mI18nPhoneNumber = i18nNumber;
        mLabel = label;
        mId = id;
        mDataVersion = dataVersion;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PhoneNumber
                && ((PhoneNumber) obj).mType == mType
                && Objects.equals(((PhoneNumber) obj).mLabel, mLabel)
                && mI18nPhoneNumber.equals(((PhoneNumber) obj).mI18nPhoneNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mI18nPhoneNumber, mType, mLabel);
    }

    /**
     * Returns a human readable string label. For example, Home, Work, etc.
     */
    public CharSequence getReadableLabel(Resources res) {
        return ContactsContract.CommonDataKinds.Phone.getTypeLabel(res, mType, mLabel);
    }

    /**
     * Gets a phone number in the international format if valid. Otherwise, returns the raw number.
     */
    public String getNumber() {
        return mI18nPhoneNumber.getNumber();
    }

    /**
     * Returns the format independent i18n {@link I18nPhoneNumberWrapper wrapper} class.
     */
    public I18nPhoneNumberWrapper getI18nPhoneNumberWrapper() {
        return mI18nPhoneNumber;
    }

    /**
     * Gets the type of phone number, for example Home or Work. Possible values are defined in
     * {@link ContactsContract.CommonDataKinds.Phone CommonDataKinds.Phone}.
     */
    public int getType() {
        return mType;
    }

    public int getId() {
        return mId;
    }

    /**
     * Merge the given phone number with this one, updating both this phone number instance, and
     * returning it.
     */
    public PhoneNumber merge(PhoneNumber phoneNumber) {
        if (equals(phoneNumber)) {
            if (mDataVersion < phoneNumber.mDataVersion) {
                mDataVersion = phoneNumber.mDataVersion;
                mId = phoneNumber.mId;
            }
        }
        return this;
    }

    /**
     * Gets the user defined label for the the contact method.
     */
    @Nullable
    public String getLabel() {
        return mLabel;
    }

    @Override
    public String toString() {
        return getNumber() + " " + String.valueOf(mLabel);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mType);
        dest.writeString(mLabel);
        dest.writeParcelable(mI18nPhoneNumber, flags);
        dest.writeInt(mId);
        dest.writeInt(mDataVersion);
    }

    public static Creator<PhoneNumber> CREATOR = new Creator<PhoneNumber>() {
        @Override
        public PhoneNumber createFromParcel(Parcel source) {
            int type = source.readInt();
            String label = source.readString();
            I18nPhoneNumberWrapper i18nPhoneNumberWrapper = source.readParcelable(
                    I18nPhoneNumberWrapper.class.getClassLoader());
            int id = source.readInt();
            int dataVersion = source.readInt();
            PhoneNumber phoneNumber = new PhoneNumber(i18nPhoneNumberWrapper, type, label, id,
                    dataVersion);
            return phoneNumber;
        }

        @Override
        public PhoneNumber[] newArray(int size) {
            return new PhoneNumber[size];
        }
    };
}
