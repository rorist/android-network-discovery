/**
 * Un peu de doc:
 * http://weblogs.java.net/blog/2006/05/30/tricks-and-tips-nio-part-i-why-you-must-handle-opwrite
 * http://www.java.net/blog/2006/06/06/tricks-and-tips-nio-part-ii-why-selectionkeyattach-evil
 * http://weblogs.java.net/blog/2006/07/07/tricks-and-tips-nio-part-iii-thread-or-not-thread
 * http://weblogs.java.net/blog/2006/07/19/tricks-and-tips-nio-part-iv-meet-selectors
 * http://weblogs.java.net/blog/2006/09/21/tricks-and-tips-nio-part-v-ssl-and-nio-friend-or-foe
 */

package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.os.AsyncTask;
import android.util.Log;

public class PortScan extends AsyncTask<Void, String, Void> {

	private final String TAG = "PortScan";
	private final int TIMEOUT_SELECT = 500;
	private final int TIMEOUT_SOCKET = 600;
	private final int SCAN_RATE = 0;
	private final int STEP = 127;
	// private final int PORT_START = 1;
	// private final int PORT_END = 128;
	int cnt_selected;
	private Selector selector = null;

	protected final int NB_PORTS = 1024;
	protected String host;
	protected int position;

	// private ExecutorService pool;

	public void setInfo(int position, String host) {
		this.position = position;
		this.host = host;
	}

	private void connectSocket(InetAddress ina, int port) {
		try {
			// Create the socket
			InetSocketAddress addr = new InetSocketAddress(ina, port);
			SocketChannel socket;
			socket = SocketChannel.open();
			socket.configureBlocking(false);
			socket.connect(addr);
			// Register the Channel with port and timestamp as attachement
			HashMap<Integer, Long> data = new HashMap<Integer, Long>();
			data.put(0, (long) port);
			data.put(1, System.currentTimeMillis());
			socket.register(selector, SelectionKey.OP_CONNECT, data);
		} catch (IOException e) {
			Log.e("getSocket", e.getMessage());
		}
	}

	protected Void doInBackground(Void... params) {
		try {
			InetAddress ina = InetAddress.getByName(host);
			main(ina, 1, 128);
			main(ina, 129, 256);
			main(ina, 257, 384);
			main(ina, 385, 512);
			main(ina, 513, 640);
			main(ina, 641, 768);
			main(ina, 769, 896);
			main(ina, 897, 1024);
		} catch (UnknownHostException e) {
			Log.e(TAG, e.getMessage());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void main(InetAddress ina, int PORT_START, int PORT_END) {
		try {
			cnt_selected = 0;
			selector = Selector.open();

			for (int i = PORT_START; i < PORT_END; i++) {
				connectSocket(ina, i);
				Thread.sleep(SCAN_RATE);
			}

			while (selector.isOpen()) {
				selector.select(TIMEOUT_SELECT);
				Iterator<SelectionKey> iterator = selector.selectedKeys()
						.iterator();

				while (iterator.hasNext()) {
					SelectionKey key = (SelectionKey) iterator.next();
					iterator.remove();
					// Si isValid() seulement = port ouvert mais non connectable
					if (key.isValid() && key.isConnectable()) {
						SocketChannel socket = (SocketChannel) key.channel();
						Map<Integer, Long> map = (HashMap<Integer, Long>) key
								.attachment();
						Long port = map.get(0);
						try {
							if (socket.isConnectionPending()) {
								socket.finishConnect();
								publishProgress(new String(port + "/tcp open"));
								// Log.v(TAG, "open="+port);
							}
						} catch (IOException e) {
							// No service
							publishProgress(new String());
							// Log.v(TAG, "closed=" + port);
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
				}
				cancelTimeouts();
				if (cnt_selected == STEP) {
					selector.close();
				}
			}
		} catch (UnknownHostException e) {
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		} catch (InterruptedException e) {
			Log.e(TAG, e.getMessage());
		} finally {
			try {
				selector.close();
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void cancelTimeouts() throws IOException {
		// Emprunté ici
		// http://72.5.124.102/thread.jspa?threadID=679818&messageID=3973992
		long now = System.currentTimeMillis();
		for (SelectionKey key : selector.keys()) {
			Map<Integer, Long> map = (HashMap<Integer, Long>) key.attachment();
			long time = map.get(1);
			if (key.isValid() && now - time > TIMEOUT_SOCKET) {
				// Log.v(TAG, "canceled=" + map.get(0));
				publishProgress(new String());
				cnt_selected++;
				key.cancel();
				key.channel().close();
			}
		}
	}
}
