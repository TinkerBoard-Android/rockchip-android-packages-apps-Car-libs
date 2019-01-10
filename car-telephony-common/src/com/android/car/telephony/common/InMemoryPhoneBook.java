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
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A singleton statically accessible helper class which pre-loads contacts list into memory so
 * that they can be accessed more easily and quickly.
 */
public class InMemoryPhoneBook {
    private static final String TAG = "CD.InMemoryPhoneBook";
    private static InMemoryPhoneBook sInMemoryPhoneBook;

    private final Context mContext;

    private boolean mIsLoaded = false;

    private ObservableAsyncQuery mObservableAsyncQuery;
    private List<Contact> mContacts = new ArrayList<>();
    private MutableLiveData<List<Contact>> mContactsLiveData = new MutableLiveData<>();
    /**
     * A map to speed up phone number searching.
     */
    private Map<I18nPhoneNumberWrapper, Contact> mPhoneNumberContactMap = new HashMap<>();

    private InMemoryPhoneBook(Context context) {
        mContext = context;
    }

    /**
     * Initialize the globally accessible {@link InMemoryPhoneBook}.
     */
    public static InMemoryPhoneBook init(Context context) {
        if (sInMemoryPhoneBook == null) {
            sInMemoryPhoneBook = new InMemoryPhoneBook(context);
            sInMemoryPhoneBook.onInit();
        } else {
            throw new IllegalStateException("Call teardown before reinitialized PhoneBook");
        }
        return get();
    }

    /** Get the global {@link InMemoryPhoneBook} instance. */
    public static InMemoryPhoneBook get() {
        if (sInMemoryPhoneBook != null) {
            return sInMemoryPhoneBook;
        } else {
            throw new IllegalStateException("Call init before get InMemoryPhoneBook");
        }
    }

    /**
     * Tears down the globally accessible {@link InMemoryPhoneBook}.
     */
    public static void tearDown() {
        sInMemoryPhoneBook.mObservableAsyncQuery.stopQuery();
        sInMemoryPhoneBook = null;
    }

    private void onInit() {
        Log.v(TAG, "onInit");
        String selection = ContactsContract.Data.MIMETYPE + " = ?";
        String[] selectionArgs = new String[1];
        selectionArgs[0] = ContactsContract.CommonDataKinds.Phone
                .CONTENT_ITEM_TYPE;
        ObservableAsyncQuery.QueryParam contactListQueryParam = new ObservableAsyncQuery.QueryParam(
                ContactsContract.Data.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC ");

        mObservableAsyncQuery = new ObservableAsyncQuery(contactListQueryParam,
                mContext.getContentResolver(), this::onDataLoaded);
        mObservableAsyncQuery.startQuery();
    }

    public boolean isLoaded() {
        return mIsLoaded;
    }

    /**
     * Returns a {@link LiveData} which monitors the contact list changes.
     */
    public LiveData<List<Contact>> getContactsLiveData() {
        return mContactsLiveData;
    }

    /**
     * Looks up a {@link Contact} by the given phone number. Returns null if can't find a Contact or
     * the {@link InMemoryPhoneBook} is still loading.
     */
    @Nullable
    public Contact lookupContactEntry(String phoneNumber) {
        Log.v(TAG, String.format("lookupContactEntry: %s", phoneNumber));
        if (!isLoaded()) {
            Log.w(TAG, "looking up a contact while loading.");
            return null;
        }

        if (TextUtils.isEmpty(phoneNumber)) {
            Log.w(TAG, "looking up an empty phone number.");
            return null;
        }

        I18nPhoneNumberWrapper i18nPhoneNumber = I18nPhoneNumberWrapper.newInstance(mContext,
                phoneNumber);
        return mPhoneNumberContactMap.get(i18nPhoneNumber);
    }

    private void onDataLoaded(Cursor cursor) {
        Map<String, Contact> result = new LinkedHashMap<>();

        while (cursor.moveToNext()) {
            Contact contact = Contact.fromCursor(mContext, cursor);
            String lookupKey = contact.getLookupKey();
            if (result.containsKey(lookupKey)) {
                Contact existingContact = result.get(lookupKey);
                existingContact.merge(contact);
            } else {
                result.put(lookupKey, contact);
            }
        }

        mIsLoaded = true;
        mContacts.clear();
        mContacts.addAll(result.values());
        Collections.sort(mContacts);

        mPhoneNumberContactMap.clear();
        for (Contact contact : mContacts) {
            for (PhoneNumber phoneNumber : contact.getNumbers()) {
                mPhoneNumberContactMap.put(phoneNumber.getI18nPhoneNumberWrapper(), contact);
            }
        }
        mContactsLiveData.setValue(mContacts);
    }
}
