package com.simplecity.amp_library.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

// https://stackoverflow.com/questions/6293886/not-getting-the-sd-card-related-intents-to-my-broadcast-receiver
public class SdCardReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action.equalsIgnoreCase( Intent.ACTION_MEDIA_REMOVED)
            || action.equalsIgnoreCase(Intent.ACTION_MEDIA_UNMOUNTED)
            || action.equalsIgnoreCase(Intent.ACTION_MEDIA_BAD_REMOVAL)
            || action.equalsIgnoreCase(Intent.ACTION_MEDIA_EJECT)) {
            IndiUtils.updateSdCard( false );
        }
    }
}
