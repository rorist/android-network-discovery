package info.lamatricexiste.network;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Prefs extends PreferenceActivity {
	// private final String TAG = "Prefs";
	public final static String KEY_VIBRATE_FINISH = "vibrate_finish";
	public final static boolean DEFAULT_VIBRATE_FINISH = true;

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
