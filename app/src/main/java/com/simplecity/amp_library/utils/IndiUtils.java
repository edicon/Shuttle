package com.simplecity.amp_library.utils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.playback.MusicService;

import java.io.File;
import java.util.Calendar;

public class IndiUtils {

    public IndiUtils() {}

    private static ImageView indiPlay,indiVol, indiReplay, indiShuffle, indiBo, indiSd1, indiSd2, indiEq;
    private static ImageView indiLoading,indiSleep, indiBT, indiWifi, indiBat;
    private static TextView volVal,batVal, indiTime;

    public static void initIndiView( AppCompatActivity a) {
        indiPlay = (ImageView) a.findViewById(R.id.indi_play);
        indiVol = (ImageView) a.findViewById(R.id.indi_vol);
        volVal = (TextView) a.findViewById(R.id.vol_val);
        indiReplay = (ImageView) a.findViewById(R.id.indi_replay);
        indiShuffle = (ImageView) a.findViewById(R.id.indi_shuffle);
        indiBo = (ImageView) a.findViewById(R.id.indi_bo);
        indiSd1 = (ImageView) a.findViewById(R.id.indi_sd1);
        indiSd2 = (ImageView) a.findViewById(R.id.indi_sd2);
        indiEq = (ImageView) a.findViewById(R.id.indi_eq);
        indiLoading = (ImageView) a.findViewById(R.id.indi_loading);
        indiSleep = (ImageView) a.findViewById(R.id.indi_sleep);
        indiBT = (ImageView) a.findViewById(R.id.indi_bluetooth);
        indiWifi = (ImageView) a.findViewById(R.id.indi_wifi);
        indiBat = (ImageView) a.findViewById(R.id.indi_bat);
        batVal = (TextView) a.findViewById(R.id.bat_val);
        indiTime = (TextView) a.findViewById(R.id.indi_time);
    }

    public static void updateIndiBar(AppCompatActivity a) {

        AudioManager audio = (AudioManager) a.getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        ConnectivityManager connManager = (ConnectivityManager) a.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        updatePlay(MusicUtils.isPlaying());
        updateVol(currentVolume);
        updateRepeat(MusicUtils.getRepeatMode());
        updateShuffle(MusicUtils.getShuffleMode());
        updateSdCard( false );
        // ToDo: Check
        // updateBo(a, false);
        // updateEQ(a, EqUtils.getEqualizerEnabled(a), null ); // ToDo: Call EqUgils.updateEq(); to get eq level from system
        updateLoading(a, false);
        // updateSleep(a, false);
        // ToDo: END
        updateBT(isBluetoothHeadsetConnected());
        updateWifi(mWifi.isConnected());
        updateBat(getBatteryPercentage(a));
    }

    public static void updatePlay( boolean on ) {
        if( indiPlay == null )
            return;
        if( on )
            indiPlay.setImageResource(R.drawable.indi_play_on);
        else
            indiPlay.setImageResource(R.drawable.indi_play_off);
    }

    public static void updateRepeat( int mode ) {
        if( indiReplay == null )
            return;
        if( mode != MusicService.RepeatMode.OFF  )
            indiReplay.setImageResource(R.drawable.indi_replay_on);
        else
            indiReplay.setImageResource(R.drawable.indi_replay_off);
    }

    public static void updateShuffle( int mode ) {
        if( indiShuffle == null )
            return;
        if( mode == MusicService.ShuffleMode.ON )
            indiShuffle.setImageResource(R.drawable.indi_shuffle_on);
        else
            indiShuffle.setImageResource(R.drawable.indi_shuffle_off);
    }

    public static void updateBo( Context cx, boolean on ) {
        if( indiBo == null )
            return;
        if( on )
            indiBo.setImageResource(R.drawable.indi_bo_on);
        else
            indiBo.setImageResource(R.drawable.indi_bo_off);
    }

    public static void updateSdCard( boolean on ) {
        if( indiSd1 == null )
            return;

        File sdFolder = new File(com.simplecity.amp_library.constants.Config.SD_EX1);
        if( sdFolder.exists())
            indiSd1.setImageResource(R.drawable.indi_sd1_on);
        else
            indiSd1.setImageResource(R.drawable.indi_sd1_off);

        sdFolder = new File(com.simplecity.amp_library.constants.Config.SD_EX2);
        if( sdFolder.exists())
            indiSd2.setImageResource(R.drawable.indi_sd2_on);
        else
            indiSd2.setImageResource(R.drawable.indi_sd2_off);
    }

    public static void updateEQ( Context cx, boolean on, float[] eq ) {
        if( indiEq == null )
            return;

        EqUtils.setVLCeq( on, eq );

        if( on )
            indiEq.setImageResource(R.drawable.indi_eq_on);
        else
            indiEq.setImageResource(R.drawable.indi_eq_off);
    }

