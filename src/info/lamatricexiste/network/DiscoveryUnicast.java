package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.os.AsyncTask;
import android.util.Log;

public class DiscoveryUnicast extends AsyncTask<Void, String, Void> {

	private final String TAG = "DiscoveryUnicast";
	private final int TIMEOUT_REACH = 2000;
	private final int MIN_THREADS = 5;
	private final int MAX_THREADS = 10;

	protected ExecutorService pool;
	protected int ip_int;
	protected int start;
	protected int end;
	protected int size = 0;

	protected Void doInBackground(Void... params) {

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
			pool.awaitTermination(3600L, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Log.e(TAG, "Got Interrupted");
		}

		return null;
	}

	private void launch(int i) {
		CheckRunnable r = new CheckRunnable(hashToIp(i));
		pool.execute(r);
	}

	private class CheckRunnable implements Runnable {
		String host;

		CheckRunnable(String host) {
			this.host = host;
		}

		public void run() {
			Reachable r = new Reachable();
			try {
				InetAddress h = InetAddress.getByName(host);
				if (h.isReachable(TIMEOUT_REACH) || r.request(h)) {
					publishProgress(host);
				} else {
					publishProgress(new String());
				}
			} catch (IOException e) {
				publishProgress(new String());
				Log.e(TAG, e.getMessage());
			}
		}
	}

	private String hashToIp(int i) {
		return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "."
				+ ((i >> 8) & 0xFF) + "." + (i & 0xFF);
	}

}
