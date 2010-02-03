package info.lamatricexiste.network.HostDiscovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

public abstract class AbstractPortScan extends AsyncTask<Void, Integer, Void> {

    private final String TAG = "AbstractPortScan";
    private int step;
    private long time;

    protected int port_start;
    protected int port_end;
    protected int nb_port;
    protected String host;
    protected int TIMEOUT_SOCKET = 1000;

    protected AbstractPortScan(String host) {
        this.host = host;
    }
    
    abstract protected void stop();
    abstract protected void scanPorts(InetAddress ina, final int PORT_START, final int PORT_END) throws InterruptedException, IOException;

    protected Void doInBackground(Void... params) {
        try {
            step = 127;
            InetAddress ina = InetAddress.getByName(host);
            if (nb_port > step) {
                for (int i = port_start; i <= port_end - step; i += step + 1) {
                    time = System.currentTimeMillis();
                    scanPorts(ina, i, i + ((i + step <= port_end - step) ? step : port_end - i));
                }
            } else {
                time = System.currentTimeMillis();
                scanPorts(ina, port_start, port_end);
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

    protected void onCancelled() {
        stop();
    }

    protected void cancelTimeouts() throws IOException {
        if ((System.currentTimeMillis() - time) > TIMEOUT_SOCKET) {
            stop();
        }
    }
}

