package info.lamatricexiste.network;

import java.net.InetAddress;

import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class NetworkInfo {
	private final String TAG = "NetworkInfo";
	private WifiManager wifi = null;
	private DhcpInfo dhcp = null;

	NetworkInfo(WifiManager wifi) {
		this.wifi = wifi;
		if (wifi != null && wifi.isWifiEnabled()) {
			dhcp = wifi.getDhcpInfo();
		}
	}

	public boolean isWifiEnabled() {
		if (wifi != null && dhcp != null && getSSID() != null
				&& wifi.getWifiState() == WifiManager.WIFI_STATE_ENABLED) {
			return true;
		}
		return false;
	}

	public int getNetCidr() {
		int i = dhcp.netmask;
		i = i - ((i >> 1) & 0x55555555);
		i = (i & 0x33333333) + ((i >> 2) & 0x33333333);
//		return ((i + (i >> 4) & 0xF0F0F0F) * 0x1010101) >> 24;
		return 24;
	}

	public InetAddress getIp() {
		return getIpFromInt(dhcp.ipAddress);
	}

	public InetAddress getNetmask() {
		return getIpFromInt(dhcp.netmask);
	}

	public InetAddress getNetIp() {
		return getIpFromInt(dhcp.ipAddress & dhcp.netmask);
	}

	public InetAddress getBroadcastIp() {
		return getIpFromInt((dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask);
	}

	public String getSSID() {
		return wifi.getConnectionInfo().getSSID();
	}

	public String getBSSID() {
		return wifi.getConnectionInfo().getBSSID();
	}

	private InetAddress getIpFromInt(int ip_int) {
		byte[] quads = new byte[4];

		for (int k = 0; k < 4; k++)
			quads[k] = (byte) ((ip_int >> k * 8) & 0xFF);
		try {
			return InetAddress.getByAddress(quads);
		} catch (java.net.UnknownHostException e) {
			Log.e(TAG, e.getMessage());
			return null;
		}
	}

}