    public static void updateVol( int value) {
        if( indiVol == null )
            return;

        try {
            volVal.setText("" + value);
            if (value > 0)
                indiVol.setImageResource(R.drawable.indi_vol_on);
            else
                indiVol.setImageResource(R.drawable.indi_vol_off);
        } catch( Exception e ) {
            e.printStackTrace();
        }
    }

    public static void updateLoading( Context cx, boolean on ) {
        if( indiLoading == null )
            return;
        if( on )
            indiLoading.setImageResource(R.drawable.indi_loading_on);
        else
            indiLoading.setImageResource(R.drawable.indi_loading_off);
    }

    public static void updateSleep( Context cx, boolean on ) {
        if( indiSleep == null )
            return;
        if( on )
            indiSleep.setImageResource(R.drawable.indi_sleep_on);
        else
            indiSleep.setImageResource(R.drawable.indi_sleep_off);
    }

    public static void updateBT( boolean on ) {
        if( indiBT == null )
            return;
        if( on )
            indiBT.setImageResource(R.drawable.indi_bluetooth_on);
        else
            indiBT.setImageResource(R.drawable.indi_bluetooth_off);
    }

    public static void updateWifi( boolean on ) {
        if( indiWifi == null )
            return;
        if( on )
            indiWifi.setImageResource(R.drawable.indi_wifi_on);
        else
            indiWifi.setImageResource(R.drawable.indi_wifi_off);
    }

    // ToDo: Body/Level을 하나의 그림으로
    public static void updateBat( int level ) {
        if( indiBat == null )
            return;

        if( level < 1 )
            indiBat.setVisibility(View.GONE);
        else if( level < 10 )
            indiBat.setImageResource(R.drawable.indi_battery_10);
        else if( level > 99 )
            indiBat.setImageResource(R.drawable.indi_battery_100);
        else if( level > 90 )
            indiBat.setImageResource(R.drawable.indi_battery_90);
        else if( level > 80 )
            indiBat.setImageResource(R.drawable.indi_battery_80);
        else if( level > 70 )
            indiBat.setImageResource(R.drawable.indi_battery_70);
        else if( level > 60 )
            indiBat.setImageResource(R.drawable.indi_battery_60);
        else if( level > 50 )
            indiBat.setImageResource(R.drawable.indi_battery_50);
        else if( level > 40 )
            indiBat.setImageResource(R.drawable.indi_battery_40);
        else if( level > 30 )
            indiBat.setImageResource(R.drawable.indi_battery_20);
        else if( level > 20 )
            indiBat.setImageResource(R.drawable.indi_battery_30);
        else if( level > 10 )
            indiBat.setImageResource(R.drawable.indi_battery_10);
        batVal.setText(""+level+"%");
    }

    // https://stackoverflow.com/questions/10634231/how-to-display-current-time-that-changes-dynamically-for-every-second-in-android
    private static CountDownTimer newtimer;
    public static void startTimer() {
        if( newtimer != null )
            newtimer.cancel();

        newtimer = new CountDownTimer(1000000000, 1000) {
            public void onTick(long millisUntilFinished) {
                Calendar c = Calendar.getInstance();
                String apm = " PM";
                if(c.get(Calendar.AM_PM) == Calendar.AM ) {
                    apm = " AM";
                }
                indiTime.setText(c.get(Calendar.HOUR)+":"+c.get(Calendar.MINUTE)+ apm); //  +":"+c.get(Calendar.SECOND));
            }
            public void onFinish() {
                Log.d("TIME", "onFinished");
                newtimer.start();
            }
        };
        newtimer.start();
    }
    public static void stopTimer() {
        if( newtimer != null )
            newtimer.cancel();
    }

    public static boolean isBluetoothHeadsetConnected() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()
            && mBluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothHeadset.STATE_CONNECTED;
    }

    // https://stackoverflow.com/questions/4715865/how-to-programmatically-tell-if-a-bluetooth-device-is-connected-android-2-2
    public static int getBatteryPercentage(Context context) {

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        return (int) (batteryPct * 100);
    }

    public static void showIndiBar( Activity a, View v ) {
        View indiBar = a.findViewById(R.id.dummyIndiStatusBar);      // MainActivity
        if( indiBar != null )
            indiBar.setVisibility(View.VISIBLE);

    }
    public static void hideIndiBar( Activity a, View v ) {
        // if( a != null )
        //     return;

        View indiBar = a.findViewById(R.id.dummyIndiStatusBar);      // MainActivity
        if( indiBar != null )
            indiBar.setVisibility(View.GONE);

        if( v != null )
            v.setVisibility(View.GONE);
    }
}
