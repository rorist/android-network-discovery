package info.lamatricexiste.network;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

public class Export {

	private final String TAG = "Export";
	@SuppressWarnings("unused") private Context ctxt;
	private List<String> hosts;
	private List<CharSequence[]> hosts_ports;
	private NetworkInfo net;

	public Export(Context ctxt, List<String> hosts,
			List<CharSequence[]> hosts_ports) {
		this.hosts = hosts;
		this.hosts_ports = hosts_ports;
		net = new NetworkInfo((WifiManager) ctxt
				.getSystemService(Context.WIFI_SERVICE));
		String data = prepareXml();
		Log.v(TAG, data);
//		writeToSd(data);
	}

//	private void writeToSd(String data) {
//		String file = makeFileName();
//	}

//	private String makeFileName() {
//		return "file.xml";
//	}

	private String prepareXml() {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<NetworkDiscovery>";
		// Network Information
		xml += "<info>"
				+ "<date>"
				+ (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"))
						.format(new Date())
				+ "</date>" // RFC 2822
				+ "<network>" + net.getNetIp().getHostAddress() + "/" + net.getNetCidr() + "</network>"
				+ "<ssid>" + net.getSSID() + "</ssid>"
				+ "<bssid>" + net.getBSSID() + "</bssid>"
				+ "<ip>" + net.getIp().getHostAddress() + "</ip>"
			+ "</info>";

		// Hosts and Ports
		if(hosts!=null){
			xml += "<hosts>";
			for (int i = 0; i < hosts.size(); i++) {
				String host = hosts.get(i);
				CharSequence[] ports = hosts_ports.get(i);
				xml += "<host value=\"" + host + "\">";
				if(ports!=null){
					for (int j = 0; j < ports.length; j++) {
						xml += "<port>" + ports[j] + "</port>";
					}
				}
				xml += "</host>";
			}
			xml += "</hosts>";
		}

		xml += "</NetworkDiscovery>";
		return xml;
	}

}
