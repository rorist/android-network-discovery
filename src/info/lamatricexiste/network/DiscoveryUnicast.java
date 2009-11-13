package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Observable;

import android.util.Log;

public class DiscoveryUnicast extends Observable implements Runnable {
	private final String TAG = "DiscoveryUnicast";
	private InetAddress ip = null;
	private final static int TIMEOUT_REACH = 600;
	private int cidr = 24;

	public void setVar(InetAddress ip, int cidr) {
		this.ip = ip;
		this.cidr = cidr;
	}

	public void run() {
		int ip_int = ip.hashCode();
		int start = ip_int & (1 - (1 << (32 - cidr)));
		int end = ip_int | ((1 << (32 - cidr)) - 1);

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

		setChanged();
		notifyObservers(null);
	}

	private void launch(int i) {
		final String host = hashToIp(i);
		Thread t = new Thread() {
			public void run() {
				if (checkHost(host)) {
					setChanged();
					notifyObservers(host);
				}
				Thread.currentThread().interrupt();
			}
		};
		t.start();
	}

	private boolean checkHost(String host) {
		Reachable r = new Reachable();
		try {
			InetAddress h = InetAddress.getByName(host);
			if (h.isReachable(TIMEOUT_REACH) || r.request(h)) {
				return true;
			}
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
		return false;
	}

	private String hashToIp(int i) {
		return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "."
				+ ((i >> 8) & 0xFF) + "." + (i & 0xFF);
	}
}
