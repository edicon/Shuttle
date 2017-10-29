package com.simplecity.amp_library.playback.mediaplayers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.savitech_ic.saviaudiolibrary.NativeDSDSetting;
import com.simplecity.amp_library.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import savitech.savitechlibrary.SavitechMediaPlayer;

public class SaviPlayer extends Activity implements View.OnClickListener {
    private NativeDSDSetting ndsd;
    private int usb_fd;
    private String usb_path;
    private UsbManager mUsbManager;
    private UsbDevice /*device,*/ mUsbDevice;
    private UsbDeviceConnection mDeviceConnection;
    private PendingIntent mPermissionIntent;
    private IntentFilter filterAttached_and_Detached = null;
    private BroadcastReceiver mUsbReceiver = null;
    private int[] mSvolume;

	private static final int UPDATE_FREQUENCY = 500;
	private static final int STEP_VALUE = 4000;
    private boolean mIsDSDOpen;
    private boolean isMovingSeekBar = false;
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
    ArrayList<File> mySongs;
    int file_position;
    Thread updateSeekBar;
    int file_type;
    String cur_fileName = null;
	private TextView tvSelectedFile = null;
	private TextView tvPlayTime = null, tvRemainTime = null;
    private SeekBar sb = null;
    private Button btPlay, btFF, btFB, btNext, btPrv;
	private boolean isStarted = true;
	private boolean isPaused = false;
	private boolean isTouchSoundsEnabled;
	private boolean isVibrateOnTouchEnabled;
	final SavitechMediaPlayer svPlr = new SavitechMediaPlayer();
	
	private final Handler handler = new Handler();
	private final Handler play_handler = new Handler();

    private final Runnable updatePositionRunnable = new Runnable() {
        public void run() {
            updatePosition();
        }
    };
	
	final ExecutorService threadPoolExecutor = Executors.newSingleThreadExecutor();
	
