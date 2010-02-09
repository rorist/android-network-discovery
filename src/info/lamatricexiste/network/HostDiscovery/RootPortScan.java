package info.lamatricexiste.network.HostDiscovery;

import java.io.IOException;
import java.net.InetAddress;

public class RootPortScan extends AbstractPortScan {

    // private final String TAG = "RootPortScan";

    protected RootPortScan(String host) {
        super(host);
    }

    protected RootPortScan(String host, final int timeout) {
        super(host);
        TIMEOUT_SOCKET = timeout;
    }

    protected void start(InetAddress ina, final int PORT_START, final int PORT_END)
            throws InterruptedException, IOException {
    }

    protected void stop() {
    }
}
