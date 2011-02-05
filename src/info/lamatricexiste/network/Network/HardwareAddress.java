/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

// http://standards.ieee.org/regauth/oui/oui.txt

package info.lamatricexiste.network.Network;

import info.lamatricexiste.network.R;
import info.lamatricexiste.network.Utils.Prefs;
import info.lamatricexiste.network.Utils.Db;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.preference.PreferenceManager;
import android.util.Log;

public class HardwareAddress {

    private final static String TAG = "HardwareAddress";
    private final static String REQ = "select vendor from oui where mac=?";
    private final static int BUF = 8 * 1024;
    private SQLiteDatabase db = null;
    private WeakReference<Activity> mActivity;

    public HardwareAddress(Activity activity) {
        mActivity = new WeakReference<Activity>(activity);
        try {
            db = SQLiteDatabase.openDatabase(Db.PATH + Db.DB_NIC, null,
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        } catch (SQLiteException e) {
            Log.e(TAG, e.getMessage());
            final Activity d = mActivity.get();
            if (d != null) {
                Context ctxt = d.getApplicationContext();
                Editor edit = PreferenceManager.getDefaultSharedPreferences(ctxt).edit();
                edit.putInt(Prefs.KEY_RESET_NICDB, 1);
                edit.commit();
            }
        }
    }

    public void dbClose() {
        if (db != null) {
            db.close();
        }
    }

    public String getHardwareAddress(String ip) {
        // Get intf
        String intf = "(tiwlan0|eth0)";
        if (mActivity != null) {
            final Activity a = mActivity.get();
            if (a != null) {
                NetInfo net = new NetInfo(a.getApplicationContext());
                intf = net.intf;
            }
        }
        // Get HW Addr
        String hw = NetInfo.NOMAC;
        try {
            if (ip != null) {
                String ptrn = "^" + ip.replace(".", "\\.")
                        + "\\s+0x1\\s+0x2\\s+([:0-9a-fA-F]+)\\s+\\*\\s+" + intf + "$";
                Pattern pattern = Pattern.compile(ptrn);
                BufferedReader bufferedReader = new BufferedReader(new FileReader("/proc/net/arp"),
                        BUF);
                String line;
                Matcher matcher;
                while ((line = bufferedReader.readLine()) != null) {
                    matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        hw = matcher.group(1);
                        break;
                    }
                }
                bufferedReader.close();
            } else {
                Log.e(TAG, "ip is null");
            }
        } catch (IOException e) {
            Log.d(TAG, "Can't open/read file ARP: " + e.getMessage());
        }
        return hw;
    }

    public String getNicVendor(String hw) throws SQLiteDatabaseCorruptException {
        if (mActivity != null) {
            final Activity a = mActivity.get();
            if (a != null) {
                final Context ctxt = a.getApplicationContext();
                String ni = ctxt.getString(R.string.info_unknown);
                if (db != null) {
                    // Db request
                    try {
                        synchronized (db) {
                            if (db.isOpen()) {
                                Cursor c = db.rawQuery(REQ, new String[] { hw.replace(":", "")
                                        .substring(0, 6).toUpperCase() });
                                if (c.moveToFirst()) {
                                    ni = c.getString(0);
                                }
                                c.close();
                            }

                        }
                    } catch (IllegalStateException e) {
                        Log.e(TAG, e.getMessage());
                    } catch (SQLiteException e) {
                        Log.e(TAG, e.getMessage());
                        Editor edit = PreferenceManager.getDefaultSharedPreferences(ctxt).edit();
                        edit.putInt(Prefs.KEY_RESET_NICDB, 1);
                        edit.commit();
                    }
                }
                return ni;
            }
        }
        return "Unknown";
    }
}
