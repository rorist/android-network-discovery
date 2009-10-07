package info.lamatricexiste.network;

import java.net.InetAddress;
import android.content.Context;

public class DiscoveryStealth implements Runnable 
{
    private final String TAG    = "Discovery";
    private Context     ctxt    =  null;
    private InetAddress ip      =  null;
    private InetAddress ip_net  =  null;
    private InetAddress ip_bc   =  null;
    private InetAddress host_id =  null;

    public void run(){
        discover();
    }
    
    public void setVar(Context ctxt, InetAddress ip, InetAddress ip_net, InetAddress ip_bc, InetAddress host_id){
        this.ctxt = ctxt;
        this.ip = ip;
        this.ip_net = ip_net;
        this.ip_bc = ip_bc;
        this.host_id = host_id;
    } 

    private void discover(){
        Log.v(TAG, "Please override me");
    }
    
}
