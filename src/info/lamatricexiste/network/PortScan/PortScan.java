/**
 * Un peu de doc:
 * http://weblogs.java.net/blog/2006/05/30/tricks-and-tips-nio-part-i-why-you-must-handle-opwrite
 * http://www.java.net/blog/2006/06/06/tricks-and-tips-nio-part-ii-why-selectionkeyattach-evil
 * http://weblogs.java.net/blog/2006/07/07/tricks-and-tips-nio-part-iii-thread-or-not-thread
 * http://weblogs.java.net/blog/2006/07/19/tricks-and-tips-nio-part-iv-meet-selectors
 * http://weblogs.java.net/blog/2006/09/21/tricks-and-tips-nio-part-v-ssl-and-nio-friend-or-foe
 */

package info.lamatricexiste.network.PortScan;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.os.AsyncTask;
import android.util.Log;

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
	protected int position;

	protected PortScan(int position, String host) {
		this.position = position;
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
        HashMap<Integer, Long> data = new HashMap<Integer, Long>(2);
        // FIXME: Trouver un autre moyen de stocker ces infos, car oblige
        // d'utiliser un long pour le numero de port a cause de ca
        data.put(0, (long) port);
        data.put(1, System.currentTimeMillis());
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
					if (key.isValid() && key.isConnectable()) {
						handleConnect(key);
					}
					iterator.remove();
				}
				cancelTimeouts(); // Filtered
				if (cnt_selected == step) {
					selector.close();
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
		Map<Integer, Long> map = (HashMap<Integer, Long>) key.attachment();
		Long port = map.get(0);
		try {
			if (socket.isConnectionPending()) {
				socket.finishConnect();
				publishProgress(port); // Open
			}
		} catch (IOException e) {
			publishProgress(new Long(0)); // Closed
		} finally {
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
			Map<Integer, Long> map = (HashMap<Integer, Long>) key.attachment();
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
	    onPostExecute(null);
	}

    private void stopSelecting(){
        try {
            if(selector!=null){
                selector.close();
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
