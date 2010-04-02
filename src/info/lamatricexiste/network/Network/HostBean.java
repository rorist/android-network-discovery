/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

// Inspired by http://connectbot.googlecode.com/svn/trunk/connectbot/src/org/connectbot/bean/HostBean.java
package info.lamatricexiste.network.Network;

public class HostBean {
    public static final String EXTRA_POSITION = "info.lamatricexiste.network.extra_position";
    public static final String EXTRA_HOST = "info.lamatricexiste.network.extra_host";
    public static final String EXTRA_TIMEOUT = "info.lamatricexiste.network.extra_timeout";
    public static final String EXTRA_HOSTNAME = "info.lamatricexiste.network.extra_hostname";
    public static final String EXTRA_PORTSO = "info.lamatricexiste.network.extra_ports_o";
    public static final String EXTRA_PORTSC = "info.lamatricexiste.network.extra_ports_c";

    public String ipAddress = null;
    public String hostname = null;
    public String hardwareAddress = "00:00:00:00:00:00";
    public String nicVendor = "Unknown";
    public String os = "Unknown";
    public float responseTime = 0;
    public int position = 0;
    public int[] portsOpen = null;
    public int[] portsClosed = null;
}
