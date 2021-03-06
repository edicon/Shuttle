package com.simplecity.amp_library.playback.mediaplayers;

import android.os.Handler;
import android.util.Log;

import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.playback.MusicService;

/**
 * Provides a unified interface for dealing with midi files and other media
 * files.
 */
public class WavevinePlayer {

    private static final String TAG = "WavevinePlayer";

    // private final WeakReference<MusicService> mService;
    // private MediaPlayer mCurrentMediaPlayer = new MediaPlayer();
    // private MediaPlayer mNextMediaPlayer;
    // private Handler mHandler;
    // private boolean mIsInitialized = false;

    private UniformMediaPlayer mCurrentMediaPlayer, mPrevMediaPlayer, mNextMediaPlayer;
    private UniformMediaPlayer mVlcPlayer; //
    private UniformMediaPlayer mSaviPlayer; //
    private UniformMediaPlayer mAndroidPlayer; //

    public WavevinePlayer(final MusicService service ) {
        // mService = new WeakReference<>(service);
        // mCurrentMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
        mVlcPlayer = new VLCMediaPlayer(service);
        mSaviPlayer = new SaviMediaPlayer(service);
        // mAndroidPlayer = new AndroidMediaPlayer(service);
        mCurrentMediaPlayer = mVlcPlayer;
    }

    private void swapPlayer() {
        if( mPrevMediaPlayer != null && mPrevMediaPlayer != mCurrentMediaPlayer ) {
            if( mPrevMediaPlayer == mSaviPlayer )
                ; // mPrevMediaPlayer.pause(); // .release();
            mPrevMediaPlayer = mCurrentMediaPlayer;
        } else
            mPrevMediaPlayer = mCurrentMediaPlayer;
    }

    public Class getPlayerInstance() {
        return mCurrentMediaPlayer.getInstance();
    }

    public void setDataSource(final String path) {
        if(BuildConfig.DEBUG)
            Log.d(TAG, "Path: " + path);

        if( path != null && (path.endsWith(".dff") || path.endsWith(".dsf")))
            mCurrentMediaPlayer = mSaviPlayer;
        else
            mCurrentMediaPlayer = mVlcPlayer;

        swapPlayer();
        mCurrentMediaPlayer.setDataSource( path );
    }

    public void setNextDataSource(final String path) {
        if( path != null && (path.endsWith(".dff") || path.endsWith(".dsf")))
            mNextMediaPlayer = mSaviPlayer;
        else
            mNextMediaPlayer = mVlcPlayer;

        mNextMediaPlayer.setNextDataSource( path );
    }

    public boolean isInitialized() {
        return mCurrentMediaPlayer.isInitialized();
    }

    public void start() {
        mCurrentMediaPlayer.start();
    }

    public void stop() {
        mCurrentMediaPlayer.stop();
    }

    public void release() {
        mCurrentMediaPlayer.release();
    }

    public void pause() {
        mCurrentMediaPlayer.pause();
    }

    public void setHandler(Handler handler) {
        mCurrentMediaPlayer.setHandler(handler);
        mVlcPlayer.setHandler(handler);
        mSaviPlayer.setHandler(handler);
        // mAndroidPlayer.setHandler(handler);
    }

    public long getDuration() {
        return mCurrentMediaPlayer.getDuration();
    }

    public long getPosition() {
        return mCurrentMediaPlayer.getCurrentPosition();
    }

    public long seekTo(long whereto) {
        return mCurrentMediaPlayer.seekTo((int) whereto);
    }

    public void setVolume(float vol) {
        mCurrentMediaPlayer.setVolume(vol);
    }

    public int getAudioSessionId() {
        return mCurrentMediaPlayer.getAudioSessionId();
    }
}
