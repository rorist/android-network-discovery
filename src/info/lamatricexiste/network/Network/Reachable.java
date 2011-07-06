/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

// FIXME: This class must be integrated into DefaultDiscovery

package info.lamatricexiste.network.Network;

import java.io.IllegalArgumentException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Reachable {
    private final static int[] dports = { 139, 445, 22, 80 };
    public static int isReachable(InetAddress host, int timeout) {
        return isReachable(dports, host, timeout);
    }

    public static int isReachable(int[] ports, InetAddress host, int timeout) {
        for (int i = 0; i < ports.length; i++) {
            try {
                Socket s = new Socket();
                s.bind(null);
                s.connect(new InetSocketAddress(host, ports[i]), timeout);
                s.close();
                return ports[i];
            } catch (IOException e) {
            } catch (IllegalArgumentException e) {
            }
        }
        return -1;
    }
}
