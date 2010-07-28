/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.Network.HostBean;
import info.lamatricexiste.network.Network.NetInfo;

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
        net.getWifiInfo();
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
        // TODO: Use getExternalFilesDir()
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
                + "\t\t<network>" + net.getNetIp() + "/" + net.cidr + "</network>\r\n"
                + "\t\t<ssid>" + net.ssid + "</ssid>\r\n" + "\t\t<bssid>" + net.bssid
                + "</bssid>\r\n" + "\t\t<ip>" + net.ip + "</ip>\r\n" + "\t</info>\r\n";

        // Hosts
        if (hosts != null) {
            xml += "\t<hosts>\r\n";
            for (int i = 0; i < hosts.size(); i++) {
                // Host info
                HostBean host = hosts.get(i);
                xml += "\t\t<host ip=\"" + host.ipAddress + "\" mac=\"" + host.hardwareAddress
                        + "\" vendor=\"" + host.nicVendor + "\">\r\n";
                // Open Ports //TODO: rething the XML structure to include close
                // and filtered ports
                if (host.portsOpen != null) {
                    for (int port : host.portsOpen) {
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
