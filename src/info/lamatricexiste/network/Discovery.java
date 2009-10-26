package info.lamatricexiste.network;

import java.net.InetAddress;

import android.content.Context;
import android.util.Log;

public class Discovery implements Runnable 
{
    protected final String TAG    = "Discovery";
    protected Context     ctxt    =  null;
    protected InetAddress ip      =  null;
    protected InetAddress ip_net  =  null;
    protected InetAddress ip_bc   =  null;
    protected InetAddress host_id =  null;

    public void run(){
        discover();
    }
    
    public void setVar(Context ctxt, InetAddress ip, InetAddress ip_net, InetAddress ip_bc, InetAddress host_id){
        Log.v(TAG, "setVar");
        this.ctxt = ctxt;
        this.ip = ip;
        this.ip_net = ip_net;
        this.ip_bc = ip_bc;
        this.host_id = host_id;
    } 

    protected void discover(){
        Log.v(TAG, "Please override me");
    }
    
}
