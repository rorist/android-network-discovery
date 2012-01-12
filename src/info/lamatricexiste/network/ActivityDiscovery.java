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
import info.lamatricexiste.network.Utils.Save;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

final public class ActivityDiscovery extends ActivityNet implements OnItemClickListener {

    private final String TAG = "ActivityDiscovery";
    public final static long VIBRATE = (long) 250;
    public final static int SCAN_PORT_RESULT = 1;
    public static final int MENU_SCAN_SINGLE = 0;
    public static final int MENU_OPTIONS = 1;
    public static final int MENU_HELP = 2;
    private static final int MENU_EXPORT = 3;
    private static LayoutInflater mInflater;
    private int currentNetwork = 0;
    private long network_ip = 0;
    private long network_start = 0;
    private long network_end = 0;
    private List<HostBean> hosts = null;
    private HostsAdapter adapter;
    private Button btn_discover;
    private AbstractDiscovery mDiscoveryTask = null;

    // private SlidingDrawer mDrawer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.discovery);
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

        // Hosts list
        adapter = new HostsAdapter(ctxt);
        ListView list = (ListView) findViewById(R.id.output);
        list.setAdapter(adapter);
        list.setItemsCanFocus(false);
        list.setOnItemClickListener(this);
        list.setEmptyView(findViewById(R.id.list_empty));

