package com.simplecity.amp_library.playback.mediaplayers;

import android.os.Handler;

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
    private UniformMediaPlayer mCurrentMediaPlayer; //  = AndroidMediaPlayer(); // new MediaPlayer();
    private UniformMediaPlayer mCurrentVlcPlayer;

    public WavevinePlayer(final MusicService service) {
        // mService = new WeakReference<>(service);
        // mCurrentMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
        mCurrentMediaPlayer = new AndroidMediaPlayer( service );
        mCurrentVlcPlayer = new VLCMediaPlayer( service );
    }

    public void setDataSource(final String path) {
        mCurrentMediaPlayer.setDataSource( path );
    }

    public void setNextDataSource(final String path) {
        mCurrentMediaPlayer.setNextDataSource( path );
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
