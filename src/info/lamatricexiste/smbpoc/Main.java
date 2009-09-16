package info.lamatricexiste.smbpoc;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.lang.NullPointerException;
import java.util.ConcurrentModificationException;

import android.app.Activity;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class Main extends Activity {
    
    //    private final String TAG = "Network hardening";
    private List<String>          hosts = new ArrayList<String>();
    private NetworkInterface      netInterface = null;
    private ArrayAdapter<String>  adapter;
    private ListView              list;
    private TextView              info;
    private Button                btn;
    private Button                btn1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        info = (TextView) findViewById(R.id.info); 
//        EditText ipedit = (EditText) findViewById(R.id.ip);
        btn = (Button) findViewById(R.id.btn);
        btn1 = (Button) findViewById(R.id.btn1);

        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });
        btn1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    hosts = netInterface.inGetReachableHosts();
                }
                catch(RemoteException e){
                    addText(e.getMessage());
                }
                updateList();
            }
        });
        
        adapter = new ArrayAdapter<String>(this, R.layout.list);
        list = (ListView) findViewById(R.id.output);
        list.setAdapter(adapter);
        
        this.bindService(new Intent(this, Network.class), mConnection, Context.BIND_AUTO_CREATE);
        addText("Main started");
    }
    
    @Override
    public void onResume(){
        super.onResume();
        startService(new Intent(this, Network.class));
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, Network.class));
    }
    
    private void updateList(){
        //adapter.clear();
        listHosts();
    }
    
    private void listHosts(){
        for(String h : hosts){
            addText(h);
        }
    }

    private void addText(String text){
        adapter.add(text);
        list.setSelection(View.FOCUS_DOWN);
    }
    
    private void addTextInfo(String text){
        try {
            info.setText(text);
        }
        catch (ConcurrentModificationException e){
            addText(e.getMessage());
        }
        catch (NullPointerException e){
            addText(e.getMessage());
        }
    }

/**
 * Service connection
 */

    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service) {
            addText("Service connected");
            netInterface = NetworkInterface.Stub.asInterface((IBinder)service);
            try {
                addTextInfo("IP: " + netInterface.inGetIp() + 
                            "\nNT: " + netInterface.inGetIpNet() +
                            "\nBC: " + netInterface.inGetIpBc());
                hosts = netInterface.inGetReachableHosts();
            }
            catch (RemoteException e){
                addText(e.getMessage());
            }
            updateList();
        }

        public void onServiceDisconnected(ComponentName className) {
            addText("Service disconnected");
            netInterface = null;
        }
    };
}
