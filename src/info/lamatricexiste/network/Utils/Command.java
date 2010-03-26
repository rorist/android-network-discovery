/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Utils;

import android.util.Log;

public class Command {

    static {
        try {
            System.load("/data/data/info.lamatricexiste.network/lib/libcommand.so");
        } catch (UnsatisfiedLinkError e) {
            Log.e("Command", "Cannot load libcommand.so");
        }
    }

    public static native int runCommand(String cmd);
}
