package info.lamatricexiste.network;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

final public class Main extends Activity {
    
    private final String          TAG = "NetworkMain";
    private final int             DEFAULT_DISCOVER = 1;
    private List<String>          hosts = null;
    private List<CharSequence[]>  hosts_ports = null;
    private NetworkInterface      netInterface = null;
    private HostsAdapter          adapter;
    private ListView              list;
//    private Button                btn;
    private Button                btn_discover;
    private SharedPreferences     prefs = null;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Send Request
//        btn = (Button) findViewById(R.id.btn);
//        btn.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                sendPacket();
//            }
//        });
        
        // Discover
        btn_discover = (Button) findViewById(R.id.btn1);
        btn_discover.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	initList();
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
//        Button btn3 = (Button) findViewById(R.id.btn3);
//        btn3.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                setSelectedHosts(true);
//            }
//        });
        
        // None
//        Button btn4 = (Button) findViewById(R.id.btn4);
//        btn4.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                setSelectedHosts(false);
//            }
//        });
        
        // Hosts list
        adapter = new HostsAdapter(this, R.layout.list, R.id.list);
        list = (ListView) findViewById(R.id.output);
        list.setAdapter(adapter);
        list.setItemsCanFocus(true);
        
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
    
    // Custom ArrayAdapter
    private class HostsAdapter extends ArrayAdapter<String>
    {
        public HostsAdapter(Context context, int resource, int textViewresourceId){
            super(context, resource, textViewresourceId);
        }

        @Override public View getView(final int position, View convertView, ViewGroup parent){
            convertView = super.getView(position, convertView, parent);
            if(convertView!=null){
                Button btn_ports = (Button) convertView.findViewById(R.id.list_port);
                btn_ports.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        scanPort(position, hosts.get(position), false);
                    }
                });
            }
            return convertView;
        }
    }
    
    // Broadcast Receiver
    private BroadcastReceiver receiver = new BroadcastReceiver(){
        public void onReceive(Context ctxt, Intent intent){
            String a = intent.getAction();
            Log.d(TAG, "Receive broadcasted "+a);
            if(a.equals(Network.ACTION_SENDHOST)){
                String h = intent.getExtras().getString("addr");
                if(!hosts.contains(h)){
                    hosts.add(h);
                    hosts_ports.add(null);
                    updateList();
                }
            }
            else if(a.equals(Network.ACTION_FINISH)){
//                setButtonOn(btn);
                setButtonOn(btn_discover);
                makeToast("Discovery finished!");
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
                    a.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)){
                setWifiInfo();
            }
        }
    };
    
    private void setWifiInfo(){
        TextView info_ip = (TextView) findViewById(R.id.info_ip);
        TextView info_nt = (TextView) findViewById(R.id.info_nt);
        TextView info_id = (TextView) findViewById(R.id.info_id);
        WifiManager wifi = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifi.getConnectionInfo();
        SupplicantState sstate = wifiInfo.getSupplicantState();
        NetworkInfo net = new NetworkInfo((WifiManager) this.getSystemService(Context.WIFI_SERVICE));
        
        info_ip.setText("");
        info_id.setText("");
        setButtonOff(btn_discover);
        switch (sstate) {
            case SCANNING:
            	info_nt.setText(R.string.wifi_scanning);
                break;
            case ASSOCIATED:
            case ASSOCIATING:
            	info_nt.setText(String.format(getString(R.string.wifi_associating), net.getSSID()));
                break;
            case COMPLETED:
            	setButtonOn(btn_discover);
                info_ip.setText("IP: "+net.getIp().getHostAddress());
                info_nt.setText("NT: "+net.getNetIp().getHostAddress()+"/"+net.getNetCidr());
                info_id.setText("SSID: "+net.getSSID());
                break;
        }
    }
    
    private void setWifiState(Intent intent){
        TextView info_ip = (TextView) findViewById(R.id.info_ip);
        TextView info_nt = (TextView) findViewById(R.id.info_nt);
        TextView info_id = (TextView) findViewById(R.id.info_id);
        
        info_ip.setText("");
        info_id.setText("");
        int WifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
        setButtonOff(btn_discover);
        switch(WifiState){
            case WifiManager.WIFI_STATE_ENABLED:
            	info_nt.setText(R.string.wifi_enabled);
                setWifiInfo();
                break;
            case WifiManager.WIFI_STATE_ENABLING:
            	info_nt.setText(R.string.wifi_enabling);
                break;
            case WifiManager.WIFI_STATE_DISABLING:
            	info_nt.setText(R.string.wifi_disabling);
                break;
            case WifiManager.WIFI_STATE_DISABLED:
            	info_nt.setText(R.string.wifi_disabled);
                break;
            case WifiManager.WIFI_STATE_UNKNOWN:
            	info_nt.setText(R.string.wifi_unknown);
                break;
            default:
            	info_nt.setText(R.string.wifi_strange);
        }
    }

