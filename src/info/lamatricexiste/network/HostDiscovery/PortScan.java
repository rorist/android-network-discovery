/**
 * Un peu de doc:
 * http://weblogs.java.net/blog/2006/05/30/tricks-and-tips-nio-part-i-why-you-must-handle-opwrite
 * http://www.java.net/blog/2006/06/06/tricks-and-tips-nio-part-ii-why-selectionkeyattach-evil
 * http://weblogs.java.net/blog/2006/07/07/tricks-and-tips-nio-part-iii-thread-or-not-thread
 * http://weblogs.java.net/blog/2006/07/19/tricks-and-tips-nio-part-iv-meet-selectors
 * http://weblogs.java.net/blog/2006/09/21/tricks-and-tips-nio-part-v-ssl-and-nio-friend-or-foe
 */

package info.lamatricexiste.network.HostDiscovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import android.os.AsyncTask;
import android.util.Log;
import android.util.SparseArray;

public class PortScan extends AsyncTask<Void, Long, Void> {

    private final String TAG = "PortScan";
    private final int TIMEOUT_SELECT = 500;
    private final int TIMEOUT_SOCKET = 1500;
    private final int SCAN_RATE = 0;
    private int step;
    private int cnt_selected;
    private Selector selector = null;

    protected int port_start;
    protected int port_end;
    protected int nb_port;
    protected String host;

    protected PortScan(String host) {
        this.host = host;
    }

    protected Void doInBackground(Void... params) {
        try {
            step = 127;
            InetAddress ina = InetAddress.getByName(host);
            if (nb_port > step) {
                for (int i = port_start; i <= port_end - step; i += step + 1) {
                    scanPorts(ina, i, i + ((i + step <= port_end - step) ? step : port_end - i));
                }
            } else {
                scanPorts(ina, port_start, port_end);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            stopSelecting();
        }
        return null;
    }

    protected void onCancelled() {
        stopSelecting();
    }

    private void scanPorts(InetAddress ina, final int PORT_START, final int PORT_END)
            throws InterruptedException, IOException {
        // Log.d(TAG, "scanPorts: start=" + PORT_START + ", end=" + PORT_END);
        cnt_selected = 0;
        selector = Selector.open();
        for (int i = PORT_START; i <= PORT_END; i++) {
            // Log.v(TAG, "port=" + i);
            connectSocket(ina, i);
            Thread.sleep(SCAN_RATE);
        }
        doSelect(PORT_END - PORT_START);
    }

    private void connectSocket(InetAddress ina, int port) throws IOException {
        // Create the socket
        InetSocketAddress addr = new InetSocketAddress(ina, port);
        SocketChannel socket;
        socket = SocketChannel.open();
        socket.configureBlocking(false);
        socket.connect(addr);
        // Register the Channel with port and timestamp as attachement
        SparseArray<Long> data = new SparseArray<Long>(2);
        // TODO: Trouver un autre moyen de stocker ces infos ? car oblige
        // d'utiliser un long pour le numero de port a cause de ca
        data.append(0, (long) port);
        data.append(1, System.currentTimeMillis());
        socket.register(selector, SelectionKey.OP_CONNECT, data);
    }

    private void doSelect(int STEP) {
        try {
            while (selector.isOpen()) {
                selector.select(TIMEOUT_SELECT);
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = (SelectionKey) iterator.next();
                    iterator.remove();
                    if (key.isValid()) {
                        if (key.isConnectable()) {
                            handleConnect(key);
                        }
                        // else if (key.isReadable()) {
                        // handleRead(key);
                        // }
                    }
                }
                cancelTimeouts(); // Filtered or Unresponsive
                if (cnt_selected >= STEP) {
                    stopSelecting();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            stopSelecting();
        }
    }

    @SuppressWarnings("unchecked")
    private void handleConnect(SelectionKey key) {
        try {
            if (((SocketChannel) key.channel()).finishConnect()) { // Open
                publishProgress(((SparseArray<Long>) key.attachment()).get(0), (long) 1);
                finishKey(key);
                // Register for reading
                // SparseArray<Long> data = (SparseArray<Long>)
                // key.attachment();
                // data.setValueAt(1, System.currentTimeMillis());
                // ((SocketChannel) key.channel()).register(selector,
                // SelectionKey.OP_READ, data);
                // key.interestOps(SelectionKey.OP_READ);
                // Log.v(TAG, "port=" + data.get(0));
            }
        } catch (IOException e) { // Closed
            publishProgress(((SparseArray<Long>) key.attachment()).get(0), (long) 0);
            finishKey(key);
        }
    }

    // @SuppressWarnings("unchecked")
    // private void handleRead(SelectionKey key) {
    // publishProgress(((SparseArray<Long>) key.attachment()).get(0));
    // finishKey(key);
    //
    // ByteBuffer buf = ByteBuffer.allocateDirect(2);
    // buf.clear();
    // int numRead = 0;
    // try {
    // numRead = ((SocketChannel) key.channel()).read(buf);
    // } catch (IOException e) {
    // Log.e(TAG, e.getMessage());
    // } finally {
    // if (numRead > 0) {
    // try {
    // Log.v(TAG, "read=" + new String(buf.array()));
    // } catch (UnsupportedOperationException e) {
    // Log.e(TAG, "UnsupportedOperationException");
    // }
    // }
    // finishKey(key);
    // }
    // }

    @SuppressWarnings("unchecked")
    private void cancelTimeouts() throws IOException {
        // Borrowed here
        // http://72.5.124.102/thread.jspa?threadID=679818&messageID=3973992
        long now = System.currentTimeMillis();
        for (SelectionKey key : selector.keys()) {
            SparseArray<Long> map = (SparseArray<Long>) key.attachment();
            long time = map.get(1);
            if (key.isValid() && (now - time) > TIMEOUT_SOCKET) {
                publishProgress(new Long(0));
                finishKey(key);
            }
        }
    }

    private void stopSelecting() {
        // Log.d(TAG, "stopSelecting");
        // synchronized (selector) {
        if (selector != null && selector.isOpen()) {
            // Force invalidate keys
            Iterator<SelectionKey> iterator = selector.keys().iterator();
            while (iterator.hasNext()) {
                finishKey((SelectionKey) iterator.next());
            }
            // Close the selector
            try {
                selector.close();
            } catch (IOException e) {
            }
        }
        // }
    }

    private void finishKey(SelectionKey key) {
        synchronized (key) {
            try {
                ((SocketChannel) key.channel()).close();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                key.cancel();
                cnt_selected++;
            }
        }
    }
}
