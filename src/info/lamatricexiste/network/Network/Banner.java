/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.os.AsyncTask;

public class Banner extends AsyncTask<Void, String, Void> {

    private String host;
    private int port;
    private int timeout;

    Banner(String host, int port, int timeout) {
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
            
            s.close();
            return null;
        } catch (IOException e) {
        }
        return null;
    }

}
