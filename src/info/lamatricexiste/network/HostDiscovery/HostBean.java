// Inspired by http://connectbot.googlecode.com/svn/trunk/connectbot/src/org/connectbot/bean/HostBean.java
package info.lamatricexiste.network.HostDiscovery;

public class HostBean {
    private String ipAddress = null;
    private String hardwareAddress = "00:00:00:00:00:00";
    private String nicVendor = "Unknown";
    private float responseTime = 0;
    private int position = 0;
    private long[] ports = null;
    private String os = "Unknown";

    public HostBean() {
    }

    public String getHardwareAddress() {
        return hardwareAddress;
    }

    public void setHardwareAddress(String addr) {
        hardwareAddress = addr;
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

    public long[] getPorts() {
        return ports;
    }

    public void setPorts(long[] p) {
        ports = p;
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
