package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.util.Log;

public class DiscoveryUnicast {
	private final String TAG = "DiscoveryUnicast";
	private final int TIMEOUT_REACH = 2000;
	private final int MIN_THREADS = 3;
	private final int MAX_THREADS = 10;
	private ExecutorService pool;
	private Observer observer;

	DiscoveryUnicast(Observer observer) {
		this.observer = observer;
	}

	public void run(int ip_int, int start, int end) {
		final int size = end - start;
		final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(
				size);
		pool = new ThreadPoolExecutor(MIN_THREADS, MAX_THREADS,
				(long) (size * TIMEOUT_REACH), TimeUnit.MILLISECONDS, queue);

		// gateway
		launch(start);
		// Rewind
		for (int i = ip_int - 1; i > start; i--) {
			launch(i);
		}
		// Forward
		for (int j = ip_int + 1; j <= end; j++) {
			launch(j);
		}

		pool.shutdown();
		try {
			pool.awaitTermination(120L, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Log.e(TAG, "Got Interrupted");
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
				} else {
					notifyObservers(null);
				}
			} catch (IOException e) {
				notifyObservers();
				Log.e(TAG, e.getMessage());
			} finally {
				// Thread.currentThread().interrupt();
			}
		}
	}

	private String hashToIp(int i) {
		return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "."
				+ ((i >> 8) & 0xFF) + "." + (i & 0xFF);
	}
}
