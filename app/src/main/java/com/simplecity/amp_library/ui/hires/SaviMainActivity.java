package com.simplecity.amp_library.ui.hires;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.simplecity.amp_library.R;
import com.simplecity.amp_library.playback.mediaplayers.SaviPlayer;

import java.io.File;
import java.util.ArrayList;


public class SaviMainActivity extends Activity {

	private final int REQUEST_PERMISSION = 101;

    String extPath;
	File extFile;
	ListView lv;
	String[] items;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.savi_activity_main);
		lv = (ListView) findViewById(R.id.lvPlaylist);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
				&& ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
			// dialog.dismiss();
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
            if (Settings.System.canWrite(this)) {
                Toast.makeText(this, "onCreate: Already Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "onCreate: Not Granted. Permission Requested", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
				return;
            }
        }

		extPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Music/";
		extFile = new File(extPath);
		final ArrayList<File> mySongs = findSongs(extFile);
		items = new String[mySongs.size()];
		for (int i = 0; i < mySongs.size(); i++) {
			// toast(mySongs.get(i).getName().toString());
			// items[i] =
			// mySongs.get(i).getName().toString().replace(".dff","").replace(".wav","");
			items[i] = mySongs.get(i).getName().toString();
		}

		ArrayAdapter<String> adp = new ArrayAdapter<String>(getApplicationContext(), R.layout.savi_song_layout,
				R.id.textView, items);
		lv.setAdapter(adp);
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// Log.i("SaviMusic", "position = "+position); //0, 1, 2, 3, 4, ...
				startActivity(new Intent(getApplicationContext(), SaviPlayer.class).putExtra("pos", position)
						.putExtra("songlist", mySongs));
			}
		});

		boolean isTouchSoundsEnabled = Settings.System.getInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 1) != 0;
		boolean isVibrateOnTouchEnabled = false; // ToDo: Settings.System.getInt(getContentResolver(), Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 1) != 0;
		if(isTouchSoundsEnabled) {
			Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 0);
		}
		if(isVibrateOnTouchEnabled) {
			// ToDo: Settings.System.putInt(getContentResolver(), Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 0);
		}
	}

	public ArrayList<File> findSongs(File root) {
		ArrayList<File> al = new ArrayList<File>();
		File[] files = root.listFiles();
		for (File singleFile : files) {
			if (singleFile.isDirectory() && !singleFile.isHidden()) {
				al.addAll(findSongs(singleFile));
			} else {
				if (singleFile.getName().endsWith(".dff") || singleFile.getName().endsWith(".dsf") || singleFile.getName().endsWith(".wav")) {
					al.add(singleFile);
				}
			}
		}
		return al;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.savi_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == REQUEST_PERMISSION) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Toast.makeText(getApplicationContext(), "OK, SD card permission ", Toast.LENGTH_SHORT).show();
			} else {
				// User refused to grant permission.
			}
		}
	}
}