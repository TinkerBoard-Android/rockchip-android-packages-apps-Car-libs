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

package com.android.car.messenger.common;

import static com.android.car.apps.common.util.SafeLog.logw;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothMapClient;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.android.car.apps.common.LetterTileDrawable;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg.AvatarIconSync;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg.ConversationNotification;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg.MessagingStyle;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg.MessagingStyleMessage;
import com.android.car.messenger.NotificationMsgProto.NotificationMsg.Person;
import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Utils methods for the car-messenger-common lib. **/
public class Utils {
    private static final String TAG = "CMC.Utils";
    /**
     * Represents the maximum length of a message substring to be used when constructing the
     * message's unique handle/key.
     */
    private static final int MAX_SUB_MESSAGE_LENGTH = 5;

    /** The Regex format of a telephone number in a {@link BluetoothMapClient} contact URI. **/
    private static final String MAP_CLIENT_URI_REGEX = "tel:(.+)";

    /** The starting substring index for a string formatted with the MAP_CLIENT_URI_REGEX above. **/
    private static final int MAP_CLIENT_URI_PHONE_NUMBER_SUBSTRING_INDEX = 4;

    /** Gets the latest message for a {@link NotificationMsg} Conversation. **/
    public static MessagingStyleMessage getLatestMessage(
            ConversationNotification notification) {
        MessagingStyle messagingStyle = notification.getMessagingStyle();
        long latestTime = 0;
        MessagingStyleMessage latestMessage = null;

        for (MessagingStyleMessage message : messagingStyle.getMessagingStyleMsgList()) {
            if (message.getTimestamp() > latestTime) {
                latestTime = message.getTimestamp();
                latestMessage = message;
            }
        }
        return latestMessage;
    }

    /**
     * Helper method to create a unique handle/key for this message. This is used as this Message's
     * {@link MessageKey#getSubKey()}.
     */
    public static String createMessageHandle(MessagingStyleMessage message) {
        String textMessage = message.getTextMessage();
        String subMessage = textMessage.substring(
                Math.min(MAX_SUB_MESSAGE_LENGTH, textMessage.length()));
        return message.getTimestamp() + "/" + message.getSender().getName() + "/" + subMessage;
    }

    /**
     * Ensure the {@link ConversationNotification} object has all the required fields.
     *
     * @param isShallowCheck should be {@code true} if the caller only wants to verify the
     *                       notification and its {@link MessagingStyle} is valid, without checking
     *                       all of the notification's {@link MessagingStyleMessage}s.
     **/
    public static boolean isValidConversationNotification(ConversationNotification notification,
            boolean isShallowCheck) {
        if (notification == null) {
            logw(TAG, "ConversationNotification is null");
            return false;
        } else if (!notification.hasMessagingStyle()) {
            logw(TAG, "ConversationNotification is missing required field: messagingStyle");
            return false;
        } else if (notification.getMessagingAppDisplayName() == null) {
            logw(TAG, "ConversationNotification is missing required field: appDisplayName");
            return false;
        } else if (notification.getMessagingAppPackageName() == null) {
            logw(TAG, "ConversationNotification is missing required field: appPackageName");
            return false;
        }
        return isValidMessagingStyle(notification.getMessagingStyle(), isShallowCheck);
    }

