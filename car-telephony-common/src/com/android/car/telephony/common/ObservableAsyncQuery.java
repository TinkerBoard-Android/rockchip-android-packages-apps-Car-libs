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

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Asynchronously queries data and observes them. A new query will be triggered automatically if
 * data set have changed.
 */
public class ObservableAsyncQuery {
    private static final int QUERY_TOKEN = 0;

    /**
     * Represents query parameters.
     */
    public static class QueryParam {
        final Uri mUri;
        final String[] mProjection;
        final String mSelection;
        final String[] mSelectionArgs;
        final String mOrderBy;

        public QueryParam(
                @NonNull Uri uri,
                @Nullable String[] projection,
                @Nullable String selection,
                @Nullable String[] selectionArgs,
                @Nullable String orderBy) {
            mUri = uri;
            mProjection = projection;
            mSelection = selection;
            mSelectionArgs = selectionArgs;
            mOrderBy = orderBy;
        }
    }

    /**
     * Called when query is finished.
     */
    public interface OnQueryFinishedListener {
        /**
         * Called when the query is finished loading. This callbacks will also be called if data
         * changed.
         *
         * <p>Called on main thread.
         */
        @MainThread
        void onQueryFinished(Cursor cursor);
    }

    private AsyncQueryHandler mAsyncQueryHandler;
    private ObservableAsyncQuery.QueryParam mQueryParam;
    private Cursor mCurrentCursor;
    private ObservableAsyncQuery.OnQueryFinishedListener mOnQueryFinishedListener;
    private ContentObserver mContentObserver;
    private boolean mIsActive = false;

    /**
     * @param queryParam Query arguments for the current query.
     * @param listener   Listener which will be called when data is available.
     */
    public ObservableAsyncQuery(
            @NonNull ObservableAsyncQuery.QueryParam queryParam,
            @NonNull ContentResolver cr,
            @NonNull ObservableAsyncQuery.OnQueryFinishedListener listener) {
        mAsyncQueryHandler = new ObservableAsyncQuery.AsyncQueryHandlerImpl(this, cr);
        mContentObserver = new ContentObserver(mAsyncQueryHandler) {
            @Override
            public void onChange(boolean selfChange) {
                startQuery();
            }
        };
        mQueryParam = queryParam;
        mOnQueryFinishedListener = listener;
    }

    /**
     * Starts the query and stops any pending query.
     */
    public void startQuery() {
        mAsyncQueryHandler.cancelOperation(QUERY_TOKEN);
        mAsyncQueryHandler.startQuery(QUERY_TOKEN, null,
                mQueryParam.mUri,
                mQueryParam.mProjection,
                mQueryParam.mSelection,
                mQueryParam.mSelectionArgs,
                mQueryParam.mOrderBy);
        mIsActive = true;
    }

    /**
     * Stops any pending query and also stops listening on the data set change.
     */
    public void stopQuery() {
        mIsActive = false;
        closeCurrentCursorIfNecessary();
        mAsyncQueryHandler.cancelOperation(QUERY_TOKEN);
    }

    private void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if (!mIsActive) {
            return;
        }
        closeCurrentCursorIfNecessary();
        if (cursor != null) {
            cursor.registerContentObserver(mContentObserver);
            mCurrentCursor = cursor;
        }
        if (mOnQueryFinishedListener != null) {
            mOnQueryFinishedListener.onQueryFinished(cursor);
        }
    }

    private void closeCurrentCursorIfNecessary() {
        if (mCurrentCursor != null && !mCurrentCursor.isClosed()) {
            mCurrentCursor.close();
        }
        mCurrentCursor = null;
    }

    private static class AsyncQueryHandlerImpl extends AsyncQueryHandler {
        private ObservableAsyncQuery mQuery;

        AsyncQueryHandlerImpl(ObservableAsyncQuery query, ContentResolver cr) {
            super(cr);
            mQuery = query;
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            super.onQueryComplete(token, cookie, cursor);
            mQuery.onQueryComplete(token, cookie, cursor);
        }
    }
}
