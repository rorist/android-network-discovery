package info.lamatricexiste.network;

import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PortScan {

	// private final String TAG = "PortScan";
	private final int PORT_START = 1;
	private final int PORT_END = 1024;
	private final int POOL_SIZE = 1024;

	public void scan(Observer observer, String host) {
		// http://www.iana.org/assignments/port-numbers
		ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);

		// ScanTCP
		for (int i = PORT_START; i <= PORT_END; i++) {
			ScanTCP s = new ScanTCP(host, i);
			s.addObserver(observer);
			pool.execute(s);
		}

		// ScanUDP
	}
}
