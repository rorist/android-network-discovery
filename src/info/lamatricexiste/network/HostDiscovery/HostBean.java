// Inspired by http://connectbot.googlecode.com/svn/trunk/connectbot/src/org/connectbot/bean/HostBean.java
package info.lamatricexiste.network.HostDiscovery;

public class HostBean {
    private String ipAddress = null;
    private String hostname = null;
    private String hardwareAddress = "00:00:00:00:00:00";
    private String nicVendor = "Unknown";
    private float responseTime = 0;
    private int position = 0;
    private long[] portsOpen = null;
    private long[] portsClosed = null;
    private String os = "Unknown";
    
    public static String EXTRA_POSITION = "position";
    public static String EXTRA_HOST = "host";
    public static String EXTRA_HOSTNAME = "hostname";
    public static String EXTRA_PORTSO = "ports_o";
    public static String EXTRA_PORTSC = "ports_c";

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

    public float getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(float response) {
        responseTime = response;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int pos) {
        position = pos;
    }

    public long[] getPortsOpen() {
        return portsOpen;
    }

    public void setPortsOpen(long[] p) {
        portsOpen = p;
    }

    public long[] getPortsClosed() {
        return portsClosed;
    }

    public void setPortsClosed(long[] p) {
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
