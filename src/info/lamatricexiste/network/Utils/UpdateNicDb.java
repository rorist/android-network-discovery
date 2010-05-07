/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.R;
import info.lamatricexiste.network.Network.DownloadFile;
import info.lamatricexiste.network.Network.NetInfo;

import java.io.IOException;
import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class UpdateNicDb extends AsyncTask<Void, String, Void> {

    private final static String TAG = "UpdateNicDb";
    private final static String DB_REMOTE = "http://download.lamatricexiste.info/oui.db";
    private final static String DB_PATH = "/data/data/info.lamatricexiste.network/";
    private final static String DB_NAME = "oui.db";
    private int nb;
    private ProgressDialog progress;
    protected WeakReference<Activity> mActivity;

    public UpdateNicDb(Activity activity) {
        mActivity = new WeakReference<Activity>(activity);
        final Activity d = mActivity.get();
        final AlertDialog.Builder dialog = new AlertDialog.Builder(d);
        dialog.setMessage(R.string.preferences_resetdb_action);
        dialog.setPositiveButton(R.string.btn_yes, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                execute();
            }
        });
        dialog.setNegativeButton(R.string.btn_no, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                cancel(true);
            }
        });
        dialog.show();
    }

    @Override
    protected void onPreExecute() {
        final Activity d = mActivity.get();
        d.setProgressBarIndeterminateVisibility(true);
        progress = ProgressDialog.show(d, "", "Downloading DB ...");
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            nb = countEntries();
            remoteCopy(mActivity.get());
        } catch (IOException e) {
            cancel(true);
        }
        return null;
    }

    private void remoteCopy(Context ctxt) throws IOException {
        Log.v(TAG, "Copying oui.db remotly");
        if (NetInfo.isConnected(ctxt)) {
            new DownloadFile(DB_REMOTE, DB_PATH + DB_NAME);
        }
    }

    private int countEntries() {
        int nb = 0;
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null,
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor c = db.rawQuery("select count(mac) from oui", null);
            if (c.getCount() > 0) {
                c.moveToFirst();
                nb = c.getInt(0);
            }
            c.close();
            db.close();
        } catch (SQLiteException e) {
            return nb;
        }
        return nb;
    }

    @Override
    protected void onPostExecute(Void unused) {
        final Activity d = mActivity.get();
        progress.dismiss();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(d.getApplication());
        d.setProgressBarIndeterminateVisibility(false);
        Toast.makeText(d.getApplicationContext(),
                String.format(d.getString(R.string.preferences_resetdb_ok), (countEntries() - nb)),
                Toast.LENGTH_LONG).show();
        try {
            Editor edit = prefs.edit();
            edit.putInt(Prefs.KEY_RESETDB, d.getPackageManager().getPackageInfo(
                    "info.lamatricexiste.network", 0).versionCode);
            edit.commit();
        } catch (NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    protected void onCancelled() {
        final Activity d = mActivity.get();
        if (progress != null) {
            progress.dismiss();
        }
        Toast.makeText(d.getApplicationContext(), R.string.preferences_error3, Toast.LENGTH_SHORT)
                .show();
    }
}
