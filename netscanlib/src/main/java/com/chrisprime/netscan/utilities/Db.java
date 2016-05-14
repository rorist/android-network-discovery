/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package com.chrisprime.netscan.utilities;

import com.chrisprime.netscan.network.DownloadFile;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.zip.GZIPInputStream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class Db {

    private final static String TAG = "Db";
    private Context mContext = null;

    // Databases information
    public static final String DB_SERVICES = "services.db";
    public static final String DB_PROBES = "probes.db";
    public static final String DB_NIC = "nic.db";
    public static final String DB_SAVES = "saves.db";

    private static String sPath;

    public Db(Context context) {
        this.mContext = context;
        getDbPath(context);
        new File(sPath, DB_SERVICES);
        new File(sPath, DB_PROBES);
        new File(sPath, DB_NIC);
        new File(sPath, DB_SAVES);

        //noinspection ResultOfMethodCallIgnored
        new File(getDbPath()).mkdirs();
    }

    public static String getDbPath() {
        return sPath;
    }


    public static String getDbPath(Context context) {
        if (sPath == null || sPath.length() == 0) {
            sPath = context.getApplicationInfo().dataDir + File.separator + "databases" + File.separator;
        }
        return sPath;
    }

    public static SQLiteDatabase openDb(String dbName) {
        return openDb(dbName, SQLiteDatabase.NO_LOCALIZED_COLLATORS);
    }

    public static SQLiteDatabase openDb(String db_name, int flags) {
        try {
            File outFile = new File(getDbPath(), db_name);
            //noinspection ResultOfMethodCallIgnored
            outFile.setWritable(true);
            return SQLiteDatabase.openDatabase(outFile.getAbsolutePath(), null, flags);
        } catch (SQLiteException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    public void copyDbToDevice(int res, String db_name) throws NullPointerException, IOException {
        // InputStream in = mContext.getResources().openRawResource(res);
        GZIPInputStream in = new GZIPInputStream(mContext.getResources().openRawResource(res));
        OutputStream out = mContext.openFileOutput(db_name, Context.MODE_PRIVATE);
        final ReadableByteChannel ic = Channels.newChannel(in);
        final WritableByteChannel oc = Channels.newChannel(out);
        DownloadFile.fastChannelCopy(ic, oc);
    }
}
