/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

//am start -a android.intent.action.MAIN -n com.android.settings/.wifi.WifiSettings
package info.lamatricexiste.network.Network;

import info.lamatricexiste.network.Utils.Prefs;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class NetInfo {
    private final String TAG = "NetInfo";
    private final String NOIP = "0.0.0.0";
    private Context ctxt;
    private WifiInfo info;
    private SharedPreferences prefs;

    public String intf = "eth0";
    public String ip = "0.0.0.0";
    public int cidr = 24;

    public String ssid = null;
    public String bssid = null;
    public String carrier = null;
    public String macAddress = "00:00:00:00:00:00";
    public Object gatewayIp = "0.0.0.0";

    public NetInfo(final Context ctxt) {
        this.ctxt = ctxt;
        prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
        getIp();
        getWifiInfo();

        // Set ARP enabled
        // try {
        // Runtime.getRuntime().exec("su -C ip link set dev " + intf +
        // " arp on");
        // } catch (Exception e) {
        // Log.e(TAG, e.getMessage());
        // }
        // Runtime.getRuntime().exec("echo 1 > /proc/sys/net/ipv4/conf/" + intf
        // + "/proxy_arp");
        // Runtime.getRuntime().exec("echo 1 > /proc/sys/net/ipv4/conf/tun0/proxy_arp");
    }

    public void getIp() {
        intf = prefs.getString(Prefs.KEY_INTF, Prefs.DEFAULT_INTF);
        try {
            if (intf == null) {
                // Automatic interface selection
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                        .hasMoreElements();) {
                    NetworkInterface ni = en.nextElement();
                    intf = ni.getName();
                    ip = getInterfaceFirstIp(ni);
                }
            } else {
                // Defined interface from Prefs
                ip = getInterfaceFirstIp(NetworkInterface.getByName(intf));
            }
        } catch (SocketException e) {
            Log.e(TAG, e.getMessage());
            Editor edit = prefs.edit();
            edit.putString(Prefs.KEY_INTF, Prefs.DEFAULT_INTF);
            edit.commit();
        }
        getCidr();
    }

    private String getInterfaceFirstIp(NetworkInterface ni) {
        if (ni != null) {
            for (Enumeration<InetAddress> nis = ni.getInetAddresses(); nis.hasMoreElements();) {
                InetAddress ia = nis.nextElement();
                if (!ia.isLoopbackAddress()) {
                    if (ia instanceof Inet6Address) {
                        Log.i(TAG, "IPv6 detected and not supported yet!");
                        return NOIP;
                    }
                    return ia.getHostAddress();
                }
            }
        }
        return NOIP;
    }

    private void getCidr() {
        String match;
        // Running ip tools
        try {
            if ((match = runCommand("/system/xbin/ip", "/system/xbin/ip -f inet addr show " + intf,
                    "\\s*inet [0-9\\.]+\\/([0-9]+) brd [0-9\\.]+ scope global " + intf + "$")) != null) {
                cidr = Integer.parseInt(match);
                return;
            } else if ((match = runCommand("/system/xbin/ip", "/system/xbin/ip -f inet addr show "
                    + intf, "\\s*inet [0-9\\.]+ peer [0-9\\.]+\\/([0-9]+) scope global " + intf
                    + "$")) != null) {
                cidr = Integer.parseInt(match);
                return;
            } else if ((match = runCommand("/system/bin/ifconfig", "/system/bin/ifconfig " + intf,
                    "^" + intf + ": ip [0-9\\.]+ mask ([0-9\\.]+) flags.*")) != null) {
                double sum = -2;
                String[] part = match.split("\\.");
                for (String p : part) {
                    sum += 256D - Double.parseDouble(p);
                }
                cidr = 32 - (int) (Math.log(sum) / Math.log(2d));
                return;
            } else {
                Log.i(TAG, "cannot find cidr, using default /24");
            }
        } catch (NumberFormatException e) {
            Log.i(TAG, "cannot find cidr, using default /24");
        }
    }

    // FIXME: Factorize, this isn't a generic runCommand()
    private String runCommand(String path, String cmd, String ptrn) {
        final File file = new File(path);
        try {
            if (file.exists() == true) {
                String line;
                Matcher matcher;
                Process p = Runtime.getRuntime().exec(cmd);
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()), 1);
                while ((line = r.readLine()) != null) {
                    matcher = Pattern.compile(ptrn).matcher(line);
                    if (matcher.matches()) {
                        return matcher.group(1);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't use native command: " + e.getMessage());
            return null;
        }
        return null;
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
            info = wifi.getConnectionInfo();
            // Set wifi variables
            ssid = info.getSSID();
            bssid = info.getBSSID();
            macAddress = info.getMacAddress();
            gatewayIp = getIpFromIntSigned(wifi.getDhcpInfo().gateway);
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
