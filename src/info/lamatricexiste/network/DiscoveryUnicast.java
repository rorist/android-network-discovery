package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class DiscoveryUnicast extends Discovery
{
    private final String TAG    = "DiscoveryUnicast";
    private Context     ctxt    =  null;
    private InetAddress ip      =  null;
    private InetAddress ip_net  =  null;
    private InetAddress ip_bc   =  null;
    private InetAddress host_id =  null;

    private void discover(){
        final List<String> hosts_all = getAllHosts();
        int len = hosts_all.size();
        int pos = hosts_all.indexOf(ip.getHostAddress());

        for(int i=pos-1; i>=0; i--){
            final int mod = i;
            final String host = hosts_all.get(mod);
            Thread t = new Thread() {
                public void run(){
                    if(checkHost(host)){
                        Intent i = new Intent(Network.ACTION_SENDHOST);
                        i.putExtra("addr", host);
                        ctxt.sendBroadcast(i);
                    }
                    Thread.currentThread().interrupt(); //FIXME Ceci est un test
                }
            };
            t.start();
        }
        
        for(int i=pos+1; i<len; i++){
            final int mod = i;
            final String host = hosts_all.get(mod);
            Thread t = new Thread() {
                public void run(){
                    if(checkHost(host)){
                        Intent i = new Intent(Network.ACTION_SENDHOST);
                        i.putExtra("addr", host);
                        ctxt.sendBroadcast(i);
                    }
                    Thread.currentThread().interrupt(); //FIXME Ceci est un test
                }
            };
            t.start();
        }
        ctxt.sendBroadcast(new Intent(Network.ACTION_FINISH));
    }
    
    private Boolean checkHost(String host){
        Reachable r = new Reachable();
        try {
            InetAddress h = InetAddress.getByName(host); 
            if(h.isReachable(Network.TIMEOUT_REACH) || r.request(h)){
                return true;
            }
        }
        catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return false;
    }

    private List<String> getAllHosts(){
        List<String> hosts_new = new ArrayList<String>();
        String[] ip_net_split = ip_net.getHostAddress().split("\\.");
        String[] ip_bc_split = ip_bc.getHostAddress().split("\\.");
        String ip_str = ip.getHostAddress();
    
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
        
        return hosts_new;
    }
}
