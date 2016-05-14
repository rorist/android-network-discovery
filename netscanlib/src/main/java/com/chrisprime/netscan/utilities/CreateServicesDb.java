package com.chrisprime.netscan.utilities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.chrisprime.netscan.activities.CannedPrefsActivity;
import com.chrisprime.netscan.activities.CannedScanActivity;
import com.chrisprime.netscan.R;
import com.chrisprime.netscan.network.NetInfo;

import java.io.IOException;
import java.lang.ref.WeakReference;

import timber.log.Timber;

/**
 * Created by cpaian on 4/30/16.
 */
public class CreateServicesDb extends AsyncTask<Void, String, Void> {
    private WeakReference<Activity> mActivity;
    private ProgressDialog progress;

    public CreateServicesDb(Activity activity) {
        mActivity = new WeakReference<Activity>(activity);
    }

    @Override
    protected void onPreExecute() {
        final Activity d = mActivity.get();
        if (d != null) {
            try {
                d.setProgressBarIndeterminateVisibility(true);
                progress = ProgressDialog.show(d, "", d.getString(R.string.task_services));
            } catch (Exception e) {
                Timber.e(e, e.getMessage());
            }
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        final Activity d = mActivity.get();
        if (d != null) {
            Db db = new Db(d.getApplicationContext());
            try {
                // db.copyDbToDevice(R.raw.probes, Db.DB_PROBES);
                db.copyDbToDevice(R.raw.services, Db.DB_SERVICES);
                db.copyDbToDevice(R.raw.saves, Db.DB_SAVES);
                // Save this device in db
                NetInfo net = new NetInfo(d.getApplicationContext());
                ContentValues values = new ContentValues();
                values.put("_id", 0);
                if (net.macAddress == null) {
                    net.macAddress = NetInfo.NOMAC;
                }
                values.put("mac", net.macAddress.replace(":", "").toUpperCase());
                values.put("name", d.getString(R.string.discover_myphone_name));
                SQLiteDatabase data = Db.openDb(Db.DB_SAVES);
                data.insert("nic", null, values);
                data.close();
            } catch (NullPointerException | IOException e) {
                Timber.e(e, e.getMessage());
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void unused) {
        final CannedScanActivity d = (CannedScanActivity) mActivity.get();
        if (d != null) {
            d.setProgressBarIndeterminateVisibility(true);
            if (progress.isShowing()) {
                progress.dismiss();
            }
            try {
                SharedPreferences.Editor edit = NetScanInitHelper.sPreferences.edit();
                edit.putInt(CannedPrefsActivity.KEY_RESET_SERVICESDB, d.getPackageManager().getPackageInfo(
                        CannedScanActivity.PKG, 0).versionCode);
                edit.apply();
            } catch (PackageManager.NameNotFoundException e) {
                Timber.e(e, e.getMessage());
            } finally {
                NetScanInitHelper.startScanningActivity(d);
            }
        }
    }
}
