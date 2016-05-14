package com.chrisprime.netscan.utilities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.chrisprime.netscan.activities.CannedDiscoveryActivity;
import com.chrisprime.netscan.activities.CannedPrefsActivity;
import com.chrisprime.netscan.activities.CannedScanActivity;

/**
 * Created by cpaian on 5/13/16.
 */
public class NetScanInitHelper {
    public static SharedPreferences sPreferences = null;

    @SuppressLint("CommitPrefEdits")
    public static void initializeNetScan(Activity activity) {
        sPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        new Db(activity);   //Initialize database path

        // Reset interface
        SharedPreferences.Editor edit = sPreferences.edit();    //Suppressed: Happens further down in a later init phase
        edit.putString(CannedPrefsActivity.KEY_INTF, CannedPrefsActivity.DEFAULT_INTF);

        initPhase2(activity);
    }

    private static void initPhase2(final Activity activity) {

        class DbUpdateProbes extends DbUpdate {
            public DbUpdateProbes() {
                super(activity, Db.DB_PROBES, "probes", "regex", 298);
            }

            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);
                final Activity activity1 = mActivity.get();
                initPhase3(activity1);
            }

            protected void onCancelled() {
                super.onCancelled();
                final Activity activity1 = mActivity.get();
                initPhase3(activity1);
            }
        }

        class DbUpdateNic extends DbUpdate {
            public DbUpdateNic() {
                super(activity, Db.DB_NIC, "oui", "mac", 253);
            }

            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);
                new DbUpdateProbes();
            }

            protected void onCancelled() {
                super.onCancelled();
                new DbUpdateProbes();
            }
        }

        // CheckNicDb
        try {
            if (sPreferences.getInt(CannedPrefsActivity.KEY_RESET_NICDB, CannedPrefsActivity.DEFAULT_RESET_NICDB) != activity.getPackageManager()
                    .getPackageInfo(CannedScanActivity.PKG, 0).versionCode) {
                new DbUpdateNic();
            } else {
                // There is a NIC Db installed
                initPhase3(activity);
            }
        } catch (PackageManager.NameNotFoundException e) {
            initPhase3(activity);
        } catch (ClassCastException e) {
            SharedPreferences.Editor edit = sPreferences.edit();
            edit.putInt(CannedPrefsActivity.KEY_RESET_NICDB, 1);
            edit.apply();
            initPhase3(activity);
        }
    }

    private static void initPhase3(final Activity activity) {
        // Install Services DB

        try {
            if (sPreferences.getInt(CannedPrefsActivity.KEY_RESET_SERVICESDB, CannedPrefsActivity.DEFAULT_RESET_SERVICESDB) != activity.getPackageManager()
                    .getPackageInfo(CannedScanActivity.PKG, 0).versionCode) {
                new CreateServicesDb(activity).execute();
            } else {
                startScanningActivity(activity);
            }
        } catch (PackageManager.NameNotFoundException e) {
            startScanningActivity(activity);
        }
    }

    public static void startScanningActivity(final Activity activity) {
        activity.startActivity(new Intent(activity, CannedDiscoveryActivity.class));
        activity.finish();
    }
}
