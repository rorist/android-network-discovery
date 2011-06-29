/*
 * Copyright (C) 2011 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Network;

import java.net.InetAddress;

import android.util.Log;

public class Ping {

    private static final String TAG = "Ping";
    private static final String CMD = "/system/bin/ping -q -n -w 1 -c 1 %s";
    private static final int TIMEOUT = 1000;

    public static void doPing(String host) {
        try {
            // TODO: Use ProcessBuilder ?
            Runtime.getRuntime().exec(String.format(CMD, host));
        } catch (Exception e) {
            Log.e(TAG, "Can't use native ping: " + e.getMessage());
            try {
                if (InetAddress.getByName(host).isReachable(TIMEOUT)) {
                    Log.i(TAG, "Using Java ICMP request instead ...");
                }
            } catch (Exception e1) {
                Log.e(TAG, e1.getMessage());
            }
        }
    }
}
