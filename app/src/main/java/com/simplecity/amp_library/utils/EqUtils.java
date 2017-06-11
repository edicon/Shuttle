package com.simplecity.amp_library.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.simplecity.amp_library.services.EqualizerService;

public class EqUtils {

    private static SharedPreferences mPrefs;
    private final static int EQUALIZER_MAX_BANDS = 10;
    private static int mNumberEqualizerBands;

    public static void updateEq(AppCompatActivity a) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(a);
        mNumberEqualizerBands = Integer.parseInt(mPrefs.getString("equalizer.number_of_bands", ""+ EQUALIZER_MAX_BANDS));

        final int[] centerFreqs = getCenterFreqs();
        final int[] bandLevelRange = getBandLevelRange();

        for (int band = 0; band < mNumberEqualizerBands; band++) {
            //Unit conversion from mHz to Hz and use k prefix if necessary to display
            float centerFreqHz = centerFreqs[band] / 1000;
            String unitPrefix = "";
            if (centerFreqHz >= 1000) {
                centerFreqHz = centerFreqHz / 1000;
                unitPrefix = "k";
            }
            int level = bandLevelRange[0]; //  + (progress * 100);
            equalizerBandUpdate(band, level);
        }
    }

    public static boolean getEqualizerEnabled(AppCompatActivity a) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(a);
        final boolean isEnabled = mPrefs.getBoolean("audiofx.global.enable", false);
        return isEnabled;
    }
    public static void saveEqEnabled(AppCompatActivity a, boolean on) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(a);
        mPrefs.edit().putBoolean("audiofx.global.enable", on).apply();
    }

    private static int[] getBandLevelRange() {
        String savedCenterFreqs = mPrefs.getString("equalizer.band_level_range", null);
        if (savedCenterFreqs == null || savedCenterFreqs.isEmpty()) {
            return new int[]{-1500, 1500};
        } else {
            String[] split = savedCenterFreqs.split(";");
            int[] freqs = new int[split.length];
            for (int i = 0; i < split.length; i++) {
                freqs[i] = Integer.valueOf(split[i]);
            }
            return freqs;
        }
    }

    private static int[] getCenterFreqs() {
        String savedCenterFreqs = mPrefs.getString("equalizer.center_freqs", EqualizerService.getZeroedBandsString(mNumberEqualizerBands));
        String[] split = savedCenterFreqs.split(";");
        int[] freqs = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            freqs[i] = Integer.valueOf(split[i]);
        }
        return freqs;
    }

    private static void equalizerBandUpdate(final int band, final int level) {

        String[] currentCustomLevels = mPrefs.getString("audiofx.eq.bandlevels.custom", EqualizerService.getZeroedBandsString(mNumberEqualizerBands)).split(";");

        currentCustomLevels[band] = String.valueOf(level);

        // save
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mNumberEqualizerBands; i++) {
            builder.append(currentCustomLevels[i]);
            builder.append(";");
        }
        builder.deleteCharAt(builder.length() - 1);
        mPrefs.edit().putString("audiofx.eq.bandlevels", builder.toString()).apply();
        mPrefs.edit().putString("audiofx.eq.bandlevels.custom", builder.toString()).apply();

        // ToDo: UpdateEQService()
    }
}
