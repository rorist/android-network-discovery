/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

//am start -a android.intent.action.MAIN -n com.android.settings/.wifi.WifiSettings
package info.lamatricexiste.network.Utils;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class NetInfo {
    private DhcpInfo dhcp;
    private WifiInfo info;

    public NetInfo(Context ctxt) {
        WifiManager wifi = (WifiManager) ctxt.getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            dhcp = wifi.getDhcpInfo();
            info = wifi.getConnectionInfo();
        }
    }

    public int getNetCidr() {
        int i = dhcp.netmask;
        i = i - ((i >> 1) & 0x55555555);
        i = (i & 0x33333333) + ((i >> 2) & 0x33333333);
        return ((i + (i >> 4) & 0xF0F0F0F) * 0x1010101) >> 24;
        // return 24;
    }

    public String getIp() {
        return getIpFromIntSigned(dhcp.ipAddress);
    }

    public String getNetmask() {
        return getIpFromIntSigned(dhcp.netmask);
    }

    public String getNetIp() {
        return getIpFromIntSigned(dhcp.ipAddress & dhcp.netmask);
    }

    public String getBroadcastIp() {
        return getIpFromIntSigned((dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask);
    }

    public Object getGatewayIp() {
        return getIpFromIntSigned(dhcp.gateway);
    }

    public String getSSID() {
        return info.getSSID();
    }

    public String getBSSID() {
        return info.getBSSID();
    }

    public String getMacAddress() {
        return info.getMacAddress();
    }

    public SupplicantState getSupplicantState() {
        return info.getSupplicantState();
    }

    public static long getUnsignedLongFromIp(String ip_addr) {
        String[] a = ip_addr.split("\\.");
        return (Integer.parseInt(a[0]) * 16777216 + Integer.parseInt(a[1]) * 65536
                + Integer.parseInt(a[2]) * 256 + Integer.parseInt(a[3]));
    }

    public static String getIpFromIntSigned(int ip_int) {
        String ip = "";
        for (int k = 0; k < 4; k++) {
            ip = ip + ((ip_int >> k * 8) & 0xFF) + ".";
        }
        return ip.substring(0, ip.length() - 1);
    }

    public static String getIpFromLongUnsigned(long ip_long) {
        String ip = "";
        for (int k = 3; k > -1; k--) {
            ip = ip + ((ip_long >> k * 8) & 0xFF) + ".";
        }
        return ip.substring(0, ip.length() - 1);
    }

    // public int getIntFromInet(InetAddress ip_addr) {
    // return getIntFromIp(ip_addr.getHostAddress());
    // }

    // private InetAddress getInetFromInt(int ip_int) {
    // byte[] quads = new byte[4];
    // for (int k = 0; k < 4; k++)
    // quads[k] = (byte) ((ip_int >> k * 8) & 0xFF); // 0xFF=255
    // try {
    // return InetAddress.getByAddress(quads);
    // } catch (java.net.UnknownHostException e) {
    // return null;
    // }
    // }
}
