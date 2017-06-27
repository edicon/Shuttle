package com.simplecity.amp_library.playback.mediaplayers;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.core.CrashlyticsCore;
import com.simplecity.amp_library.playback.MusicService;

import java.lang.ref.WeakReference;

/**
 * Provides a unified interface for dealing with midi files and other media
 * files.
 */
public class WavevinePlayer
// implements
//         MediaPlayer.OnErrorListener,
//         MediaPlayer.OnCompletionListener
{

    private static final String TAG = "WavevinePlayer";

    // private final WeakReference<MusicService> mService;
    // private MediaPlayer mCurrentMediaPlayer = new MediaPlayer();
    // private MediaPlayer mNextMediaPlayer;
    // private Handler mHandler;
    // private boolean mIsInitialized = false;
    private UniformMediaPlayer mCurrentMediaPlayer; //  = AndroidMediaPlayer(); // new MediaPlayer();

    public WavevinePlayer(final MusicService service) {
        // mService = new WeakReference<>(service);
        // mCurrentMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
        mCurrentMediaPlayer = new AndroidMediaPlayer( service );
    }

    public void setDataSource(final String path) {
        mCurrentMediaPlayer.setDataSource( path );
        // mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, path);
        // if (mIsInitialized) {
        //     setNextDataSource(null);
       //  }
    }

    public void setNextDataSource(final String path) {
        mCurrentMediaPlayer.setNextDataSource( path );
    }

    public boolean isInitialized() {
        return mCurrentMediaPlayer.isInitialized();
        // return mIsInitialized;
    }

    public void start() {
        mCurrentMediaPlayer.start();
        // try {
        //     mCurrentMediaPlayer.start();
        // } catch (RuntimeException e) {
        //     CrashlyticsCore.getInstance().log("MusicService.start() failed. Exception: " + e.toString());
        // }
    }

    public void stop() {
        mCurrentMediaPlayer.stop();
        /*
        try {
            mCurrentMediaPlayer.reset();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error stopping MultiPlayer: " + e.getLocalizedMessage());
            CrashlyticsCore.getInstance().log("stop() failed. Error: " + e.getLocalizedMessage());
        }
        mIsInitialized = false;
        */
    }

    /**
     * You CANNOT use this player anymore after calling release()
     */
    public void release() {
        mCurrentMediaPlayer.release();
        /*
        stop();
        mCurrentMediaPlayer.release();
        */
    }

    public void pause() {
        mCurrentMediaPlayer.pause();
        /*
        try {
            mCurrentMediaPlayer.pause();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error pausing MultiPlayer: " + e.getLocalizedMessage());
        }
        */
    }

    public void setHandler(Handler handler) {
        mCurrentMediaPlayer.setHandler(handler);
        // mHandler = handler;
    }

    public long getDuration() {
        return mCurrentMediaPlayer.getDuration();
        /*
        try {
            return mCurrentMediaPlayer.getDuration();
        } catch (IllegalStateException ignored) {
            return 0;
        }
        */
    }

    public long getPosition() {
        return mCurrentMediaPlayer.getCurrentPosition();
        /*
        try {
            return mCurrentMediaPlayer.getCurrentPosition();
        } catch (IllegalStateException ignored) {
            return 0;
        }
        */
    }

    public long seekTo(long whereto) {
        return mCurrentMediaPlayer.seekTo((int) whereto);
        /*
        try {
            mCurrentMediaPlayer.seekTo((int) whereto);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error seeking MultiPlayer: " + e.getLocalizedMessage());
        }
        return whereto;
        */
    }

    public void setVolume(float vol) {
        mCurrentMediaPlayer.setVolume(vol);
        /*
        try {
            mCurrentMediaPlayer.setVolume(vol, vol);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error setting MultiPlayer volume: " + e.getLocalizedMessage());
        }
        */
    }

    public int getAudioSessionId() {
        return mCurrentMediaPlayer.getAudioSessionId();
        /*
        int sessionId = 0;
        try {
            sessionId = mCurrentMediaPlayer.getAudioSessionId();
        } catch (IllegalStateException ignored) {
            //Nothing to do
        }
        return sessionId;
        */
    }

    /*
    @Override
    public boolean onError(final MediaPlayer mp, final int what, final int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                mIsInitialized = false;
                mCurrentMediaPlayer.release();
                mCurrentMediaPlayer = new MediaPlayer();
                mCurrentMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MusicService.PlayerHandler.SERVER_DIED), 2000);
                return true;
            default:
                break;
        }
        return false;
    }

    @Override
    public void onCompletion(final MediaPlayer mp) {
        if (mp == mCurrentMediaPlayer && mNextMediaPlayer != null) {
            mCurrentMediaPlayer.release();
            mCurrentMediaPlayer = mNextMediaPlayer;
            mNextMediaPlayer = null;
            mHandler.sendEmptyMessage(MusicService.PlayerHandler.TRACK_WENT_TO_NEXT);
        } else {
            mService.get().mWakeLock.acquire(30000);
            mHandler.sendEmptyMessage(MusicService.PlayerHandler.TRACK_ENDED);
            mHandler.sendEmptyMessage(MusicService.PlayerHandler.RELEASE_WAKELOCK);
        }
    }
    */
}
