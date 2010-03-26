/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.HostDiscovery.HostBean;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class Export {

    private final String TAG = "Export";
    private List<HostBean> hosts;
    private NetInfo net;

    public Export(Context ctxt, List<HostBean> hosts) {
        this.hosts = hosts;
        net = new NetInfo(ctxt);
    }

    public boolean writeToSd(String file) {
        String xml = prepareXml();
        try {
            FileWriter f = new FileWriter(file);
            f.write(xml);
            f.flush();
            f.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean fileExists(String filename) {
        File file = new File(filename);
        return file.exists();
    }

    public String getFileName() {
        return Environment.getExternalStorageDirectory().toString() + "/discovery-"
                + net.getNetIp() + ".xml";
    }

    private String prepareXml() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + "<NetworkDiscovery>\r\n";
        // Network Information
        xml += "\t<info>\r\n"
                + "\t\t<date>"
                + (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")).format(new Date())
                + "</date>\r\n" // RFC 2822
                + "\t\t<network>" + net.getNetIp() + "/" + net.getNetCidr() + "</network>\r\n"
                + "\t\t<ssid>" + net.getSSID() + "</ssid>\r\n" + "\t\t<bssid>" + net.getBSSID()
                + "</bssid>\r\n" + "\t\t<ip>" + net.getIp() + "</ip>\r\n" + "\t</info>\r\n";

        // Hosts
        if (hosts != null) {
            xml += "\t<hosts>\r\n";
            for (int i = 0; i < hosts.size(); i++) {
                // Host info
                HostBean host = hosts.get(i);
                xml += "\t\t<host ip=\"" + host.getIpAddress() + "\" mac=\""
                        + host.getHardwareAddress() + "\" vendor=\""
                        + host.getNicVendor() + "\">\r\n";
                // Open Ports //TODO: rething the XML structure to include close
                // and filtered ports
                int[] ports = host.getPortsOpen();
                if (ports != null) {
                    for (int port : ports) {
                        xml += "\t\t\t<port>" + port + "/tcp open</port>\r\n";
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
