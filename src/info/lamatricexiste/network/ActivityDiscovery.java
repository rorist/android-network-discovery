/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network;

import info.lamatricexiste.network.Network.HardwareAddress;
import info.lamatricexiste.network.Network.HostBean;
import info.lamatricexiste.network.Network.NetInfo;
import info.lamatricexiste.network.Utils.Export;
import info.lamatricexiste.network.Utils.Help;
import info.lamatricexiste.network.Utils.Prefs;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

final public class ActivityDiscovery extends Activity {

    private final String TAG = "ActivityDiscover";
    public final static long VIBRATE = (long) 250;
    public final static int SCAN_PORT_RESULT = 1;
    public static final int MENU_SCAN_SINGLE = 0;
    public static final int MENU_OPTIONS = 1;
    public static final int MENU_HELP = 2;
    private static final int MENU_EXPORT = 3;
    private static LayoutInflater mInflater;
    private List<HostBean> hosts = null;
    private HostsAdapter adapter;
    private HardwareAddress mHardwareAddress;
    private Button btn_discover;
    public SharedPreferences prefs = null;
    private ConnectivityManager connMgr;
    private AbstractDiscovery mDiscoveryTask = null;
    private RootDaemon mRootDaemon = null;
    private Context ctxt;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.discovery);
        ctxt = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
        mInflater = LayoutInflater.from(ctxt);

        // Discover
        btn_discover = (Button) findViewById(R.id.btn_discover);
        btn_discover.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startDiscovering();
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
        ImageButton btn_wifi = (ImageButton) findViewById(R.id.btn_wifi);
        btn_wifi.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });

        // Hosts list
        adapter = new HostsAdapter(ctxt);
        ListView list = (ListView) findViewById(R.id.output);
        list.setAdapter(adapter);
        list.setItemsCanFocus(true);

        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
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
    public void onStart() {
        super.onStart();
        if (prefs.getString(Prefs.KEY_METHOD_DISCOVER, Prefs.DEFAULT_METHOD_DISCOVER) == "1") {
            mRootDaemon = new RootDaemon(ActivityDiscovery.this);
            mRootDaemon.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (prefs.getString(Prefs.KEY_METHOD_DISCOVER, Prefs.DEFAULT_METHOD_DISCOVER) == "1") {
            if (mRootDaemon != null) {
                mRootDaemon.kill();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, ActivityDiscovery.MENU_SCAN_SINGLE, 0, R.string.scan_single_title).setIcon(
                android.R.drawable.ic_menu_mylocation);
        menu.add(0, ActivityDiscovery.MENU_EXPORT, 0, R.string.preferences_export).setIcon(
                android.R.drawable.ic_menu_save);
        menu.add(0, ActivityDiscovery.MENU_OPTIONS, 0, "Options").setIcon(
                android.R.drawable.ic_menu_preferences);
        menu.add(0, ActivityDiscovery.MENU_HELP, 0, R.string.preferences_help).setIcon(
                android.R.drawable.ic_menu_help);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case ActivityDiscovery.MENU_SCAN_SINGLE:
                scanSingle(this, null);
                return true;
            case ActivityDiscovery.MENU_OPTIONS:
                startActivity(new Intent(ctxt, Prefs.class));
                return true;
            case ActivityDiscovery.MENU_HELP:
                startActivity(new Intent(ctxt, Help.class));
                return true;
            case ActivityDiscovery.MENU_EXPORT:
                export();
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
                    // Get scanned ports
                    Bundle extra = data.getExtras();
                    int position = extra.getInt(HostBean.EXTRA_POSITION);
                    HostBean host = hosts.get(position);
                    host.banners = extra.getStringArray(HostBean.EXTRA_BANNERS);
                    host.portsOpen = extra.getIntArray(HostBean.EXTRA_PORTSO);
                    host.portsClosed = extra.getIntArray(HostBean.EXTRA_PORTSC);
                    // OS Fingerprint check
                    // host.setOs(OsFingerprint.finger(extra.getLongArray(HostBean.EXTRA_)));
                }
            default:
                break;
        }
    }

    static class ViewHolder {
        TextView host;
        TextView mac;
        TextView vendor;
        ImageButton btn_ports;
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
                holder.mac = (TextView) convertView.findViewById(R.id.mac);
                holder.vendor = (TextView) convertView.findViewById(R.id.vendor);
                holder.btn_ports = (ImageButton) convertView.findViewById(R.id.list_port);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final HostBean host = hosts.get(position);
            if (prefs.getBoolean(Prefs.KEY_RESOLVE_NAME, Prefs.DEFAULT_RESOLVE_NAME) == true
                    && host.hostname != null) {
                holder.host.setText(host.hostname);
            } else {
                holder.host.setText(host.ipAddress);
            }
            holder.mac.setText(host.hardwareAddress);
            holder.vendor.setText(host.nicVendor);
            holder.btn_ports.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    startPortscan(host, position);
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
        TextView info_in = (TextView) findViewById(R.id.info_in);
        TextView info_mo = (TextView) findViewById(R.id.info_mo);

        info_ip.setText("");
        info_mo.setText("");
        setButtonOff(btn_discover, R.drawable.disabled);

        NetInfo net = new NetInfo(ctxt);

        // Wifi state
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int WifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
                // Log.d(TAG, "WifiState=" + WifiState);
                switch (WifiState) {
                    case WifiManager.WIFI_STATE_ENABLING:
                        info_in.setText(R.string.wifi_enabling);
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        info_in.setText(R.string.wifi_enabled);
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        info_in.setText(R.string.wifi_disabling);
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        info_in.setText(R.string.wifi_disabled);
                        break;
                    default:
                        info_in.setText(R.string.wifi_unknown);
                }
            }

            if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION) && net.getWifiInfo()) {
                SupplicantState sstate = net.getSupplicantState();
                // Log.d(TAG, "SupplicantState=" + sstate);
                if (sstate == SupplicantState.SCANNING) {
                    info_in.setText(R.string.wifi_scanning);
                } else if (sstate == SupplicantState.ASSOCIATING) {
                    info_in.setText(String.format(getString(R.string.wifi_associating),
                            (net.ssid != null ? net.ssid : (net.bssid != null ? net.bssid
                                    : net.macAddress))));
                } else if (sstate == SupplicantState.COMPLETED) {
                    info_in.setText(String.format(getString(R.string.wifi_dhcp), net.ssid));
                }
            }
        }

        // 3G(connected) -> Wifi(connected)
        // Support Ethernet, with ConnectivityManager.TYPE_ETHER=3
        final NetworkInfo ni = connMgr.getActiveNetworkInfo();
        if (ni != null) {
            if (ni.getState() == NetworkInfo.State.CONNECTED) {
                int type = ni.getType();
                if (type == ConnectivityManager.TYPE_WIFI) { // WIFI
                    net.getWifiInfo();
                    if (net.ssid != null) {
                        info_mo.setText("MODE: WiFi");
                        info_ip.setText("IP: " + net.ip + "/" + net.cidr);
                        info_in.setText("SSID: " + net.ssid);
                        setButtonOn(btn_discover, R.drawable.discover);
                    }
                } else if (type == ConnectivityManager.TYPE_MOBILE) { // 3G
                    net.getMobileInfo();
                    if (net.carrier != null) {
                        info_mo.setText("MODE: Mobile");
                        info_ip.setText("IP: " + net.ip + "/" + net.cidr);
                        info_in.setText("CARRIER: " + net.carrier);
                        setButtonOn(btn_discover, R.drawable.discover);
                    }
                } else if (type == 3) { // ETH
                    Log.i(TAG, "Ethernet connectivity detected!");
                    info_mo.setText("MODE: Ethernet");
                }
            } else if (mDiscoveryTask != null) {
                cancelTasks();
            }
        } else if (mDiscoveryTask != null) {
            cancelTasks();
        }
    }

    /**
     * Discover hosts
     */
    private void startDiscovering() {
        Log.v(TAG, "METHOD="
                + prefs.getString(Prefs.KEY_METHOD_DISCOVER, Prefs.DEFAULT_METHOD_DISCOVER));
        if (prefs.getString(Prefs.KEY_METHOD_DISCOVER, Prefs.DEFAULT_METHOD_DISCOVER) == "0") {
            mDiscoveryTask = new DefaultDiscovery(ActivityDiscovery.this);
        } else if (prefs.getString(Prefs.KEY_METHOD_DISCOVER, Prefs.DEFAULT_METHOD_DISCOVER) == "1") {
            mDiscoveryTask = new RootDiscovery(ActivityDiscovery.this);
        }
        mHardwareAddress = new HardwareAddress();
        makeToast(R.string.discover_start);
        setProgressBarVisibility(true);
        setProgressBarIndeterminateVisibility(true);
        initList();
        mDiscoveryTask.execute();
        btn_discover.setText(R.string.btn_discover_cancel);
        setButtonOff(btn_discover, R.drawable.cancel, false);
        btn_discover.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cancelTasks();
            }
        });
    }

    public void stopDiscovering() {
        mHardwareAddress.dbClose();
        mDiscoveryTask = null;
        setProgressBarVisibility(false);
        setProgressBarIndeterminateVisibility(false);
        btn_discover.setText(R.string.btn_discover);
        setButtonOn(btn_discover, R.drawable.discover);
        btn_discover.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startDiscovering();
            }
        });
    }

    private void cancelTasks() {
        if (mDiscoveryTask != null) {
            mDiscoveryTask.cancel(true);
            mDiscoveryTask = null;
        }
    }

    private void initList() {
        // setSelectedHosts(false);
        adapter.clear();
        hosts = new ArrayList<HostBean>();
    }

    public void addHost(String addr, long timeout) {
        String haddr = mHardwareAddress.getHardwareAddress(addr);
        if (!hardwareAddressAlreadyExists(haddr)) {
            HostBean host = new HostBean();
            host.hardwareAddress = haddr;
            try {
                host.nicVendor = mHardwareAddress.getNicVendor(ctxt, haddr);
            } catch(SQLiteDatabaseCorruptException e) {
                Log.e(TAG, e.getMessage());
                Editor edit = prefs.edit();
                edit.putInt(Prefs.KEY_RESET_NICDB, Prefs.DEFAULT_RESET_NICDB);
                edit.commit();
            }
            host.ipAddress = addr;
            host.position = hosts.size();
            host.responseTime = timeout;
            if (prefs.getBoolean(Prefs.KEY_RESOLVE_NAME, Prefs.DEFAULT_RESOLVE_NAME) == true) {
                try {
                    host.hostname = (InetAddress.getByName(addr)).getCanonicalHostName();
                } catch (UnknownHostException e) {
                    return;
                }
            }
            hosts.add(host);
            adapter.add(null);
        } else {
            if (mDiscoveryTask != null) {
                cancelTasks();
            }
            NetInfo net = new NetInfo(ctxt);
            AlertDialog.Builder infoDialog = new AlertDialog.Builder(this);
            infoDialog.setTitle(R.string.discover_proxy_title);
            infoDialog.setMessage(String.format(getString(R.string.discover_proxy_msg),
                    net.gatewayIp));
            infoDialog.setNegativeButton(R.string.btn_close, null);
            infoDialog.show();
        }
    }

    private boolean hardwareAddressAlreadyExists(String addr) {
        // TODO: Find a more performant method
        for (HostBean host : hosts) {
            if (host.hardwareAddress == addr) {
                return true;
            }
        }
        return false;
    }

    private void startPortscan(HostBean host, int position) {
        Intent intent = new Intent(ctxt, ActivityPortscan.class);
        if (NetInfo.isConnected(ctxt) == false) {
            intent.putExtra("wifiDisabled", true);
        }
        intent.putExtra(HostBean.EXTRA_TIMEOUT, (int) host.responseTime);
        intent.putExtra(HostBean.EXTRA_POSITION, position);
        intent.putExtra(HostBean.EXTRA_HOST, host.ipAddress);
        intent.putExtra(HostBean.EXTRA_HOSTNAME, host.hostname);
        intent.putExtra(HostBean.EXTRA_BANNERS, host.banners);
        intent.putExtra(HostBean.EXTRA_PORTSO, host.portsOpen);
        intent.putExtra(HostBean.EXTRA_PORTSC, host.portsClosed);
        startActivityForResult(intent, SCAN_PORT_RESULT);
    }

    // private void showHostInfo(HostBean host) {
    // View v = mInflater.inflate(R.layout.info, null);
    // // Build info dialog
    // AlertDialog.Builder infoDialog = new
    // AlertDialog.Builder(ActivityDiscovery.this);
    // infoDialog.setTitle(host.ipAddress);
    // // Add all available infos
    // LinearLayout root = (LinearLayout) v.findViewById(R.id.info);
    // root.addView(createHostInfoLine(R.string.info_mac,
    // host.hardwareAddress));
    // root.addView(createHostInfoLine(R.string.info_nic, host.nicVendor));
    // if (host.portsOpen != null) {
    // root.addView(createHostInfoLine(R.string.info_ports_open, String
    // .valueOf(host.portsOpen.length)));
    // }
    // if (host.portsClosed != null) {
    // root.addView(createHostInfoLine(R.string.info_ports_closed, String
    // .valueOf(host.portsClosed.length)));
    // }
    // // Show dialog
    // infoDialog.setView(v);
    // infoDialog.setNegativeButton(R.string.btn_close, null);
    // infoDialog.show();
    // }

    // private LinearLayout createHostInfoLine(int title, String value) {
    // LinearLayout line = (LinearLayout) mInflater.inflate(R.layout.info_line,
    // null);
    // ((TextView) line.findViewById(R.id.info_title)).setText(title);
    // ((TextView) line.findViewById(R.id.info_value)).setText(value);
    // return line;
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
                Intent intent = new Intent(ctxt, ActivityPortscan.class);
                intent.putExtra(HostBean.EXTRA_HOST, txt.getText().toString());
                try {
                    intent.putExtra(HostBean.EXTRA_HOSTNAME, (InetAddress.getByName(txt.getText()
                            .toString()).getHostName()));
                } catch (UnknownHostException e) {
                    intent.putExtra(HostBean.EXTRA_HOSTNAME, txt.getText().toString());
                }
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
                    AlertDialog.Builder fileExists = new AlertDialog.Builder(ActivityDiscovery.this);
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

    public void makeToast(int msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void setButtonOff(Button b, int drawable, boolean disable) {
        if (disable) {
            setButtonOff(b, drawable);
        } else {
            b.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0);
        }
    }

    private void setButtonOff(Button b, int drawable) {
        b.setClickable(false);
        b.setEnabled(false);
        b.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0);
    }

    private void setButtonOn(Button b, int drawable) {
        b.setClickable(true);
        b.setEnabled(true);
        b.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0);
    }
}
