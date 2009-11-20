package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import android.os.AsyncTask;
import android.util.Log;

public class PortScan extends AsyncTask<Void, String, Void> {

    private final String TAG = "PortScan";
    private final int TIMEOUT = 200;
    private final int NB_PORTS = 1024;
    private final int PORT_START = 1;
    private final int PORT_END = 1024; // Reference Table limit = 512
    final int[] PORTS_SUP = { 5060, 5900, 5901, 5800, 9100 }; // SIP, VNC, VNCWeb, JET
    private Selector selector;
    protected String host;
    protected int position;

    public void setInfo(int position, String host) {
        this.position = position;
        this.host = host;
    }

    private void getSocket(InetAddress ina, int port) {
        try {
            // Create the socket
            InetSocketAddress addr = new InetSocketAddress(ina, port);
            SocketChannel socket;
            socket = SocketChannel.open();
            socket.configureBlocking(false);
            //socket.socket().setSoTimeout(TIMEOUT);
            socket.connect(addr);
            // Register the Channel
            socket.register(selector, SelectionKey.OP_CONNECT,
                    new Integer(port));
        } catch (IOException e) {
            Log.e("getSocket", e.getMessage());
        }
    }

    protected Void doInBackground(Void... params) {
        //socketchannel with timeout: http://bit.ly/2djwRV
        try {
            selector = Selector.open();
            InetAddress ina = InetAddress.getByName(host);
            for (int i = PORT_START; i <= PORT_END; i++) {
                getSocket(ina, i);
            }
            while (true) {
                //selector.select(TIMEOUT*NB_PORTS);
                selector.selectNow();
                Set<SelectionKey> keys = selector.selectedKeys();
                for (Iterator<SelectionKey> iterator = keys.iterator(); iterator
                        .hasNext();) {
                    SelectionKey key = (SelectionKey) iterator.next();
                    SocketChannel socket = (SocketChannel) key.channel();
                    if (key.isConnectable()) {
                        Log.v(TAG, "isConnectable="+key.attachment());
                        // Do a timeout here?
                        if (socket.finishConnect()) {
                            Log.v(TAG, "isConnected="+key.attachment());
                            publishProgress(new String(key.attachment() + "/tcp open"));
                        }
                    } else {
                        publishProgress(new String());
                    }
                    socket.close();
                    iterator.remove();
                }
            }
        } catch (UnknownHostException e) {
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }
}
