package info.lamatricexiste.network.HostDiscovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Reachable {

	final int[] ports = { 139, 445, 22, 111, 80 };
	final int timeout = 300;
	final int len = ports.length;

	public boolean request(InetAddress host) {
		for (int i = 0; i < len; i++) {
			try {
				Socket s = new Socket();
				s.bind(null);
				s.connect(new InetSocketAddress(host, ports[i]), timeout);
				s.close();
				return true;
			} catch (IOException e) {
				// Log.e("Reachable", e.getMessage());
			}
		}
		return false;
	}
}
