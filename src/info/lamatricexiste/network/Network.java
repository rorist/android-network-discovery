package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class Network extends Service
{
    private final   String          TAG               =  "NetworkService";
    public  final static String     ACTION_SENDHOST   =  "info.lamatricexiste.network.SENDHOST";
    public  final static String     ACTION_FINISH     =  "info.lamatricexiste.network.FINISH";
    public  final static String     ACTION_UPDATELIST =  "info.lamatricexiste.network.UPDATELIST";
    public  final static String     ACTION_WIFI       =  "info.lamatricexiste.network.WIFI";
    private final   int             TIMEOUT_REACH     =  600;
    private final   long            UPDATE_INTERVAL   =  60000; //1mn
    private         WifiManager     wifi              =  null;
    private         DhcpInfo        dhcp              =  null;
    private         Timer           timer             =  new Timer();
    private         List<InetAddress> hosts           =  new ArrayList<InetAddress>();
    private         InetAddress     ip_net            =  null;
    private         InetAddress     ip_bc             =  null;
    private         InetAddress     host_id           =  null;
    private         InetAddress     net_id            =  null;

    @Override public void onCreate(){
        super.onCreate();
    }

    @Override public void onDestroy() {
        stopService();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent){
        getWifi();
        if(hosts.isEmpty()){
            Log.v(TAG, "HOSTS IS EMPTY");
            onUpdate();
        }
        sendBroadcast(new Intent(ACTION_UPDATELIST));
//        try {
//            ip_bc = InetAddress.getByName("10.0.10.50");
//            ip_net = InetAddress.getByName("10.0.10.0");
//            host_id = InetAddress.getByName("0.0.0.255");
//        } catch (UnknownHostException e) {
//            Log.e(TAG, e.getMessage());
//        }
        return mBinder;
    }

    private void startService(final List<InetAddress> hosts_send, final int request){
        timer.scheduleAtFixedRate(
            new TimerTask() {
                public void run() {
                    launchRequest(hosts_send, request);
                }
            }, 0, UPDATE_INTERVAL);
    }

    private void stopService(){
        if(timer!=null) timer.cancel();
    }
    
/**
 * Runnable requests
 */
    
    private void onUpdate(){
        hosts = new ArrayList<InetAddress>();
        
        final List<InetAddress> hosts_all = getAllHosts();
        int len = hosts_all.size();
        int pos = hosts_all.indexOf(getIp(dhcp.ipAddress));

        for(int i=pos-1; i>=0; i--){
            final int mod = i;
            final InetAddress host = hosts_all.get(mod);
            Thread t = new Thread() {
                public void run(){
                    int msg = checkHost(host);
                    if(msg!=0){
                        hosts.add(host);
                        Intent i = new Intent(ACTION_SENDHOST);
                        i.putExtra("addr", host.getHostAddress());
                        sendBroadcast(i);
                    }
                    Thread.currentThread().interrupt(); //FIXME Ceci est un test
                }
            };
            t.start();
        }
        
        for(int i=pos+1; i<len; i++){
            final int mod = i;
            final InetAddress host = hosts_all.get(mod);
            Thread t = new Thread() {
                public void run(){
                    int msg = checkHost(host);
                    if(msg!=0){
                        hosts.add(host);
                        Intent i = new Intent(ACTION_SENDHOST);
                        i.putExtra("addr", host.getHostAddress());
                        sendBroadcast(i);
                    }
                    Thread.currentThread().interrupt(); //FIXME Ceci est un test
                }
            };
            t.start();
        }
        sendBroadcast(new Intent(ACTION_FINISH));
    }
    
    private void launchRequest(List<InetAddress> hosts_send, int request){
        for(InetAddress h : hosts_send){
            Thread t = new Thread(getRunnable(h, request));
            t.start();
        }
    }
    
    private Runnable getRunnable(final InetAddress host, int request){
        switch(request){
        case 0:
            SmbPoc smb = new SmbPoc();
            smb.setHost(host);
            return (Runnable) smb;
        default:
            return new Runnable() {
                @Override public void run() {
                    try {
                        host.isReachable(TIMEOUT_REACH);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
        }
    }

/**
 * Interface binder
 */

    private final NetworkInterface.Stub mBinder = new NetworkInterface.Stub() {

        public void inSearchReachableHosts() throws DeadObjectException  {
            Log.v(TAG, "inSearchReachableHosts");
            onUpdate();
        }

        public List<String> inGetHosts() throws RemoteException {
            Log.v(TAG, "inGetHosts");
            return hostsToStr();
        }

        public void inSendPacket(List<String> hosts_send, int request, boolean repeat) {
            Log.v(TAG, "inSendPacket");
            List<InetAddress> hosts_receive = hostsFromStr(hosts_send);
            if(repeat){
                startService(hosts_receive, request);
            }
            else {
                stopService();
            }
            launchRequest(hosts_receive, request);
        }

        public String inNetInfo() throws DeadObjectException {
            WifiInfo wifiInfo = wifi.getConnectionInfo();
            return  "ip: " + getIp(dhcp.ipAddress).getHostAddress() + "\t nt: " + ip_net.getHostAddress() + "/" + getNetCidr() +"\n"+
                    "ssid: " + wifiInfo.getSSID() + "\t bssid: " + wifiInfo.getBSSID();
        }
    };

/**
 * Network Logic
 */
    private void getWifi(){
        wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if(wifi.isWifiEnabled()) {
            dhcp = wifi.getDhcpInfo();
            ip_bc = getBroadcastIP();
            ip_net = getNetIP();
            host_id = getHostId();
            net_id = getNetId();
            sendBroadcast(new Intent(ACTION_WIFI));
        }
    }

    private List<String> hostsToStr(){
        List<String> hosts_str = new ArrayList<String>();
        for(InetAddress h : hosts){
            hosts_str.add(h.getHostAddress());
        }
        return hosts_str;
    }

    private List<InetAddress> hostsFromStr(List<String> hosts_str){
        List<InetAddress> hosts_new = new ArrayList<InetAddress>();
        for(String h : hosts_str){
            try {
                hosts_new.add(InetAddress.getByName(h));
            } catch (UnknownHostException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return hosts_new;
    }
    
    private int checkHost(InetAddress host){
        Reachable r = new Reachable();
        try {
            if(host.isReachable(TIMEOUT_REACH) || r.request(host)){
                return getIpInt(host);
            }
        }
        catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return 0;
    }

    private List<InetAddress> getAllHosts(){
        List<String> hosts_new = new ArrayList<String>();
        String[] ip_net_split = ip_net.getHostAddress().split("\\.");
        String[] ip_bc_split = ip_bc.getHostAddress().split("\\.");
        String ip_str = getIp(dhcp.ipAddress).getHostAddress();
    
        switch (host_id.hashCode()) {
      
        case 7:
        case 255:
        case 65535:
        case 16777215:
            // 1.0.0.1 to 126.255.255.254 255.255.255.0
            Integer start = Integer.parseInt(ip_net_split[(ip_net_split.length-1)])+1;
            Integer end = Integer.parseInt(ip_bc_split[(ip_bc_split.length-1)]);
            String ip_start = ip_str.substring(0, ip_str.lastIndexOf("."));
            for(int i=start; i<end; i++){
                hosts_new.add(ip_start+"."+i);
            }
            break;
          
//      case 65535:
//          // 128.1.0.1 to 191.255.255.254 255.255.0.0
//          Integer s1 = Integer.parseInt(ip_net_split[(ip_net_split.length-2)]);
//          Integer e1 = Integer.parseInt(ip_bc_split[(ip_bc_split.length-2)]);
//          Integer s2 = Integer.parseInt(ip_net_split[(ip_net_split.length-1)]);
//          Integer e2 = Integer.parseInt(ip_bc_split[(ip_bc_split.length-1)]);
//          String ip1 = ip_net_str.substring(0, ip_net_str.indexOf(".", 4));
//          for(int i=s1; i<=e1; i++){
//              for(int j=s2; j<=e2; j++){
//                  InetAddress ip_host = InetAddress.getByName(ip1+"."+i+"."+j);
//                  hosts_new.add(ip_host);
//              }
//          }
//          break;
//          
//      case 16777215:
//          // 192.0.1.1 to 223.255.254.254 255.0.0.0
//          break;
          
        }
        
        return hostsFromStr(hosts_new);
    }
    
    private int getNetCidr(){
        String[] addr = net_id.getHostAddress().split("\\.");
        int cidr = 0;
        for(String a : addr){
            int i = Integer.parseInt(a) + 1;
            cidr += Math.floor(i/32);
        }
        return cidr;
    }
    
    private InetAddress getHostId(){
        int network = ~dhcp.netmask;
        return getIp(network);
    }
    
    private InetAddress getNetId(){
        int network = dhcp.netmask;
        return getIp(network);
    }

    private InetAddress getNetIP(){
        int network = (dhcp.ipAddress & dhcp.netmask);
        return getIp(network);
    }

    private InetAddress getBroadcastIP(){
        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        return getIp(broadcast);
    }
    
    private InetAddress getIp(int ip_int){
        byte[] quads = new byte[4];
        
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((ip_int >> k * 8) & 0xFF);
        try {
            return InetAddress.getByAddress(quads);
        }
        catch (java.net.UnknownHostException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
    }
    
    private int getIpInt(InetAddress ip_addr) {
        String[] a = ip_addr.getHostAddress().split("\\.");
        return (
                Integer.parseInt(a[3])*16777216 + 
                Integer.parseInt(a[2])*65536 +
                Integer.parseInt(a[1])*256 +
                Integer.parseInt(a[0])
        );
    }

}
