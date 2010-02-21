// Inspired by http://connectbot.googlecode.com/svn/trunk/connectbot/src/org/connectbot/bean/HostBean.java
package info.lamatricexiste.network.HostDiscovery;

public class HostBean {
    private String ipAddress = null;
    private String hostname = null;
    private String hardwareAddress = "00:00:00:00:00:00";
    private String nicVendor = "Unknown";
    private long responseTime = 0;
    private int position = 0;
    private int[] portsOpen = null;
    private int[] portsClosed = null;
    private String os = "Unknown";

    public static final String EXTRA_POSITION = "info.lamatricexiste.network.extra_position";
    public static final String EXTRA_HOST = "info.lamatricexiste.network.extra_host";
    public static final String EXTRA_TIMEOUT = "info.lamatricexiste.network.extra_timeout";
    public static final String EXTRA_HOSTNAME = "info.lamatricexiste.network.extra_hostname";
    public static final String EXTRA_PORTSO = "info.lamatricexiste.network.extra_ports_o";
    public static final String EXTRA_PORTSC = "info.lamatricexiste.network.extra_ports_c";

    public String getHardwareAddress() {
        return hardwareAddress;
    }

    public void setHardwareAddress(String addr) {
        hardwareAddress = addr;
    }

    public String getHostname() {
        if (hostname != null) {
            return hostname;
        }
        return getIpAddress();
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getNicVendor() {
        return nicVendor;
    }

    public void setNicVendor(String vendor) {
        nicVendor = vendor;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long timeout) {
        responseTime = timeout;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int pos) {
        position = pos;
    }

    public int[] getPortsOpen() {
        return portsOpen;
    }

    public void setPortsOpen(int[] is) {
        portsOpen = is;
    }

    public int[] getPortsClosed() {
        return portsClosed;
    }

    public void setPortsClosed(int[] p) {
        portsClosed = p;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getOs() {
        return os;
    }
}
