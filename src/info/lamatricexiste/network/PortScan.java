package info.lamatricexiste.network;

import java.net.InetAddress;

public class PortScan {
	
	public CharSequence[] scan(InetAddress host){
		//ScanTCP
		//ScanUDP
		final CharSequence[] result = {
				"80/tcp open http",
				"443/tcp open https"
		};
		return result;
	}
}
