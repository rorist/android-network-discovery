/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.AbstractRoot;
import info.lamatricexiste.network.ActivityMain;
import info.lamatricexiste.network.R;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.Settings;
import android.view.Window;
import android.widget.Toast;

public class Prefs extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    // TODO: Show value in summary
    // private final String TAG = "Prefs";

    public final static String KEY_RESOLVE_NAME = "resolve_name";
    public final static boolean DEFAULT_RESOLVE_NAME = false;

    public final static String KEY_VIBRATE_FINISH = "vibrate_finish";
    public final static boolean DEFAULT_VIBRATE_FINISH = false;

    public final static String KEY_PORT_START = "port_start";
    public final static String DEFAULT_PORT_START = "1";

    public final static String KEY_PORT_END = "port_end";
    public final static String DEFAULT_PORT_END = "1024";
    public final static int MAX_PORT_END = 65535;

    public static final String KEY_SSH_USER = "ssh_user";
    public static final String DEFAULT_SSH_USER = "root";

    public static final String KEY_NTHREADS = "nthreads";
    public static final String DEFAULT_NTHREADS = "32";

    public static final String KEY_RESET_NICDB = "resetdb";
    public static final int DEFAULT_RESET_NICDB = 1;

    public static final String KEY_RESET_SERVICESDB = "resetservicesdb";
    public static final int DEFAULT_RESET_SERVICESDB = 1;

    public static final String KEY_ROOT_INSTALLED = "root_installed";
    public static final int DEFAULT_ROOT_INSTALLED = 0;

    public static final String KEY_METHOD_DISCOVER = "method_discovery";
    public static final String DEFAULT_METHOD_DISCOVER = "0";

    public static final String KEY_METHOD_PORTSCAN = "method_portscan";
    public static final String DEFAULT_METHOD_PORTSCAN = "0";

    public final static String KEY_TIMEOUT_FORCE = "timeout_force";
    public final static boolean DEFAULT_TIMEOUT_FORCE = false;

    public final static String KEY_TIMEOUT_PORTSCAN = "timeout_portscan";
    public final static String DEFAULT_TIMEOUT_PORTSCAN = "500";

    public static final String KEY_RATECTRL_ENABLE = "ratecontrol_enable";
    public static final boolean DEFAULT_RATECTRL_ENABLE = true;

    public final static String KEY_TIMEOUT_DISCOVER = "timeout_discover";
    public final static String DEFAULT_TIMEOUT_DISCOVER = "500";

    public static final String KEY_BANNER = "banner";
    public static final boolean DEFAULT_BANNER = true;

    public static final String KEY_MOBILE = "allow_mobile";
    public static final boolean DEFAULT_MOBILE = false;

    public static final String KEY_DONATE = "donate";
    public static final String KEY_WEBSITE = "website";
    public static final String KEY_VERSION = "version";
    public static final String KEY_WIFI = "wifi";

    private static final String URL_DONATE = "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=MDSDWG83PJSNG&lc=CH&item_name=Network%20Discovery%20for%20Android&currency_code=CHF&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHosted";
    private static final String URL_WEB = "http://rorist.github.com/android-network-discovery/";

    private Context ctxt;
    private PreferenceScreen ps = null;
    private String before_port_start;
    private String before_port_end;

    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        ctxt = getApplicationContext();

        ps = getPreferenceScreen();
        ps.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        // Default state of checkboxes
        checkTimeout(KEY_TIMEOUT_PORTSCAN, KEY_TIMEOUT_FORCE, true);
        checkTimeout(KEY_TIMEOUT_DISCOVER, KEY_RATECTRL_ENABLE, false);

        // Reset Nic DB click listener
        Preference resetdb = (Preference) ps.findPreference(KEY_RESET_NICDB);
        resetdb.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                new UpdateNicDb(Prefs.this);
                return false;
            }
        });

        // Before change values
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
        before_port_start = prefs.getString(KEY_PORT_START, DEFAULT_PORT_START);
        before_port_end = prefs.getString(KEY_PORT_END, DEFAULT_PORT_END);

        // Root check
        if (!AbstractRoot.checkRoot()) {
            ListPreference md = (ListPreference) ps.findPreference(KEY_METHOD_DISCOVER);
            if (md != null) {
                md.setEnabled(false);
            }
        }

        // Wifi settings listener
        ((Preference) ps.findPreference(KEY_WIFI))
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        return true;
                    }
                });

        // Donate click listener
        ((Preference) ps.findPreference(KEY_DONATE))
                .setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(URL_DONATE));
                        startActivity(i);
                        return true;
                    }
                });

        // Website
        Preference website = (Preference) ps.findPreference(KEY_WEBSITE);
        website.setSummary(URL_WEB);
        website.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(URL_WEB));
                startActivity(i);
                return true;
            }
        });

        // Version
        Preference version = (Preference) ps.findPreference(KEY_VERSION);
        try {
            version.setSummary(getPackageManager().getPackageInfo(ActivityMain.TAG, 0).versionName);
        } catch (NameNotFoundException e) {
            version.setSummary("0.3x");
        }

    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(KEY_PORT_START) || key.equals(KEY_PORT_END)) {
            checkPortRange();
        } else if (key.equals(KEY_NTHREADS)) {
            checkMaxThreads();
        } else if (key.equals(KEY_TIMEOUT_FORCE)) {
            checkTimeout(KEY_TIMEOUT_PORTSCAN, KEY_TIMEOUT_FORCE, true);
        } else if (key.equals(KEY_RATECTRL_ENABLE)) {
            checkTimeout(KEY_TIMEOUT_DISCOVER, KEY_RATECTRL_ENABLE, false);
        }
    }

    private void checkTimeout(String key_pref, String key_cb, boolean value) {
        EditTextPreference timeout = (EditTextPreference) ps.findPreference(key_pref);
        CheckBoxPreference cb = (CheckBoxPreference) ps.findPreference(key_cb);
        if (cb.isChecked()) {
            timeout.setEnabled(value);
        } else {
            timeout.setEnabled(!value);
        }
    }

    private void checkPortRange() {
        // Check if port start is bigger or equal than port end
        EditTextPreference portStartEdit = (EditTextPreference) ps.findPreference(KEY_PORT_START);
        EditTextPreference portEndEdit = (EditTextPreference) ps.findPreference(KEY_PORT_END);
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

    private void checkMaxThreads() {
        // Check if nthreads is numeric and between 1-256
        EditTextPreference threads = (EditTextPreference) ps.findPreference(KEY_NTHREADS);
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
