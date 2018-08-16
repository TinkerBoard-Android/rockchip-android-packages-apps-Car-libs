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
import android.graphics.Bitmap;
import android.media.session.MediaController;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProviders;

import com.android.car.media.common.playback.AlbumArtLiveData;
import com.android.car.media.common.playback.PlaybackViewModel;

import com.bumptech.glide.request.target.Target;

/**
 * {@link Fragment} that can be used to display and control the currently playing media item. Its
 * requires the android.Manifest.permission.MEDIA_CONTENT_CONTROL permission be held by the hosting
 * application.
 */
public class PlaybackFragment extends Fragment {
    // TODO(keyboardr): replace with MediaSourceViewModel when available
    private ActiveMediaSourceManager mActiveMediaSourceManager;

    private MutableLiveData<MediaController> mMediaController = new MutableLiveData<>();


    private ActiveMediaSourceManager.Observer mActiveSourceObserver =
            new ActiveMediaSourceManager.Observer() {
                @Override
                public void onActiveSourceChanged() {
                    mMediaController.setValue(mActiveMediaSourceManager.getMediaController());
                }
            };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        PlaybackViewModel playbackViewModel = ViewModelProviders.of(getActivity())
                .get(PlaybackViewModel.class);
        playbackViewModel.setMediaController(mMediaController);
        ViewModel innerViewModel = ViewModelProviders.of(getActivity()).get(ViewModel.class);
        innerViewModel.init(playbackViewModel);

        View view = inflater.inflate(R.layout.car_playback_fragment, container, false);
        mActiveMediaSourceManager = new ActiveMediaSourceManager(getContext());

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
    public void onStart() {
        super.onStart();
        mActiveMediaSourceManager.registerObserver(mActiveSourceObserver);
    }

    @Override
    public void onStop() {
        super.onStop();
        mActiveMediaSourceManager.unregisterObserver(mActiveSourceObserver);
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

        public ViewModel(Application application) {
            super(application);
        }

        void init(PlaybackViewModel playbackViewModel) {
            if (mPlaybackViewModel == playbackViewModel) {
                return;
            }
            mPlaybackViewModel = playbackViewModel;
            mMediaSource = mapNonNull(playbackViewModel.getMediaController(),
                    controller -> new MediaSource(getApplication(), controller.getPackageName()));
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
