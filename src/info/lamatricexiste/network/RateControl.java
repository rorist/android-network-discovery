/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network;

import java.io.InputStreamReader;
import android.util.Log;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.File;
import java.lang.Process;
import java.util.regex.Pattern;

public class RateControl {

    // TODO: Calculate a rounded up value from experiments in different networks
    private final String TAG = "RateControl";
    private final long RATE_BASE = 1000;
    private long rate = RATE_BASE; // Slow start
    private String[] indicator;
    private boolean indicator_discovered = false;

    public long getRate() {
        return rate;
    }

    public void setRate(long rate) {
        this.rate = rate;
    }

    public void setIndicator(String... indicator) {
        this.indicator = indicator;
    }

    public String[] getIndicator() {
        return indicator;
    }

    public boolean isIndicatorDiscovered() {
        return indicator_discovered;
    }

    public void adaptRate() {
        long response_time = 0;
        // TODO: Use an indicator with a port, calculate java round trip time
        // if (indicator.length > 1) {
        // Log.v(TAG, "use a socket here, port=" + getIndicator()[1]);
        // } else {
        indicator_discovered = true;
        if ((response_time = getAvgResponseTime(getIndicator()[0], 3)) > 0) {
            // Add 30% to the response time
            setRate(response_time + (response_time * 3 / 10));
            Log.v(TAG, "rate=" + getRate());
        }
        // }
    }

    private long getAvgResponseTime(String host, int count) {
        try {
            if ((new File("/system/bin/ping")).exists() == true) {
                String line;
                Matcher matcher;
                Process p = Runtime.getRuntime().exec("ping -q -n -W 2 -c " + count + " " + host);
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
