/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network;

import info.lamatricexiste.network.Utils.Db;
import info.lamatricexiste.network.Utils.Prefs;
import info.lamatricexiste.network.Utils.UpdateNicDb;

import java.io.IOException;
import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Window;

final public class ActivityMain extends Activity {

    public final static String TAG = "info.lamatricexiste.network";
    public static SharedPreferences prefs = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
        setTitle(R.string.app_loading);
        final Context ctxt = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);

        // Reset interface
        Editor edit = prefs.edit();
        edit.putString(Prefs.KEY_INTF, Prefs.DEFAULT_INTF);
        edit.commit();

        // Determine the needed installation phases
        if (prefs.getString(Prefs.KEY_METHOD_DISCOVER, Prefs.DEFAULT_METHOD_DISCOVER) == "1") {
            phase1(ctxt);
        } else {
            phase2(ctxt);
        }
    }

    private void phase1(final Context ctxt) {
        phase2(ctxt);
        // Check Root and Install Daemon
        // final RootDaemon rootDaemon = new RootDaemon(this);
        // if (rootDaemon.hasRoot) {
        // if (prefs.getInt(Prefs.KEY_ROOT_INSTALLED,
        // Prefs.DEFAULT_ROOT_INSTALLED) == 0) {
        // // Install
        // AlertDialog.Builder d = new AlertDialog.Builder(this);
        // d.setTitle(R.string.discover_root_title);
        // d.setMessage(R.string.discover_root_install);
        // d.setPositiveButton(R.string.btn_yes, new
        // DialogInterface.OnClickListener() {
        // public void onClick(DialogInterface dlg, int sumthin) {
        // rootDaemon.install();
        // rootDaemon.permission();
        // Editor edit = prefs.edit();
        // edit.putInt(Prefs.KEY_ROOT_INSTALLED, 1);
        // edit.commit();
        // // rootDaemon.restartActivity();
        // phase2(ctxt);
        // }
        // });
        // d.setNegativeButton(R.string.btn_no, new
        // DialogInterface.OnClickListener() {
        // public void onClick(DialogInterface dlg, int sumthin) {
        // phase2(ctxt);
        // }
        // });
        // d.show();
        // } else {
        // // Root daemon already installed
        // phase2(ctxt);
        // }
        // } else {
        // // Don't have root
        // phase2(ctxt);
        // }
    }

    private void phase2(final Context ctxt) {

        class UpdateNicDbMain extends UpdateNicDb {
            public UpdateNicDbMain(Activity activity) {
                super(activity);
            }

            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);
                final Activity d = mActivity.get();
                phase3(d);
            }

            protected void onCancelled() {
                super.onCancelled();
                final Activity d = mActivity.get();
                phase3(d);
            }
        }

        // CheckNicDb
        try {
            if (prefs.getInt(Prefs.KEY_RESET_NICDB, Prefs.DEFAULT_RESET_NICDB) != getPackageManager()
                    .getPackageInfo(TAG, 0).versionCode) {
                new UpdateNicDbMain(ActivityMain.this);
            } else {
                // There is a NIC Db installed
                phase3(ctxt);
            }
        } catch (NameNotFoundException e) {
            phase3(ctxt);
        } catch (ClassCastException e) {
            Editor edit = prefs.edit();
            edit.putInt(Prefs.KEY_RESET_NICDB, 1);
            edit.commit();
            phase3(ctxt);
        }
    }

    private void phase3(final Context ctxt) {
        // Install Services DB

        try {
            if (prefs.getInt(Prefs.KEY_RESET_SERVICESDB, Prefs.DEFAULT_RESET_SERVICESDB) != getPackageManager()
                    .getPackageInfo(TAG, 0).versionCode) {
                new CreateServicesDb(ActivityMain.this).execute();
            } else {
                startDiscoverActivity(ctxt);
            }
        } catch (NameNotFoundException e) {
            startDiscoverActivity(ctxt);
        }
    }

    private void startDiscoverActivity(final Context ctxt) {
        startActivity(new Intent(ctxt, ActivityDiscovery.class));
        finish();
    }

    static class CreateServicesDb extends AsyncTask<Void, String, Void> {
        private WeakReference<Activity> mActivity;
        private ProgressDialog progress;

        public CreateServicesDb(Activity activity) {
            mActivity = new WeakReference<Activity>(activity);
        }

        @Override
        protected void onPreExecute() {
            final Activity d = mActivity.get();
            if (d != null) {
                d.setProgressBarIndeterminateVisibility(true);
                progress = ProgressDialog.show(d, "", d.getString(R.string.task_services));
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            final Activity d = mActivity.get();
            if (d != null) {
                Db db = new Db(d.getApplicationContext());
                try {
                    db.copyDbToDevice(R.raw.services, Db.DB_SERVICES);
                    db.copyDbToDevice(R.raw.probes, Db.DB_PROBES);
                } catch (NullPointerException e) {
                    Log.e(TAG, e.getMessage());
                } catch (IOException e) {
                    if (e != null && e.getMessage() != null) {
                        Log.e(TAG, e.getMessage());
                    } else {
                        Log.e(TAG, "IOException");
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            final ActivityMain d = (ActivityMain) mActivity.get();
            if (d != null) {
                d.setProgressBarIndeterminateVisibility(true);
                if (progress.isShowing()) {
                    progress.dismiss();
                }
                try {
                    Editor edit = prefs.edit();
                    edit.putInt(Prefs.KEY_RESET_SERVICESDB, d.getPackageManager().getPackageInfo(
                            "info.lamatricexiste.network", 0).versionCode);
                    edit.commit();
                } catch (NameNotFoundException e) {
                    Log.e(TAG, e.getMessage());
                } finally {
                    d.startDiscoverActivity(d);
                }
            }
        }
    }
}
