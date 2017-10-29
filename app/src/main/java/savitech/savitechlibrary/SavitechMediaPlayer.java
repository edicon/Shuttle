package savitech.savitechlibrary;

/**
 * Created by http://www.savitech-ic.com/ on 2016/10/3.
 */
public class SavitechMediaPlayer {
	static {
		System.loadLibrary("savitech-media-player");
	}

	public native void acceptSavitechCopyright();

	public native boolean openMusicFile(String filePath);

	public native int getFileType();

	public native void playFile();

	public native int getDuration();

	public native int getCurrentPosition();

	public native boolean isPlaying();

	public native boolean pause();

	public native boolean stopMusic();

	public native boolean resumeMusicPosition();

	public native boolean seekTo(int seektime);

	public native boolean isFinished();
}
