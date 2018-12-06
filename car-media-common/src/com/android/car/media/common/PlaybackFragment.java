/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.car.media.common;

import static com.android.car.arch.common.LiveDataFunctions.mapNonNull;

import android.app.Application;
import android.car.Car;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;

import com.android.car.media.common.playback.AlbumArtLiveData;
import com.android.car.media.common.playback.PlaybackViewModel;
import com.android.car.media.common.source.MediaSource;
import com.android.car.media.common.source.MediaSourceViewModel;

import com.bumptech.glide.request.target.Target;

/**
 * {@link Fragment} that can be used to display and control the currently playing media item. Its
 * requires the android.Manifest.permission.MEDIA_CONTENT_CONTROL permission be held by the hosting
 * application.
 */
public class PlaybackFragment extends Fragment {

    private MediaSourceViewModel mMediaSourceViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        FragmentActivity activity = requireActivity();
        PlaybackViewModel playbackViewModel =
                ViewModelProviders.of(activity).get(PlaybackViewModel.class);
        mMediaSourceViewModel = ViewModelProviders.of(activity).get(
                MediaSourceViewModel.class);
        playbackViewModel.setMediaController(mMediaSourceViewModel.getMediaController());

        ViewModel innerViewModel = ViewModelProviders.of(activity).get(ViewModel.class);
        innerViewModel.init(mMediaSourceViewModel, playbackViewModel);

        View view = inflater.inflate(R.layout.playback_fragment, container, false);

        PlaybackControls playbackControls = view.findViewById(R.id.playback_controls);
        playbackControls.setModel(playbackViewModel, getViewLifecycleOwner());

        ImageView appIcon = view.findViewById(R.id.app_icon);
        innerViewModel.getAppIcon().observe(getViewLifecycleOwner(), appIcon::setImageBitmap);

        TextView appName = view.findViewById(R.id.app_name);
        innerViewModel.getAppName().observe(getViewLifecycleOwner(), appName::setText);

        TextView title = view.findViewById(R.id.title);
        innerViewModel.getTitle().observe(getViewLifecycleOwner(), title::setText);

        TextView subtitle = view.findViewById(R.id.subtitle);
        innerViewModel.getSubtitle().observe(getViewLifecycleOwner(), subtitle::setText);

        CrossfadeImageView albumBackground = view.findViewById(R.id.album_background);
        innerViewModel.getAlbumArt().observe(getViewLifecycleOwner(),
                albumArt -> albumBackground.setImageBitmap(albumArt, true));
        LiveData<Intent> openIntent = innerViewModel.getOpenIntent();
        openIntent.observe(getViewLifecycleOwner(), intent -> {
            // Ensure open intent data stays fresh while view is clickable.
        });
        albumBackground.setOnClickListener(v -> {
            Intent intent = openIntent.getValue();
            if (intent != null
                    && intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMediaSourceViewModel.setSelectedMediaSource(getSelectedSourceFromContentProvider());
    }

    private MediaSource getSelectedSourceFromContentProvider() {
        Cursor cursor = getContext().getContentResolver().query(MediaConstants.URI_MEDIA_SOURCE,
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            return new MediaSource(requireActivity(), cursor.getString(0));
        }
        return null;
    }

    /**
     * ViewModel for the PlaybackFragment
     */
    public static class ViewModel extends AndroidViewModel {

        private static final Intent MEDIA_TEMPLATE_INTENT =
                new Intent(Car.CAR_INTENT_ACTION_MEDIA_TEMPLATE);

        private LiveData<MediaSource> mMediaSource;
        private LiveData<CharSequence> mAppName;
        private LiveData<Bitmap> mAppIcon;
        private LiveData<Intent> mOpenIntent;
        private LiveData<CharSequence> mTitle;
        private LiveData<CharSequence> mSubtitle;
        private LiveData<Bitmap> mAlbumArt;

        private PlaybackViewModel mPlaybackViewModel;
        private MediaSourceViewModel mMediaSourceViewModel;

        public ViewModel(Application application) {
            super(application);
        }

        void init(MediaSourceViewModel mediaSourceViewModel, PlaybackViewModel playbackViewModel) {
            if (mMediaSourceViewModel == mediaSourceViewModel
                    && mPlaybackViewModel == playbackViewModel) {
                return;
            }
            mPlaybackViewModel = playbackViewModel;
            mMediaSourceViewModel = mediaSourceViewModel;
            mMediaSource = mMediaSourceViewModel.getSelectedMediaSource();
            mAppName = mapNonNull(mMediaSource, MediaSource::getName);
            mAppIcon = mapNonNull(mMediaSource, MediaSource::getRoundPackageIcon);
            mOpenIntent = mapNonNull(mMediaSource, MEDIA_TEMPLATE_INTENT, source -> {
                if (source.isCustom()) {
                    // We are playing a custom app. Jump to it, not to the template
                    return getApplication().getPackageManager()
                            .getLaunchIntentForPackage(source.getPackageName());
                } else {
                    // We are playing a standard app. Open the template to browse it.
                    Intent intent = new Intent(Car.CAR_INTENT_ACTION_MEDIA_TEMPLATE);
                    intent.putExtra(Car.CAR_EXTRA_MEDIA_PACKAGE, source.getPackageName());
                    return intent;
                }
            });
            mTitle = mapNonNull(playbackViewModel.getMetadata(), MediaItemMetadata::getTitle);
            mSubtitle = mapNonNull(playbackViewModel.getMetadata(), MediaItemMetadata::getSubtitle);
            mAlbumArt = AlbumArtLiveData.getAlbumArt(getApplication(),
                    Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL, false,
                    playbackViewModel.getMetadata());
        }

        LiveData<CharSequence> getAppName() {
            return mAppName;
        }

        LiveData<Bitmap> getAppIcon() {
            return mAppIcon;
        }

        LiveData<Intent> getOpenIntent() {
            return mOpenIntent;
        }

        LiveData<CharSequence> getTitle() {
            return mTitle;
        }

        LiveData<CharSequence> getSubtitle() {
            return mSubtitle;
        }

        LiveData<Bitmap> getAlbumArt() {
            return mAlbumArt;
        }
    }
}
