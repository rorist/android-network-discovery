package info.lamatricexiste.smbpoc;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

final public class Main extends Activity {
    
    private final String TAG = "NetworkMain";
    private List<String>          hosts = new ArrayList<String>();
    private NetworkInterface      netInterface = null;
    private ArrayAdapter<String>  adapter;
    private ListView              list;
    private TextView              info;
    private Button                btn;
    private Button                btn1;
    private CheckBox              cb;
    static  Main                  singleton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        singleton = this;
        setContentView(R.layout.main);
        
        info = (TextView) findViewById(R.id.info); 
        cb = (CheckBox) findViewById(R.id.repeat);
//        EditText ipedit = (EditText) findViewById(R.id.ip);
        
        btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendPacket();
            }
        });
        
        btn1 = (Button) findViewById(R.id.btn1);
        btn1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    btn1.setClickable(false);
                    netInterface.inSearchReachableHosts();
                    updateList();
                    btn1.setClickable(true);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });
        
        adapter = new ArrayAdapter<String>(this, R.layout.list);
        list = (ListView) findViewById(R.id.output);
        list.setAdapter(adapter);
        
        Intent intent = new Intent(this, Network.class);
        this.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }
    
    @Override
    public void onResume(){
        super.onResume();
        this.bindService(new Intent(this, Network.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    public void onPause(){
        super.onPause();
        this.unbindService(mConnection);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, Network.class));
    }
    
    private void updateList(){
        getHosts();
        adapter.clear();
        listHosts();
        makeToast("Updating Hosts ...");
    }
    
    private void listHosts(){
        for(String h : hosts){
            addText(h);
        }
    }
    
    private void makeToast(String txt){
        Toast.makeText(getApplicationContext(), (CharSequence)txt, Toast.LENGTH_SHORT).show();
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
            Log.e(TAG, e.getMessage());
        }
        catch (NullPointerException e){
            Log.e(TAG, e.getMessage());
        }
    }

/**
 * Service connection
 */
    
    private void sendPacket(){
        boolean repeat = cb.isChecked();
        try {
            netInterface.inSendPacket(repeat);
            makeToast("Sending Packets ...");
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage());
        }
    }
    
    private void getHosts(){
        try {
            hosts = netInterface.inGetHosts();
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage());
        }
    }
    
    public final Handler uiThreadCallback = new Handler();
    
    public final Runnable runInUiThread = new Runnable(){
        @Override public void run(){
            updateList();
            Log.v(TAG, "RuninUiThread");
        }
    };

    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service) {
            netInterface = NetworkInterface.Stub.asInterface((IBinder)service);
            try {
                btn1.setClickable(false);
                addTextInfo("IP: " + netInterface.inGetIp() + "\nNT: " + netInterface.inGetIpNet() + "\nBC: " + netInterface.inGetIpBc());
//                try {
//                    netInterface.inSearchReachableHosts();
                    updateList();
//                } catch (RemoteException e) {
//                    Log.e(TAG, e.getMessage());
//                }
                btn1.setClickable(true);
            }
            catch (RemoteException e){
                Log.e(TAG, e.getMessage());
            }
            catch (NullPointerException e){
                Log.e(TAG, e.getMessage());
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            netInterface = null;
        }
    };
}
