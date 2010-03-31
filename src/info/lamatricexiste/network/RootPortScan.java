/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;

public class RootPortScan extends AbstractPortScan {

    // private final String TAG = "RootPortScan";

    protected RootPortScan(String host, final int timeout) {
        super(host, timeout);
    }

    protected void start(InetAddress ina, final int PORT_START, final int PORT_END)
            throws InterruptedException, IOException {
    }

    protected void stop() {
    }
}
