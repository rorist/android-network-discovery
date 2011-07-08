/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class RateControl {

    private final String TAG = "RateControl";
    private static final int BUF = 512;
    private final int REACH_TIMEOUT = 5000;
    private final String CMD = "/system/bin/ping -A -q -n -w 3 -W 2 -c 3 ";
    private final String PTN = "^rtt min\\/avg\\/max\\/mdev = [0-9\\.]+\\/[0-9\\.]+\\/([0-9\\.]+)\\/[0-9\\.]+ ms.*";
    private Pattern mPattern;
    private String line;
    public String indicator = null;
    public int rate = 800; // Slow start

    public RateControl() {
        mPattern = Pattern.compile(PTN);
    }

    public void adaptRate() {
        int response_time = 0;
        if ((response_time = getAvgResponseTime(indicator)) > 0) {
            if (response_time > 100) { // Most distanced hosts
                rate = response_time * 5; // Minimum 500ms
            } else {
                rate = response_time * 10; // Maximum 1000ms
            }
            if (rate > REACH_TIMEOUT) {
                rate = REACH_TIMEOUT;
            }
        }
    }

    private int getAvgResponseTime(String host) {
        // TODO: Reduce allocation
        BufferedReader reader = null;
        Matcher matcher;
        try {
            final Process proc = Runtime.getRuntime().exec(CMD + host);
            reader = new BufferedReader(new InputStreamReader(proc.getInputStream()), BUF);
            while ((line = reader.readLine()) != null) {
                matcher = mPattern.matcher(line);
                if (matcher.matches()) {
                    reader.close();
                    return (int) Float.parseFloat(matcher.group(1));
                }
            }
            reader.close();
        } catch (Exception e) {
            Log.e(TAG, "Can't use native ping: " + e.getMessage());
            try {
                final long start = System.nanoTime();
                if (InetAddress.getByName(host).isReachable(REACH_TIMEOUT)) {
                    Log.i(TAG, "Using Java ICMP request instead ...");
                    return (int) ((System.nanoTime() - start) / 1000);
                }
            } catch (Exception e1) {
                Log.e(TAG, e1.getMessage());
            }
        } finally {
            try {
            if (reader != null) {
                reader.close();
            }
            } catch(IOException e){
            }
        }
        return rate;
    }
}
