package info.lamatricexiste.network;

import info.lamatricexiste.network.HostDiscovery.DiscoveryUnicast;
import info.lamatricexiste.network.HostDiscovery.HostBean;
import info.lamatricexiste.network.Utils.Export;
import info.lamatricexiste.network.Utils.HardwareAddress;
import info.lamatricexiste.network.Utils.NetInfo;
import info.lamatricexiste.network.Utils.Prefs;
import info.lamatricexiste.network.Utils.UpdateNicDb;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

final public class DiscoverActivity extends Activity {

    // private final String TAG = "NetworkMain";
    // private final int DEFAULT_DISCOVER = 1;
    public final static long VIBRATE = (long) 250;
    public final static int SCAN_PORT_RESULT = 1;
    public static final int MENU_SCAN_SINGLE = 0;
    public static final int MENU_OPTIONS = 1;
    private static LayoutInflater mInflater;
    private List<HostBean> hosts = null;
    private HostsAdapter adapter;
    // private Button btn;
    private Button btn_discover;
    private Button btn_export;
    private SharedPreferences prefs = null;
    // private boolean rooted = false;
    private ConnectivityManager connMgr;
    private CheckHostsTask checkHostsTask = null;
    private Context ctxt;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
        ctxt = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
        mInflater = LayoutInflater.from(ctxt);

        // Check NIC db
        if (prefs.getString("resetdb", Prefs.DEFAULT_RESETDB) == "1") {
            UpdateNicDb.localCopy(ctxt);
            Editor edit = prefs.edit();
            edit.putString(Prefs.KEY_RESETDB, "0");
            edit.commit();
        }

