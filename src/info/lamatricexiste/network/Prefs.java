package info.lamatricexiste.network;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Prefs extends PreferenceActivity {
	// private final String TAG = "Prefs";

	public final static String KEY_VIBRATE_FINISH = "vibrate_finish";
	public final static boolean DEFAULT_VIBRATE_FINISH = true;

	public final static String KEY_PORT_START = "port_start";
	public final static String KEY_PORT_END = "port_end";
	public final static String DEFAULT_PORT_START = "1";
	public final static String DEFAULT_PORT_END = "1024";

	// private CheckBoxPreference mVibrate;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		// PreferenceScreen preferenceScreen = getPreferenceScreen();
		// preferenceScreen.getSharedPreferences()
		// .registerOnSharedPreferenceChangeListener(this);
		// mVibrate = (CheckBoxPreference) preferenceScreen
		// .findPreference(KEY_VIBRATE_FINISH);
	}

	// public void onSharedPreferenceChanged(SharedPreferences prefs, String
	// key) {
	// }
}
