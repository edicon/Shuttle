/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package com.simplecity.amp_library.playback.mediaplayers;

import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.core.CrashlyticsCore;
import com.simplecity.amp_library.playback.MusicService;
import com.simplecity.amp_library.playback.mediaplayers.PrefUtils.PreferenceUtils;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.AndroidUtil;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * This class wraps a libvlc mediaplayer instance.
 */
public class VLCMediaPlayer extends UniformMediaPlayer {

    private static final String TAG = VLCMediaPlayer.class.getSimpleName();

    private final WeakReference<MusicService> mService;
    private static MediaPlayer mCurrentMediaPlayer;
    private MediaPlayer mNextMediaPlayer;
    private android.media.MediaPlayer powerMediaPlayer = new android.media.MediaPlayer(); // ToDo: for fake
    private Handler mHandler;
    private boolean mIsInitialized = false;

    // private UniformMediaPlayerCallback mMediaPlayerCallback;

    private static LibVLC sLibVLC;

    public VLCMediaPlayer(final MusicService service) {
        mService = new WeakReference<>(service);
        powerMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);

        // ToDo: static와 우선 관계
        mCurrentMediaPlayer = new MediaPlayer(sLibVLC);
        mCurrentMediaPlayer.setEventListener( mediaPlayerListener );
        setEqualizer();

