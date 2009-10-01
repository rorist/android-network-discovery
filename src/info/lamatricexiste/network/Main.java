package info.lamatricexiste.network;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

final public class Main extends Activity {
    
    private final String          TAG = "NetworkMain";
    private List<String>          hosts = new ArrayList<String>();
    private NetworkInterface      netInterface = null;
    private ArrayAdapter<String>  adapter;
    private ListView              list;
    private TextView              info;
    private Button                btn;
    private Button                btn1;
    private CheckBox              cb;
    private final CharSequence[]  items = {"Ping (ICMP)","Samba exploit"};
    private BroadcastReceiver     receiver = new BroadcastReceiver(){
        public void onReceive(Context ctxt, Intent intent){
            String a = intent.getAction();
            Log.v(TAG, "Receive broadcasted "+a);
            if(a.equals(Network.ACTION_SENDHOST)){
                String h = intent.getExtras().getString("addr");
                if(!hosts.contains(h)){
                    hosts.add(h);
                    updateList();
                }
                
            }
            else if(a.equals(Network.ACTION_FINISH)){
                setButtonOn(btn);
                setButtonOn(btn1);
            }
            else if(a.equals(Network.ACTION_UPDATELIST)){
                updateList();
            }
            else if(a.equals(Network.ACTION_WIFI)){
                try {
                    addTextInfo(netInterface.inNetInfo());
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        info = (TextView) findViewById(R.id.info); 
        cb = (CheckBox) findViewById(R.id.repeat);

        // Send Request
        btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendPacket();
            }
        });
        
        // Reload
        btn1 = (Button) findViewById(R.id.btn1);
        btn1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setSelectedHosts(false);
                getUpdate();
            }
        });
        
        // Wifi Settings
        Button btn2 = (Button) findViewById(R.id.btn2);
        btn2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });
        
        // All
        Button btn3 = (Button) findViewById(R.id.btn3);
        btn3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setSelectedHosts(true);
            }
        });
        
        // None
        Button btn4 = (Button) findViewById(R.id.btn4);
        btn4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                setSelectedHosts(false);
            }
        });
        
        // Hosts list
        adapter = new ArrayAdapter<String>(this, R.layout.list, R.id.list);
        list = (ListView) findViewById(R.id.output);
        list.setAdapter(adapter);
        
        startService(new Intent(this, Network.class));
    }
    
    @Override public void onResume(){
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Network.ACTION_SENDHOST);
        filter.addAction(Network.ACTION_FINISH);
        filter.addAction(Network.ACTION_WIFI);
        registerReceiver(receiver, filter);
        this.bindService(new Intent(this, Network.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override public void onPause(){
        super.onPause();
        this.unbindService(mConnection);
        unregisterReceiver(receiver);
    }
    
    @Override protected void onStop() {
        super.onStop();
        stopService(new Intent(this, Network.class));
    }

    private void getUpdate(){
        setButtonOff(btn1);
        new CheckHostsTask().execute();
        makeToast("Updating list ...");
    }
    
    private void updateList(){
        adapter.clear();
        listHosts();
    }
        
    private void sendPacket(){
        final boolean repeat = cb.isChecked();
        setButtonOff(btn);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select method");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                try {
                    makeToast("Sending request ...");
                    netInterface.inSendPacket(getSelectedHosts(), item, repeat);
                } catch (RemoteException e) {
                    Log.e(TAG, e.getMessage());
                } catch (IllegalStateException e){
                    Log.e(TAG, e.getMessage());
                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }
//FIXME
//    private void getHosts(){
//        try {
//            hosts = netInterface.inGetHosts();
//        } catch (RemoteException e) {
//            Log.e(TAG, e.getMessage());
//        }
//    }
    
    private void listHosts(){
        for(String h : hosts){
            addText(h);
        }
//        list.setSelection(View.FOCUS_DOWN);
    }
    
    private List<String> getSelectedHosts(){
        List<String> hosts_s = new ArrayList<String>();
        int listCount = list.getChildCount();
        for(int i=0; i<listCount; i++){
            CheckBox cb = (CheckBox) list.getChildAt(i).findViewById(R.id.list);
            if(cb.isChecked()){
                hosts_s.add(hosts.get(i));
            }
        }
        return hosts_s;
    }
    
    private void setSelectedHosts(Boolean all){
        int listCount = list.getChildCount();
        for(int i=0; i<listCount; i++){
            CheckBox cb = (CheckBox) list.getChildAt(i).findViewById(R.id.list);
            if(all){
                cb.setChecked(true);
            } else {
                cb.setChecked(false);
            }
        }
    }
    
    private void makeToast(String txt){
        Toast.makeText(getApplicationContext(), (CharSequence)txt, Toast.LENGTH_SHORT).show();
    }

    private void addText(String text){
        adapter.add(text);
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
    
    private void setButtonOff(Button b){
        b.setClickable(false);
        b.setEnabled(false);
    }
    
    private void setButtonOn(Button b){
        b.setClickable(true);
        b.setEnabled(true);
    }

/**
 * Service connection
 */
    
    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.v(TAG, "Service binded");
            netInterface = NetworkInterface.Stub.asInterface((IBinder)service);
            try {
                addTextInfo(netInterface.inNetInfo());
            } catch (RemoteException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.v(TAG, "Service unbinded");
            netInterface = null;
        }
    };

    private class CheckHostsTask extends AsyncTask<Void, Integer, Long> {
        protected Long doInBackground(Void... v) {
            Log.v(TAG, "CheckHostsTask, doInBackground");
            try {
                netInterface.inSearchReachableHosts();
            } catch (RemoteException e) {
                Log.e(TAG, e.getMessage());
            }
            return (long) 1;
        }
        protected void onPostExecute(Long result) {
            Log.v(TAG, "CheckHostsTask, onPostExecute " + result);
        }
    }
}
