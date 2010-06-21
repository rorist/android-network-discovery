/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

// Inspired by http://connectbot.googlecode.com/svn/trunk/connectbot/src/org/connectbot/bean/HostBean.java
package info.lamatricexiste.network.Network;

public class HostBean {
    public static final String PKG = "info.lamatricexiste.network";
    public static final String EXTRA_POSITION = PKG + ".extra_position";
    public static final String EXTRA_HOST = PKG + ".extra_host";
    public static final String EXTRA_TIMEOUT = PKG + ".network.extra_timeout";
    public static final String EXTRA_HOSTNAME = PKG + ".extra_hostname";
    public static final String EXTRA_BANNERS = PKG + ".extra_banners";
    public static final String EXTRA_PORTSO = PKG + ".extra_ports_o";
    public static final String EXTRA_PORTSC = PKG + ".extra_ports_c";
    public static final String EXTRA_SERVICES = PKG + ".extra_services";
    public boolean isGateway = false;
    public String ipAddress = null;
    public String hostname = null;
    public String hardwareAddress = "00:00:00:00:00:00";
    public String nicVendor = "Unknown";
    public String os = "Unknown";
    public float responseTime = 0;
    public int position = 0;
    public String[] services = null;
    public String[] banners = null;
    public int[] portsOpen = null;
    public int[] portsClosed = null;
}
