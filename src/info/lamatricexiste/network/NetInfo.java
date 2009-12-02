//am start -a android.intent.action.MAIN -n com.android.settings/.wifi.WifiSettings
package info.lamatricexiste.network;

import java.net.InetAddress;

import android.net.DhcpInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class NetInfo {
	private WifiManager wifi;
	private DhcpInfo dhcp;
	private WifiInfo info;

	NetInfo(WifiManager wifi) {
		this.setWifi(wifi);
		if (wifi != null) {
			dhcp = getWifi().getDhcpInfo();
			info = getWifi().getConnectionInfo();
		}
	}

	public int getNetCidr() {
		int i = dhcp.netmask;
		i = i - ((i >> 1) & 0x55555555);
		i = (i & 0x33333333) + ((i >> 2) & 0x33333333);
		return ((i + (i >> 4) & 0xF0F0F0F) * 0x1010101) >> 24;
		// return 26;
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

	public int getIntFromIp(InetAddress ip_addr) {
		String[] a = ip_addr.getHostAddress().split("\\.");
		return (Integer.parseInt(a[0]) * 16777216 + Integer.parseInt(a[1])
				* 65536 + Integer.parseInt(a[2]) * 256 + Integer.parseInt(a[3]));
	}

	private InetAddress getIpFromInt(int ip_int) {
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
			quads[k] = (byte) ((ip_int >> k * 8) & 0xFF); // 0xFF=255
		try {
			return InetAddress.getByAddress(quads);
		} catch (java.net.UnknownHostException e) {
			return null;
		}
	}

	private void setWifi(WifiManager wifi) {
		this.wifi = wifi;
	}

	private WifiManager getWifi() {
		return wifi;
	}
}
