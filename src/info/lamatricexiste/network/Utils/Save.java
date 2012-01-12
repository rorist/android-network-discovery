/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.Network.HostBean;

import java.lang.IllegalStateException;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class Save {

    private static final String TAG = "Save";
    private static final String SELECT = "select name from nic where mac=?";
    private static final String INSERT = "insert or replace into nic (name,mac) values (?,?)";
    private static final String DELETE = "delete from nic where mac=?";
    private static SQLiteDatabase db;

    public void closeDb(){
        if(db != null && db.isOpen()){
            db.close();
        }
    }

    public synchronized String getCustomName(HostBean host) {
        String name = null;
        Cursor c = null;
        try {
            db = getDb();
            c = db.rawQuery(SELECT, new String[] { host.hardwareAddress.replace(":", "").toUpperCase() });
            if (c.moveToFirst()) {
                name = c.getString(0);
            } else if(host.hostname != null) {
                name = host.hostname;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, e.getMessage());
        } catch (IllegalStateException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return name;
    }

    public void setCustomName(final String name, final String mac) {
        db = Db.openDb(Db.DB_SAVES, SQLiteDatabase.NO_LOCALIZED_COLLATORS|SQLiteDatabase.OPEN_READWRITE);
        try {
            if (db.isOpen()) {
                db.execSQL(INSERT, new String[] { name, mac.replace(":", "").toUpperCase() });
            }
        } catch (SQLiteException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            closeDb();
        }
    }

    public boolean removeCustomName(String mac) {
        db = Db.openDb(Db.DB_SAVES, SQLiteDatabase.NO_LOCALIZED_COLLATORS|SQLiteDatabase.OPEN_READWRITE);
        try {
            if (db.isOpen()) {
                db.execSQL(DELETE, new String[] { mac.replace(":", "").toUpperCase() });
                return true;
            }
            return false;
        } catch (SQLiteException e) {
            Log.e(TAG, e.getMessage());
            return false;
        } finally {
            closeDb();
        }
    }

    private static synchronized SQLiteDatabase getDb(){
        if(db == null || !db.isOpen()) {
            // FIXME: read only ?
            db = Db.openDb(Db.DB_SAVES, SQLiteDatabase.NO_LOCALIZED_COLLATORS|SQLiteDatabase.OPEN_READONLY);
        }
        return db;
    }
}
