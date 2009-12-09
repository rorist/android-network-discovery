package info.lamatricexiste.network;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class Prefs extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {
	// private final String TAG = "Prefs";

	public final static String KEY_VIBRATE_FINISH = "vibrate_finish";
	public final static boolean DEFAULT_VIBRATE_FINISH = true;

	public final static String KEY_PORT_START = "port_start";
	public final static String KEY_PORT_END = "port_end";
    public static final String KEY_SSH_USER = "ssh_user";
	public final static String DEFAULT_PORT_START = "1";
	public final static String DEFAULT_PORT_END = "1024";
    public static final String DEFAULT_SSH_USER = "root";

	private PreferenceScreen preferenceScreen = null;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		preferenceScreen = getPreferenceScreen();
		preferenceScreen.getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

		if (key.equals(KEY_PORT_START) || key.equals(KEY_PORT_END)) {
			EditTextPreference port = (EditTextPreference) preferenceScreen
					.findPreference(key);
			// Check if port are numeric
			try {
				Integer.parseInt(port.getText());
			} catch (NumberFormatException e) {
				if (key.equals(KEY_PORT_START)) {
					port.setText(DEFAULT_PORT_START);
				} else if (key.equals(KEY_PORT_END)) {
					port.setText(DEFAULT_PORT_END);
				}
				Toast.makeText(getApplicationContext(),
						e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			}
			// Check if port start is bigger or equal than port end
			EditTextPreference portStartEdit = (EditTextPreference) preferenceScreen
					.findPreference(KEY_PORT_START);
			EditTextPreference portEndEdit = (EditTextPreference) preferenceScreen
					.findPreference(KEY_PORT_END);
			int portStart = Integer.parseInt(portStartEdit.getText());
			int portEnd = Integer.parseInt(portEndEdit.getText());
			if (portStart >= portEnd) {
				portStartEdit.setText(DEFAULT_PORT_START);
				portEndEdit.setText(DEFAULT_PORT_END);
				Toast.makeText(getApplicationContext(),
						R.string.preferences_error1, Toast.LENGTH_LONG).show();
			}
		}
	}
}
