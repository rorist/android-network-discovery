/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

// FIXME: This class must be integrated into DefaultDiscovery

package info.lamatricexiste.network.Network;

import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Reachable {
    private final static int[] dports = { 139, 445, 22, 80 };
    public static int isReachable(InetAddress host, int timeout) {
        return isReachable(dports, host, timeout);
    }

    public static int isReachable(int[] ports, InetAddress host, int timeout) {
        Socket s = new Socket();
        try {
            s.bind(null);
        } catch (Exception  e){
        }
        for (int i = 0; i < ports.length; i++) {
            try {
                s.connect(new InetSocketAddress(host, ports[i]), timeout);
                return ports[i];
            } catch (IOException e) {
            } catch (IllegalArgumentException e) {
            }
        }
        try {
            s.close();
        } catch (Exception e){
        }
        return -1;
    }
}