	final Runnable playMusic = new Runnable() {

			@Override
			public void run() {
				svPlr.playFile();
			}
	};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		/*
        */
		isTouchSoundsEnabled = Settings.System.getInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 1) != 0;
		isVibrateOnTouchEnabled = false; // ToDo Settings.System.getInt(getContentResolver(), Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 1) != 0;
		if(isTouchSoundsEnabled) {
			Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 0);
		}
		if(isVibrateOnTouchEnabled) {
			// ToDo: Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 0);
		}

     setContentView(R.layout.savi_player);

		tvSelectedFile = (TextView) findViewById(R.id.tvSelectedFile);
		tvPlayTime = (TextView) findViewById(R.id.tvPlayTime);
        tvRemainTime = (TextView) findViewById(R.id.tvRemainTime);
		btPlay = (Button) findViewById(R.id.btPlay);
        btFF = (Button) findViewById(R.id.btFF);
        btFB = (Button) findViewById(R.id.btFB);
        btNext = (Button) findViewById(R.id.btNxt);
        btPrv = (Button) findViewById(R.id.btPrv);

        btPlay.setOnClickListener(this);
        btFF.setOnClickListener(this);
        btFB.setOnClickListener(this);
        btNext.setOnClickListener(this);
        btPrv.setOnClickListener(this);

        sb = (SeekBar) findViewById(R.id.seekBar);

        Intent i = getIntent();
        Bundle b = i.getExtras();
        mySongs = (ArrayList) b.getParcelableArrayList("songlist");
        file_position = b.getInt("pos", 0);

        cur_fileName = getCurFile(file_position);
        tvSelectedFile.setText(cur_fileName.replace("/storage/emulated/0/Music/",""));
		svPlr.acceptSavitechCopyright();
        if(svPlr.openMusicFile(cur_fileName)) {
            file_type = svPlr.getFileType();
			setDsdCmdOpen();
			final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playMusic);
			sb.setMax(svPlr.getDuration());
			updatePosition();
			isStarted = true;
        }else{
			Log.e("SaviPlayer", "svPlr.openMusicFile() failed");
			messageBox("SaviPlayer","openMusicFile failed!");
		}

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if(svPlr.isFinished()){
					//Log.i("SaviPlayer", "End of Song, play Next.\n");
					setDsdCmdClosed();
					handler.removeCallbacks(updatePositionRunnable);
					
					file_position = (file_position + 1) % mySongs.size();
                    cur_fileName = getCurFile(file_position);
                    //Log.i("SaviPlayer", "cur_fileName = " + cur_fileName);
					tvSelectedFile.setText(cur_fileName.replace("/storage/emulated/0/Music/",""));
					
					if(svPlr.openMusicFile(cur_fileName)) {
						file_type = svPlr.getFileType();
						setDsdCmdOpen();
					
						final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playMusic);
						
						btPlay.setText(getResources().getString(R.string.btPause));
						sb.setMax(svPlr.getDuration());
						updatePosition();
					}else{
						Log.e("SaviPlayer", "svPlr.openMusicFile() failed");
						messageBox("SaviPlayer","openMusicFile failed!");
					}
				}
                if (isMovingSeekBar) {
					if(isPaused){
						if(svPlr.openMusicFile(cur_fileName)) {
							if(svPlr.seekTo(seekBar.getProgress())){
								final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playMusic);
								isPaused = false;
								btPlay.setText(getResources().getString(R.string.btPause));
								updatePosition();
							}
						}else{
							Log.e("SaviPlayer", "svPlr.openMusicFile() failed");
							messageBox("SaviPlayer","openMusicFile failed!");
						}
					}else if(svPlr.pause()){
						//delayHalfSec();
						if(svPlr.openMusicFile(cur_fileName)) {
							if(svPlr.seekTo(seekBar.getProgress())){
								final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playMusic);
							}
						}else{
							Log.e("SaviPlayer", "svPlr.openMusicFile() failed");
							messageBox("SaviPlayer","openMusicFile failed!");
						}
					}
				}
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
				//Log.i("OnSeekBarChangeListener","onStartTrackingTouch");
                isMovingSeekBar = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
				//Log.i("OnSeekBarChangeListener","onStopTrackingTouch");
                isMovingSeekBar = false;
            }
        });
    }
	
	@Override
    protected void onResume()
    {
		super.onResume();
		isTouchSoundsEnabled = Settings.System.getInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 1) != 0;
		isVibrateOnTouchEnabled = false; // Settings.System.getInt(getContentResolver(), Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 1) != 0;
		if(isTouchSoundsEnabled) {
			Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 0);
		}
		if(isVibrateOnTouchEnabled) {
			// Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 0);
		}
	}

	@Override
    protected void onDestroy()
    {
        super.onDestroy();
		
		handler.removeCallbacks(updatePositionRunnable);
		if(svPlr.stopMusic()){
			//delayHalfSec();
			
			if(!(file_type<3)) {
				if (sendDSDCommand(file_type, false) < 0) {
					Log.e("SaviPlayer", "sendDSDCommand close Failed");
				}
				
				delayHalfSec();
			
				//unregisterReceiver(mUsbReceiver);

				if (mDeviceConnection != null)
				{
					mDeviceConnection.close();
				}
				mDeviceConnection = null;
			}
			if(mUsbReceiver != null){
				unregisterReceiver(mUsbReceiver);
			}
		}
		if(isTouchSoundsEnabled) {
			Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 1);
		}
		if(isVibrateOnTouchEnabled) {
			// ToDo: Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 1);
		}
    }
	
	private void messageBox( CharSequence title, CharSequence message)
	{
		new AlertDialog.Builder(SaviPlayer.this)
				.setTitle(title)
				.setMessage(message)
				.setPositiveButton("OK", new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{

					}
				})
				.show();
	}

    public String getCurFile(int pos){
        String fileName = null;
        Uri u = Uri.parse(mySongs.get(pos).getPath().toString());
        fileName = u.toString();
		//Log.i("SaviPlayer","fileName = "+fileName);
        return fileName;
    }
	
	private void updatePosition() {
        handler.removeCallbacks(updatePositionRunnable);

        sb.setProgress(svPlr.getCurrentPosition());
		
		tvPlayTime.setText(String.format("%02d:%02d:%02d", svPlr.getCurrentPosition()/(1000*60*60), (svPlr.getCurrentPosition()%(1000*60*60))/(1000*60), ((svPlr.getCurrentPosition()%(1000*60*60))%(1000*60))/1000));   //worked
        
		int time_remaining = svPlr.getDuration()-svPlr.getCurrentPosition();
        
		tvRemainTime.setText(String.format("-%02d:%02d:%02d", time_remaining/(1000*60*60), (time_remaining%(1000*60*60))/(1000*60), (time_remaining%(1000*60*60))%(1000*60)/1000));
		
        handler.postDelayed(updatePositionRunnable, UPDATE_FREQUENCY);
    }

    @Override
    public void onClick(View v) {
            int id = v.getId();
            switch (id) {
                case R.id.btPlay:{
                    if (svPlr.isPlaying()) {
						handler.removeCallbacks(updatePositionRunnable);
                        if(svPlr.pause()){
							isPaused = true;
							btPlay.setText(getResources().getString(R.string.btPlay));
						}
                    } else {
                        if (isStarted && isPaused) {
							// Do resume playing music.
							if(svPlr.openMusicFile(cur_fileName)) {
								if(svPlr.resumeMusicPosition()){
									final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playMusic);
								}
							}else{
								Log.e("SaviPlayer", "svPlr.openMusicFile() failed");
								messageBox("SaviPlayer","openMusicFile failed!");
								break;
							}
							btPlay.setText(getResources().getString(R.string.btPause));
                            updatePosition();
							isPaused = false;
                        } else {
							final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playMusic);
							btPlay.setText(getResources().getString(R.string.btPause));
                        }
                    }

                    break;
                }
				
				case R.id.btFF:{
					int seekto = 0;
					if (!isPaused) {
						seekto = svPlr.getCurrentPosition() + STEP_VALUE;
						if (seekto > svPlr.getDuration()){
							seekto = svPlr.getDuration();
						}
						if(svPlr.pause()){
							//delayHalfSec();
							if(svPlr.openMusicFile(cur_fileName)) {
								if(svPlr.seekTo(seekto)){
									final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playMusic);
								}
							}else{
								Log.e("SaviPlayer", "svPlr.openMusicFile() failed");
								messageBox("SaviPlayer","openMusicFile failed!");
								break;
							}
						}
					}else{	//after pause must call resume
						if(svPlr.openMusicFile(cur_fileName)) {
							if(svPlr.resumeMusicPosition()){
								seekto = svPlr.getCurrentPosition() + STEP_VALUE;
								if (seekto > svPlr.getDuration()){
									seekto = svPlr.getDuration();
								}
								if(svPlr.openMusicFile(cur_fileName)) {
									if(svPlr.seekTo(seekto)){
										final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playMusic);
										isPaused = false;
										btPlay.setText(getResources().getString(R.string.btPause));
										updatePosition();
									}
								}
							}
						}else{
							Log.e("SaviPlayer", "svPlr.openMusicFile() failed");
							messageBox("SaviPlayer","openMusicFile failed!");
							break;
						}
					}
                    break;
				}
				
				case R.id.btFB: {
					int seekto = 0;
					if (!isPaused){	
						seekto = svPlr.getCurrentPosition() - STEP_VALUE;
						if (seekto < 0){
							seekto = 0;
						}
						if(svPlr.pause()){
							//delayHalfSec();
							if(svPlr.openMusicFile(cur_fileName)) {
								if(svPlr.seekTo(seekto)){
									final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playMusic);
								}
							}else{
								Log.e("SaviPlayer", "svPlr.openMusicFile() failed");
								messageBox("SaviPlayer","openMusicFile failed!");
								break;
							}
						}
					}else{	//after pause must call resume
						if(svPlr.openMusicFile(cur_fileName)) {
							if(svPlr.resumeMusicPosition()){
								seekto = svPlr.getCurrentPosition() - STEP_VALUE;
								if (seekto < 0){
									seekto = 0;
								}
								if(svPlr.openMusicFile(cur_fileName)){
									if(svPlr.seekTo(seekto)){
										final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playMusic);
										isPaused = false;
										btPlay.setText(getResources().getString(R.string.btPause));
										updatePosition();
									}
								}
							}
						}else{
							Log.e("SaviPlayer", "svPlr.openMusicFile() failed");
							messageBox("SaviPlayer","openMusicFile failed!");
							break;
						}
					}
                    break;
                }
				
                case R.id.btNxt:{
					handler.removeCallbacks(updatePositionRunnable);
					if(svPlr.stopMusic()){
					
						setDsdCmdClosed();
						
						file_position = (file_position + 1) % mySongs.size();
						cur_fileName = getCurFile(file_position);
						//Log.i("SaviPlayer", "cur_fileName = " + cur_fileName);
						tvSelectedFile.setText(cur_fileName.replace("/storage/emulated/0/Music/",""));
						
						if(svPlr.openMusicFile(cur_fileName)) {
							file_type = svPlr.getFileType();
							setDsdCmdOpen();
						
							final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playMusic);
							
							btPlay.setText(getResources().getString(R.string.btPause));
							sb.setMax(svPlr.getDuration());
							updatePosition();
						}else{
							Log.e("SaviPlayer", "svPlr.openMusicFile() failed");
							messageBox("SaviPlayer","openMusicFile failed!");
							break;
						}
					}
					break;
				}
                case R.id.btPrv:{
                
					handler.removeCallbacks(updatePositionRunnable);
					if(svPlr.stopMusic()){
						setDsdCmdClosed();
						file_position = (file_position - 1 < 0) ? mySongs.size() - 1 : file_position - 1;
						cur_fileName = getCurFile(file_position);
						//Log.i("SaviPlayer", "cur_fileName = " + cur_fileName);
						tvSelectedFile.setText(cur_fileName.replace("/storage/emulated/0/Music/",""));
						
						if(svPlr.openMusicFile(cur_fileName)) {
							file_type = svPlr.getFileType();
							setDsdCmdOpen();
							
							final Future longRunningPlayMusicTaskFuture = threadPoolExecutor.submit(playMusic);
						
							btPlay.setText(getResources().getString(R.string.btPause));
							sb.setMax(svPlr.getDuration());
							updatePosition();
						}else{
							Log.e("SaviPlayer","svPlr.openMusicFile() failed");
							messageBox("SaviPlayer","openMusicFile failed!");
						}
					}
					break;
				}
            }
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
					
			if(!(file_type<3)) {
				if (sendDSDCommand(file_type, false) < 0) {
					Log.e("SaviPlayer", "sendDSDCommand Failed");
				}
			}
			delayHalfSec();
	}
	
	public void setDsdCmdOpen(){
		if(!(file_type<3)) {
			if (sendDSDCommand(file_type, true) < 0) {
				Log.e("SaviPlayer", "sendDSDCommand Failed");
			}
		}
	}

    private void USBDevicesCheck()
    {

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent("com.android.recipes.USB_PERMISSION"), 0);
        filterAttached_and_Detached = new IntentFilter();
        filterAttached_and_Detached.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED");
        filterAttached_and_Detached.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED");
        filterAttached_and_Detached.addAction("com.android.recipes.USB_PERMISSION");

        mUsbReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent){
                String action = intent.getAction();
                if(UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)){
                    UsbDevice device =(UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if(device !=null){
                        Log.e("SaviPlayer","onRecever.. ACTION_USB_DEVICE_DETACHED");
						if(svPlr.pause()) {
							isPaused = true;
							btPlay.setText(getResources().getString(R.string.btPlay));
						}
						if (mDeviceConnection != null)
						{
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
        if (mUsbManager != null)
        {
            HashMap<String, UsbDevice> localHashMap = mUsbManager.getDeviceList();
            Iterator<UsbDevice> deviter = localHashMap.values().iterator();

            recheck:
            while ( deviter.hasNext() )
            {
                UsbDevice localUsbDevice = (UsbDevice)deviter.next();
                int i = localUsbDevice.getVendorId();
                int j = localUsbDevice.getProductId();
                
                if ((i != 0x262a) || ((j != 0x17F8) && (j != 0x17F9)))
                {
                    mUsbDevice = null;
                }
                else
                {
                    mUsbDevice = localUsbDevice;
                    int k = mUsbDevice.getVendorId();
                    int m = mUsbDevice.getProductId();
                    usb_path = mUsbDevice.getDeviceName();
                    break;
                }
            }
        }

        if( mUsbDevice == null )
        {
            messageBox("SaviPlayer","mUsbManager.openDevice failed!");
        }
    }

    private int sendDSDCommand(int dsdType, boolean open) {
        if(mIsDSDOpen && open){
            return 0;
        }
        int cmd = -1;
        if(mUsbDevice == null || mDeviceConnection == null){
            Log.w("SaviPlayer", "usb device not connected");
            USBDevicesCheck();
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

}
