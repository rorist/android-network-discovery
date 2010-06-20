/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.R;
import android.content.Context;

public class DbServices extends Db {
    protected static String DB_NAME = "services.db";
    public static String DB_TABLE = "services";
    protected static String DROP_TABLE = "DROP TABLE IF EXISTS " + DB_TABLE;
    public static int DB_TABLE_RES = R.raw.services;
    protected static int DB_VERSION = 1;

    public DbServices(Context context) {
        super(context, DB_NAME, DB_VERSION);
    }
}
