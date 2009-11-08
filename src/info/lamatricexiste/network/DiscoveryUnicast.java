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

        //gateway
        launch(start);
        
        //rewind
        for(int i=ip_int-1; i>start; i--){
            launch(i);
        }
        
        //forward
        for(int j=ip_int+1; j<=end; j++){
            launch(j);
        }

        ctxt.sendBroadcast(new Intent(Network.ACTION_FINISH));
    }
    
    private void launch(int i){
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
    
    private boolean checkHost(String host){
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
