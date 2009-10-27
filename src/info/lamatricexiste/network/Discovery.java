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
    protected InetAddress ip_mask =  null;
    protected int         cidr    =  24;

    public void run(){
        discover();
    }
    
    public void setVar(Context ctxt, InetAddress ip, InetAddress ip_net, InetAddress ip_bc, InetAddress ip_mask, int cidr){
        this.ctxt = ctxt;
        this.ip = ip;
        this.ip_net = ip_net;
        this.ip_bc = ip_bc;
        this.ip_mask = ip_mask;
        this.cidr = cidr;
    } 

    protected void discover(){
        Log.v(TAG, "Please override me");
    }
    
}
