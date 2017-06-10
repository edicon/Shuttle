package com.simplecity.amp_library.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// https://stackoverflow.com/questions/4715865/how-to-programmatically-tell-if-a-bluetooth-device-is-connected-android-2-2
public class BtReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
        }
        else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
            IndiUtils.updateBT( true );
        }
        else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
        }
        else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
        }
        else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
            IndiUtils.updateBT( false );
        }
    }
}
