package com.simplecity.amp_library.playback.mediaplayers;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Handler;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import com.crashlytics.android.core.CrashlyticsCore;
import com.savitech_ic.saviaudiolibrary.NativeDSDSetting;
import com.simplecity.amp_library.BuildConfig;
import com.simplecity.amp_library.playback.MusicService;

import org.videolan.libvlc.util.AndroidUtil;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import savitech.savitechlibrary.SavitechMediaPlayer;

import static com.simplecity.amp_library.utils.FileHelper.getPathFromUri;

public class SaviMediaPlayer extends UniformMediaPlayer {

	private static final String TAG = SaviMediaPlayer.class.getSimpleName();

	private Handler mHandler;
	private final WeakReference<MusicService> mService;
	private static SavitechMediaPlayer mCurrentMediaPlayer;
	private SavitechMediaPlayer mNextMediaPlayer;
	private android.media.MediaPlayer powerMediaPlayer = new android.media.MediaPlayer(); // ToDo: for fake
	private boolean mIsInitialized = false;

    private NativeDSDSetting ndsd;
    private int usb_fd;
    // private String usb_path;
    private UsbManager mUsbManager;
    private UsbDevice /*device,*/ mUsbDevice;
    private UsbDeviceConnection mDeviceConnection;

    // private PendingIntent mPermissionIntent;
    // private IntentFilter filterAttached_and_Detached = null;
    // private BroadcastReceiver mUsbReceiver = null;

	private boolean mIsDSDOpen;
    private static final int DFF_TYPE_64 = 3;
    private static final int DFF_TYPE_128 = 4;
    private static final int DFF_TYPE_256 = 5;
	private static final int DSF_TYPE_64 = 6;
	private static final int DSF_TYPE_128 = 7;
	private static final int DSF_TYPE_256 = 8;
    private static final int SV_CMD_NATIVE_DSD64_OPEN = 0 ;
    private static final int SV_CMD_NATIVE_DSD64_CLOSE = 1 ;
    private static final int SV_CMD_NATIVE_DSD128_OPEN = 2 ;
    private static final int SV_CMD_NATIVE_DSD128_CLOSE = 3 ;
    private static final int SV_CMD_NATIVE_DSD256_OPEN = 4 ;
    private static final int SV_CMD_NATIVE_DSD256_CLOSE = 5 ;

    int fileType;
    String currFileName = null;
	private boolean isStarted = false;
	private boolean isPaused = false;
	private boolean isFinished = false;

	private boolean isTouchSoundsEnabled;
	private boolean isVibrateOnTouchEnabled;

	private boolean isRunnablePlayer = true;
	final ExecutorService threadPoolExecutor = Executors.newSingleThreadExecutor();
	final ExecutorService nextThreadPoolExecutor = Executors.newSingleThreadExecutor();
	final Runnable playCurrMusic = new Runnable() {
		@Override
		public void run() {
			if( BuildConfig.DEBUG ) {
				Log.d(TAG, "playCurrMusic: run");
			}
			isStarted = true;
			mCurrentMediaPlayer.playFile();
		}
	};
	// ToDo: playCurrMusic으로만 할지 검토
	final Runnable playNextMusic = new Runnable() {
		@Override
		public void run() {
			if( BuildConfig.DEBUG ) {
				Log.d(TAG, "playNextMusic: run");
			}
			mNextMediaPlayer.playFile();
		}
	};


	public SaviMediaPlayer( final MusicService service ) {

		mService = new WeakReference<>(service);
		powerMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);

		mUsbManager = service.mUsbManager;
		mUsbDevice = service.mUsbDevice;
		mDeviceConnection = service.mDeviceConnection;

		mCurrentMediaPlayer = new SavitechMediaPlayer();
		mCurrentMediaPlayer.acceptSavitechCopyright();

		checkSystemSetting();
		setEqualizer(mCurrentMediaPlayer);

