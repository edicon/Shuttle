package com.simplecity.amp_library.utils;

import android.content.IntentFilter;

/**
 * Created by hslee on 2017. 8. 20..
 */

public class SettingUtils {
    private static final String ACTION_STREAMING_CHANGED = "android.intent.action.STREAMING";
    private static final String STREAMING_ON = "streaming_on";
    private static final String STREAMING_MODE = "streaming_mode";

    private static final String ACTION_NETWORK_PLAY_CHANGED = "android.intent.action.NETWORK_PLAY";
    private static final String NETWORK_PLAY_ON = "network_play_on";

    private static final String ACTION_GAIN_MODE_CHANGED = "android.intent.action.GAIN_MODE";
    private static final String GAIN_MODE = "gain_mode";

    private static final String ACTION_SLEEP_MODE_CHANGED = "android.intent.action.SLEEP_MODE";
    private static final String SLEEP_MODE_ON = "sleep_mode_on";
    private static final String SLEEP_MODE_VALUE = "sleep_mode_value";

    private static final String ACTION_BALANCED_OUT_CHANGED = "android.intent.action.BALANCED_OUT";
    private static final String BALANCED_OUT_ON = "balanced_out_on";

    private static final String ACTION_USB_DAC_CHANGED = "android.intent.action.USB_DAC";
    private static final String USB_DAC_ON = "usb_dac_on";

    private static final String ACTION_HOLD_CHANGED = "android.intent.action.HOLD";
    private static final String HOLD_ON = "hold_on";

    private static final String ACTION_EQUALIZER_CHANGED = "android.intent.action.EQUALIZER";
    private static final String EQUALIZER_ON = "equalizer_on";
    private static final String EQUALIZER_VALUE = "equalizer_value";

    private static final String ACTION_GAPLESS_CHANGED = "android.intent.action.GAPLESS";
    private static final String GAPLESS_ON = "gapless_on";

    private static final String ACTION_LINEOUT_CHANGED = "android.intent.action.LINE_OUT";
    private static final String LINEOUT_ON = "line_out_on";

    private static final String ACTION_SCREEN_OFF_CHANGED = "android.intent.action.SCREEN_OFF";
    private static final String SCREEN_OFF_TIME_OUT = "screen_off_timeout";

    private static final String ACTION_LED_CHANGED = "android.intent.action.LED";
    private static final String LED_ON = "led_on";

    private static final String ACTION_VOLUME_BALANCE_CHANGED = "android.intent.action.VOLUME_BALANCE";
    private static final String VOLUME_BALANCE = "volume_balance";

    public static IntentFilter getSettingIntentFilter() {

        final IntentFilter filter = new IntentFilter();

        filter.addAction(ACTION_STREAMING_CHANGED);
        filter.addAction(ACTION_NETWORK_PLAY_CHANGED);
        filter.addAction(ACTION_GAIN_MODE_CHANGED);
        filter.addAction(ACTION_SLEEP_MODE_CHANGED);
        filter.addAction(ACTION_BALANCED_OUT_CHANGED);
        filter.addAction(ACTION_USB_DAC_CHANGED);
        filter.addAction(ACTION_HOLD_CHANGED);
        filter.addAction(ACTION_EQUALIZER_CHANGED);
        filter.addAction(ACTION_GAPLESS_CHANGED);
        filter.addAction(ACTION_LINEOUT_CHANGED);
        filter.addAction(ACTION_SCREEN_OFF_CHANGED);
        filter.addAction(ACTION_LED_CHANGED);
        filter.addAction(VOLUME_BALANCE);

        return filter;
    }
}
