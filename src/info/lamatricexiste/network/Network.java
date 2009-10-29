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
    public  final   static String   ACTION_TOTALHOSTS =  "info.lamatricexiste.network.TOTALHOSTS";
    public  final   static int      TIMEOUT_REACH     =  600;
    private final   long            UPDATE_INTERVAL   =  60000; //1mn
    public  static  int             WifiState         =  -1;
    private         WifiManager     wifi              =  null;
    private         DhcpInfo        dhcp              =  null;
    private         Timer           timer             =  new Timer();
    private         List<InetAddress> hosts           =  new ArrayList<InetAddress>();
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
                WifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            }
        }
    };

    @Override public void onCreate(){
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Network.ACTION_SENDHOST);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
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
//        sendBroadcast(new Intent(ACTION_UPDATELIST));
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
                    run.setVar(this, getIp(), getNetIP(), getBroadcastIP(), getNetmask(), getNetCidr());
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
    
//    private InetAddress getInvertedNetmask(){
//        int network = ~dhcp.netmask;
//        return getIp(network);
//    }
    
    private InetAddress getIp(){
        return getIpFromInt(dhcp.ipAddress);
    }
    
    private InetAddress getNetmask(){
        return getIpFromInt(dhcp.netmask);
    }

    private InetAddress getNetIP(){
        return getIpFromInt(dhcp.ipAddress & dhcp.netmask);
    }

    private InetAddress getBroadcastIP(){
        return getIpFromInt((dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask);
    }
    
    private InetAddress getIpFromInt(int ip_int){
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

}
