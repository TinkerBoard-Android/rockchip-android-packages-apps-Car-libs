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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Settings;
import android.telecom.Call;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.android.car.apps.common.LetterTileDrawable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.util.Locale;

/** Helper methods. */
public class TelecomUtils {
    private static final String TAG = "Em.TelecomUtils";

    private static final String[] CONTACT_ID_PROJECTION = new String[]{
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.TYPE,
            ContactsContract.PhoneLookup.LABEL,
            ContactsContract.PhoneLookup._ID
    };

    private static String sVoicemailNumber;
    private static TelephonyManager sTelephonyManager;

    /**
     * Return the label for the given phone number.
     *
     * @param number Caller phone number
     * @return the label if it is found, 0 otherwise.
     */
    public static CharSequence getTypeFromNumber(Context context, String number) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "getTypeFromNumber, number: " + number);
        }
        String defaultLabel = "";
        if (number == null || number.isEmpty()) {
            return defaultLabel;
        }

        ContentResolver cr = context.getContentResolver();
        Resources res = context.getResources();
        Uri uri = Uri.withAppendedPath(
                PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        Cursor cursor = cr.query(uri, CONTACT_ID_PROJECTION, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                int typeColumn = cursor.getColumnIndex(PhoneLookup.TYPE);
                int type = cursor.getInt(typeColumn);
                int labelColumn = cursor.getColumnIndex(PhoneLookup.LABEL);
                String label = cursor.getString(labelColumn);
                CharSequence typeLabel =
                        Phone.getTypeLabel(res, type, label);
                return typeLabel;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return defaultLabel;
    }

    /**
     * Get the voicemail number.
     */
    public static String getVoicemailNumber(Context context) {
        if (sVoicemailNumber == null) {
            sVoicemailNumber = getTelephonyManager(context).getVoiceMailNumber();
        }
        return sVoicemailNumber;
    }

    /**
     * Returns {@code true} if the given number is a voice mail number.
     *
     * @see TelephonyManager#getVoiceMailNumber()
     */
    public static boolean isVoicemailNumber(Context context, String number) {
        return !TextUtils.isEmpty(number) && number.equals(getVoicemailNumber(context));
    }

    /**
     * Get the {@link TelephonyManager} instance.
     */
    // TODO(deanh): remove this, getSystemService is not slow.
    public static TelephonyManager getTelephonyManager(Context context) {
        if (sTelephonyManager == null) {
            sTelephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        }
        return sTelephonyManager;
    }

    /**
     * Format a number as a phone number.
     */
    public static String getFormattedNumber(Context context, String number) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "getFormattedNumber: " + number);
        }
        if (number == null) {
            return "";
        }

        String countryIso = getIsoDefaultCountryNumber(context);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "PhoneNumberUtils.formatNumberToE16, number: "
                    + number + ", country: " + countryIso);
        }
        String e164 = PhoneNumberUtils.formatNumberToE164(number, countryIso);
        String formattedNumber = PhoneNumberUtils.formatNumber(number, e164, countryIso);
        formattedNumber = TextUtils.isEmpty(formattedNumber) ? number : formattedNumber;
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "getFormattedNumber, result: " + formattedNumber);
        }
        return formattedNumber;
    }

    private static String getIsoDefaultCountryNumber(Context context) {
        String countryIso = getTelephonyManager(context).getSimCountryIso().toUpperCase(Locale.US);
        if (countryIso.length() != 2) {
            countryIso = Locale.getDefault().getCountry();
            if (countryIso == null || countryIso.length() != 2) {
                countryIso = "US";
            }
        }

        return countryIso;
    }

    /**
     * Creates a new instance of {@link Phonenumber#Phonenumber} base on the given number and sim
     * card country code. Returns {@code null} if the number in an invalid number.
     */
    @Nullable
    public static Phonenumber.PhoneNumber createI18nPhoneNumber(Context context, String number) {
        try {
            return PhoneNumberUtil.getInstance().parse(number, getIsoDefaultCountryNumber(context));
        } catch (NumberParseException e) {
            return null;
        }
    }

    /**
     * Get the display name of the given number (e.g. if it's the voicemail number, return a string
     * that represents voicemail, if it's a contact, get the contact's name, etc).
     */
    public static String getDisplayName(Context context, String number) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "getDisplayName: " + number);
        }

        if (TextUtils.isEmpty(number)) {
            return context.getString(R.string.unknown);
        }

        ContentResolver cr = context.getContentResolver();
        String name;
        if (number.equals(getVoicemailNumber(context))) {
            name = context.getResources().getString(R.string.voicemail);
        } else {
            name = getContactNameFromNumber(cr, number);
        }

        if (name == null) {
            name = getFormattedNumber(context, number);
        }

        if (name == null) {
            name = context.getString(R.string.unknown);
        }
        return name;
    }

    private static String getContactNameFromNumber(ContentResolver cr, String number) {
        Uri uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        Cursor cursor = null;
        String name = null;
        try {
            cursor = cr.query(uri,
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                name = cursor.getString(0);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return name;
    }

    /**
     * @return A formatted string that has information about the phone call
     * Possible strings:
     * "Mobile · Dialing"
     * "Mobile · 1:05"
     * "Bluetooth disconnected"
     */
    public static String getCallInfoText(Context context, CallDetail callDetail, int callState,
            String number) {
        CharSequence label = TelecomUtils.getTypeFromNumber(context, number);
        String text;
        if (callState == Call.STATE_ACTIVE) {
            long duration = callDetail.getConnectTimeMillis() > 0 ? System.currentTimeMillis()
                    - callDetail.getConnectTimeMillis() : 0;
            String durationString = DateUtils.formatElapsedTime(duration / 1000);
            if (!TextUtils.isEmpty(durationString) && !TextUtils.isEmpty(label)) {
                text = context.getString(R.string.phone_label_with_info, label, durationString);
            } else if (!TextUtils.isEmpty(durationString)) {
                text = durationString;
            } else if (!TextUtils.isEmpty(label)) {
                text = (String) label;
            } else {
                text = "";
            }
        } else {
            String state = callStateToUiString(context, callState);
            if (!TextUtils.isEmpty(label)) {
                text = context.getString(R.string.phone_label_with_info, label, state);
            } else {
                text = state;
            }
        }
        return text;
    }

    /**
     * @return A string representation of the call state that can be presented to a user.
     */
    public static String callStateToUiString(Context context, int state) {
        Resources res = context.getResources();
        switch (state) {
            case Call.STATE_ACTIVE:
                return res.getString(R.string.call_state_call_active);
            case Call.STATE_HOLDING:
                return res.getString(R.string.call_state_hold);
            case Call.STATE_NEW:
            case Call.STATE_CONNECTING:
                return res.getString(R.string.call_state_connecting);
            case Call.STATE_SELECT_PHONE_ACCOUNT:
            case Call.STATE_DIALING:
                return res.getString(R.string.call_state_dialing);
            case Call.STATE_DISCONNECTED:
                return res.getString(R.string.call_state_call_ended);
            case Call.STATE_RINGING:
                return res.getString(R.string.call_state_call_ringing);
            case Call.STATE_DISCONNECTING:
                return res.getString(R.string.call_state_call_ending);
            default:
                throw new IllegalStateException("Unknown Call State: " + state);
        }
    }

    /**
     * Returns true if the telephony network is available.
     */
    public static boolean isNetworkAvailable(Context context) {
        TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getNetworkType() != TelephonyManager.NETWORK_TYPE_UNKNOWN
                && tm.getSimState() == TelephonyManager.SIM_STATE_READY;
    }

    /**
     * Returns true if airplane mode is on.
     */
    public static boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    /**
     * Sets a Contact avatar onto the provided {@code icon}. The first letter of the contact's
     * display name or {@code fallbackDisplayName} will be used as a fallback resource if avatar
     * loading fails.
     */
    public static void setContactBitmapAsync(
            Context context,
            final ImageView icon,
            @Nullable final Contact contact,
            @Nullable final String fallbackDisplayName) {
        Uri avatarUri = contact != null ? contact.getAvatarUri() : null;
        String displayName = contact != null ? contact.getDisplayName() : fallbackDisplayName;

        setContactBitmapAsync(context, icon, avatarUri, displayName);
    }

    /**
     * Sets a Contact avatar onto the provided {@code icon}. The first letter of the contact's
     * display name will be used as a fallback resource if avatar loading fails.
     */
    public static void setContactBitmapAsync(
            Context context,
            final ImageView icon,
            final Uri avatarUri,
            final String displayName) {
        LetterTileDrawable letterTileDrawable = new LetterTileDrawable(context.getResources());
        letterTileDrawable.setIsCircular(true);

        if (avatarUri != null) {
            Glide.with(context)
                    .load(avatarUri)
                    .apply(new RequestOptions().circleCrop().error(letterTileDrawable))
                    .into(icon);
            return;
        }

        // Use the letter tile as avatar if there is no avatar available from content provider.
        letterTileDrawable.setContactDetails(displayName, displayName);
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setImageDrawable(letterTileDrawable);
    }

    /** Set the given phone number as the primary phone number for its associated contact. */
    public static void setAsPrimaryPhoneNumber(Context context, PhoneNumber phoneNumber) {
        // Update the primary values in the data record.
        ContentValues values = new ContentValues(1);
        values.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
        values.put(ContactsContract.Data.IS_PRIMARY, 1);

        context.getContentResolver().update(
                ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, phoneNumber.getId()),
                values, null, null);
    }

    /** Add a contact to favorite or remove it from favorite. */
    public static int setAsFavoriteContact(Context context, Contact contact, boolean isFavorite) {
        if (contact.isStarred() == isFavorite) {
            return 0;
        }

        ContentValues values = new ContentValues(1);
        values.put(ContactsContract.Contacts.STARRED, isFavorite ? 1 : 0);

        String where = ContactsContract.Contacts._ID + " = ?";
        String[] selectionArgs = new String[]{Long.toString(contact.getId())};
        return context.getContentResolver().update(ContactsContract.Contacts.CONTENT_URI, values,
                where, selectionArgs);
    }

}
