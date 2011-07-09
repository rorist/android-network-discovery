/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.ActivityMain;
import info.lamatricexiste.network.R;
import info.lamatricexiste.network.Network.DownloadFile;
import info.lamatricexiste.network.Network.NetInfo;

import java.io.IOException;
import java.lang.NullPointerException;
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
    private final static String DB_REMOTE = "http://download.lamatricexiste.info/nic.db.gz";
    private final static String REQ = "select count(mac) from oui";
    private int nb;
    private ProgressDialog progress;
    protected WeakReference<Activity> mActivity;

    public UpdateNicDb(Activity activity) {
        mActivity = new WeakReference<Activity>(activity);
        if (mActivity != null) {
            final Activity d = mActivity.get();
            if (d != null) {
                final AlertDialog.Builder dialog = new AlertDialog.Builder(d);
                dialog.setMessage(d.getString(R.string.preferences_resetdb_action, Db.DB_NIC, 253));
                dialog.setPositiveButton(R.string.btn_yes, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (!getStatus().equals(Status.RUNNING)) {
                            execute();
                        }
                    }
                });
                dialog.setNegativeButton(R.string.btn_no, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        cancel(true);
                    }
                });
                dialog.show();
            }
        }
    }

    @Override
    protected void onPreExecute() {
        if (mActivity != null) {
            final Activity d = mActivity.get();
            if (d != null) {
                d.setProgressBarIndeterminateVisibility(true);
                progress = ProgressDialog.show(d, "", d.getString(R.string.task_db, Db.DB_NIC));
            }
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            nb = countEntries();
            if (mActivity != null) {
                final Activity d = mActivity.get();
                if (d != null) {
                    remoteCopy(d);
                }
            }
        } catch (IOException e) {
            cancel(true);
        } catch (NullPointerException e) {
            cancel(true);
        }
        return null;
    }

    private void remoteCopy(final Context ctxt) throws IOException {
        Log.v(TAG, "Copying oui.db remotly");
        if (NetInfo.isConnected(ctxt)) {
            new DownloadFile(ctxt, DB_REMOTE, ctxt.openFileOutput(Db.DB_NIC, Context.MODE_PRIVATE));
        }
    }

    private int countEntries() {
        int nb = 0;
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(Db.PATH + Db.DB_NIC, null,
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor c = db.rawQuery(REQ, null);
            if (c.moveToFirst()) {
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
        if (mActivity != null) {
            final Activity d = mActivity.get();
            if (d != null) {
                if (progress.isShowing()) {
                    progress.dismiss();
                }
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(d
                        .getApplication());
                d.setProgressBarIndeterminateVisibility(false);
                Toast.makeText(d.getApplicationContext(),
                        d.getString(R.string.preferences_resetdb_ok, (countEntries() - nb)),
                        Toast.LENGTH_LONG).show();
                try {
                    Editor edit = prefs.edit();
                    edit.putInt(Prefs.KEY_RESET_NICDB, d.getPackageManager().getPackageInfo(
                            ActivityMain.PKG, 0).versionCode);
                    edit.commit();
                } catch (NameNotFoundException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }

    @Override
    protected void onCancelled() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
        if (mActivity != null) {
            final Activity d = mActivity.get();
            if (d != null) {
                Toast.makeText(d.getApplicationContext(), R.string.preferences_error3,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