/**
 * Service connection
 */
    
    private ServiceConnection mConnection = new ServiceConnection()
    {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Service binded");
            netInterface = NetworkInterface.Stub.asInterface((IBinder)service);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "Service unbinded");
            netInterface = null;
        }
    };
 
/**
 * Discover hosts
 */
    
    private void getUpdate(){
        setButtonOff(btn_discover);
        new CheckHostsTask().execute();
        makeToast("Updating list ...");
    }

    private class CheckHostsTask extends AsyncTask<Void, Integer, Long> {
        protected Long doInBackground(Void... v) {
            Log.d(TAG, "CheckHostsTask, doInBackground");
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
//            Log.d(TAG, "CheckHostsTask, onPostExecute " + result);
        }
    }
    
/**
 * Port Scan
 */
    
    private class ScanPortTask extends AsyncTask<Void, Integer, Long> {
        private int position;
        private String host;
        private ProgressDialog progress = null;
        private CharSequence[] ports = null;
        protected void onPreExecute(){
    	    progress = ProgressDialog.show(Main.this, "", "Scanning ports ...", false);
        }
        protected Long doInBackground(Void... v) {
            ports = new PortScan().scan(host);
            return (long) 1;
        }
        protected void onPostExecute(Long result) {
        	hosts_ports.set(position, ports);
            progress.dismiss();
            showPorts(ports, position, host);
        }
        public void setInfo(int position, String host){
            this.position = position;
            this.host = host;
        }
    }

    private void scanPort(final int position, final String host, boolean force){
    	CharSequence[] ports = hosts_ports.get(position); 
    	if(force || ports==null){
	        ScanPortTask task = new ScanPortTask();
	        task.setInfo(position, host);
	        task.execute();
    	}
    	else {
    		showPorts(ports, position, host);
    	}
	}
    
    private void showPorts(final CharSequence[] ports, final int position, final String host){
		AlertDialog.Builder scanDone = new AlertDialog.Builder(Main.this);
        scanDone
        	.setTitle(host)
            .setPositiveButton("Rescan", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dlg, int sumthin) {
                    scanPort(position, host, true);
                }
            })
            .setNegativeButton("Close", null);
    	if(ports.length>0){
            scanDone.setItems(ports, null);
    	}
    	else {
    		scanDone.setMessage("No open port found");
    	}
    	scanDone.show();
    }
    
/**
 * Main
 */
        
//    private void sendPacket(){
//        CheckBox cb = (CheckBox) findViewById(R.id.repeat); //FIXME: This is bad
//        final boolean repeat = cb.isChecked();
//        final CharSequence[]  items = {"Ping (ICMP)","Samba exploit"};
//        setButtonOff(btn);
//        @SuppressWarnings("unused")
//		AlertDialog dialog = new AlertDialog.Builder(this)
//	        .setTitle("Select method")
//	        .setItems(items, new DialogInterface.OnClickListener() {
//	            public void onClick(DialogInterface dialog, int item) {
//	                try {
//	                    makeToast("Sending request ...");
//	                    netInterface.inSendPacket(getSelectedHosts(), item, repeat);
//	                } catch (RemoteException e) {
//	                    Log.e(TAG, e.getMessage());
//	                } catch (IllegalStateException e){
//	                    Log.e(TAG, e.getMessage());
//	                }
//	            }
//	        })
//	        .show();
//    }

    private void initList(){
//        setSelectedHosts(false);
        adapter.clear();
        hosts = new ArrayList<String>();
        hosts_ports = new ArrayList<CharSequence[]>();
    }
    
    private void updateList(){
        adapter.clear();
        listHosts();
    }
    
    private void listHosts(){
        for(String h : hosts){
            addText(h);
        }
    }

    private void addText(String text){
        adapter.add(text);
    }
    
//    private List<String> getSelectedHosts(){
//        List<String> hosts_s = new ArrayList<String>();
//        int listCount = list.getChildCount();
//        for(int i=0; i<listCount; i++){
//            CheckBox cb = (CheckBox) list.getChildAt(i).findViewById(R.id.list);
//            if(cb.isChecked()){
//                hosts_s.add(hosts.get(i));
//            }
//        }
//        return hosts_s;
//    }
//    
//    private void setSelectedHosts(Boolean all){
//        int listCount = list.getChildCount();
//        for(int i=0; i<listCount; i++){
//            CheckBox cb = (CheckBox) list.getChildAt(i).findViewById(R.id.list);
//            if(all){
//                cb.setChecked(true);
//            } else {
//                cb.setChecked(false);
//            }
//        }
//    }
    
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
