/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.ActivityMain;
import info.lamatricexiste.network.R;
import info.lamatricexiste.network.Network.NetInfo;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.net.wifi.WifiManager;
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
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

public class Prefs extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    // TODO: Show values in summary

    private final String TAG = "Prefs";

    public final static String KEY_RESOLVE_NAME = "resolve_name";
    public final static boolean DEFAULT_RESOLVE_NAME = true;

    public final static String KEY_VIBRATE_FINISH = "vibrate_finish";
    public final static boolean DEFAULT_VIBRATE_FINISH = false;

    public final static String KEY_PORT_START = "port_start";
    public final static String DEFAULT_PORT_START = "1";

    public final static String KEY_PORT_END = "port_end";
    public final static String DEFAULT_PORT_END = "1024";
    public final static int MAX_PORT_END = 65535;

    public static final String KEY_SSH_USER = "ssh_user";
    public static final String DEFAULT_SSH_USER = "root";

    //public static final String KEY_NTHREADS = "nthreads";
    //public static final String DEFAULT_NTHREADS = "8";

    public static final String KEY_RESET_NICDB = "resetdb";
    public static final int DEFAULT_RESET_NICDB = 1;

    public static final String KEY_RESET_SERVICESDB = "resetservicesdb";
    public static final int DEFAULT_RESET_SERVICESDB = 1;

    public static final String KEY_METHOD_DISCOVER = "discovery_method";
    public static final String DEFAULT_METHOD_DISCOVER = "0";

    // public static final String KEY_METHOD_PORTSCAN = "method_portscan";
    // public static final String DEFAULT_METHOD_PORTSCAN = "0";

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

    public static final String KEY_INTF = "interface";
    public static final String DEFAULT_INTF = null;

    public static final String KEY_IP_START = "ip_start";
    public static final String DEFAULT_IP_START = "0.0.0.0";

    public static final String KEY_IP_END = "ip_end";
    public static final String DEFAULT_IP_END = "0.0.0.0";

    public static final String KEY_IP_CUSTOM = "ip_custom";
    public static final boolean DEFAULT_IP_CUSTOM = false;
    
    public static final String KEY_CIDR_CUSTOM = "cidr_custom";
    public static final boolean DEFAULT_CIDR_CUSTOM = false;

    public static final String KEY_CIDR = "cidr";
    public static final String DEFAULT_CIDR = "24";

    public static final String KEY_DONATE = "donate";
    public static final String KEY_WEBSITE = "website";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_VERSION = "version";
    public static final String KEY_WIFI = "wifi";

    private static final String URL_DONATE = "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=MDSDWG83PJSNG&lc=CH&item_name=Network%20Discovery%20for%20Android&currency_code=CHF&bn=PP%2dDonationsBF%3abtn_donate_LG%2egif%3aNonHosted";
    private static final String URL_WEB = "http://rorist.github.com/android-network-discovery/";
    private static final String URL_EMAIL = "aubort.jeanbaptiste@gmail.com";

    private Context ctxt;
    private PreferenceScreen ps = null;
    private String before_ip_start;
    private String before_ip_end;
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
        before_ip_start = prefs.getString(KEY_IP_START, DEFAULT_IP_START);
        before_ip_end = prefs.getString(KEY_IP_END, DEFAULT_IP_END);
        before_port_start = prefs.getString(KEY_PORT_START, DEFAULT_PORT_START);
        before_port_end = prefs.getString(KEY_PORT_END, DEFAULT_PORT_END);

        // Interfaces list
        ListPreference intf = (ListPreference) ps.findPreference(KEY_INTF);
        try {
            ArrayList<NetworkInterface> nis = Collections.list(NetworkInterface
                    .getNetworkInterfaces());
            final int len = nis.size();
            // If there's more than just 2 interfaces (local + network)
            if (len > 2) {
                String[] intf_entries = new String[len - 1];
                String[] intf_values = new String[len - 1];
                int i = 0;
                for (int j = 0; j < len; j++) {
                    NetworkInterface ni = nis.get(j);
                    if (!ni.getName().equals("lo")) {
                        intf_entries[i] = ni.getDisplayName();
                        intf_values[i] = ni.getName();
                        i++;
                    }
                }
                intf.setEntries(intf_entries);
                intf.setEntryValues(intf_values);
            } else {
                intf.setEnabled(false);
            }
        } catch (SocketException e) {
            Log.e(TAG, e.getMessage());
            intf.setEnabled(false);
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

        // Contact
        Preference contact = (Preference) ps.findPreference(KEY_EMAIL);
        contact.setSummary(URL_EMAIL);
        contact.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                final Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setType("plain/text");
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { URL_EMAIL });
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Network Discovery");
                try {
                    startActivity(emailIntent);
                } catch (ActivityNotFoundException e) {
                }
                return true;
            }
        });

        // Version
        Preference version = (Preference) ps.findPreference(KEY_VERSION);
        try {
            version.setSummary(getPackageManager().getPackageInfo(ActivityMain.PKG, 0).versionName);
        } catch (NameNotFoundException e) {
            version.setSummary("0.3.x");
        }

    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(KEY_PORT_START) || key.equals(KEY_PORT_END)) {
            checkPortRange();
        } else if (key.equals(KEY_IP_START) || key.equals(KEY_IP_END)) {
            checkIpRange();
        //} else if (key.equals(KEY_NTHREADS)) {
        //    checkMaxThreads();
        } else if (key.equals(KEY_RATECTRL_ENABLE)) {
            checkTimeout(KEY_TIMEOUT_DISCOVER, KEY_RATECTRL_ENABLE, false);
        } else if (key.equals(KEY_CIDR_CUSTOM)) {
            CheckBoxPreference cb = (CheckBoxPreference) ps.findPreference(KEY_CIDR_CUSTOM);
            if (cb.isChecked()) {
                ((CheckBoxPreference)ps.findPreference(KEY_IP_CUSTOM)).setChecked(false);
            }
            sendBroadcast(new Intent(WifiManager.WIFI_STATE_CHANGED_ACTION));
        } else if (key.equals(KEY_IP_CUSTOM)) {
            CheckBoxPreference cb = (CheckBoxPreference) ps.findPreference(KEY_IP_CUSTOM);
            if (cb.isChecked()) {
                ((CheckBoxPreference)ps.findPreference(KEY_CIDR_CUSTOM)).setChecked(false);
            }
            sendBroadcast(new Intent(WifiManager.WIFI_STATE_CHANGED_ACTION));
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

    private void checkIpRange() {
        EditTextPreference ipStartEdit = (EditTextPreference) ps.findPreference(KEY_IP_START);
        EditTextPreference ipEndEdit = (EditTextPreference) ps.findPreference(KEY_IP_END);
        // Check if these are valid IP's
        Pattern pattern = Pattern
                .compile("((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                        + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                        + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                        + "|[1-9][0-9]|[0-9]))");
        Matcher matcher1 = pattern.matcher(ipStartEdit.getText());
        Matcher matcher2 = pattern.matcher(ipEndEdit.getText());
        if (!matcher1.matches() || !matcher2.matches()) {
            ipStartEdit.setText(before_ip_start);
            ipEndEdit.setText(before_ip_end);
            Toast.makeText(ctxt, R.string.preferences_error4, Toast.LENGTH_LONG).show();
            return;
        }
        // Check if ip start is bigger or equal than ip end
        try {
            long ipStart = NetInfo.getUnsignedLongFromIp(ipStartEdit.getText());
            long ipEnd = NetInfo.getUnsignedLongFromIp(ipEndEdit.getText());
            if (ipStart > ipEnd) {
                ipStartEdit.setText(before_ip_start);
                ipEndEdit.setText(before_ip_end);
                Toast.makeText(ctxt, R.string.preferences_error1, Toast.LENGTH_LONG).show();
            }
        } catch (NumberFormatException e) {
            ipStartEdit.setText(before_ip_start);
            ipEndEdit.setText(before_ip_end);
            Toast.makeText(ctxt, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
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

    //private void checkMaxThreads() {
    //    // Check if nthreads is numeric and between 1-256
    //    EditTextPreference threads = (EditTextPreference) ps.findPreference(KEY_NTHREADS);
    //    int nthreads = 0;
    //    try {
    //        nthreads = Integer.parseInt(threads.getText());
    //    } catch (NumberFormatException e) {
    //        threads.setText(DEFAULT_NTHREADS);
    //        Toast.makeText(ctxt, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
    //    }
    //    if (nthreads < 1 || nthreads > 256) {
    //        threads.setText(DEFAULT_NTHREADS);
    //        Toast.makeText(ctxt, R.string.preferences_error2, Toast.LENGTH_LONG).show();
    //    }
    //}
}
