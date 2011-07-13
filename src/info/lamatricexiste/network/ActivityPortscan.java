/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network;

import info.lamatricexiste.network.Network.HostBean;
import info.lamatricexiste.network.Network.NetInfo;
import info.lamatricexiste.network.Utils.Db;
import info.lamatricexiste.network.Utils.Help;
import info.lamatricexiste.network.Utils.Prefs;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

final public class ActivityPortscan extends TabActivity {

    private static final String REQ_SERVICE = "select service from services where port=? limit 1";
    private static final String REQ_PROBES = "select service, regex from probes";
    private static final int PROGRESS_MAX = 10000;
    private final String TAG = "ActivityPortscan";
    private final String PLACEHOLDER = "placeholder";
    private Context ctxt;
    private SharedPreferences prefs;
    private LayoutInflater mInflater;
    private ScanPortTask scanPortTask;
    private HostBean host;
    private PortsAdapter adapter_open;
    private PortsAdapter adapter_closed;
    private int cnt_open;
    private int cnt_closed;
    private List<String> knownServices;
    private TextView mTabOpen;
    private TextView mTabClosed;
    private Button btn_scan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.portscan);
        ctxt = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
        mInflater = LayoutInflater.from(ctxt);

        // Get Intent information
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (intent.hasExtra(HostBean.EXTRA)) {
                host = intent.getParcelableExtra(HostBean.EXTRA);
            } else {
                // Intents for 3rd party usage
                host = new HostBean();
                host.ipAddress = extras.getString(HostBean.EXTRA_HOST);
                host.hostname = extras.getString(HostBean.EXTRA_HOSTNAME);
                host.position = extras.getInt(HostBean.EXTRA_POSITION);
                host.portsOpen = intArrayToArrayList(extras.getIntArray(HostBean.EXTRA_PORTSO));
                host.portsClosed = intArrayToArrayList(extras.getIntArray(HostBean.EXTRA_PORTSC));
                host.responseTime = extras.getInt(HostBean.EXTRA_TIMEOUT, Integer
                        .parseInt(Prefs.DEFAULT_TIMEOUT_PORTSCAN));
                // FIXME: banners and services not supported (HashMap's)
            }
        }
        cnt_open = (host.portsOpen == null) ? 0 : host.portsOpen.size();
        cnt_closed = (host.portsClosed == null) ? 0 : host.portsClosed.size();

        // Logo
        // TODO: Support more types (based on what?)
        ImageView logo = (ImageView) findViewById(R.id.logo);
        if (host.deviceType == HostBean.TYPE_GATEWAY) {
            logo.setImageResource(R.drawable.router);
        } else {
            logo.setImageResource(R.drawable.computer);
        }

        // Title
        ((TextView) findViewById(R.id.host)).setSelected(true); // for marquee
        if (host.hostname != null && !host.hostname.equals(host.ipAddress)) {
            ((TextView) findViewById(R.id.host)).setText(host.hostname + " (" + host.ipAddress
                    + ")");
        } else {
            ((TextView) findViewById(R.id.host)).setText(host.ipAddress);
        }

        // MAC Address
        TextView hw = (TextView) findViewById(R.id.mac);
        if (host.hardwareAddress != null && !host.hardwareAddress.equals(NetInfo.NOMAC)) {
            hw.setText(host.hardwareAddress);
        } else {
            hw.setVisibility(View.GONE);
        }

        // Vendor name
        TextView nic = (TextView) findViewById(R.id.vendor);
        if (host.nicVendor != null && !host.hardwareAddress.equals(NetInfo.NOMAC)) {
            nic.setText(host.nicVendor);
        } else {
            nic.setVisibility(View.GONE);
        }

        // Scan
        btn_scan = (Button) findViewById(R.id.btn_scan);
        if (extras.getBoolean(ActivityNet.EXTRA_WIFI) == false) {
            btn_scan.setClickable(false);
            btn_scan.setEnabled(false);
            btn_scan.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.disabled, 0, 0);
        } else {
            btn_scan.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    startScan();
                }
            });
        }

        // Back
        ((Button) findViewById(R.id.btn_back)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        // Tabs
        TabHost tabHost = getTabHost();
        tabHost.addTab(tabHost.newTabSpec("tab_open").setIndicator(
                getString(R.string.scan_open, cnt_open),
                getResources().getDrawable(R.drawable.open)).setContent(R.id.list_open));
        tabHost.addTab(tabHost.newTabSpec("tab_closed").setIndicator(
                getString(R.string.scan_closed, cnt_closed),
                getResources().getDrawable(R.drawable.closed)).setContent(R.id.list_closed));
        tabHost.setCurrentTab(0);
        // Ugly hack to have the view holding the tabs
        mTabOpen = (TextView) tabHost.getTabWidget().getChildAt(0).findViewById(android.R.id.title);
        mTabClosed = (TextView) tabHost.getTabWidget().getChildAt(1).findViewById(
                android.R.id.title);

        // Lists
        adapter_open = new PortsAdapter(ctxt, preparePort(host.portsOpen), "open");
        ListView list_open = (ListView) findViewById(R.id.list_open);
        list_open.setAdapter(adapter_open);
        list_open.setItemsCanFocus(true);

        adapter_closed = new PortsAdapter(ctxt, preparePort(host.portsClosed), "closed");
        ListView list_closed = (ListView) findViewById(R.id.list_closed);
        list_closed.setAdapter(adapter_closed);
        list_closed.setItemsCanFocus(true);

        // TODO: Get from resource array ?
        knownServices = new ArrayList<String>();
        // knownServices.add("sftp");
        // knownServices.add("ftps");
        // knownServices.add("ftp");
        knownServices.add("ssh");
        knownServices.add("telnet");
        knownServices.add("http");
        knownServices.add("https");

        // Start scan if ports empty
        if (host.portsOpen == null && host.portsClosed == null) {
            startScan();
        }
    }

    @Override
    protected void onStop() {
        if (scanPortTask != null) {
            scanPortTask.cancel(true);
        }
        super.onStop();
    }

    // TODO: Handle orientation change without canceling the scan
    // @Override
    // public void onConfigurationChanged(Configuration newConfig) {
    // super.onConfigurationChanged(newConfig);
    // if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
    // setContentView(R.layout.portscan_portrait);
    // } else {
    // setContentView(R.layout.portscan);
    // }
    // }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, ActivityDiscovery.MENU_SCAN_SINGLE, 0, R.string.scan_single_title).setIcon(
                android.R.drawable.ic_menu_mylocation);
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
                ActivityDiscovery.scanSingle(this, host.ipAddress);
                return true;
            case ActivityDiscovery.MENU_OPTIONS:
                startActivity(new Intent(ctxt, Prefs.class));
                return true;
            case ActivityDiscovery.MENU_HELP:
                startActivity(new Intent(ctxt, Help.class));
                return true;
        }
        return false;
    }

    static class ViewHolder {
        TextView port;
        TextView banner;
        Button btn_c;
    }

    // Custom ArrayAdapter
    private class PortsAdapter extends ArrayAdapter<String> {
        private String type;

        public PortsAdapter(Context context, List<String> objects, String type) {
            super(context, R.layout.list_port, R.id.list, objects);
            this.type = type;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            // Views
            final ViewHolder holder; // WTF
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_port, null);
                holder = new ViewHolder();
                holder.port = (TextView) convertView.findViewById(R.id.list);
                holder.banner = (TextView) convertView.findViewById(R.id.banner);
                holder.btn_c = (Button) convertView.findViewById(R.id.list_connect);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            // Port & Service
            final int port = (type == "open") ? host.portsOpen.get(position) : host.portsClosed
                    .get(position);
            if (host.services != null) {
                final String service = host.services.get(port);
                holder.port.setText(port + "/tcp" + " (" + service + ")");

                // Service is supported
                if (knownServices.contains(service)) {
                    holder.btn_c.setText(R.string.scan_connect);
                    holder.btn_c.setCompoundDrawablesWithIntrinsicBounds(R.drawable.connect, 0, 0,
                            0);
                    holder.btn_c.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            openPortService(service, port);
                        }
                    });
                } else {
                    // No action for this service
                    holder.btn_c.setText(null);
                    holder.btn_c.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    holder.btn_c.setOnClickListener(null);
                }
            } else {
                holder.port.setText(port + "/tcp ");
            }
            // Banner
            if (host.banners != null && host.banners.get(port) != null) {
                holder.banner.setText(host.banners.get(port));
                holder.banner.setVisibility(View.VISIBLE);
                holder.banner.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        final AlertDialog.Builder dialog = new AlertDialog.Builder(
                                ActivityPortscan.this);
                        dialog.setTitle(getString(R.string.scan_banner_title, port));
                        dialog.setMessage(holder.banner.getText());
                        dialog.setNegativeButton(R.string.btn_close, null);
                        dialog.show();
                    }
                });
            } else {
                holder.banner.setVisibility(View.GONE);
            }
            return convertView;
        }
    }

    private void openPortService(String service, int port) {
        // Action for the service
        String pk = "";
        String search = null;
        Intent intent = null;
        // if (service.equals("ftp") || service.equals("ftps") ||
        // service.equals("sftp")) {
        // pk = "AndFTP";
        // search = "market://search?q=andftp";
        // intent = new Intent(Intent.ACTION_PICK);
        // // intent.setData(service.equals("ftp") ? Uri.parse("ftp://" +
        // // host.ipAddress) : Uri
        // // .parse("sftp://" + host.ipAddress));
        // intent.setDataAndType(service.equals("ftp") ? Uri.parse("ftp://" +
        // host.ipAddress)
        // : Uri.parse("sftp://" + host.ipAddress),
        // "vnd.android.cursor.dir/lysesoft.andftp.uri");
        // intent.putExtra("ftp_pasv", "true");
        // // intent.putExtra("ftp_username", "anonymous");
        // // intent.putExtra("ftp_password", "anonymous");
        // // intent.putExtra("ftp_keyfile", "/sdcard/dsakey.txt");
        // // intent.putExtra("ftp_keypass", "");
        // // intent.putExtra("ftp_resume", "true");
        // // intent.putExtra("ftp_encoding", "UTF8");
        // } else
        if (service.equals("ssh")) {
            pk = "ConnectBot (ssh)";
            search = "market://search?q=pname:org.connectbot";
            String user = prefs.getString(Prefs.KEY_SSH_USER, Prefs.DEFAULT_SSH_USER);
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("ssh://" + user + "@" + host.ipAddress + ":" + port + "/#"
                    + user + "@" + host.ipAddress + ":" + port));
        } else if (service.equals("telnet")) {
            pk = "ConnectBot (telnet)";
            search = "market://search?q=pname:org.connectbot";
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("telnet://" + host.ipAddress + ":" + port));
        } else if (service.equals("http") || service.equals("https")) {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(service + "://"
                    + (host.hostname != null ? host.hostname : host.ipAddress) + ":" + port));
        } else {
            makeToast(R.string.scan_noaction);
        }

        if (intent != null) {
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                if (search != null) {
                    makeToast(getString(R.string.package_missing, pk));
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(search)));
                    } catch (ActivityNotFoundException e2) {
                        Log.e(TAG, "Market not found !");
                    }
                }
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private class ScanPortTask extends AsyncPortscan {
        // FIXME: Create AbstractPortscan class
        private int progress_current = 0;
        private SQLiteDatabase dbServices;
        private SQLiteDatabase dbProbes;
        private String service;
        private Cursor c;

        ScanPortTask(Activity activity, String ip, int timeout) {
            super(activity, ip, timeout);
            WeakReference<Activity> a = new WeakReference<Activity>(activity);
            final Activity d = a.get();
            if (d != null) {
                Db db = new Db(d.getApplicationContext());
                dbServices = db.openDb(Db.DB_SERVICES);
                dbProbes = db.openDb(Db.DB_PROBES);
            }
        }

        @Override
        protected void onPreExecute() {
            // Get start/end ports
            try {
                port_start = Integer.parseInt(prefs.getString(Prefs.KEY_PORT_START,
                        Prefs.DEFAULT_PORT_START));
                port_end = Integer.parseInt(prefs.getString(Prefs.KEY_PORT_END,
                        Prefs.DEFAULT_PORT_END));
            } catch (NumberFormatException e) {
                port_start = Integer.parseInt(Prefs.DEFAULT_PORT_START);
                port_end = Integer.parseInt(Prefs.DEFAULT_PORT_END);
            }
            nb_port = port_end - port_start + 2;
            // Initialize arrays and views
            host.banners = new HashMap<Integer, String>();
            host.services = new HashMap<Integer, String>();
            host.portsOpen = new ArrayList<Integer>();
            host.portsClosed = new ArrayList<Integer>();
            mTabOpen.setText(getString(R.string.scan_open, 0));
            mTabClosed.setText(getString(R.string.scan_closed, 0));
            setProgress(0);
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            if (!isCancelled()) {
                if (values.length == 3) {
                    final Integer port = (Integer) values[0];
                    final int type = (Integer) values[1];
                    if (!port.equals(new Integer(0))) {
                        if (type == AsyncPortscan.OPEN) {
                            // Open
                            if (values[2] != null) {
                                host.banners.put(port, (String) values[2]);
                                host.services.put(port, getPortService(port));
                            }
                            if (findLocationAndAdd(host.portsOpen, port)) {
                                host.services.put(port, getPortService(port));
                                adapter_open.add(PLACEHOLDER);
                                cnt_open++;
                                mTabOpen.setText(getString(R.string.scan_open, cnt_open));
                            }
                            adapter_open.notifyDataSetChanged();
                        } else if (type == AsyncPortscan.CLOSED) {
                            // Closed
                            if (findLocationAndAdd(host.portsClosed, port)) {
                                host.services.put(port, getPortService(port));
                                adapter_closed.add(PLACEHOLDER);
                                cnt_closed++;
                                mTabClosed.setText(getString(R.string.scan_closed, cnt_closed));
                            }
                            adapter_closed.notifyDataSetChanged();
                        } else if (type == AsyncPortscan.UNREACHABLE) {
                            cancel(true);
                            makeToast(R.string.scan_host_unreachable);
                            Log.e(TAG, "Host Unreachable: " + ipAddr + ":" + port);
                        }
                        // FIXME: do something ?
                        else if (type == AsyncPortscan.TIMEOUT) {
                        } else if (type == AsyncPortscan.FILTERED) {
                        }
                    } else {
                        cancel(true);
                        makeToast(R.string.scan_host_unreachable);
                        Log.e(TAG, "Host Unreachable: " + ipAddr);
                    }
                }
                progress_current++;
                setProgress(progress_current * PROGRESS_MAX / nb_port);
            }
        }

        @Override
        protected void onPostExecute(Void unused) {
            // Finishing
            if (prefs.getBoolean(Prefs.KEY_VIBRATE_FINISH, Prefs.DEFAULT_VIBRATE_FINISH) == true) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(ActivityDiscovery.VIBRATE);
            }
            if (host.portsOpen.size() == 0) {
                makeToast(R.string.scan_noport);
            }
            if (dbServices != null) {
                dbServices.close();
            }
            if (dbProbes != null) {
                dbProbes.close();
            }
            stopScan();
            makeToast(R.string.scan_finished);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            makeToast(R.string.scan_canceled);
            if (dbServices != null) {
                dbServices.close();
            }
            if (dbProbes != null) {
                dbProbes.close();
            }
            stopScan();
        }

        private String getPortService(int port) {
            service = null;
            // Determinate service with banners
            if (host.banners != null && host.banners.containsKey(port) && dbProbes != null) {
                try {
                    Cursor c = dbProbes.rawQuery(REQ_PROBES, null);
                    if (c.moveToFirst()) {
                        do {
                            try {
                                final Pattern pattern = Pattern.compile(c.getString(1));
                                final Matcher matcher = pattern.matcher(host.banners.get(port));
                                if (matcher.find()) {
                                    service = c.getString(0);
                                    break;
                                }
                            } catch (PatternSyntaxException e) {
                                // Log.e(TAG, e.getMessage());
                            }
                        } while (c.moveToNext());
                    }
                    c.close();
                } catch (SQLiteException e) {
                    Log.e(TAG, e.getMessage());
                    Editor edit = PreferenceManager.getDefaultSharedPreferences(ctxt).edit();
                    edit.putInt(Prefs.KEY_RESET_SERVICESDB, 1);
                    edit.commit();
                }
            }

            // Get the service from port number
            if (service == null && dbServices != null) {
                c = dbServices.rawQuery(REQ_SERVICE, new String[] { "" + port });
                if (c.moveToFirst()) {
                    service = c.getString(0);
                } else {
                    service = getString(R.string.info_unknown);
                }
                c.close();
            }

            return service;
        }
    }

    private boolean findLocationAndAdd(ArrayList<Integer> array, int value) {
        int index = 0;
        int size = array.size();
        for (index = 0; index < size; index++) {
            int current = array.get(index);
            if (value > current) {
                continue;
            } else if (value < current) {
                // Add new value
                array.add(index, value);
                return true;
            } else if (value == current) {
                // Value already exists
                return false;
            }
        }
        if (index == size) {
            array.add(value);
            return true;
        }
        return false;
    }

    private void startScan() {
        if (!NetInfo.isConnected(ctxt)) {
            return;
        }
        makeToast(R.string.scan_start);
        setProgressBarVisibility(true);
        setProgressBarIndeterminateVisibility(true);
        adapter_open.clear();
        adapter_closed.clear();
        cnt_open = 0;
        cnt_closed = 0;
        scanPortTask = new ScanPortTask(this, host.ipAddress, getTimeout());
        scanPortTask.execute();
        btn_scan.setText(R.string.btn_discover_cancel);
        btn_scan.setCompoundDrawablesWithIntrinsicBounds(R.drawable.cancel, 0, 0, 0);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scanPortTask.cancel(true);
            }
        });
    }

    private void stopScan() {
        // Set result
        Intent intent = new Intent();
        intent.putExtra(HostBean.EXTRA, host);
        setResult(RESULT_OK, intent);
        // Reset scan
        setProgressBarVisibility(false);
        setProgressBarIndeterminateVisibility(false);
        setProgress(PROGRESS_MAX);
        btn_scan.setText(R.string.btn_scan);
        btn_scan.setCompoundDrawablesWithIntrinsicBounds(R.drawable.discover, 0, 0, 0);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScan();
            }
        });
    }

    private int getTimeout() {
        if (prefs.getBoolean(Prefs.KEY_TIMEOUT_FORCE, Prefs.DEFAULT_TIMEOUT_FORCE)) {
            return Integer.parseInt(prefs.getString(Prefs.KEY_TIMEOUT_PORTSCAN,
                    Prefs.DEFAULT_TIMEOUT_PORTSCAN));
        }
        return host.responseTime;
    }

    private List<String> preparePort(ArrayList<Integer> ports) {
        List<String> portsChar = new ArrayList<String>();
        if (ports != null) {
            int len = ports.size();
            for (int i = 0; i < len; i++) {
                portsChar.add(PLACEHOLDER);
            }
        }
        return portsChar;
    }

    private ArrayList<Integer> intArrayToArrayList(int[] intArray) {
        ArrayList<Integer> out = new ArrayList<Integer>();
        if (intArray != null) {
            for (int i = 0; i < intArray.length; i++) {
                out.add(intArray[i]);
            }
            return out;
        }
        return null;
    }

    // private boolean isPackageInstalled(Context context, String p) {
    // PackageManager packageManager = context.getPackageManager();
    // try {
    // packageManager.getPackageInfo(p, 0);
    // } catch (NameNotFoundException e) {
    // return false;
    // }
    // return true;
    // }

    private void makeToast(String msg) {
        Toast.makeText(getApplicationContext(), (CharSequence) msg, Toast.LENGTH_SHORT).show();
    }

    private void makeToast(int msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
