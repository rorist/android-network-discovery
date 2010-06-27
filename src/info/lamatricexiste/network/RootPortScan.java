/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;

import android.os.AsyncTask;

public class RootPortScan extends AsyncTask<Void, Integer, Void> {

    // private final String TAG = "RootPortScan";
    protected long timeout;
    protected String ipAddr = null;

    protected RootPortScan(String host, final long timeout) {
        this.ipAddr = host;
        this.timeout = timeout;
    }

    @Override
    protected Void doInBackground(Void... params) {
        return null;
    }

    protected void start(InetAddress ina, final int PORT_START, final int PORT_END)
            throws InterruptedException, IOException {
    }

    protected void stop() {
    }
}
