/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.os.AsyncTask;
import android.util.Log;

public class Banner extends AsyncTask<Void, String, Void> {

    private final String TAG = "Banner";
    private static final int BUF = 8 * 1024;
    private String host;
    private int port;
    private int timeout;
    private String banner = "";

    public Banner(String host, int port, int timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            Socket s = new Socket();
            s.bind(null);
            s.connect(new InetSocketAddress(host, port), timeout);
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()), BUF);
            while ((banner = in.readLine()) != null) {
                break;
            }
            in.close();
            s.close();
            Log.v(TAG, banner);
            return null;
        } catch (IOException e) {
        }
        return null;
    }

}
