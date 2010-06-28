/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Network;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class RateControl {

    // TODO: Calculate a rounded up value from experiments in different networks
    private final String TAG = "RateControl";
    public String[] indicator;
    public long rate = 800; // Slow start
    public boolean is_indicator_discovered = false;

    public void adaptRate() {
        long response_time = 0;
        // TODO: Use an indicator with a port, calculate java round trip time
        // if (indicator.length > 1) {
        // Log.v(TAG, "use a socket here, port=" + getIndicator()[1]);
        // } else {
        is_indicator_discovered = true;
        if ((response_time = getAvgResponseTime(indicator[0], 3)) > 0) {
            rate = response_time * 2; // TODO: Be adaptative
            Log.v(TAG, "rate=" + rate);
        }
        // }
    }

    private long getAvgResponseTime(String host, int count) {
        try {
            String cmd = "/system/bin/ping";
            if ((new File(cmd)).exists() == true) {
                String line;
                Matcher matcher;
                Process p = Runtime.getRuntime().exec(cmd + " -q -n -W 2 -c " + count + " " + host);
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()), 1);
                while ((line = r.readLine()) != null) {
                    matcher = Pattern
                            .compile(
                                    "^rtt min\\/avg\\/max\\/mdev = [0-9\\.]+\\/([0-9\\.]+)\\/[0-9\\.]+\\/[0-9\\.]+ ms$")
                            .matcher(line);
                    if (matcher.matches()) {
                        return Long.parseLong(matcher.group(1));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't use native ping: " + e.getMessage());
        }
        return 0;
    }
}
