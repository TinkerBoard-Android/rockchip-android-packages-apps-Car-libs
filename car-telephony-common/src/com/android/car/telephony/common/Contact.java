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
import android.icu.text.Collator;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates data about a phone Contact entry. Typically loaded from the local Contact store.
 */
public class Contact implements Parcelable, Comparable<Contact> {
    private static final String TAG = "CD.Contact";

    /**
     * Contact belongs to TYPE_LETTER if its display name starts with a letter
     */
    private static final int TYPE_LETTER = 1;

    /**
     * Contact belongs to TYPE_DIGIT if its display name starts with a digit
     */
    private static final int TYPE_DIGIT = 2;

    /**
     * Contact belongs to TYPE_OTHER if it does not belong to TYPE_LETTER or TYPE_DIGIT
     * Such as empty display name or the display name starts with "_"
     */
    private static final int TYPE_OTHER = 3;


    /**
     * A reference to the {@link ContactsContract.Contacts#_ID} that this data belongs to. See
     * {@link ContactsContract.Contacts.Entity#CONTACT_ID}
     */
    private long mId;

    /**
     * Whether this contact entry is starred by user.
     */
    private boolean mIsStarred;

    /**
     * Contact-specific information about whether or not a contact has been pinned by the user at
     * a particular position within the system contact application's user interface.
     */
    private int mPinnedPosition;

    /**
     * All phone numbers of this contact mapping to the unique primary key for the raw data entry.
     */
    private List<PhoneNumber> mPhoneNumbers = new ArrayList<>();

    /**
     * The display name.
     */
    private String mDisplayName;

    /**
     * A URI that can be used to retrieve a thumbnail of the contact's photo.
     */
    private Uri mAvatarThumbnailUri;

    /**
     * A URI that can be used to retrieve the contact's full-size photo.
     */
    private Uri mAvatarUri;

    /**
     * An opaque value that contains hints on how to find the contact if its row id changed
     * as a result of a sync or aggregation. If a contact has multiple phone numbers, all phone
     * numbers are recorded in a single entry and they all have the same look up key in a single
     * load.
     */
    private String mLookupKey;

    /**
     * Whether this contact represents a voice mail.
     */
    private boolean mIsVoiceMail;

    private PhoneNumber mPrimaryPhoneNumber;

