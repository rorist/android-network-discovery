/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.Network.DownloadFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class Db {

    private final String TAG = "Db";
    private Context ctxt = null;

    // Databases information
    public static final String PATH = "/data/data/info.lamatricexiste.network/files/";
    public static final String DB_SERVICES = "services.db";
    public static final String DB_PROBES = "probes.db";
    public static final String DB_NIC = "nic.db";
    public static final String DB_SAVE = "save.db";

    public Db(Context ctxt) {
        this.ctxt = ctxt;
        // new File(PATH).mkdirs();
    }

    public SQLiteDatabase openDb(String db_name) {
        try {
            return SQLiteDatabase.openDatabase(PATH + db_name, null,
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        } catch (SQLiteException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

    public void copyDbToDevice(int res, String db_name) throws NullPointerException, IOException {
        InputStream in = ctxt.getResources().openRawResource(res);
        // OutputStream out = new FileOutputStream(PATH + db_name);
        OutputStream out = ctxt.openFileOutput(db_name, Context.MODE_PRIVATE);
        final ReadableByteChannel ic = Channels.newChannel(in);
        final WritableByteChannel oc = Channels.newChannel(out);
        DownloadFile.fastChannelCopy(ic, oc);
    }
}
