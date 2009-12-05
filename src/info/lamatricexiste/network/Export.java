package info.lamatricexiste.network;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Environment;

public class Export {

    // private final String TAG = "Export";
    private List<String> hosts;
    private List<Long[]> hosts_ports;
    private NetInfo net;
    private Context ctxt;

    public Export(Context ctxt, List<String> hosts, List<Long[]> hosts_ports) {
        this.hosts = hosts;
        this.hosts_ports = hosts_ports;
        this.ctxt = ctxt;
        net = new NetInfo((WifiManager) ctxt
                .getSystemService(Context.WIFI_SERVICE));
    }

    public void writeToSd(String file) throws IOException {
        String xml = prepareXml();
        FileWriter f = new FileWriter(file);
        f.write(xml);
        f.flush();
        f.close();
    }

    public String getFileName() {
        return Environment.getExternalStorageDirectory().toString()
                + "/discovery-" + net.getNetIp() + ".xml";
    }

    private String prepareXml() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                + "<NetworkDiscovery>\r\n";
        // Network Information
        xml += "\t<info>\r\n"
                + "\t\t<date>"
                + (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"))
                        .format(new Date())
                + "</date>\r\n" // RFC 2822
                + "\t\t<network>" + net.getNetIp() + "/" + net.getNetCidr()
                + "</network>\r\n" + "\t\t<ssid>" + net.getSSID()
                + "</ssid>\r\n" + "\t\t<bssid>" + net.getBSSID()
                + "</bssid>\r\n" + "\t\t<ip>" + net.getIp() + "</ip>\r\n"
                + "\t</info>\r\n";

        // Hosts
        if (hosts != null) {
            HardwareAddress hw = new HardwareAddress(ctxt);
            xml += "\t<hosts>\r\n";
            for (int i = 0; i < hosts.size(); i++) {
                // Host info
                String host = hosts.get(i);
                xml += "\t\t<host value=\"" + host + "\" mac=\""
                        + hw.getHardwareAddress(host) + "\">\r\n";
                // Ports
                Long[] portsL = hosts_ports.get(i);
                if (portsL != null) {
                    CharSequence[] ports = Main.preparePort(portsL);
                    for (int j = 0; j < ports.length; j++) {
                        xml += "\t\t\t<port>" + ports[j] + "</port>\r\n";
                    }
                }
                xml += "\t\t</host>\r\n";
            }
            xml += "\t</hosts>\r\n";
        }

        xml += "</NetworkDiscovery>";
        return xml;
    }

}
