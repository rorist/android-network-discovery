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
    private final int TIMEOUT_SOCKET = 600;
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
            int limit = port_end - step;
            for (int i = port_start; i <= limit; i += step + 1) {
                scanPorts(ina, i, i + step);
            }
        } catch (Exception e) {
            stopSelecting();
        }
        return null;
    }

    private void scanPorts(InetAddress ina, int PORT_START, int PORT_END)
            throws InterruptedException, IOException {
        cnt_selected = 0;
        selector = Selector.open();
        for (int i = PORT_START; i < PORT_END; i++) {
            connectSocket(ina, i);
            Thread.sleep(SCAN_RATE);
        }
        doSelect();
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

    private void doSelect() {
        try {
            while (selector.isOpen()) {
                selector.select(TIMEOUT_SELECT);
                Iterator<SelectionKey> iterator = selector.selectedKeys()
                        .iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = (SelectionKey) iterator.next();
                    if (key.isValid()) {
                        if (key.isConnectable()) {
                            handleConnect(key);
                        } else if (key.isReadable()) {
                            Log.v(TAG, "key is readable=" + key.toString());
                            cnt_selected++;
                            key.channel().close();
                            key.cancel();
                        }
                    }
                    iterator.remove();
                }
                cancelTimeouts(); // Filtered
                if (cnt_selected == step) {
                    syncronized(selector){
                        selector.close();
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            stopSelecting();
        }
    }

    @SuppressWarnings("unchecked")
    protected void handleConnect(SelectionKey key) {
        SocketChannel socket = (SocketChannel) key.channel();
        SparseArray<Long> map = (SparseArray<Long>) key.attachment();
        Long port = map.get(0);
        try {
            if (socket.isConnectionPending()) { //FIXME: not clear if it's neededs
                socket.finishConnect();
                SparseArray<Long> data = new SparseArray<Long>(2);
                data.append(0, (long) port);
                data.append(1, System.currentTimeMillis());

                // trying to read data
                /*
                 * try { ByteBuffer buf = ByteBuffer.allocateDirect(1024); int
                 * numRead = 0; while(numRead>=0){ buf.rewind(); numRead =
                 * socket.read(buf); buf.rewind(); Log.v(TAG,
                 * "ReadFromSocket="+(new String(buf.array()))); } }
                 * catch(Exception e){ Log.e(TAG, "port="+port+", "+
                 * e.getMessage()); } finally { socket.finishConnect();
                 * publishProgress(port); // Open FIXME: use Bundle instead of
                 * Long }
                 */
                publishProgress(port); // Open FIXME: use Bundle instead of Long
            }
        } catch (IOException e) {
            publishProgress(new Long(0)); // Closed
            cnt_selected++;
            key.cancel();
            try {
                socket.close();
            } catch (IOException e1) {
                Log.e(TAG, e1.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void cancelTimeouts() throws IOException {
        // Borrowed here
        // http://72.5.124.102/thread.jspa?threadID=679818&messageID=3973992
        long now = System.currentTimeMillis();
        for (SelectionKey key : selector.keys()) {
            SparseArray<Long> map = (SparseArray<Long>) key.attachment();
            long time = map.get(1);
            if (key.isValid() && now - time > TIMEOUT_SOCKET) {
                publishProgress(new Long(0));
                cnt_selected++;
                key.cancel();
                key.channel().close();
            }
        }
    }

    protected void onCancelled() {
        stopSelecting();
    }

    private void stopSelecting() {
        try {
            syncronised(selector){
                if (selector != null) {
                    selector.close();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
