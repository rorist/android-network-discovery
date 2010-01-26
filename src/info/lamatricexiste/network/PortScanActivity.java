// TODO: Detect wifi status
package info.lamatricexiste.network;

import info.lamatricexiste.network.HostDiscovery.HostBean;
import info.lamatricexiste.network.HostDiscovery.PortScan;
import info.lamatricexiste.network.Utils.Help;
import info.lamatricexiste.network.Utils.Prefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

final public class PortScanActivity extends TabActivity {

    // private final String TAG = "PortScanActivity";
    private SharedPreferences prefs;
    private ScanPortTask scanPortTask;
    private String host;
    private int position;
    private PortsAdapter adapter_open;
    private PortsAdapter adapter_closed;
    private ArrayList<Long> ports_open = null;
    private ArrayList<Long> ports_closed = null;
    private int cnt_open;
    private int cnt_closed;
    private Button btn_scan;
    private LayoutInflater mInflater;
    private Context ctxt;
    private TextView mTabOpen;
    private TextView mTabClosed;
    private List<Long> openPortsConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.portscan);
        ctxt = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
        mInflater = LayoutInflater.from(ctxt);

        Bundle extra = getIntent().getExtras();
        host = extra.getString(HostBean.EXTRA_HOST);
        position = extra.getInt(HostBean.EXTRA_POSITION);
        ports_open = portsToArrayList(extra.getLongArray(HostBean.EXTRA_PORTSO));
        ports_closed = portsToArrayList(extra.getLongArray(HostBean.EXTRA_PORTSC));
        cnt_open = (ports_open == null) ? 0 : ports_open.size();
        cnt_closed = (ports_closed == null) ? 0 : ports_closed.size();

        // Adapt title size
        // DisplayMetrics metrics = new DisplayMetrics();
        // getWindowManager().getDefaultDisplay().getMetrics(metrics);
        // ((TextView) findViewById(R.id.host))
        // .setWidth((int) (metrics.widthPixels - (2 * 104 * (metrics.density /
        // 160))));

        // Title
        if (prefs.getBoolean(Prefs.KEY_RESOLVE_NAME, Prefs.DEFAULT_RESOLVE_NAME) == true) {
            ((TextView) findViewById(R.id.host)).setText(extra.getString(HostBean.EXTRA_HOSTNAME));
        } else {
            ((TextView) findViewById(R.id.host)).setText(host);
        }

        // Scan
        btn_scan = (Button) findViewById(R.id.btn_scan);
        if (extra.getBoolean("wifiDisabled") == true) {
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
                String.format(getString(R.string.scan_open), cnt_open),
                getResources().getDrawable(R.drawable.open)).setContent(R.id.list_open));
        tabHost.addTab(tabHost.newTabSpec("tab_closed").setIndicator(
                String.format(getString(R.string.scan_closed), cnt_closed),
                getResources().getDrawable(R.drawable.closed)).setContent(R.id.list_closed));
        tabHost.setCurrentTab(0);
        // Ugly hack to have the view holding the tabs
        mTabOpen = (TextView) tabHost.getTabWidget().getChildAt(0).findViewById(android.R.id.title);
        mTabClosed = (TextView) tabHost.getTabWidget().getChildAt(1).findViewById(
                android.R.id.title);

        // Lists
        adapter_open = new PortsAdapter(ctxt, preparePort(ports_open, "open"), "open");
        ListView list_open = (ListView) findViewById(R.id.list_open);
        list_open.setAdapter(adapter_open);
        list_open.setItemsCanFocus(true);

        adapter_closed = new PortsAdapter(ctxt, preparePort(ports_closed, "closed"), "closed");
        ListView list_closed = (ListView) findViewById(R.id.list_closed);
        list_closed.setAdapter(adapter_closed);
        list_closed.setItemsCanFocus(true);

        openPortsConnect = new ArrayList<Long>(4);
        openPortsConnect.add((long) 22);
        openPortsConnect.add((long) 23);
        openPortsConnect.add((long) 80);
        openPortsConnect.add((long) 443);

        // Start scan if ports empty
        if (ports_open == null && ports_closed == null) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, DiscoverActivity.MENU_SCAN_SINGLE, 0, R.string.scan_single_title).setIcon(
                android.R.drawable.ic_menu_mylocation);
        menu.add(0, DiscoverActivity.MENU_OPTIONS, 0, "Options").setIcon(
                android.R.drawable.ic_menu_preferences);
        menu.add(0, DiscoverActivity.MENU_HELP, 0, R.string.preferences_help).setIcon(
                android.R.drawable.ic_menu_help);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case DiscoverActivity.MENU_SCAN_SINGLE:
                DiscoverActivity.scanSingle(this, host);
                return true;
            case DiscoverActivity.MENU_OPTIONS:
                startActivity(new Intent(ctxt, Prefs.class));
                return true;
            case DiscoverActivity.MENU_HELP:
                startActivity(new Intent(ctxt, Help.class));
                return true;
        }
        return false;
    }

    static class ViewHolder {
        TextView port;
        Button btn_connect;
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
            final long port = (type == "open") ? ports_open.get(position) : ports_closed
                    .get(position);

            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_port, null);
                holder = new ViewHolder();
                holder.port = (TextView) convertView.findViewById(R.id.list);
                holder.btn_connect = (Button) convertView.findViewById(R.id.list_connect);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.port.setText(port + "/tcp " + type);
            if (openPortsConnect.contains(port)) {
                // That is an awful hack
                holder.btn_connect.setText(R.string.scan_connect);
                holder.btn_connect.setCompoundDrawablesWithIntrinsicBounds(R.drawable.connect, 0,
                        0, 0);
                holder.btn_connect.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        openPortService(port);
                    }
                });
            } else {
                holder.btn_connect.setText(null);
                holder.btn_connect.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                holder.btn_connect.setOnClickListener(null);
            }
            return convertView;
        }
    }

    private class ScanPortTask extends PortScan {
        private int progress_current = 0;

        ScanPortTask(String host) {
            super(host);
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
            nb_port = port_end - port_start + 1;
            setProgress(0);
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            if (!isCancelled()) {
                if (values.length > 0) {
                    if (!values[0].equals(new Long(0))) {
                        if (values[1] == 1) {
                            addPort(ports_open, adapter_open, values[0]);
                            cnt_open++;
                            mTabOpen
                                    .setText(String.format(getString(R.string.scan_open), cnt_open));

                        } else if (values[1] == 0) {
                            addPort(ports_closed, adapter_closed, values[0]);
                            cnt_closed++;
                            mTabClosed.setText(String.format(getString(R.string.scan_closed),
                                    cnt_closed));
                        } else if (values[1] == -1) {
                            // If host is unknown
                            makeToast(R.string.scan_host_unreachable);
                        }
                    }
                }
                progress_current++;
                setProgress(progress_current * 10000 / nb_port);
            }
        }

        @Override
        protected void onPostExecute(Void unused) {
            if (prefs.getBoolean(Prefs.KEY_VIBRATE_FINISH, Prefs.DEFAULT_VIBRATE_FINISH) == true) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(DiscoverActivity.VIBRATE);
            }
            if (ports_open.size() == 0) {
                makeToast(R.string.scan_noport);
            }
            stopScan();
            makeToast(R.string.scan_finished);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            makeToast(R.string.scan_canceled);
            stopScan();
        }

        private void addPort(ArrayList<Long> ports, PortsAdapter adapter, Long value) {
            ports.add(value);
            adapter.add("placeholder");
            //Collections.sort(ports); // FIXME: cause GC to collect
            //adapter.insert("placeholder", ports.indexOf(value));
        }
    }

    private void startScan() {
        makeToast(R.string.scan_start);
        setProgressBarVisibility(true);
        setProgressBarIndeterminateVisibility(true);
        adapter_open.clear();
        adapter_closed.clear();
        cnt_open = 0;
        cnt_closed = 0;
        ports_open = new ArrayList<Long>();
        ports_closed = new ArrayList<Long>();
        scanPortTask = new ScanPortTask(host);
        scanPortTask.execute();
        btn_scan.setText(R.string.btn_discover_cancel);
        btn_scan.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.cancel, 0, 0);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scanPortTask.cancel(true);
            }
        });
    }

    private void stopScan() {
        // Set result
        Intent intent = new Intent();
        intent.putExtra(HostBean.EXTRA_POSITION, position);
        intent.putExtra(HostBean.EXTRA_PORTSO, portsToLongArray(ports_open));
        intent.putExtra(HostBean.EXTRA_PORTSC, portsToLongArray(ports_closed));
        setResult(RESULT_OK, intent);
        // Reset scan
        setProgressBarVisibility(false);
        setProgressBarIndeterminateVisibility(false);
        setProgress(10000);
        btn_scan.setText(R.string.btn_scan);
        btn_scan.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.discover, 0, 0);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScan();
            }
        });
    }

    private List<String> preparePort(ArrayList<Long> ports, String state) {
        List<String> portsChar = new ArrayList<String>();
        if (ports != null) {
            for (Long port : ports) {
                portsChar.add(String.valueOf(port) + "/tcp " + state);
            }
        }
        return portsChar;
    }

    private long[] portsToLongArray(ArrayList<Long> ports) {
        long[] portsArray = new long[ports.size()];
        for (int i = 0; i < ports.size(); i++) {
            portsArray[i] = ports.get(i);
        }
        return portsArray;
    }

    private ArrayList<Long> portsToArrayList(long[] longArray) {
        ArrayList<Long> ports = new ArrayList<Long>();
        if (longArray != null) {
            for (int i = 0; i < longArray.length; i++) {
                ports.add(longArray[i]);
            }
            return ports;
        }
        return null;
    }

    private void openPortService(Long port) {
        String pk = "";
        String action = "";
        Intent intent = null;
        int portInt = (int) ((long) port);
        switch (portInt) {
            case 22:
                pk = "org.connectbot";
                action = Intent.ACTION_VIEW;
                if (isPackageInstalled(ctxt, pk)) {
                    String user = prefs.getString(Prefs.KEY_SSH_USER, Prefs.DEFAULT_SSH_USER);
                    intent = new Intent(action);
                    intent.setData(Uri.parse("ssh://" + user + "@" + host + ":22/#" + user + "@"
                            + host + ":22"));
                } else {
                    makeToast(String.format(getString(R.string.package_missing, "ConnectBot")));
                    intent = new Intent(Intent.ACTION_VIEW).setData(Uri
                            .parse("market://search?q=pname:" + pk));
                }
                break;
            case 23:
                pk = "org.connectbot";
                action = Intent.ACTION_VIEW;
                if (isPackageInstalled(ctxt, pk)) {
                    intent = new Intent(action);
                    intent.setData(Uri.parse("telnet://" + host + ":23"));
                } else {
                    makeToast(String.format(getString(R.string.package_missing, "ConnectBot")));
                    intent = new Intent(Intent.ACTION_VIEW).setData(Uri
                            .parse("market://search?q=pname:" + pk));
                }
                break;
            case 80:
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://" + host));
                break;
            case 443:
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://" + host));
                break;
            default:
                makeToast(R.string.scan_noaction);
                // TODO: Use something like netcat to fetch identification
                // message of service
        }
        if (intent != null) {
            startActivity(intent);
        }
    }

    private boolean isPackageInstalled(Context context, String p) {
        PackageManager packageManager = context.getPackageManager();
        try {
            packageManager.getPackageInfo(p, 0);
        } catch (NameNotFoundException e) {
            return false;
        }
        return true;
    }

    private void makeToast(String msg) {
        Toast.makeText(getApplicationContext(), (CharSequence) msg, Toast.LENGTH_SHORT).show();
    }

    private void makeToast(int msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