        Log.d(TAG, "VLCMediaPlayer: MediaPlayer: Created");
    }

    static {
        ArrayList<String> options = new ArrayList<>();
        options.add("--http-reconnect");
        options.add("--network-caching=2000");

        sLibVLC = new LibVLC(options);
        Log.d(TAG, "VLCMediaPlayer: Static: ver: " +  sLibVLC.version());
    }

    private static void setEqualizer() {
        if (PreferenceUtils.getBoolean(PreferenceUtils.EQUALIZER_ENABLED)) {
            MediaPlayer.Equalizer equalizer = MediaPlayer.Equalizer.create();
            float[] bands = PreferenceUtils.getFloatArray(PreferenceUtils.EQUALIZER_VALUES);
            equalizer.setPreAmp(bands[0]);
            for (int i = 0; i < MediaPlayer.Equalizer.getBandCount(); i++) {
                equalizer.setAmp(i, bands[i + 1]);
            }
            mCurrentMediaPlayer.setEqualizer(equalizer);
        }
    }

    public static LibVLC getLibVlcInstance() {
        return sLibVLC;
    }

    public static MediaPlayer getMediaPlayerInstance() {
        return mCurrentMediaPlayer;
    }

    @Override
    public void pause() {
        try {
            mCurrentMediaPlayer.pause();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error pausing MultiPlayer: " + e.getLocalizedMessage());
        }
    }

    /*
    @Override
    public boolean isPlaying() {
        try {
            return mCurrentMediaPlayer != null && mCurrentMediaPlayer.isPlaying();
        } catch (IllegalStateException e) {
            //ignored
        }
        return false;
    }
    */

    @Override
    public long getDuration() {
        try {
            return mCurrentMediaPlayer.getLength(); // .getDuration();
        } catch (IllegalStateException ignored) {
            return 0;
        }
    }

    @Override
    public long getCurrentPosition() {
        try {
            // Android:MediaPlayer.getCurrentPosition();
            // VLC:MediaPlayer.getTime(); getPosition: Movie(Video)Position
            return mCurrentMediaPlayer.getTime();
        } catch (IllegalStateException ignored) {
            return 0;
        }
    }

    @Override
    public long seekTo(long whereto) {
        try {
            mCurrentMediaPlayer.setTime( whereto );     // .seekTo((int) whereto);
            // mCurrentMediaPlayer.setPosition((float)whereto );     // .seekTo((int) whereto);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error seeking MultiPlayer: " + e.getLocalizedMessage());
        }
        return whereto;
    }

    @Override
    public void setVolume(float vol) {
        try {
            // ToDo: Skip seting of Vol
            Log.e(TAG, "setVolumeL: SKIPPED  " + vol );
            // mCurrentMediaPlayer.setVolume((int) vol );  // .setVolume(vol, vol);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error setting MultiPlayer volume: " + e.getLocalizedMessage());
        }
    }

    @Override
    public int getAudioSessionId() {
        int sessionId = 0;
        try {
            // ToDo;
            sessionId = mCurrentMediaPlayer.getAudioTrack(); // .getAudioSessionId();
        } catch (IllegalStateException ignored) {
            //Nothing to do
        }
        return sessionId;
    }

    @Override
    public boolean isInitialized() {
        return mIsInitialized;
    }

    @Override
    public void start() {
        try {
            mCurrentMediaPlayer.play();     // .start();
        } catch (RuntimeException e) {
            CrashlyticsCore.getInstance().log("MusicService.start() failed. Exception: " + e.toString());
        }
    }

    @Override
    public void stop() {
        try {
            // ToDo;
            mCurrentMediaPlayer.stop();     // .reset();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error stopping MultiPlayer: " + e.getLocalizedMessage());
            CrashlyticsCore.getInstance().log("stop() failed. Error: " + e.getLocalizedMessage());
        }
        mIsInitialized = false;
    }

    /**
     * Prepare the given url
    @Override
    public void prepare(final Query query, UniformMediaPlayerCallback callback) {
        Log.d(TAG, "prepare() query: " + query);
        mMediaPlayerCallback = callback;
        getMediaPlayerInstance().stop();
        mPreparedQuery = null;
        mPreparingQuery = query;
        getStreamUrl(query.getPreferredTrackResult()).done(new DoneCallback<String>() {
            @Override
            public void onDone(String url) {
                Log.d(TAG, "Received stream url: " + url + " for query: " + query);
                if (mPreparingQuery != null && mPreparingQuery == query) {
                    Log.d(TAG, "Starting to prepare stream url: " + url + " for query: " + query);
                    Media media = new Media(sLibVLC, AndroidUtil.LocationToUri(url));
                    getMediaPlayerInstance().setMedia(media);
                    mPreparedQuery = mPreparingQuery;
                    mPreparingQuery = null;
                    mMediaPlayerCallback.onPrepared(VLCMediaPlayer.this, mPreparedQuery);
                    handlePlayState();
                    Log.d(TAG, "onPrepared() url: " + url + " for query: " + query);
                } else {
                    Log.d(TAG, "Ignoring stream url: " + url + " for query: " + query
                            + ", because preparing query is: " + mPreparingQuery);
                }
            }
        });
    }
     */

    @Override
    public void release() {
        stop();
        // ToDO: check resource release
        // mCurrentMediaPlayer.release();
    }


    @Override
    public void setBitrate(int mode) {
        // ToDo:
        mCurrentMediaPlayer.setRate((float)mode);
    }

    @Override
    public void setDataSource(String path) {
        mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, path);
        if (mIsInitialized) {
            setNextDataSource(null);
        }
    }

    private boolean setDataSourceImpl(final MediaPlayer mediaPlayer, final String path) {
        if (TextUtils.isEmpty(path) || mediaPlayer == null) {
            return false;
        }

        getMediaPlayerInstance().stop();

        // ToDo: Check path or uri
        Uri uri = AndroidUtil.PathToUri(path);
        Log.d(TAG, "setDataSourceImpl: path: " + path + "\n Uri: "  + uri.toString() );

        Media media = new Media(sLibVLC, uri );
        // Media media = new Media(sLibVLC, AndroidUtil.LocationToUri(path));
        mCurrentMediaPlayer.setMedia(media);
        mCurrentMediaPlayer.setEventListener( mediaPlayerListener);
        mCurrentMediaPlayer.play();

        debugPlayer( mCurrentMediaPlayer );
        debugMedia( media );

        /*
        try {
            mediaPlayer.reset();
            mediaPlayer.setOnPreparedListener(null);
            if (path.startsWith("content://")) {
                Uri uri = Uri.parse(path);
                mediaPlayer.setDataSource(mService.get(), uri);
            } else {
                mediaPlayer.setDataSource(path);
            }
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepare();
        } catch (final Exception e) {
            Log.e(TAG, "setDataSource failed: " + e.getLocalizedMessage());
            CrashlyticsCore.getInstance().log("setDataSourceImpl failed. Path: [" + path + "] error: " + e.getLocalizedMessage());
            return false;
        }
        mediaPlayer.setOnCompletionListener(completionListener);
        mediaPlayer.setOnErrorListener(errorListener);
        */

        return true;
    }

    public void setNextDataSource(final String path) {
        try {
            // ToDo: MediaListPlayer
            // mCurrentMediaPlayer.setNextMediaPlayer(null);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Next media player is current one, continuing");
        } catch (IllegalStateException e) {
            Log.e(TAG, "Media player not initialized!");
            CrashlyticsCore.getInstance().log("setNextDataSource failed for. Media player not intitialized.");
            return;
        }
        if (mNextMediaPlayer != null) {
            mNextMediaPlayer.release(); // ToDo: Check release or stop
            mNextMediaPlayer = null;
        }
        if (TextUtils.isEmpty(path)) {
            return;
        }
        mNextMediaPlayer = new MediaPlayer(sLibVLC);
        powerMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
        // ToDo:
        // mNextMediaPlayer.setAudioSessionId(getAudioSessionId());
        if (setDataSourceImpl(mNextMediaPlayer, path)) {
            try {
                // mCurrentMediaPlayer.setNextMediaPlayer(mNextMediaPlayer);
            } catch (Exception e) {
                Log.e(TAG, "setNextDataSource failed - failed to call setNextMediaPlayer on mCurrentMediaPlayer. Error: " + e.getLocalizedMessage());
                CrashlyticsCore.getInstance().log("setNextDataSource failed - failed to call setNextMediaPlayer on mCurrentMediaPlayer. Error: " + e.getLocalizedMessage());
                if (mNextMediaPlayer != null) {
                    mNextMediaPlayer.release();
                    mNextMediaPlayer = null;
                }
            }
        } else {
            Log.e(TAG, "setDataSourceImpl failed for path: [" + path + "]. Setting next media player to null");
            CrashlyticsCore.getInstance().log("setDataSourceImpl failed for path: [" + path + "]. Setting next media player to null");
            if (mNextMediaPlayer != null) {
                mNextMediaPlayer.release();
                mNextMediaPlayer = null;
            }
        }
    }

    @Override
    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    private MediaPlayer.EventListener  mediaPlayerListener = new MediaPlayer.EventListener() {
        @Override
        public void onEvent(final MediaPlayer.Event event) {
            String x = String.format("%04x", event.type);
            Log.d(TAG, "onEvent.type: [" + x + "]");
            switch (event.type) {
                case MediaPlayer.Event.MediaChanged:    // 0x100
                    break;
                case MediaPlayer.Event.Opening:
                    break;
                case MediaPlayer.Event.Playing:         // 0x104
                    break;
                case MediaPlayer.Event.Paused:          // 0x105
                    break;
                case MediaPlayer.Event.Stopped:         // 0x106
                    break;
                case MediaPlayer.Event.TimeChanged:     // 0x10b
                    break;
                case MediaPlayer.Event.PositionChanged: // 0x10c
                    break;
                case MediaPlayer.Event.SeekableChanged:
                    break;
                case MediaPlayer.Event.ESAdded:         // 0x114/115
                case MediaPlayer.Event.ESDeleted:
                    break;
                case MediaPlayer.Event.EndReached:
                    if ( mCurrentMediaPlayer != null && mNextMediaPlayer != null) {
                        mCurrentMediaPlayer.release();
                        mCurrentMediaPlayer = mNextMediaPlayer;
                        mNextMediaPlayer = null;
                        mHandler.sendEmptyMessage(MusicService.PlayerHandler.TRACK_WENT_TO_NEXT);
                    } else {
                        mService.get().mWakeLock.acquire(30000);
                        mHandler.sendEmptyMessage(MusicService.PlayerHandler.TRACK_ENDED);
                        mHandler.sendEmptyMessage(MusicService.PlayerHandler.RELEASE_WAKELOCK);
                    }
                    break;
                case MediaPlayer.Event.EncounteredError:
                    mIsInitialized = false;
                    mCurrentMediaPlayer.release();
                    mCurrentMediaPlayer = new MediaPlayer(sLibVLC);
                    // ToDo;
                    powerMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MusicService.PlayerHandler.SERVER_DIED), 2000);
                    break;
                default:
                    Log.d(TAG, "onEvent.type(UnKnown): [" + x + "]");
                    break;
            }
        }
    };

    private void debugMedia( Media media ) {
        Log.d( TAG, "Media :" + "\n" +
            "getDuration: " + media.getDuration()     + "/" +
            "getState: "    + media.getState()        + "/" +
            "getTrackCount: " + media.getTrackCount() + "/" +
            "getType: "     + media.getType()         + "/" +
            "getUri: "      + media.getUri()          + "/" +
            "isParsed: "    + media.isParsed()        + "/" +
            "isReleased: "  + media.isReleased()
        );
    }

    private void debugPlayer( MediaPlayer player ) {
        Log.d(TAG, "Player :" + "\n" +
            "getLength: "           + player.getLength()        + "/" +
            "isReleased: "          + player.isReleased()       + "/" +
            "getPosition: "         + player.getPosition()      + "/" +
            "getAudioTrack: "       + player.getAudioTrack()    + "/" +
            "getAudioTracksCount: " + player.getAudioTracksCount() + "/" +
            "getTime: "             + player.getTime()          + "/" +
            "getPlayerState: "      + player.getPlayerState()   + "/" +
            "getRate: "             + player.getRate()          + "/" +
            "isReleased: "          + player.isReleased()
        );
    }
}