        // Discover
        btn_discover = (Button) findViewById(R.id.btn_discover);
        btn_discover.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkHostsTask = new CheckHostsTask(DiscoverActivity.this);
                startDiscovering();
            }
        });

        // Export
        btn_export = (Button) findViewById(R.id.btn_export);
        btn_export.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                export();
            }
        });

        // Options
        Button btn_options = (Button) findViewById(R.id.btn_options);
        btn_options.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(ctxt, Prefs.class));
            }
        });

        // Wifi Settings
        Button btn_wifi = (Button) findViewById(R.id.btn_wifi);
        btn_wifi.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });

        // Send Request
        // btn = (Button) findViewById(R.id.btn);
        // btn.setOnClickListener(new View.OnClickListener() {
        // public void onClick(View v) {
        // sendPacket();
        // }
        // });

        // All
        // Button btn3 = (Button) findViewById(R.id.btn3);
        // btn3.setOnClickListener(new View.OnClickListener() {
        // public void onClick(View v) {
        // setSelectedHosts(true);
        // }
        // });

        // None
        // Button btn4 = (Button) findViewById(R.id.btn4);
        // btn4.setOnClickListener(new View.OnClickListener() {
        // public void onClick(View v) {
        // setSelectedHosts(false);
        // }
        // });

        // Hosts list
        adapter = new HostsAdapter(ctxt);
        ListView list = (ListView) findViewById(R.id.output);
        list.setAdapter(adapter);
        list.setItemsCanFocus(true);

        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // checkRoot();

        // Fake hosts
        // adapter.add("10.0.10.1");
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, DiscoverActivity.MENU_SCAN_SINGLE, 0, R.string.scan_single_title).setIcon(
                android.R.drawable.ic_menu_mylocation);
        menu.add(0, DiscoverActivity.MENU_OPTIONS, 0, "Options").setIcon(
                android.R.drawable.ic_menu_preferences);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DiscoverActivity.MENU_SCAN_SINGLE:
                scanSingle(this, null);
                return true;
            case DiscoverActivity.MENU_OPTIONS:
                startActivity(new Intent(ctxt, Prefs.class));
                return true;
        }
        return false;
    }

    // Sub Activity result
    // Listen for results.
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Log.v(TAG, "result=" + resultCode + "(" + RESULT_OK + ")");
        switch (requestCode) {
            case SCAN_PORT_RESULT:
                if (resultCode == RESULT_OK) {
                    // Get scannde ports
                    Bundle extra = data.getExtras();
                    int position = extra.getInt("position");
                    HostBean host = hosts.get(position);
                    host.setPortsOpen(extra.getLongArray("ports_o"));
                    host.setPortsClosed(extra.getLongArray("ports_c"));
                    // OS Fingerprint check
                    // host.setOs(OsFingerprint.finger(extra.getLongArray("ports")));
                }
            default:
                break;
        }
    }

    static class ViewHolder {
        TextView host;
        Button btn_ports;
        Button btn_info;
    }

    // Custom ArrayAdapter
    private class HostsAdapter extends ArrayAdapter<Void> {
        public HostsAdapter(Context ctxt) {
            super(ctxt, R.layout.list_host, R.id.list);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_host, null);
                holder = new ViewHolder();
                holder.host = (TextView) convertView.findViewById(R.id.list);
                holder.btn_ports = (Button) convertView.findViewById(R.id.list_port);
                holder.btn_info = (Button) convertView.findViewById(R.id.list_info);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final HostBean host = hosts.get(position);
            holder.host.setText(host.getIpAddress());
            holder.btn_ports.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(ctxt, PortScanActivity.class);
                    if (wifiConnected() == false) {
                        intent.putExtra("wifiDisabled", true);
                    }
                    intent.putExtra("position", position);
                    intent.putExtra("host", host.getIpAddress());
                    intent.putExtra("ports_o", host.getPortsOpen());
                    intent.putExtra("ports_c", host.getPortsClosed());
                    startActivityForResult(intent, SCAN_PORT_RESULT);
                }
            });
            holder.btn_info.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showHostInfo(host);
                }
            });
            return convertView;
        }
    }

    // Broadcast Receiver
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            networkStateChanged(intent);
        }
    };

    private void networkStateChanged(Intent intent) {
        // Use NetworkInfo
        TextView info_ip = (TextView) findViewById(R.id.info_ip);
        TextView info_nt = (TextView) findViewById(R.id.info_nt);
        TextView info_id = (TextView) findViewById(R.id.info_id);

        info_ip.setText("");
        info_id.setText("");
        setButtonOff(btn_discover);
        setButtonOff(btn_export);

        NetInfo net = new NetInfo(ctxt);

        // Wifi state
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int WifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
                // Log.d(TAG, "WifiState=" + WifiState);
                switch (WifiState) {
                    case WifiManager.WIFI_STATE_ENABLING:
                        info_nt.setText(R.string.wifi_enabling);
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        info_nt.setText(R.string.wifi_enabled);
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        info_nt.setText(R.string.wifi_disabling);
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        info_nt.setText(R.string.wifi_disabled);
                        break;
                    default:
                        info_nt.setText(R.string.wifi_unknown);
                }
            }

            if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
                SupplicantState sstate = net.getSupplicantState();
                // Log.d(TAG, "SupplicantState=" + sstate);
                if (sstate == SupplicantState.SCANNING) {
                    info_nt.setText(R.string.wifi_scanning);
                } else if (sstate == SupplicantState.ASSOCIATING) {
                    String bssid = net.getBSSID();
                    String ssid = net.getSSID();
                    String mac = net.getMacAddress();
                    String id = ssid != null ? ssid : (bssid != null ? bssid : mac);
                    info_nt.setText(String.format(getString(R.string.wifi_associating), id));
                } else if (sstate == SupplicantState.COMPLETED) {
                    info_nt.setText(String.format(getString(R.string.wifi_dhcp), net.getSSID()));
                }

            }
        }

        // 3G(connected) -> Wifi(connected)
        final NetworkInfo network_info = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (network_info != null) {
            NetworkInfo.State state = network_info.getState();
            // Log.d(TAG, "netinfo=" + state + " with " +
            // network_info.getType());
            // Connection check
            if (net.getSSID() != null && state == NetworkInfo.State.CONNECTED) {
                info_ip.setText("IP: " + net.getIp());
                info_nt.setText("NT: " + net.getNetIp() + "/" + net.getNetCidr());
                info_id.setText("SSID: " + net.getSSID());
                setButtonOn(btn_discover, R.drawable.discover);
                setButtonOn(btn_export, R.drawable.export);
            } else if (checkHostsTask != null) {
                cancelAllTasks();
            }
        } else if (checkHostsTask != null) {
            cancelAllTasks();
        }
    }

    private boolean wifiConnected() {
        final NetworkInfo network_info = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (network_info.getState() == NetworkInfo.State.CONNECTED) {
            return true;
        }
        return false;
    }

    /**
     * Discover hosts
     */

    private static class CheckHostsTask extends DiscoveryUnicast {
        private WeakReference<DiscoverActivity> mDiscover;
        private int hosts_done = 0;

        public CheckHostsTask(DiscoverActivity discover) {
            mDiscover = new WeakReference<DiscoverActivity>(discover);
        }

        @Override
        protected void onPreExecute() {
            final DiscoverActivity discover = mDiscover.get();
            prefsMgr = discover.prefs;
            NetInfo net = new NetInfo(discover);
            ip = NetInfo.getUnsignedLongFromIp(net.getIp());
            int shift = (32 - net.getNetCidr());
            start = (ip >> shift << shift) + 1;
            end = (start | ((1 << shift) - 1)) - 1;
            size = (int) (end - start + 1);
            discover.setProgress(0);
        }

        @Override
        protected void onProgressUpdate(String... item) {
            final DiscoverActivity discover = mDiscover.get();
            if (!isCancelled()) {
                if (item[0] != null) {
                    discover.addHost(item[0]);
                }
                hosts_done++;
                discover.setProgress(hosts_done * 10000 / size);
            }
        }

        @Override
        protected void onPostExecute(Void unused) {
            final DiscoverActivity discover = mDiscover.get();
            if (discover.prefs.getBoolean(Prefs.KEY_VIBRATE_FINISH, Prefs.DEFAULT_VIBRATE_FINISH) == true) {
                Vibrator v = (Vibrator) discover.getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(VIBRATE);
            }
            discover.makeToast(R.string.discover_finished);
            discover.stopDiscovering();
        }

        @Override
        protected void onCancelled() {
            final DiscoverActivity discover = mDiscover.get();
            pool.shutdownNow();
            discover.makeToast(R.string.discover_canceled);
            discover.stopDiscovering();
        }
    }

    private void startDiscovering() {
        makeToast(R.string.discover_start);
        setProgressBarVisibility(true);
        setProgressBarIndeterminateVisibility(true);
        initList();
        checkHostsTask.execute();
        btn_discover.setText(R.string.btn_discover_cancel);
        btn_discover.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.cancel, 0, 0);
        btn_discover.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkHostsTask.cancel(true);
            }
        });
    }

    private void stopDiscovering() {
        setProgressBarVisibility(false);
        setProgressBarIndeterminateVisibility(false);
        btn_discover.setText(R.string.btn_discover);
        btn_discover.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.discover, 0, 0);
        btn_discover.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkHostsTask = new CheckHostsTask(DiscoverActivity.this);
                startDiscovering();
            }
        });
    }

    private void cancelAllTasks() {
        if (checkHostsTask != null) {
            checkHostsTask.cancel(true);
            checkHostsTask = null;
        }
    }

    private void initList() {
        // setSelectedHosts(false);
        adapter.clear();
        hosts = new ArrayList<HostBean>();
    }

    private void addHost(String addr) {
        HardwareAddress hw = new HardwareAddress(ctxt, addr);
        String haddr = hw.getHardwareAddress();
        if (!hardwareAddressAlreadyExists(haddr)) {
            HostBean host = new HostBean();
            host.setHardwareAddress(haddr);
            host.setNicVendor(hw.getNicVendor());
            host.setIpAddress(addr);
            host.setPosition(hosts.size());
            hosts.add(host);
            adapter.add(null);
        } else {
            if (checkHostsTask != null) {
                checkHostsTask.cancel(true);
            }
            NetInfo net = new NetInfo(ctxt);
            AlertDialog.Builder infoDialog = new AlertDialog.Builder(this);
            infoDialog.setTitle(R.string.discover_proxy_title);
            infoDialog.setMessage(String.format(getString(R.string.discover_proxy_msg), net
                    .getGatewayIp()));
            infoDialog.setNegativeButton(R.string.btn_close, null);
            infoDialog.show();
        }
    }

    private boolean hardwareAddressAlreadyExists(String addr) {
        // TODO: Find a more performant method
        for (HostBean host : hosts) {
            if (host.getHardwareAddress() == addr) {
                return true;
            }
        }
        return false;
    }

    private void showHostInfo(HostBean host) {
        View v = mInflater.inflate(R.layout.info, null);
        // Build info dialog
        AlertDialog.Builder infoDialog = new AlertDialog.Builder(DiscoverActivity.this);
        infoDialog.setTitle(host.getIpAddress());
        ((TextView) v.findViewById(R.id.info_mac)).setText(host.getHardwareAddress());
        ((TextView) v.findViewById(R.id.info_nic)).setText(host.getNicVendor());
        // Show dialog
        infoDialog.setView(v);
        infoDialog.setNegativeButton(R.string.btn_close, null);
        infoDialog.show();
    }

    // private void checkRoot() {
    // // Borrowed here: http://bit.ly/754iGA
    // try {
    // File su = new File("/system/bin/su");
    // if (su.exists() == false) {
    // rooted = false;
    // }
    // } catch (Exception e) {
    // Log.d(TAG, "Can't obtain root: " + e.getMessage());
    // rooted = false;
    // }
    // }

    // private void sendPacket(){
    // CheckBox cb = (CheckBox) findViewById(R.id.repeat);
    // final boolean repeat = cb.isChecked();
    // final CharSequence[] items = {"Ping (ICMP)","Samba exploit"};
    // setButtonOff(btn);
    // @SuppressWarnings("unused")
    // AlertDialog dialog = new AlertDialog.Builder(ctxt)
    // .setTitle("Select method")
    // .setItems(items, new DialogInterface.OnClickListener() {
    // public void onClick(DialogInterface dialog, int item) {
    // try {
    // makeToast("Sending request ...");
    // netInterface.inSendPacket(getSelectedHosts(), item, repeat);
    // } catch (RemoteException e) {
    // Log.e(TAG, e.getMessage());
    // } catch (IllegalStateException e){
    // Log.e(TAG, e.getMessage());
    // }
    // }
    // })
    // .show();
    // }

    public static void scanSingle(final Context ctxt, String ip) {
        // Alert dialog
        View v = mInflater.inflate(R.layout.scan_single, null);
        final EditText txt = (EditText) v.findViewById(R.id.ip);
        if (ip != null) {
            txt.setText(ip);
        }
        AlertDialog.Builder dialogIp = new AlertDialog.Builder(ctxt);
        dialogIp.setTitle(R.string.scan_single_title);
        dialogIp.setView(v);
        dialogIp.setPositiveButton(R.string.btn_scan, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int sumthin) {
                // start scanportactivity
                Intent intent = new Intent(ctxt, PortScanActivity.class);
                intent.putExtra("host", txt.getText().toString());
                ctxt.startActivity(intent);
            }
        });
        dialogIp.setNegativeButton(R.string.btn_discover_cancel, null);
        dialogIp.show();
    }

    private void export() {
        final Export e = new Export(ctxt, hosts);
        final String file = e.getFileName();

        View v = mInflater.inflate(R.layout.file, null);
        final EditText txt = (EditText) v.findViewById(R.id.export_file);
        txt.setText(file);

        AlertDialog.Builder getFileName = new AlertDialog.Builder(this);
        getFileName.setTitle(R.string.export_choose);
        getFileName.setView(v);
        getFileName.setPositiveButton(R.string.export_save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dlg, int sumthin) {
                final String fileEdit = txt.getText().toString();
                if (e.fileExists(fileEdit)) {
                    AlertDialog.Builder fileExists = new AlertDialog.Builder(DiscoverActivity.this);
                    fileExists.setTitle(R.string.export_exists_title);
                    fileExists.setMessage(R.string.export_exists_msg);
                    fileExists.setPositiveButton(R.string.btn_yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (e.writeToSd(fileEdit)) {
                                        makeToast(R.string.export_finished);
                                    } else {
                                        export();
                                    }
                                }
                            });
                    fileExists.setNegativeButton(R.string.btn_no, null);
                    fileExists.show();
                } else {
                    if (e.writeToSd(fileEdit)) {
                        makeToast(R.string.export_finished);
                    } else {
                        export();
                    }
                }
            }
        });
        getFileName.setNegativeButton(R.string.btn_discover_cancel, null);
        getFileName.show();
    }

    // private List<String> getSelectedHosts(){
    // List<String> hosts_s = new ArrayList<String>();
    // int listCount = list.getChildCount();
    // for(int i=0; i<listCount; i++){
    // CheckBox cb = (CheckBox) list.getChildAt(i).findViewById(R.id.list);
    // if(cb.isChecked()){
    // hosts_s.add(hosts.get(i));
    // }
    // }
    // return hosts_s;
    // }
    //    
    // private void setSelectedHosts(Boolean all){
    // int listCount = list.getChildCount();
    // for(int i=0; i<listCount; i++){
    // CheckBox cb = (CheckBox) list.getChildAt(i).findViewById(R.id.list);
    // if(all){
    // cb.setChecked(true);
    // } else {
    // cb.setChecked(false);
    // }
    // }
    // }

    // private void makeToast(String msg) {
    // Toast.makeText(getApplicationContext(), (CharSequence) msg,
    // Toast.LENGTH_SHORT).show();
    // }

    private void makeToast(int msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void setButtonOff(Button b) {
        b.setClickable(false);
        b.setEnabled(false);
        b.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.disabled, 0, 0);
    }

    private void setButtonOn(Button b, int drawable) {
        b.setClickable(true);
        b.setEnabled(true);
        b.setCompoundDrawablesWithIntrinsicBounds(0, drawable, 0, 0);
    }
}
