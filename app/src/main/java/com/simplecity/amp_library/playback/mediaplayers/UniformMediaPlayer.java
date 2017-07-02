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

import android.os.Handler;

public abstract class UniformMediaPlayer {

    public abstract Class getInstance();

    public abstract boolean isInitialized();
    public abstract void start();
    public abstract void stop();
    // public abstract void prepare();
    public abstract void release();

    // public abstract void play();
    public abstract void pause();
    // public abstract boolean isPlaying();

    public abstract long getDuration();
    public abstract long getCurrentPosition();
    public abstract long seekTo(long msec);

    public abstract void setVolume(float vol);
    public abstract int getAudioSessionId();

    public abstract void setBitrate(int mode);

    public abstract void setDataSource( String path );
    public abstract void setNextDataSource(String path);
    public abstract void setHandler( Handler handler);

}