package info.lamatricexiste.network.HostDiscovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Reachable {

    final int[] ports = { 135, 139, 22, 111, 80 };
    final int timeout = 500; // FIXME: Point of failure, MUST use NIO
    final int len = ports.length;

    public int isReachable(InetAddress host) {
        for (int i = 0; i < len; i++) {
            try {
                Socket s = new Socket();
                s.bind(null);
                s.connect(new InetSocketAddress(host, ports[i]), timeout);
                s.close();
                return i;
            } catch (IOException e) {
                // Log.e("Reachable", e.getMessage());
            }
        }
        return -1;
    }
}
