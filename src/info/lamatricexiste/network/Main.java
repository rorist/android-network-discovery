package info.lamatricexiste.network;

import java.util.ArrayList;
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
import android.content.SharedPreferences;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

final public class Main extends Activity {
    
    private final String          TAG = "NetworkMain";
    private final int             DEFAULT_DISCOVER = 1;
    private List<String>          hosts = new ArrayList<String>();
    private NetworkInterface      netInterface = null;
    private ArrayAdapter<String>  adapter;
    private ListView              list;
    private Button                btn;
    private Button                btn1;
    private SharedPreferences     prefs = null;;
    private final CharSequence[]  items = {"Ping (ICMP)","Samba exploit"};

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
//        info = (TextView) findViewById(R.id.info);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

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
        filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
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

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(getApplication()).inflate(R.menu.options, menu);
        return (super.onCreateOptionsMenu(menu));
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.settings){
            startActivity(new Intent(this, Prefs.class));
            return true;
        }
        return (super.onOptionsItemSelected(item));
    }
    
    // Broadcast Receiver
    private BroadcastReceiver receiver = new BroadcastReceiver(){
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
            else if(a.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
                setWifiState(intent);
            }
            else if(a.equals(WifiManager.NETWORK_IDS_CHANGED_ACTION) ||
                    a.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION) ||
                    a.equals(WifiManager.RSSI_CHANGED_ACTION) ||
                    a.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) ||
                    a.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION) ||
                    a.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION) ||
                    a.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
                setWifiInfo();
            }
        }
    };
    
    private void setWifiInfo(){
        TextView info_ip = (TextView) findViewById(R.id.info_ip);
        TextView info_nt = (TextView) findViewById(R.id.info_nt);
        TextView info_id = (TextView) findViewById(R.id.info_id);
        ImageView info_status = (ImageView) findViewById(R.id.info_status);
        WifiManager wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifi.getConnectionInfo();
        SupplicantState sstate = wifiInfo.getSupplicantState();
        
        info_ip.setText("");
        info_nt.setText("");
        switch (sstate) {
            case SCANNING:
                info_id.setText(R.string.wifi_scanning);
                info_status.setImageResource(R.drawable.wifi_inactive);
                break;
            case ASSOCIATED:
            case ASSOCIATING:
                info_id.setText(String.format(getString(R.string.wifi_associating), "myNetwork"));
                info_status.setImageResource(R.drawable.wifi_inactive);
                break;
            case COMPLETED:
                info_ip.setText("IP: ");
                info_nt.setText("NT: ");
                info_id.setText("SSID: ");
                info_status.setImageResource(R.drawable.wifi_inactive);
                break;
            default:
                info_id.setText(R.string.wifi_other);
                info_status.setImageResource(R.drawable.wifi_inactive);
        }
    }
    
    private void setWifiState(Intent intent){
        TextView info_ip = (TextView) findViewById(R.id.info_ip);
        TextView info_nt = (TextView) findViewById(R.id.info_nt);
        TextView info_id = (TextView) findViewById(R.id.info_id);
        ImageView info_status = (ImageView) findViewById(R.id.info_status);
        
        info_ip.setText("");
        info_nt.setText("");
        int WifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
        switch(WifiState){
            case WifiManager.WIFI_STATE_ENABLED:
                info_id.setText(R.string.wifi_enabled);
                info_status.setImageResource(R.drawable.wifi_inactive);
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                info_id.setText(R.string.wifi_enabling);
                info_status.setImageResource(R.drawable.wifi_inactive);
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                info_id.setText(R.string.wifi_disabling);
                info_status.setImageResource(R.drawable.wifi_disabled);
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                info_id.setText(R.string.wifi_disabled);
                info_status.setImageResource(R.drawable.wifi_disabled);
                break;
            case WifiManager.WIFI_STATE_UNKNOWN:
                info_id.setText(R.string.wifi_unknown);
                info_status.setImageResource(R.drawable.wifi_disabled);
                break;
            default:
                info_id.setText(R.string.wifi_strange);
                info_status.setImageResource(R.drawable.wifi_disabled);
        }
    }

/**
 * Service connection
 */
    
    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.v(TAG, "Service binded");
            netInterface = NetworkInterface.Stub.asInterface((IBinder)service);
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
                int method = Integer.parseInt(prefs.getString("discover_method", String.valueOf(DEFAULT_DISCOVER)));
                netInterface.inSearchReachableHosts(method);
            } catch (RemoteException e) {
                Log.e(TAG, e.getMessage());
            } catch(ClassCastException e) {
                Log.e(TAG, e.getMessage());
            }
            return (long) 1;
        }
        protected void onPostExecute(Long result) {
            Log.v(TAG, "CheckHostsTask, onPostExecute " + result);
        }
    }

/**
 * Main
 */

    private void getUpdate(){
        setButtonOff(btn1);
        new CheckHostsTask().execute();
        makeToast("Updating list ...");
    }
        
    private void sendPacket(){
        CheckBox cb = (CheckBox) findViewById(R.id.repeat); //FIXME: This is bad
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
    
    private void updateList(){
        adapter.clear();
        listHosts();
    }
    
    private void listHosts(){
        for(String h : hosts){
            addText(h);
        }
//        list.setSelection(View.FOCUS_DOWN);
    }

    private void addText(String text){
        adapter.add(text);
//        Button btn_list = (Button) findViewById(R.id.btn_list);
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
    
    private void setButtonOff(Button b){
        b.setClickable(false);
        b.setEnabled(false);
    }
    
    private void setButtonOn(Button b){
        b.setClickable(true);
        b.setEnabled(true);
    }
}