		if (BuildConfig.DEBUG)
			Log.d(TAG, "SavitechMediaPlayer: Created");
	}

	private final Handler seekEndHandler = new Handler();
	private final Runnable seekEndRunnable = new Runnable() {
		public void run() {
			checkSeekPosition();
		}
	};
	private void checkSeekPosition() {
		seekEndHandler.removeCallbacks(seekEndRunnable);
		if( mCurrentMediaPlayer != null && mCurrentMediaPlayer.isPlaying()) {
			if(mCurrentMediaPlayer.isFinished()) {
				isFinished = true;
                if( BuildConfig.DEBUG ) {
					int dur = mCurrentMediaPlayer.getDuration();
					int pos = mCurrentMediaPlayer.getCurrentPosition();
					Log.e(TAG, "checkPosition --> isFinished: " + dur + "/" + pos);
					// return;
				}
				// MusicService --> stop --> if(isFinished) skip stopMusic
				setDsdCmdClosed();
				seekEndHandler.removeCallbacks(seekEndRunnable);

				if (mCurrentMediaPlayer != null && mNextMediaPlayer != null) {
					// mCurrentMediaPlayer.release();
					mCurrentMediaPlayer = mNextMediaPlayer;
					mNextMediaPlayer = null;
					mHandler.sendEmptyMessage(MusicService.PlayerHandler.TRACK_WENT_TO_NEXT);
				} else {
                    /*
					if (mCurrentMediaPlayer.openMusicFile(currFileName)) {
						fileType = mCurrentMediaPlayer.getFileType();
						setDsdCmdOpen();
					} else {
                        goError("checkPosition");
					}
					*/

					mService.get().mWakeLock.acquire(30000);
					mHandler.sendEmptyMessage(MusicService.PlayerHandler.TRACK_ENDED);
					mHandler.sendEmptyMessage(MusicService.PlayerHandler.RELEASE_WAKELOCK);
				}
			}
		}
		seekEndHandler.postDelayed(seekEndRunnable, 500);
	}

	private void checkSystemSetting() {
		// ToDo: Check System Setting
        /*
		isTouchSoundsEnabled = Settings.System.getInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 1) != 0;
		isVibrateOnTouchEnabled = false;
		System.getInt(getContentResolver(), Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 1) != 0;
		if (isTouchSoundsEnabled) {
			Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 0);
		}
		if (isVibrateOnTouchEnabled) {
			Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 0);
		}
		*/
	}

	public static void setEqualizer( SavitechMediaPlayer mCurrentMediaPlayer ) {
		if (BuildConfig.DEBUG)
			Log.d(TAG, "SavitechMediaPlayer: setEqualizer");
	}

	public static SavitechMediaPlayer getSaviTechInstance() {
		return mCurrentMediaPlayer;
	}

	public static SavitechMediaPlayer getMediaPlayerInstance() {
		return mCurrentMediaPlayer;
	}

	@Override
	public Class getInstance() {
		return SaviMediaPlayer.class;
	}

	@Override
	public boolean isInitialized() {
		return mIsInitialized;
	}

	@Override
	public void start() {
		if( BuildConfig.DEBUG )
			Log.d(TAG, "start");

		if( isPaused ) {
			if (mCurrentMediaPlayer.openMusicFile(currFileName)) {
				if (mCurrentMediaPlayer.resumeMusicPosition()) {
					if( isRunnablePlayer ) {
						final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playCurrMusic);
					} else
                        mCurrentMediaPlayer.playFile();
				}
			} else {
                goError("start");
			}
		} else {
			if( isRunnablePlayer ) {
				final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playCurrMusic);
			} else
				mCurrentMediaPlayer.playFile();
		}
		isPaused = false;
		seekEndHandler.postDelayed(seekEndRunnable, 500);
	}

	@Override
	public void pause() {
		if( BuildConfig.DEBUG )
			Log.d(TAG, "pause");

		if( mCurrentMediaPlayer.isFinished()) {
			Log.d(TAG, "pause: isFinished");
			// return;
		}

        if( mCurrentMediaPlayer.isPlaying()) {
			mCurrentMediaPlayer.pause();
		}
		isPaused = true;
	}

	// Ref.: btFF, btFB
	@Override
	public long seekTo(long whereto) { // msec
		if( BuildConfig.DEBUG )
			Log.d(TAG, "seekTo: " + whereto);

		if( !isStarted ) {
			Log.e(TAG, "seekTo: !isStrted" + whereto);
			return whereto;
		}

        if( isPaused ) {
			if(mCurrentMediaPlayer.openMusicFile(currFileName)) {
				if(mCurrentMediaPlayer.resumeMusicPosition()) {
					// ToDo: check long --> int
					if(mCurrentMediaPlayer.openMusicFile(currFileName)) {
						if (mCurrentMediaPlayer.seekTo((int) whereto)) {
							if( isRunnablePlayer ) {
								final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playCurrMusic);
							} else
								mCurrentMediaPlayer.playFile();
							isPaused = false;
						}
					}
				}
			} else {
				goError("seekTo");
			}
		} else if( mCurrentMediaPlayer.pause()){
			//delayHalfSec();
			if(mCurrentMediaPlayer.openMusicFile(currFileName)) {
				if(mCurrentMediaPlayer.seekTo((int)whereto)) {
					if( isRunnablePlayer ) {
						final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playCurrMusic);
					} else
						mCurrentMediaPlayer.playFile();
				}
			}else{
				goError("seekTo");
			}
		}
		return whereto;
	}

	@Override
	public void setDataSource(String path) {
		if( BuildConfig.DEBUG ) {
			Log.e(TAG, "setDataSource: path: " + path );
		}

		mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, path);
		if (mIsInitialized) {
			setNextDataSource(null);
		}
	}

	public boolean setDataSourceImpl(final SavitechMediaPlayer mediaPlayer, final String path) {
		if (TextUtils.isEmpty(path) || mediaPlayer == null) {
			return false;
		}
		// Ref: AndroidMediaPlayer.java
		try {
			if( mIsInitialized ) {
                pause();

				stop();
				if( stopped ) {
					setDsdCmdClosed();
				}
			}
			// File Reset?
			isPaused = false;

			// get file path from uri
			//  -https://stackoverflow.com/questions/5657411/android-getting-a-file-uri-from-a-content-uri
			String filePath = path;
			if (path.startsWith("content://")) {
				filePath = getPathFromUri( mService.get(), Uri.parse(path ));
			}

			Uri uri = AndroidUtil.PathToUri(filePath);

			currFileName = path;
			if(mediaPlayer.openMusicFile(path)) {
				fileType = mediaPlayer.getFileType();
				setDsdCmdOpen();
				// seekEndHandler.postDelayed(seekEndRunnable, 500);

				// if( isRunnablePlayer ) {
				// 	final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playCurrMusic);
				// } else
				// 	mCurrentMediaPlayer.playFile();
			} else {
				Log.e("SaviPlayer", "svPlr.openMusicFile() failed");
			}

			// Initial Play or Not --> NOT
			//  -Comment out: cause of setNextDataSource and setDataSource가 동시 실행됨
			// mediaPlayer.play();

			if (BuildConfig.DEBUG) {
				Log.d(TAG, "setDataSourceImpl: filePath: " + filePath + "\n Uri: " + uri.toString());
			}

		} catch( Exception e ) {
			Log.e(TAG, "setDataSource failed: " + e.getLocalizedMessage());
			CrashlyticsCore.getInstance().log("setDataSourceImpl failed. Path: [" + path + "] error: " + e.getLocalizedMessage());
			return false;
		}

		// return mIsInitialized
		return true;
	}

	private boolean nextInitialized;
	@Override
	public void setNextDataSource(String path) {

		if( BuildConfig.DEBUG ) {
			Log.e(TAG, "setNextDataSource: path: " + path );
		}

		if (TextUtils.isEmpty(path)) {
			Log.e(TAG, "setNextDataSource: isEmpty: " + path );
			return;
		}

		if ( nextInitialized && mNextMediaPlayer != null) {
			mNextMediaPlayer.stopMusic();
			setDsdCmdClosed();
			// ToDo: Check setDsdCmdClosed();
			mNextMediaPlayer = null;
			nextInitialized = false;
		}

		mNextMediaPlayer = new SavitechMediaPlayer();
		// ToDo: Check CopyRight
		// mNextMediaPlayer.acceptSavitechCopyright();
		powerMediaPlayer.setWakeMode(mService.get(), PowerManager.PARTIAL_WAKE_LOCK);
		// ToDo:
		// mNextMediaPlayer.setAudioSessionId(getAudioSessionId());
		if (setDataSourceImpl(mNextMediaPlayer, path)) {
			try {
				nextInitialized = true;
			} catch (Exception e) {
				Log.e(TAG, "setNextDataSource failed - failed to call setNextMediaPlayer on mCurrentMediaPlayer. Error: " + e.getLocalizedMessage());
				CrashlyticsCore.getInstance().log("setNextDataSource failed - failed to call setNextMediaPlayer on mCurrentMediaPlayer. Error: " + e.getLocalizedMessage());
				if (mNextMediaPlayer != null) {
					mNextMediaPlayer.stopMusic();
					// ToDo: Check setDsdCmdClosed()
					mNextMediaPlayer = null;
				}
				nextInitialized = false;
			}
		} else {
			Log.e(TAG, "setDataSourceImpl failed for path: [" + path + "]. Setting next media player to null");
			CrashlyticsCore.getInstance().log("setDataSourceImpl failed for path: [" + path + "]. Setting next media player to null");
			if (mNextMediaPlayer != null) {
				mNextMediaPlayer.stopMusic();
				// ToDo: Check setDsdCmdClosed()
				mNextMediaPlayer = null;
			}
			nextInitialized = false;
		}
	}

	@Override
	public long getDuration() {
        try {
			int dur = mCurrentMediaPlayer.getDuration();
			if( BuildConfig.DEBUG )
				Log.d(TAG, "getDuration: " + dur);
			return dur;
		} catch ( Exception e ) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public long getCurrentPosition() {
        try {
			int pos = mCurrentMediaPlayer.getCurrentPosition();
			if( false && BuildConfig.DEBUG )
				Log.d(TAG, "getCurrentPosition: " + pos );
			return pos;
		} catch( Exception e ) {
			e.printStackTrace();
            return 0;
		}
	}

	private boolean stopped;
	@Override
	public void stop() {
		if( BuildConfig.DEBUG )
			Log.d(TAG, "stop");

		if( isFinished ) {
			if( BuildConfig.DEBUG )
                Log.d(TAG, "stop: isFinished");
            // isFinished() --> setDsdCmdClosed --> mIsInitialized = false
			mIsInitialized = false;
			return;
		}
		if( !isStarted ) {
			if( BuildConfig.DEBUG )
				Log.d(TAG, "stop: isStarted: false");
			mIsInitialized = false;
			return;
		}

		stopped = mCurrentMediaPlayer.stopMusic();
		if( stopped ) {
			setDsdCmdClosed();
		}

		seekEndHandler.removeCallbacks(seekEndRunnable);
		mIsInitialized = false;
	}

	@Override
	public void release() {
		if( BuildConfig.DEBUG )
			Log.d(TAG, "release");

		stop();

		if(stopped){
			//delayHalfSec();
			if(!( fileType < 3 )) {
				if (sendDSDCommand(fileType, false) < 0) {
					Log.e("SaviPlayer", "sendDSDCommand close Failed");
				}

				delayHalfSec();

				//unregisterReceiver(mUsbReceiver);

				if (mDeviceConnection != null) {
					mDeviceConnection.close();
				}
				mDeviceConnection = null;
			}
			// if(mUsbReceiver != null) {
			// ToDo:
			// unregisterReceiver(mUsbReceiver);
			// }
		}
		if(isTouchSoundsEnabled) {
			// Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 1);
		}
		if(isVibrateOnTouchEnabled) {
			// ToDo: Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 1);
		}
	}

	@Override
	public void setVolume(float vol) {
        if( BuildConfig.DEBUG )
            Log.d(TAG, "setVolume: Dummy Function");
	}

	@Override
	public int getAudioSessionId() {
		if( BuildConfig.DEBUG )
			Log.d(TAG, "getAudioSessionId: Dummy Function");
		return 0;
	}

	@Override
	public void setBitrate(int mode) {
		if( BuildConfig.DEBUG )
			Log.d(TAG, "setBitrate: Dummy Function");
	}


	@Override
	public void setHandler(Handler handler) {
        mHandler = handler;
	}

	public void delayHalfSec(){
		try {
            //TimeUnit.SECONDS.sleep(1);
            TimeUnit.MILLISECONDS.sleep(500);
		} catch (InterruptedException e) {
            Log.e("SaviPlayer", "sleep exp", e);
		}
	}
	
	public void setDsdCmdClosed(){
        //delayHalfSec();
        if(!(fileType < 3)) {
            if (sendDSDCommand(fileType, false) < 0) {
                Log.e("SaviPlayer", "sendDSDCommand Failed");
            }
        }
        delayHalfSec();
	}
	
	public void setDsdCmdOpen(){
		if(!(fileType < 3)) {
			if (sendDSDCommand(fileType, true) < 0) {
				Log.e("SaviPlayer", "sendDSDCommand Failed");
			}
		}
	}

    private int sendDSDCommand(int dsdType, boolean open) {
        if(mIsDSDOpen && open){
            return 0;
        }
        int cmd = -1;
        if(mUsbDevice == null || mDeviceConnection == null){
            Log.w("SaviPlayer", "usb device not connected");
			// ToDo:
            // USBDevicesCheck();
        }
		if(mDeviceConnection != null){
			mDeviceConnection.close();
			mDeviceConnection = null;
		}
		mDeviceConnection = mUsbManager.openDevice(mUsbDevice);

        ndsd = new NativeDSDSetting();
        ndsd.getSavitechCopyright(true);

		if(mDeviceConnection == null){
			return -1;	//failed to set dsd mode
		}

        usb_fd = -1;
        usb_fd = mDeviceConnection.getFileDescriptor();

		int rawDescriptorsSize = -1;
		byte[] rawDescriptors = null;
		rawDescriptors = mDeviceConnection.getRawDescriptors();
		rawDescriptorsSize = rawDescriptors.length;

        switch(dsdType){
            case DFF_TYPE_64:
			case DSF_TYPE_64:
                cmd = open ? SV_CMD_NATIVE_DSD64_OPEN : SV_CMD_NATIVE_DSD64_CLOSE;
                break;
            case DFF_TYPE_128:
			case DSF_TYPE_128:
                cmd = open ? SV_CMD_NATIVE_DSD128_OPEN : SV_CMD_NATIVE_DSD128_CLOSE;
                break;
            case DFF_TYPE_256:
			case DSF_TYPE_256:
                cmd = open ? SV_CMD_NATIVE_DSD256_OPEN : SV_CMD_NATIVE_DSD256_CLOSE;
                break;
            default:
                Log.e("SaviPlayer", "invalid dsd type: " + dsdType);
                return -1;
        }

        int result =  ndsd.setNativeDSD(usb_fd, mUsbDevice.getDeviceName(), rawDescriptors, rawDescriptorsSize, cmd);
		
        mIsDSDOpen = (result >= 0) && open ? true : false;

		mDeviceConnection.close();

		mDeviceConnection = null;

        return result;
    }

	private void goError( String tag ) {
		Log.e(TAG, tag + "svPlr.openMusicFile() failed");

		release();
		mIsInitialized = false;

		mCurrentMediaPlayer = new SavitechMediaPlayer();
		mCurrentMediaPlayer.acceptSavitechCopyright();
		mHandler.sendMessageDelayed(mHandler.obtainMessage(MusicService.PlayerHandler.SERVER_DIED), 2000);
	}

    /*
    private void USBDevicesCheck() {

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.android.recipes.USB_PERMISSION"), 0);
        filterAttached_and_Detached = new IntentFilter();
        filterAttached_and_Detached.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        filterAttached_and_Detached.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        filterAttached_and_Detached.addAction("com.android.recipes.USB_PERMISSION");

        mUsbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent){
                String action = intent.getAction();
                if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)){
                    UsbDevice device =(UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if(device !=null){
                        Log.e("SaviPlayer","onRecever.. ACTION_USB_DEVICE_DETACHED");
						if( mCurrentMediaPlayer.pause()) {
							isPaused = true;
							// btPlay.setText(getResources().getString(R.string.btPlay));
						}
						if (mDeviceConnection != null) {
							mDeviceConnection.close();
						}
						mDeviceConnection = null;
                    }
                }
                else if(UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)){
                    UsbDevice device =(UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if(device !=null){
                        Log.e("SaviPlayer","onRecever.. USB_DEVICE_ATTACHED");
                    }
                }

            }
        };

        registerReceiver(mUsbReceiver, filterAttached_and_Detached);

        mUsbManager = ((UsbManager)getSystemService(Context.USB_SERVICE));
        if( mUsbManager != null) {
            HashMap<String, UsbDevice> localHashMap = mUsbManager.getDeviceList();
            Iterator<UsbDevice> deviter = localHashMap.values().iterator();

            recheck:
            while ( deviter.hasNext() ) {
                UsbDevice localUsbDevice = (UsbDevice)deviter.next();
                int i = localUsbDevice.getVendorId();
                int j = localUsbDevice.getProductId();

                if ((i != 0x262a) || ((j != 0x17F8) && (j != 0x17F9))) {
                    mUsbDevice = null;
                }
                else {
                    mUsbDevice = localUsbDevice;
                    int k = mUsbDevice.getVendorId();
                    int m = mUsbDevice.getProductId();
                    usb_path = mUsbDevice.getDeviceName();
                    break;
                }
            }
        }

        if( mUsbDevice == null ) {
			Log.e("SaviPlayer","mUsbManager.openDevice failed!");
        }
    }
    */
}
