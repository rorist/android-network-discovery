package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;

public class DiscoveryUnicast {
	private final String TAG = "DiscoveryUnicast";
	private final static int TIMEOUT_REACH = 600;
	private final static int POOL_SIZE = 20;
	private ExecutorService pool;
	private Observer observer;

	DiscoveryUnicast(Observer observer) {
		this.observer = observer;
		pool = Executors.newFixedThreadPool(POOL_SIZE);
	}

	public void run(int ip_int, int start, int end) {
		// gateway
		launch(start);

		// rewind
		for (int i = ip_int - 1; i > start; i--) {
			launch(i);
		}

		// forward
		for (int j = ip_int + 1; j <= end; j++) {
			launch(j);
		}
	}

	private void launch(int i) {
		CheckRunnable r = new CheckRunnable(hashToIp(i));
		r.addObserver(observer);
		pool.execute(r);
	}

	private class CheckRunnable extends Observable implements Runnable {
		String host;

		CheckRunnable(String host) {
			this.host = host;
		}

		public void run() {
			setChanged();
			Reachable r = new Reachable();
			try {
				InetAddress h = InetAddress.getByName(host);
				if (h.isReachable(TIMEOUT_REACH) || r.request(h)) {
					notifyObservers(host);
				}
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			}
			notifyObservers(null);
		}
	}

	private String hashToIp(int i) {
		return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "."
				+ ((i >> 8) & 0xFF) + "." + (i & 0xFF);
	}
}
