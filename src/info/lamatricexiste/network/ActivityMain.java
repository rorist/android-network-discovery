/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network;

import info.lamatricexiste.network.Utils.Prefs;
import info.lamatricexiste.network.Utils.UpdateNicDb;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Window;

final public class ActivityMain extends Activity {

    private final String TAG = "info.lamatricexiste.network";
    public SharedPreferences prefs = null;
    private Context ctxt;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
        setTitle(R.string.app_loading);
        ctxt = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);

        // Determine the needed installation phases
        if (prefs.getString(Prefs.KEY_METHOD_DISCOVER, Prefs.DEFAULT_METHOD_DISCOVER) == "1") {
            phase1();
        } else {
            phase2();
        }
    }

    private void phase1() {
        // Check Root and Install Daemon
        final RootDaemon rootDaemon = new RootDaemon(this);
        if (rootDaemon.hasRoot) {
            if (prefs.getInt(Prefs.KEY_ROOT_INSTALLED, Prefs.DEFAULT_ROOT_INSTALLED) == 0) {
                // Install
                AlertDialog.Builder d = new AlertDialog.Builder(this);
                d.setTitle(R.string.discover_root_title);
                d.setMessage(R.string.discover_root_install);
                d.setPositiveButton(R.string.btn_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int sumthin) {
                        rootDaemon.install();
                        rootDaemon.permission();
                        Editor edit = prefs.edit();
                        edit.putInt(Prefs.KEY_ROOT_INSTALLED, 1);
                        edit.commit();
                        // rootDaemon.restartActivity();
                        phase2();
                    }
                });
                d.setNegativeButton(R.string.btn_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int sumthin) {
                        phase2();
                    }
                });
                d.show();
            } else {
                // Root daemon already installed
                phase2();
            }
        } else {
            // Don't have root
            phase2();
        }
    }

    private void phase2() {

        class UpdateNicDbMain extends UpdateNicDb {
            private ProgressDialog progress;
            private Context ctxt;

            public UpdateNicDbMain(Context ctxt, SharedPreferences prefs) {
                super(ctxt, prefs); // FIXME: memory leak (make soft ref)
                this.ctxt = ctxt;
            }

            protected void onPreExecute() {
                progress = ProgressDialog.show(this.ctxt, "", "Downloading DB ..."); // FIXME:
                // memory
                // leak
                super.onPreExecute();

            }

            protected void onPostExecute(Void unused) {
                progress.dismiss();
                startDiscoverActivity();
                super.onPostExecute(unused);
            }

            protected void onCancelled() {
                if (progress != null) {
                    progress.dismiss();
                }
                startDiscoverActivity();
                super.onCancelled();
            }
        }

        // CheckNicDb
        try {
            if (prefs.getInt(Prefs.KEY_RESETDB, Prefs.DEFAULT_RESETDB) != getPackageManager()
                    .getPackageInfo(TAG, 0).versionCode) {
                new UpdateNicDbMain(ActivityMain.this, prefs);
            } else {
                // There is a NIC Db installed
                startDiscoverActivity();
            }
        } catch (NameNotFoundException e) {
            startDiscoverActivity();
        } catch (ClassCastException e) {
            Editor edit = prefs.edit();
            edit.putInt(Prefs.KEY_RESETDB, 1);
            edit.commit();
            startDiscoverActivity();
        }
    }

    private void startDiscoverActivity() {
        startActivity(new Intent(ctxt, ActivityDiscovery.class));
        finish();
    }
}
