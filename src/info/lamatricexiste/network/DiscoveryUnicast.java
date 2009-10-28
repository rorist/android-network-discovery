package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;

import android.content.Intent;
import android.util.Log;

public class DiscoveryUnicast extends Discovery
{
    protected final String TAG    = "DiscoveryUnicast";

    protected void discover(){
        int ip_int = ip.hashCode();
        int start = ip_int & (1 - (1<<(32 - cidr)));
        int end = ip_int | ((1<<(32 - cidr)) - 1);
        
        Intent intent = new Intent(Network.ACTION_TOTALHOSTS);
        intent.putExtra("total", (end-start));
        ctxt.sendBroadcast(intent);

        for(int i=start; i<=end; i++){
            final String host = hashToIp(i);
            Thread t = new Thread() {
                public void run(){
                    if(checkHost(host)){
                        Intent i = new Intent(Network.ACTION_SENDHOST);
                        i.putExtra("addr", host);
                        ctxt.sendBroadcast(i);
                    }
                    Thread.currentThread().interrupt();
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
    
    private String hashToIp(int i) {
        return ((i >> 24 ) & 0xFF) + "." +
               ((i >> 16 ) & 0xFF) + "." +
               ((i >>  8 ) & 0xFF) + "." +
               ( i        & 0xFF);
    }
}