    /**
     * Ensure the {@link MessagingStyle} object has all the required fields.
     **/
    private static boolean isValidMessagingStyle(MessagingStyle messagingStyle,
            boolean isShallowCheck) {
        if (messagingStyle == null) {
            logw(TAG, "MessagingStyle is null");
            return false;
        } else if (messagingStyle.getConvoTitle() == null) {
            logw(TAG, "MessagingStyle is missing required field: convoTitle");
            return false;
        } else if (messagingStyle.getUserDisplayName() == null) {
            logw(TAG, "MessagingStyle is missing required field: userDisplayName");
            return false;
        } else if (messagingStyle.getMessagingStyleMsgCount() == 0) {
            logw(TAG, "MessagingStyle is missing required field: messagingStyleMsg");
            return false;
        }
        if (!isShallowCheck) {
            for (MessagingStyleMessage message : messagingStyle.getMessagingStyleMsgList()) {
                if (!isValidMessagingStyleMessage(message)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Ensure the {@link MessagingStyleMessage} object has all the required fields.
     **/
    public static boolean isValidMessagingStyleMessage(MessagingStyleMessage message) {
        if (message == null) {
            logw(TAG, "MessagingStyleMessage is null");
            return false;
        } else if (message.getTextMessage() == null) {
            logw(TAG, "MessagingStyleMessage is missing required field: textMessage");
            return false;
        } else if (!message.hasSender()) {
            logw(TAG, "MessagingStyleMessage is missing required field: sender");
            return false;
        }
        return isValidSender(message.getSender());
    }

    /**
     * Ensure the {@link Person} object has all the required fields.
     **/
    public static boolean isValidSender(Person person) {
        if (person.getName() == null) {
            logw(TAG, "Person is missing required field: name");
            return false;
        }
        return true;
    }

    /**
     * Ensure the {@link AvatarIconSync} object has all the required fields.
     **/
    public static boolean isValidAvatarIconSync(AvatarIconSync iconSync) {
        if (iconSync == null) {
            logw(TAG, "AvatarIconSync is null");
            return false;
        } else if (iconSync.getMessagingAppPackageName() == null) {
            logw(TAG, "AvatarIconSync is missing required field: appPackageName");
            return false;
        } else if (iconSync.getPerson().getName() == null) {
            logw(TAG, "AvatarIconSync is missing required field: Person's name");
            return false;
        } else if (iconSync.getPerson().getAvatar() == null) {
            logw(TAG, "AvatarIconSync is missing required field: Person's avatar");
            return false;
        }
        return true;
    }

    /**
     * Ensure the {@link BluetoothMapClient} intent has all the required fields.
     **/
    public static boolean isValidMapClientIntent(Intent intent) {
        if (intent == null) {
            logw(TAG, "BluetoothMapClient intent is null");
            return false;
        } else if (intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) == null) {
            logw(TAG, "BluetoothMapClient intent is missing required field: device");
            return false;
        } else if (intent.getStringExtra(BluetoothMapClient.EXTRA_MESSAGE_HANDLE) == null) {
            logw(TAG, "BluetoothMapClient intent is missing required field: senderName");
            return false;
        } else if (intent.getStringExtra(BluetoothMapClient.EXTRA_SENDER_CONTACT_NAME) == null) {
            logw(TAG, "BluetoothMapClient intent is missing required field: handle");
            return false;
        } else if (intent.getStringExtra(android.content.Intent.EXTRA_TEXT) == null) {
            logw(TAG, "BluetoothMapClient intent is missing required field: messageText");
            return false;
        }
        return true;
    }

    /**
     * Creates a Letter Tile Icon that will display the given initials. If the initials are null,
     * then an avatar anonymous icon will be drawn.
     **/
    public static Bitmap createLetterTile(Context context, @Nullable String initials,
            String identifier, int avatarSize, float cornerRadiusPercent) {
        // TODO(b/135446418): use TelecomUtils once car-telephony-common supports bp.
        LetterTileDrawable letterTileDrawable = createLetterTileDrawable(context, initials,
                identifier);
        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(
                context.getResources(), letterTileDrawable.toBitmap(avatarSize));
        return createFromRoundedBitmapDrawable(roundedBitmapDrawable, avatarSize,
                cornerRadiusPercent);
    }

    /** Creates an Icon based on the given roundedBitmapDrawable. **/
    private static Bitmap createFromRoundedBitmapDrawable(
            RoundedBitmapDrawable roundedBitmapDrawable, int avatarSize,
            float cornerRadiusPercent) {
        // TODO(b/135446418): use TelecomUtils once car-telephony-common supports bp.
        float radius = avatarSize * cornerRadiusPercent;
        roundedBitmapDrawable.setCornerRadius(radius);

        final Bitmap result = Bitmap.createBitmap(avatarSize, avatarSize,
                Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(result);
        roundedBitmapDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        roundedBitmapDrawable.draw(canvas);
        return roundedBitmapDrawable.getBitmap();
    }


    /**
     * Create a {@link LetterTileDrawable} for the given initials.
     *
     * @param initials   is the letters that will be drawn on the canvas. If it is null, then an
     *                   avatar anonymous icon will be drawn
     * @param identifier will decide the color for the drawable. If null, a default color will be
     *                   used.
     */
    private static LetterTileDrawable createLetterTileDrawable(
            Context context,
            @Nullable String initials,
            @Nullable String identifier) {
        // TODO(b/135446418): use TelecomUtils once car-telephony-common supports bp.
        int numberOfLetter = context.getResources().getInteger(
                R.integer.config_number_of_letters_shown_for_avatar);
        String letters = initials != null
                ? initials.substring(0, Math.min(initials.length(), numberOfLetter)) : null;
        LetterTileDrawable letterTileDrawable = new LetterTileDrawable(context.getResources(),
                letters, identifier);
        return letterTileDrawable;
    }


    /**
     * Returns the initials based on the name and nameAlt.
     *
     * @param name    should be the display name of a contact.
     * @param nameAlt should be alternative display name of a contact.
     */
    public static String getInitials(String name, String nameAlt) {
        // TODO(b/135446418): use TelecomUtils once car-telephony-common supports bp.
        StringBuilder initials = new StringBuilder();
        if (!TextUtils.isEmpty(name) && Character.isLetter(name.charAt(0))) {
            initials.append(Character.toUpperCase(name.charAt(0)));
        }
        if (!TextUtils.isEmpty(nameAlt)
                && !TextUtils.equals(name, nameAlt)
                && Character.isLetter(nameAlt.charAt(0))) {
            initials.append(Character.toUpperCase(nameAlt.charAt(0)));
        }
        return initials.toString();
    }

    /** Returns whether the {@link BluetoothMapClient} intent represents a group conversation. **/
    public static boolean isGroupConversation(Intent intent) {
        return (intent.getStringArrayExtra(Intent.EXTRA_CC) != null
                && intent.getStringArrayExtra(Intent.EXTRA_CC).length > 0);
    }

    /** Returns the list of sender uri for a {@link BluetoothMapClient} intent. **/
    public static String getSenderUri(Intent intent) {
        return intent.getStringExtra(BluetoothMapClient.EXTRA_SENDER_CONTACT_URI);
    }

    /** Returns the sender name for a {@link BluetoothMapClient} intent. **/
    public static String getSenderName(Intent intent) {
        return intent.getStringExtra(BluetoothMapClient.EXTRA_SENDER_CONTACT_NAME);
    }

    /** Returns the list of recipient uris for a {@link BluetoothMapClient} intent. **/
    public static List<String> getInclusiveRecipientsUrisList(Intent intent) {
        List<String> ccUris = new ArrayList<>();
        ccUris.add(getSenderUri(intent));
        if (isGroupConversation(intent)) {
            ccUris.addAll(Arrays.asList(intent.getStringArrayExtra(Intent.EXTRA_CC)));
            Collections.sort(ccUris);
        }
        return ccUris;
    }

    /**
     * Extracts the phone number from the {@link BluetoothMapClient} contact Uri.
     **/
    @Nullable
    public static String getPhoneNumberFromMapClient(@Nullable String senderContactUri) {
        if (senderContactUri == null || !senderContactUri.matches(MAP_CLIENT_URI_REGEX)) {
            return null;
        }

        return senderContactUri.substring(MAP_CLIENT_URI_PHONE_NUMBER_SUBSTRING_INDEX);
    }

    /** Comparator that sorts names alphabetically first, then phone numbers numerically. **/
    public static final Comparator<String> ALPHA_THEN_NUMERIC_COMPARATOR =
            new Comparator<String>() {
                private boolean isPhoneNumber(String input) {
                    PhoneNumberUtil util = PhoneNumberUtil.getInstance();
                    try {
                        Phonenumber.PhoneNumber phoneNumber = util.parse(input, /* defaultRegion */
                                null);
                        return util.isValidNumber(phoneNumber);
                    } catch (NumberParseException e) {
                        return false;
                    }
                }

                private boolean isOfSameType(String o1, String o2) {
                    boolean isO1PhoneNumber = isPhoneNumber(o1);
                    boolean isO2PhoneNumber = isPhoneNumber(o2);
                    return isO1PhoneNumber == isO2PhoneNumber;
                }

                @Override
                public int compare(String o1, String o2) {
                    // if both are names, sort based on names.
                    // if both are number, sort numerically.
                    // if one is phone number and the other is a name, give name precedence.
                    if (!isOfSameType(o1, o2)) {
                        return isPhoneNumber(o1) ? 1 : -1;
                    } else {
                        return o1.compareTo(o2);
                    }
                }
            };
}
