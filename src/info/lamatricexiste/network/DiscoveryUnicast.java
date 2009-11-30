package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.os.AsyncTask;
import android.util.Log;

public class DiscoveryUnicast extends AsyncTask<Void, String, Void> {

	private final String TAG = "DiscoveryUnicast";
	private final int TIMEOUT_REACH = 1000;
	private final int nTHREADS = 64;
	private int pt_forward;
	private int pt_backward;
	private int pt_move = 2; // 1=backward 2=forward

	protected ExecutorService pool;
	protected int ip_int;
	protected int start;
	protected int end;
	protected int size = 0;

	protected Void doInBackground(Void... params) {
		pool = Executors.newFixedThreadPool(nTHREADS);

		// gateway
		launch(start);

		// hosts
		pt_backward = ip_int - 1;
		pt_forward = ip_int + 1;
		for (int i = 0; i < size - 2; i++) {
			if (pt_move == 1 && pt_backward > start) {
				launch(pt_backward);
				pt_backward--;
				pt_move = 2;
			} else if (pt_move == 2 && pt_forward <= end) {
				launch(pt_forward);
				pt_forward++;
				pt_move = 1;
			} else {
				Log.d(TAG, "Error discovering, move=" + pt_move + ", fw="
						+ pt_forward + ", bk=" + pt_backward);
			}
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
		CheckRunnable r = new CheckRunnable(intToIp(i));
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

	private String intToIp(int i) {
		return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "."
				+ ((i >> 8) & 0xFF) + "." + (i & 0xFF);
	}

}