    /**
     * Parses a Contact entry for a Cursor loaded from the Contact Database.
     */
    public static Contact fromCursor(Context context, Cursor cursor) {
        int contactIdColumn = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
        int starredColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED);
        int pinnedColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PINNED);
        int displayNameColumn = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int avatarUriColumn = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI);
        int avatarThumbnailColumn = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI);
        int lookupKeyColumn = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY);
        int typeColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
        int labelColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL);
        int numberColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
        int rawDataIdColumn = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID);
        int dataVersionColumn = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.DATA_VERSION);
        // IS_PRIMARY means primary entry of the raw contact and IS_SUPER_PRIMARY means primary
        // entry of the aggregated contact. It is guaranteed that only one data entry is super
        // primary.
        int isPrimaryColumn = cursor.getColumnIndex(
                ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY);

        Contact contact = new Contact();
        contact.mId = cursor.getLong(contactIdColumn);
        contact.mDisplayName = cursor.getString(displayNameColumn);

        PhoneNumber number = PhoneNumber.newInstance(
                context,
                cursor.getString(numberColumn),
                cursor.getInt(typeColumn),
                cursor.getString(labelColumn),
                cursor.getInt(isPrimaryColumn) > 0,
                cursor.getLong(rawDataIdColumn),
                cursor.getInt(dataVersionColumn));
        contact.mPhoneNumbers.add(number);
        if (number.isPrimary()) {
            contact.mPrimaryPhoneNumber = number;
        }

        contact.mIsStarred = cursor.getInt(starredColumn) > 0;
        contact.mPinnedPosition = cursor.getInt(pinnedColumn);
        contact.mIsVoiceMail = TelecomUtils.isVoicemailNumber(context, number.getNumber());

        String avatarUriStr = cursor.getString(avatarUriColumn);
        contact.mAvatarUri = avatarUriStr == null ? null : Uri.parse(avatarUriStr);

        String avatarThumbnailStringUri = cursor.getString(avatarThumbnailColumn);
        contact.mAvatarThumbnailUri = avatarThumbnailStringUri == null ? null : Uri.parse(
                avatarThumbnailStringUri);

        String lookUpKey = cursor.getString(lookupKeyColumn);
        if (lookUpKey != null) {
            contact.mLookupKey = lookUpKey;
        } else {
            Log.w(TAG, "Look up key is null. Fallback to use display name");
            contact.mLookupKey = contact.mDisplayName;
        }
        return contact;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Contact && mLookupKey.equals(((Contact) obj).mLookupKey);
    }

    @Override
    public int hashCode() {
        return mLookupKey.hashCode();
    }

    @Override
    public String toString() {
        return mDisplayName + mPhoneNumbers;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public boolean isVoicemail() {
        return mIsVoiceMail;
    }

    @Nullable
    public Uri getAvatarUri() {
        return mAvatarThumbnailUri != null ? mAvatarThumbnailUri : mAvatarUri;
    }

    public String getLookupKey() {
        return mLookupKey;
    }

    public Uri getLookupUri() {
        return ContactsContract.Contacts.getLookupUri(mId, mLookupKey);
    }

    /** Return all phone numbers associated with this contact. */
    public List<PhoneNumber> getNumbers() {
        return mPhoneNumbers;
    }

    /** Return the aggregated contact id. */
    public long getId() {
        return mId;
    }

    public boolean isStarred() {
        return mIsStarred;
    }

    public int getPinnedPosition() {
        return mPinnedPosition;
    }

    /**
     * Merges a Contact entry with another if they represent different numbers of the same contact.
     *
     * @return A merged contact.
     */
    public Contact merge(Contact contact) {
        if (equals(contact)) {
            for (PhoneNumber phoneNumber : contact.mPhoneNumbers) {
                int indexOfPhoneNumber = mPhoneNumbers.indexOf(phoneNumber);
                if (indexOfPhoneNumber < 0) {
                    mPhoneNumbers.add(phoneNumber);
                } else {
                    PhoneNumber existingPhoneNumber = mPhoneNumbers.get(indexOfPhoneNumber);
                    existingPhoneNumber.merge(phoneNumber);
                }
            }
            if (contact.mPrimaryPhoneNumber != null) {
                mPrimaryPhoneNumber = contact.mPrimaryPhoneNumber.merge(mPrimaryPhoneNumber);
            }
        }
        return this;
    }

    /**
     * Looks up a {@link PhoneNumber} of this contact for the given phone number. Returns {@code
     * null} if this contact doesn't contain the given phone number.
     */
    @Nullable
    public PhoneNumber getPhoneNumber(String number) {
        for (PhoneNumber phoneNumber : mPhoneNumbers) {
            if (PhoneNumberUtils.compare(phoneNumber.getNumber(), number)) {
                return phoneNumber;
            }
        }
        return null;
    }

    public PhoneNumber getPrimaryPhoneNumber() {
        return mPrimaryPhoneNumber;
    }

    /** Return if this contact has a primary phone number. */
    public boolean hasPrimaryPhoneNumber() {
        return mPrimaryPhoneNumber != null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeBoolean(mIsStarred);
        dest.writeInt(mPinnedPosition);
        dest.writeInt(mPhoneNumbers.size());
        for (PhoneNumber phoneNumber : mPhoneNumbers) {
            dest.writeParcelable(phoneNumber, flags);
        }
        dest.writeString(mDisplayName);
        dest.writeParcelable(mAvatarThumbnailUri, 0);
        dest.writeParcelable(mAvatarUri, 0);
        dest.writeString(mLookupKey);
        dest.writeBoolean(mIsVoiceMail);
    }

    public static final Creator<Contact> CREATOR = new Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel source) {
            return Contact.fromParcel(source);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };

    /** Create {@link Contact} object from saved parcelable. */
    private static Contact fromParcel(Parcel source) {
        Contact contact = new Contact();
        contact.mId = source.readLong();
        contact.mIsStarred = source.readBoolean();
        contact.mPinnedPosition = source.readInt();
        int phoneNumberListLength = source.readInt();
        contact.mPhoneNumbers = new ArrayList<>();
        for (int i = 0; i < phoneNumberListLength; i++) {
            PhoneNumber phoneNumber = source.readParcelable(PhoneNumber.class.getClassLoader());
            contact.mPhoneNumbers.add(phoneNumber);
            if (phoneNumber.isPrimary()) {
                contact.mPrimaryPhoneNumber = phoneNumber;
            }
        }
        contact.mDisplayName = source.readString();
        contact.mAvatarThumbnailUri = source.readParcelable(Uri.class.getClassLoader());
        contact.mAvatarUri = source.readParcelable(Uri.class.getClassLoader());
        contact.mLookupKey = source.readString();
        contact.mIsVoiceMail = source.readBoolean();
        return contact;
    }

    @Override
    public int compareTo(Contact otherContact) {
        // Use a helper function to classify Contacts
        int type = getNameType(mDisplayName);
        int otherType = getNameType(otherContact.mDisplayName);
        if (type != otherType) {
            return Integer.compare(type, otherType);
        }
        Collator collator = Collator.getInstance();
        return collator.compare(mDisplayName == null ? "" : mDisplayName,
                otherContact.mDisplayName == null ? "" : otherContact.mDisplayName);
    }

    private static int getNameType(String displayName) {
        // A helper function to classify Contacts
        if (!TextUtils.isEmpty(displayName)) {
            if (Character.isLetter(displayName.charAt(0))) {
                return TYPE_LETTER;
            }
            if (Character.isDigit(displayName.charAt(0))) {
                return TYPE_DIGIT;
            }
        }
        return TYPE_OTHER;
    }
}
