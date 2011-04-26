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

public class DbUpdate extends AsyncTask<Void, String, Void> {

    private final static String TAG = "DbUpdate";
    private final static String DB_REMOTE = "http://download.lamatricexiste.info/%s.gz";
    private final static String REQ = "select count(%1$s) from %2$s";
    private int nb;
    private String file;
    private String database;
    private String field;
    private ProgressDialog progress;
    protected WeakReference<Activity> mActivity;

    public DbUpdate(Activity activity, String file, String database, String field, int kilobytes) {
        this.file = file;
        this.database = database;
        this.field = field;
        mActivity = new WeakReference<Activity>(activity);
        if (mActivity != null) {
            final Activity d = mActivity.get();
            if (d != null) {
                final AlertDialog.Builder dialog = new AlertDialog.Builder(d);
                dialog
                        .setMessage(d.getString(R.string.preferences_resetdb_action, file,
                                kilobytes));
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
                progress = ProgressDialog.show(d, "", d.getString(R.string.task_db, file));
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
        } catch (Exception e) {
            cancel(true);
        }
        return null;
    }

    private void remoteCopy(final Context ctxt) throws IOException, NullPointerException {
        Log.v(TAG, "Copying " + file + " remotly");
        if (NetInfo.isConnected(ctxt)) {
            new DownloadFile(ctxt, String.format(DB_REMOTE, file), ctxt.openFileOutput(file,
                    Context.MODE_PRIVATE));
        }
    }

    private int countEntries() {
        SQLiteDatabase db = null;
        int nb = 0;
        try {
            db = SQLiteDatabase.openDatabase(Db.PATH + file, null,
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor c = db.rawQuery(String.format(REQ, field, database), null);
            if (c.moveToFirst()) {
                nb = c.getInt(0);
            }
            c.close();
            db.close();
        } catch (SQLiteException e) {
            if (db != null) {
                db.close();
            }
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
