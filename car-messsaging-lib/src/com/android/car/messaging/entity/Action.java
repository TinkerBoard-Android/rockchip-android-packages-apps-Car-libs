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

package com.android.car.messaging.entity;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.os.Bundle;
import android.os.Parcel;

import androidx.annotation.Nullable;

/**
 * Semantic actions that 3p can perform.
 */
public class Action extends BaseEntity {
    private String mSemanticAction;
    private PendingIntent mCallback;
    private Bundle mExtra;
    private RemoteInput mRemoteInput;

    public static final Creator<Action> CREATOR = new Creator<Action>() {
        @Override
        public Action createFromParcel(Parcel in) {
            return new Action(in);
        }

        @Override
        public Action[] newArray(int size) {
            return new Action[size];
        }
    };

    protected Action() {
        super();
    }

    protected Action(Parcel in) {
        super(in);
        mSemanticAction = in.readString();
        mCallback = in.readParcelable(PendingIntent.class.getClassLoader());
        mRemoteInput = in.readParcelable(RemoteInput.class.getClassLoader());
        mExtra = in.readBundle(getClass().getClassLoader());
    }

    /**
     * Returns the semantic action.
     */
    @Nullable
    public String getSemanticAction() {
        return mSemanticAction;
    }

    /**
     * Returns the callback which will be fired when the action is triggered.
     */
    public PendingIntent getCallback() {
        return mCallback;
    }

    /**
     * Returns the extra which helps to fulfill the action.
     */
    public Bundle getExtra() {
        return mExtra;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSemanticAction);
        dest.writeValue(mCallback);
        dest.writeValue(mRemoteInput);
        dest.writeBundle(mExtra);
    }

    /**
     * Builder for {@link Action}.
     */
    public static class Builder {
        private String mSemanticAction;
        private PendingIntent mCallback;
        private Bundle mExtra;

        public void setSemanticAction(String semanticAction) {
            mSemanticAction = semanticAction;
        }

        public void setCallback(PendingIntent callback) {
            mCallback = callback;
        }

        public void setExtra(Bundle extra) {
            mExtra = extra;
        }

        /**
         * Build {@link Action}.
         */
        public Action build() {
            Action action = new Action();
            action.mCallback = mCallback;
            action.mExtra = mExtra;
            action.mSemanticAction = mSemanticAction;
            return action;
        }
    }
}
