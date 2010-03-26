/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.HostDiscovery;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Reachable {

    final int[] ports = { 445, 22, 80, 111 };
    final int len = ports.length;

    public int isReachable(InetAddress host, int timeout) {
        for (int i = 0; i < len; i++) {
            try {
                Socket s = new Socket();
                s.bind(null);
                s.connect(new InetSocketAddress(host, ports[i]), timeout);
                s.close();
                return ports[i];
            } catch (IOException e) {
                // Log.e("Reachable", e.getMessage());
            }
        }
        return -1;
    }
}
