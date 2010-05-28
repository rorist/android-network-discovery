/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

public class ServicesDb extends SQLiteOpenHelper {
    private final static String TAG = "ServicesDb";
    private final static String DB_NAME = "services.db";
    private final static String DB_TABLE = "services";
    private final static String DROP_TABLE = "DROP TABLE IF EXISTS " + DB_TABLE;
    private final static int DB_TABLE_RES = R.raw.services;
    private final static int DB_VERSION = 1;
    private Context ctxt;

    public ServicesDb(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        ctxt = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.v(TAG, "onCreate");
    }

    public void onOpen(SQLiteDatabase db) {
        Log.v(TAG, "onOpen");
        // FIXME: Do this in onCreate ???
        createTable(db, DB_TABLE, DB_TABLE_RES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int Old, int New) {
        Log.v(TAG, "onUpgrade");
        db.execSQL(DROP_TABLE);
        onCreate(db);
    }

    private void createTable(SQLiteDatabase db, String table_name, int resource) {
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='"
                + table_name + "'", null);
        try {
            if (c.getCount() == 0) {
                // Initial table creation and insert
                Log.v(TAG, "createTable " + table_name);
                InputStream stream = ctxt.getResources().openRawResource(resource);
                InputStreamReader is = new InputStreamReader(stream);
                BufferedReader in = new BufferedReader(is, 4);
                String str;
                while ((str = in.readLine()) != null) {
                    if (!str.equals("BEGIN TRANSACTION;") && !str.equals("COMMIT;")
                            && !str.equals("")) {
                        db.execSQL(str);
                    }
                }
                // Set Nic DB as installed
                Editor edit = PreferenceManager.getDefaultSharedPreferences(ctxt).edit();
                edit.putInt(Prefs.KEY_RESET_SERVICESDB, ctxt.getPackageManager().getPackageInfo(
                        "info.lamatricexiste.network", 0).versionCode);
                edit.commit();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLiteException e) {
            e.printStackTrace();
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        } finally {
            c.close();
        }
    }
}