        // Drawer
        /*
         * final View info = findViewById(R.id.info_container); mDrawer =
         * (SlidingDrawer) findViewById(R.id.drawer);
         * mDrawer.setOnDrawerScrollListener(new
         * SlidingDrawer.OnDrawerScrollListener() { public void
         * onScrollStarted() {
         * info.setBackgroundResource(R.drawable.drawer_bg2); }
         * 
         * public void onScrollEnded() { } });
         * mDrawer.setOnDrawerCloseListener(new
         * SlidingDrawer.OnDrawerCloseListener() { public void onDrawerClosed()
         * { info.setBackgroundResource(R.drawable.drawer_bg); } }); EditText
         * cidr_value = (EditText) findViewById(R.id.cidr_value); ((Button)
         * findViewById(R.id.btn_cidr_plus)).setOnClickListener(new
         * View.OnClickListener() { public void onClick(View v) { } });
         * ((Button) findViewById(R.id.btn_cidr_minus)).setOnClickListener(new
         * View.OnClickListener() { public void onClick(View v) { } });
         */
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, ActivityDiscovery.MENU_SCAN_SINGLE, 0, R.string.scan_single_title).setIcon(
                android.R.drawable.ic_menu_mylocation);
        menu.add(0, ActivityDiscovery.MENU_EXPORT, 0, R.string.preferences_export).setIcon(
                android.R.drawable.ic_menu_save);
        menu.add(0, ActivityDiscovery.MENU_OPTIONS, 0, R.string.btn_options).setIcon(
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

    protected void setInfo() {
        // Info
        ((TextView) findViewById(R.id.info_ip)).setText(info_ip_str);
        ((TextView) findViewById(R.id.info_in)).setText(info_in_str);
        ((TextView) findViewById(R.id.info_mo)).setText(info_mo_str);

        // Scan button state
        if (mDiscoveryTask != null) {
            setButton(btn_discover, R.drawable.cancel, false);
            btn_discover.setText(R.string.btn_discover_cancel);
            btn_discover.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    cancelTasks();
                }
            });
        }

        if (currentNetwork != net.hashCode()) {
            Log.i(TAG, "Network info has changed");
            currentNetwork = net.hashCode();

            // Cancel running tasks
            cancelTasks();
        } else {
            return;
        }

        // Get ip information
        network_ip = NetInfo.getUnsignedLongFromIp(net.ip);
        if (prefs.getBoolean(Prefs.KEY_IP_CUSTOM, Prefs.DEFAULT_IP_CUSTOM)) {
            // Custom IP
            network_start = NetInfo.getUnsignedLongFromIp(prefs.getString(Prefs.KEY_IP_START,
                    Prefs.DEFAULT_IP_START));
            network_end = NetInfo.getUnsignedLongFromIp(prefs.getString(Prefs.KEY_IP_END,
                    Prefs.DEFAULT_IP_END));
        } else {
            // Custom CIDR
            if (prefs.getBoolean(Prefs.KEY_CIDR_CUSTOM, Prefs.DEFAULT_CIDR_CUSTOM)) {
                net.cidr = Integer.parseInt(prefs.getString(Prefs.KEY_CIDR, Prefs.DEFAULT_CIDR));
            }
            // Detected IP
            int shift = (32 - net.cidr);
            if (net.cidr < 31) {
                network_start = (network_ip >> shift << shift) + 1;
                network_end = (network_start | ((1 << shift) - 1)) - 1;
            } else {
                network_start = (network_ip >> shift << shift);
                network_end = (network_start | ((1 << shift) - 1));
            }
            // Reset ip start-end (is it really convenient ?)
            Editor edit = prefs.edit();
            edit.putString(Prefs.KEY_IP_START, NetInfo.getIpFromLongUnsigned(network_start));
            edit.putString(Prefs.KEY_IP_END, NetInfo.getIpFromLongUnsigned(network_end));
            edit.commit();
        }
    }

    protected void setButtons(boolean disable) {
        if (disable) {
            setButtonOff(btn_discover, R.drawable.disabled);
        } else {
            setButtonOn(btn_discover, R.drawable.discover);
        }
    }

    protected void cancelTasks() {
        if (mDiscoveryTask != null) {
            mDiscoveryTask.cancel(true);
            mDiscoveryTask = null;
        }
    }

    // Listen for Activity results
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SCAN_PORT_RESULT:
                if (resultCode == RESULT_OK) {
                    // Get scanned ports
                    if (data != null && data.hasExtra(HostBean.EXTRA)) {
                        HostBean host = data.getParcelableExtra(HostBean.EXTRA);
                        if (host != null) {
                            hosts.set(host.position, host);
                        }
                    }
                }
            default:
                break;
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        final HostBean host = hosts.get(position);
        AlertDialog.Builder dialog = new AlertDialog.Builder(ActivityDiscovery.this);
        dialog.setTitle(R.string.discover_action_title);
        dialog.setItems(new CharSequence[] { getString(R.string.discover_action_scan),
                getString(R.string.discover_action_rename) }, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        // Start portscan
                        Intent intent = new Intent(ctxt, ActivityPortscan.class);
                        intent.putExtra(EXTRA_WIFI, NetInfo.isConnected(ctxt));
                        intent.putExtra(HostBean.EXTRA, host);
                        startActivityForResult(intent, SCAN_PORT_RESULT);
                        break;
                    case 1:
                        // Change name
                        // FIXME: TODO

                        final View v = mInflater.inflate(R.layout.dialog_edittext, null);
                        final EditText txt = (EditText) v.findViewById(R.id.edittext);
                        final Save s = new Save();
                        txt.setText(s.getCustomName(host));

                        final AlertDialog.Builder rename = new AlertDialog.Builder(
                                ActivityDiscovery.this);
                        rename.setView(v);
                        rename.setTitle(R.string.discover_action_rename);
                        rename.setPositiveButton(R.string.btn_ok, new OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final String name = txt.getText().toString();
                                host.hostname = name;
                                s.setCustomName(name, host.hardwareAddress);
                                adapter.notifyDataSetChanged();
                                Toast.makeText(ActivityDiscovery.this,
                                        R.string.discover_action_saved, Toast.LENGTH_SHORT).show();
                            }
                        });
                        rename.setNegativeButton(R.string.btn_remove, new OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                host.hostname = null;
                                s.removeCustomName(host.hardwareAddress);
                                adapter.notifyDataSetChanged();
                                Toast.makeText(ActivityDiscovery.this,
                                        R.string.discover_action_deleted, Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });
                        rename.show();
                        break;
                }
            }
        });
        dialog.setNegativeButton(R.string.btn_discover_cancel, null);
        dialog.show();
    }

    static class ViewHolder {
        TextView host;
        TextView mac;
        TextView vendor;
        ImageView logo;
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
                holder.logo = (ImageView) convertView.findViewById(R.id.logo);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final HostBean host = hosts.get(position);
            if (host.deviceType == HostBean.TYPE_GATEWAY) {
                holder.logo.setImageResource(R.drawable.router);
            } else if (host.isAlive == 1 || !host.hardwareAddress.equals(NetInfo.NOMAC)) {
                holder.logo.setImageResource(R.drawable.computer);
            } else {
                holder.logo.setImageResource(R.drawable.computer_down);
            }
            if (host.hostname != null && !host.hostname.equals(host.ipAddress)) {
                holder.host.setText(host.hostname + " (" + host.ipAddress + ")");
            } else {
                holder.host.setText(host.ipAddress);
            }
            if (!host.hardwareAddress.equals(NetInfo.NOMAC)) {
                holder.mac.setText(host.hardwareAddress);
                if(host.nicVendor != null){
                    holder.vendor.setText(host.nicVendor);
                } else {
                    holder.vendor.setText(R.string.info_unknown);
                }
                holder.mac.setVisibility(View.VISIBLE);
                holder.vendor.setVisibility(View.VISIBLE);
            } else {
                holder.mac.setVisibility(View.GONE);
                holder.vendor.setVisibility(View.GONE);
            }
            return convertView;
        }
    }

    /**
     * Discover hosts
     */
    private void startDiscovering() {
        int method = 0;
        try {
            method = Integer.parseInt(prefs.getString(Prefs.KEY_METHOD_DISCOVER,
                    Prefs.DEFAULT_METHOD_DISCOVER));
        } catch (NumberFormatException e) {
            Log.e(TAG, e.getMessage());
        }
        switch (method) {
            case 1:
                mDiscoveryTask = new DnsDiscovery(ActivityDiscovery.this);
                break;
            case 2:
                // Root
                break;
            case 0:
            default:
                mDiscoveryTask = new DefaultDiscovery(ActivityDiscovery.this);
        }
        mDiscoveryTask.setNetwork(network_ip, network_start, network_end);
        mDiscoveryTask.execute();
        btn_discover.setText(R.string.btn_discover_cancel);
        setButton(btn_discover, R.drawable.cancel, false);
        btn_discover.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                cancelTasks();
            }
        });
        makeToast(R.string.discover_start);
        setProgressBarVisibility(true);
        setProgressBarIndeterminateVisibility(true);
        initList();
    }

    public void stopDiscovering() {
        Log.e(TAG, "stopDiscovering()");
        mDiscoveryTask = null;
        setButtonOn(btn_discover, R.drawable.discover);
        btn_discover.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startDiscovering();
            }
        });
        setProgressBarVisibility(false);
        setProgressBarIndeterminateVisibility(false);
        btn_discover.setText(R.string.btn_discover);
    }

    private void initList() {
        // setSelectedHosts(false);
        adapter.clear();
        hosts = new ArrayList<HostBean>();
    }

    public void addHost(HostBean host) {
        host.position = hosts.size();
        hosts.add(host);
        adapter.add(null);
    }

    public static void scanSingle(final Context ctxt, String ip) {
        // Alert dialog
        View v = LayoutInflater.from(ctxt).inflate(R.layout.scan_single, null);
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

        View v = mInflater.inflate(R.layout.dialog_edittext, null);
        final EditText txt = (EditText) v.findViewById(R.id.edittext);
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

    private void setButton(Button btn, int res, boolean disable) {
        if (disable) {
            setButtonOff(btn, res);
        } else {
            setButtonOn(btn, res);
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
