package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

public class Network extends Service
{
    private final   String          TAG               =  "NetworkService";
    public  final   static String   ACTION_SENDHOST   =  "info.lamatricexiste.network.SENDHOST";
    public  final   static String   ACTION_FINISH     =  "info.lamatricexiste.network.FINISH";
    public  final   static String   ACTION_UPDATELIST =  "info.lamatricexiste.network.UPDATELIST";
    public  final   static String   ACTION_WIFI       =  "info.lamatricexiste.network.WIFI";
    public  final   static int      TIMEOUT_REACH     =  600;
    private final   long            UPDATE_INTERVAL   =  60000; //1mn
    public  static  int             WifiState         =  -1;
    private         WifiManager     wifi              =  null;
    private         DhcpInfo        dhcp              =  null;
    private         Timer           timer             =  new Timer();
    private         List<InetAddress> hosts           =  new ArrayList<InetAddress>();
    private         InetAddress     ip_net            =  null;
    private         InetAddress     ip_bc             =  null;
    @SuppressWarnings("unused")
    private SharedPreferences       prefs             =  null;
    private BroadcastReceiver       receiver          =  new BroadcastReceiver(){
        public void onReceive(Context ctxt, Intent intent){
            String a = intent.getAction();
            if(a.equals(Network.ACTION_SENDHOST)){
                String h = intent.getExtras().getString("addr");
                try {
                    InetAddress i = InetAddress.getByName(h);
                    if(!hosts.contains(i)){
                        hosts.add(i);
                    }
                } catch (UnknownHostException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            else if(a.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
                int extra = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
                if(WifiState!=extra){
                    WifiState = extra;
                    sendBroadcast(new Intent(ACTION_WIFI));
                }
            }
            else if(a.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
                // Wifi is associated
                sendBroadcast(new Intent(ACTION_WIFI));
            }
        }
    };

    @Override public void onCreate(){
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Network.ACTION_SENDHOST);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(receiver, filter);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override public void onDestroy() {
        stopService();
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent){
        getWifi();
        /*
        if(hosts.isEmpty()){
            Log.v(TAG, "HOSTS IS EMPTY");
            onUpdate(1); //FIXME
        }
        */
        sendBroadcast(new Intent(ACTION_UPDATELIST));
//        try {
//            ip_bc = InetAddress.getByName("10.0.10.50");
//            ip_net = InetAddress.getByName("10.0.10.0");
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
    
    private void onUpdate(int method){
        hosts = new ArrayList<InetAddress>();
        //TODO: handler multiple methods
        if(isWifiEnabled()){
            switch(method){
                case 1:
                    DiscoveryUnicast run = new DiscoveryUnicast();
                    run.setVar(this, getIp(dhcp.ipAddress), ip_net, ip_bc, getNetmask(), getNetCidr());
                    new Thread(run).start();
                    break;
                default:
                    Log.v(TAG, "No discovery method selected!");
            }
        }
    }
    
    private void launchRequest(List<InetAddress> hosts_send, int request){
        if(isWifiEnabled()){
            for(InetAddress h : hosts_send){
                Thread t = new Thread(getRunnable(h, request));
                t.start();
            }
            sendBroadcast(new Intent(ACTION_FINISH)); //TODO: Move to SendSmbNegotiate and so on
        }
    }
    
    private Runnable getRunnable(final InetAddress host, int request){
        switch(request){
        case 0:
            SendSmbNegotiate smb = new SendSmbNegotiate();
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

        public void inSearchReachableHosts(int method) throws DeadObjectException  {
            Log.v(TAG, "inSearchReachableHosts");
            onUpdate(method);
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
            String ret = "";
            switch(WifiState){
            
                case WifiManager.WIFI_STATE_ENABLED:
                    getWifi();
                    if(isWifiEnabled()){
                        WifiInfo wifiInfo = wifi.getConnectionInfo();
                        ret = "ip: " + getIp(dhcp.ipAddress).getHostAddress() + "\t nt: " + ip_net.getHostAddress() + "/" + getNetCidr() +"\n"+
                              "ssid: " + wifiInfo.getSSID() + "\t bssid: " + wifiInfo.getBSSID();
                    } else {
                        ret = "Wifi is enabled";
                    }
                    break;
                    
                case WifiManager.WIFI_STATE_ENABLING:
                    ret = "Wifi is enabling";
                    break;
                    
                case WifiManager.WIFI_STATE_DISABLED:
                    ret = "Wifi is disabled";
                    break;
                
                case WifiManager.WIFI_STATE_DISABLING:
                    ret = "Wifi is disabling";
                    break;
                
                case WifiManager.WIFI_STATE_UNKNOWN:
                    ret = "Wifi state unknown";
                    break;
                
                default:
                    ret = "Wifi is acting strangely";
            
            }
            return ret;
        }
    };

/**
 * Network Logic
 */
    private Boolean isWifiEnabled(){
        if( wifi!=null && dhcp!=null && 
            wifi.getConnectionInfo().getBSSID()!=null &&
            WifiState==WifiManager.WIFI_STATE_ENABLED){
            return true;
        }
        return false;
    }
    
    private void getWifi(){
        wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if(wifi.isWifiEnabled()) {
            dhcp = wifi.getDhcpInfo();
            ip_bc = getBroadcastIP();
            ip_net = getNetIP();
//            sendBroadcast(new Intent(ACTION_WIFI));
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
    
    private int getNetCidr(){
        int i = dhcp.netmask;
        i = i - ((i >> 1) & 0x55555555);
        i = (i & 0x33333333) + ((i >> 2) & 0x33333333);
        return ((i + (i >> 4) & 0xF0F0F0F) * 0x1010101) >> 24;
    }
    
    private InetAddress getInvertedNetmask(){
        int network = ~dhcp.netmask;
        return getIp(network);
    }
    
    private InetAddress getNetmask(){
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
    
//    private int getIpInt(InetAddress ip_addr) {
//        String[] a = ip_addr.getHostAddress().split("\\.");
//        return (
//                Integer.parseInt(a[3])*16777216 + 
//                Integer.parseInt(a[2])*65536 +
//                Integer.parseInt(a[1])*256 +
//                Integer.parseInt(a[0])
//        );
//    }

}
