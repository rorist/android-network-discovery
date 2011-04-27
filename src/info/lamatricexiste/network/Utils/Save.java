/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Utils;

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

    public static String getCustomName(String mac) {
        db = Db.openDb(Db.DB_SAVES);
        String name = null;
        Cursor c = null;
        try {
            if (db != null && db.isOpen()) {
                c = db.rawQuery(SELECT, new String[] { mac.replace(":", "").toUpperCase() });
                if (c.moveToFirst()) {
                    name = c.getString(0);
                }
            }
        } catch (SQLiteException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (c != null) {
                c.close();
            }
            if (db != null) {
                db.close();
            }
        }
        return name;
    }

    public static void setCustomName(final String name, final String mac) {
        db = Db.openDb(Db.DB_SAVES);
        try {
            if (db.isOpen()) {
                db.execSQL(INSERT, new String[] { name, mac.replace(":", "").toUpperCase() });
            }
        } catch (SQLiteException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (db != null) {
                db.close();
            }
        }
    }

    public static boolean removeCustomName(String mac) {
        db = Db.openDb(Db.DB_SAVES);
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
            if (db != null) {
                db.close();
            }
        }
    }
}
