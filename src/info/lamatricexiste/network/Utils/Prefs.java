package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.R;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.Window;
import android.widget.Toast;

public class Prefs extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    // private final String TAG = "Prefs";

    public final static String KEY_RESOLVE_NAME = "resolve_name";
    public final static boolean DEFAULT_RESOLVE_NAME = false;

    public final static String KEY_VIBRATE_FINISH = "vibrate_finish";
    public final static boolean DEFAULT_VIBRATE_FINISH = true;

    public final static String KEY_PORT_START = "port_start";
    public final static String DEFAULT_PORT_START = "1";

    public final static String KEY_PORT_END = "port_end";
    public final static String DEFAULT_PORT_END = "1024";
    public final static int MAX_PORT_END = 65535;

    public static final String KEY_SSH_USER = "ssh_user";
    public static final String DEFAULT_SSH_USER = "root";

    public static final String KEY_NTHREADS = "nthreads";
    public static final String DEFAULT_NTHREADS = "32";

    public static final String KEY_RESETDB = "resetdb";
    public static final int DEFAULT_RESETDB = 1;

    private Context ctxt;
    private PreferenceScreen preferenceScreen = null;
    private String before_port_start;
    private String before_port_end;

    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        ctxt = getApplicationContext();

        preferenceScreen = getPreferenceScreen();
        preferenceScreen.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        // Reset DB click listener
        Preference resetdb = (Preference) preferenceScreen.findPreference(KEY_RESETDB);
        resetdb.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                new UpdateNicDb(Prefs.this, PreferenceManager.getDefaultSharedPreferences(ctxt));
                return false;
            }
        });

        // Before change values
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
        before_port_start = prefs.getString(KEY_PORT_START, DEFAULT_PORT_START);
        before_port_end = prefs.getString(KEY_PORT_END, DEFAULT_PORT_END);
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(KEY_PORT_START) || key.equals(KEY_PORT_END)) {
            checkPortRange();
        } else if (key.equals(KEY_NTHREADS)) {
            checkMaxThreads(key);
        }
    }

    private void checkPortRange() {
        // Check if port start is bigger or equal than port end
        EditTextPreference portStartEdit = (EditTextPreference) preferenceScreen
                .findPreference(KEY_PORT_START);
        EditTextPreference portEndEdit = (EditTextPreference) preferenceScreen
                .findPreference(KEY_PORT_END);
        try {
            int portStart = Integer.parseInt(portStartEdit.getText());
            int portEnd = Integer.parseInt(portEndEdit.getText());
            if (portStart >= portEnd) {
                portStartEdit.setText(before_port_start);
                portEndEdit.setText(before_port_end);
                Toast.makeText(ctxt, R.string.preferences_error1, Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            portStartEdit.setText(before_port_start);
            portEndEdit.setText(before_port_end);
            Toast.makeText(ctxt, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkMaxThreads(String key) {
        // Check if nthreads is numeric and between 1-256
        EditTextPreference threads = (EditTextPreference) preferenceScreen.findPreference(key);
        int nthreads = 0;
        try {
            nthreads = Integer.parseInt(threads.getText());
        } catch (NumberFormatException e) {
            threads.setText(DEFAULT_NTHREADS);
            Toast.makeText(ctxt, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
        if (nthreads < 1 || nthreads > 256) {
            threads.setText(DEFAULT_NTHREADS);
            Toast.makeText(ctxt, R.string.preferences_error2, Toast.LENGTH_LONG).show();
        }
    }
}
