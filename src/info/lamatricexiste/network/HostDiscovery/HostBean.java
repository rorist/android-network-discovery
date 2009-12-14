// Inspired by http://connectbot.googlecode.com/svn/trunk/connectbot/src/org/connectbot/bean/HostBean.java
package info.lamatricexiste.network.HostDiscovery;

import java.net.InetAddress;

public class HostBean {
    private InetAddress inetAddress = null;
    private String hardwareAddress = "00:00:00:00:00:00";
    private String nicVendor = "Unknown";
    private int responseTime = 0;
    private int position = 0;
    private long[] ports = null;
   
    public HostBean(){
    }

    public InetAddress getInetAddress(){
        return inetAddress;
    }

    public void setInetAddress(InetAddress addr){
        inetAddress = addr;
    }

    public String getHardwareAddress(){
        return hardwareAddress;
    }

    public void setHardwareAddress(String addr){
        hardwareAddress = addr;
    }

    public String getNicVendor(){
        return nicVendor;
    }

    public void setNicVendor(String vendor){
        nicVendor = vendor;
    }

    public int getResponseTime(){
        return responseTime;
    }

    public void setResponseTime(int response){
        responseTime = response;
    }

    public int getPosition(){
        return position;
    }

    public void setPosition(int pos){
        position = pos;
    }

    public long[] getPorts(){
        return ports;
    }

    public void setPorts(long[] p){
        ports = p;
    }
}
