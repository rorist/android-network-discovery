/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;

public abstract class AbstractPortScan extends AsyncTask<Void, Integer, Void> {

    private final String TAG = "AbstractPortScan";
    private int step;
    protected long time;
    protected long timeout;

    protected int port_start;
    protected int port_end;
    protected int nb_port;
    protected String host;

    protected AbstractPortScan(String host, final long timeout) {
        this.host = host;
        this.timeout = timeout;
    }
    
    abstract protected void stop();
    abstract protected void start(InetAddress ina, final int PORT_START, final int PORT_END) throws InterruptedException, IOException;

    protected Void doInBackground(Void... params) {
        Log.v(TAG, "timeout=" + timeout);
        try {
            step = 127;
            InetAddress ina = InetAddress.getByName(host);
            if (nb_port > step) {
                for (int i = port_start; i <= port_end - step; i += step + 1) {
                    time = System.nanoTime();
                    start(ina, i, i + ((i + step <= port_end - step) ? step : port_end - i));
                }
            } else {
                time = System.nanoTime();
                start(ina, port_start, port_end);
            }
        } catch (UnknownHostException e) {
            publishProgress((int) -1, (int) -1);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            stop();
        }
        return null;
    }

    protected void cancelTimeouts() throws IOException {
        if ((System.nanoTime() - time) > timeout) {
            stop();
        }
    }

    protected void onCancelled() {
        stop();
    }
}
