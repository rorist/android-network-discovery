package info.lamatricexiste.network;

import java.util.Observer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.util.Log;

public class PortScan {

	private final String TAG = "PortScan";
	private final int PORT_START = 1;
	private final int PORT_END = 1024;
	private final int MIN_THREADS = 2;
	private final int MAX_THREADS = 10;

	public void scan(Observer observer, String host) {
		// http://www.iana.org/assignments/port-numbers
		final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(
				1024);
		ExecutorService pool = new ThreadPoolExecutor(MIN_THREADS, MAX_THREADS,
				120L, TimeUnit.SECONDS, queue);
		for (int i = PORT_START; i <= PORT_END; i++) {
			ScanTCP s = new ScanTCP(host, i);
			s.addObserver(observer);
			pool.execute(s);
		}
		pool.shutdown();
		try {
			pool.awaitTermination(120L, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Log.e(TAG, "Got Interrupted");
		}
	}
}
