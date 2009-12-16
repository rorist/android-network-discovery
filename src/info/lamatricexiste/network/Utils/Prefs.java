package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.R;

import java.io.IOException;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.widget.Toast;

public class Prefs extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {
    private final String TAG = "Prefs";

    public final static String KEY_VIBRATE_FINISH = "vibrate_finish";
    public final static boolean DEFAULT_VIBRATE_FINISH = true;

    public final static String KEY_PORT_START = "port_start";
    public final static String DEFAULT_PORT_START = "1";

    public final static String KEY_PORT_END = "port_end";
    public final static String DEFAULT_PORT_END = "1024";

    public static final String KEY_SSH_USER = "ssh_user";
    public static final String DEFAULT_SSH_USER = "root";

    public static final String KEY_NTHREADS = "nthreads";
    public static final String DEFAULT_NTHREADS = "32";

    public static final String KEY_RESETDB = "resetdb";
    public static final String DEFAULT_RESETDB = "1";

    private PreferenceScreen preferenceScreen = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context ctxt = this;

        addPreferencesFromResource(R.xml.preferences);

        preferenceScreen = getPreferenceScreen();
        preferenceScreen.getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        // Action preference
        Preference resetdb = (Preference) preferenceScreen
                .findPreference(KEY_RESETDB);
        resetdb.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(ctxt);
                dialog.setTitle(R.string.preferences_resetdb_action);
                dialog.setPositiveButton(R.string.btn_yes,
                        new OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                    int which) {
                                try {
                                    UpdateNicDb.remoteCopy();
                                    Toast.makeText(getApplicationContext(),
                                            R.string.preferences_resetdb_ok,
                                            Toast.LENGTH_SHORT).show();
                                } catch (IOException e) {
                                    Log.e(TAG, e.getMessage());
                                    Toast.makeText(getApplicationContext(),
                                            R.string.preferences_error3,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                dialog.setNegativeButton(R.string.btn_no, null);
                dialog.show();
                return false;
            }
        });
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

        // Ports Scan
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
        // Max Threads
        else if (key.equals(KEY_NTHREADS)) {
            EditTextPreference threads = (EditTextPreference) preferenceScreen
                    .findPreference(key);
            // Check if nthreads is numeric and between 1-256
            int nthreads = 0;
            try {
                nthreads = Integer.parseInt(threads.getText());
            } catch (NumberFormatException e) {
                threads.setText(DEFAULT_NTHREADS);
                Toast.makeText(getApplicationContext(),
                        e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
            if (nthreads < 1 || nthreads > 256) {
                threads.setText(DEFAULT_NTHREADS);
                Toast.makeText(getApplicationContext(),
                        R.string.preferences_error2, Toast.LENGTH_LONG).show();
            }
        }
    }
}
