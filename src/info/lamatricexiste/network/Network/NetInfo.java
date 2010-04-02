/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

//am start -a android.intent.action.MAIN -n com.android.settings/.wifi.WifiSettings
package info.lamatricexiste.network.Network;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class NetInfo {
    private final String TAG = "NetInfo";
    private Context ctxt;
    // private DhcpInfo dhcp;
    private WifiInfo info;

    public String intf = "eth0";
    public String ip = "0.0.0.0";
    public int cidr = 24;

    public String ssid;
    public String bssid;
    public String carrier;
    public String macAddress;
    public Object gatewayIp;

    public NetInfo(Context ctxt) {
        this.ctxt = ctxt;
        getIp();
        getCidr();
        getWifiInfo();
    }

    private void getIp() {
        // Iterate throught interfaces
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements();) {
                NetworkInterface ni = en.nextElement();
                intf = ni.getName();
                for (Enumeration<InetAddress> nis = ni.getInetAddresses(); nis.hasMoreElements();) {
                    InetAddress ia = nis.nextElement();
                    if (!ia.isLoopbackAddress()) {
                        ip = ia.getHostAddress();
                        return;
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    private void getCidr() {
        // Running ip tools
        if (runCommand(new File("/system/xbin/ip"), "ip -f inet addr show " + intf,
                "\\s*inet [0-9\\.]+\\/([0-9]+) brd [0-9\\.]+ scope global " + intf + "$")) {
            return;
        } else if (runCommand(new File("/system/bin/ifconfig"), "ifconfig " + intf, "^" + intf
                + ": ip [0-9\\.]+ mask ([0-9\\.]+) flags")) {
            return;
        }
    }

    private boolean runCommand(File file, String cmd, String ptrn) {
        try {
            if (file.exists() == true) {
                String line;
                Matcher matcher;
                Process p = Runtime.getRuntime().exec(cmd);
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()), 1);
                // Log.i(TAG, "CMD=" + cmd);
                while ((line = r.readLine()) != null) {
                    // Log.v(TAG, line);
                    matcher = Pattern.compile(ptrn).matcher(line);
                    if (matcher.matches()) {
                        // Log.i(TAG, "MATCH=" + matcher.group(1));
                        cidr = Integer.parseInt(matcher.group(1));
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't use native command: " + e.getMessage());
        }
        return false;
    }

    public boolean getMobileInfo() {
        TelephonyManager tm = (TelephonyManager) ctxt.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {
            carrier = tm.getNetworkOperatorName();
        }
        return false;
    }

    public boolean getWifiInfo() {
        WifiManager wifi = (WifiManager) ctxt.getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            // dhcp = wifi.getDhcpInfo();
            info = wifi.getConnectionInfo();
            // Set wifi variables
            ssid = info.getSSID();
            bssid = info.getBSSID();
            macAddress = info.getMacAddress();
            // gatewayIp = getIpFromIntSigned(dhcp.gateway);
            // broadcastIp = getIpFromIntSigned((dhcp.ipAddress & dhcp.netmask)
            // | ~dhcp.netmask);
            // netmaskIp = getIpFromIntSigned(dhcp.netmask);
            return true;
        }
        return false;
    }

    public String getNetIp() {
        int shift = (32 - cidr);
        int start = ((int) getUnsignedLongFromIp(ip) >> shift << shift);
        return getIpFromLongUnsigned((long) start);
    }

    /*
     * public String getIp() { return getIpFromIntSigned(dhcp.ipAddress); }
     * public int getNetCidr() { int i = dhcp.netmask; i = i - ((i >> 1) &
     * 0x55555555); i = (i & 0x33333333) + ((i >> 2) & 0x33333333); return ((i +
     * (i >> 4) & 0xF0F0F0F) * 0x1010101) >> 24; // return 24; } public String
     * getNetIp() { return getIpFromIntSigned(dhcp.ipAddress & dhcp.netmask); }
     */
    // public String getNetmask() {
    // return getIpFromIntSigned(dhcp.netmask);
    // }

    // public String getBroadcastIp() {
    // return getIpFromIntSigned((dhcp.ipAddress & dhcp.netmask) |
    // ~dhcp.netmask);
    // }

    // public Object getGatewayIp() {
    // return getIpFromIntSigned(dhcp.gateway);
    // }

    public SupplicantState getSupplicantState() {
        return info.getSupplicantState();
    }

    public static boolean isConnected(Context ctxt) {
        NetworkInfo nfo = ((ConnectivityManager) ctxt
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (nfo != null) {
            return nfo.isConnected();
        }
        return false;
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
