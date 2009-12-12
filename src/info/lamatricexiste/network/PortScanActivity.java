package info.lamatricexiste.network;

import info.lamatricexiste.network.PortScan.PortScan;
import info.lamatricexiste.network.Utils.Prefs;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

final public class PortScanActivity extends ListActivity {

    // private final String TAG = "PortScanActivity";
    private SharedPreferences prefs;
    private ScanPortTask scanPortTask;
    // private ConnectivityManager connMgr;
    private ArrayAdapter<String> adapter;
    private String host;
    private ArrayList<Long> ports = null;
    private Button btn_scan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.portscan);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Bundle extra = getIntent().getExtras();
        host = extra.getString("host");
        populatePorts(extra.getLongArray("ports"));

        // Scan
        btn_scan = (Button) findViewById(R.id.btn_scan);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scanPortTask = new ScanPortTask(host);
            }
        });

        // Back
        Button btn_back = (Button) findViewById(R.id.btn_back);
        btn_back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScan();
                finish();
            }
        });

        // List
        adapter = new PortsAdapter(this, R.layout.list_port, R.id.list,
                preparePort());
        setListAdapter(adapter);
        ListView list = (ListView) findViewById(android.R.id.list);
        list.setItemsCanFocus(true);

        // Start scan if ports empty
        if (ports == null) {
            startScan();
        }
    }

    private void populatePorts(long[] longArray) {
        if (longArray != null) {
            ports = new ArrayList<Long>();
            for (int i = 0; i < longArray.length; i++) {
                ports.add(longArray[i]);
            }
        }
    }

    private class ScanPortTask extends PortScan {
        private int progress_current = 0;

        ScanPortTask(String host) {
            super(host);
        }

        @Override
        protected void onPreExecute() {
            // Get preferences
            String port_start_pref = prefs.getString(Prefs.KEY_PORT_START,
                    Prefs.DEFAULT_PORT_START);
            String port_end_pref = prefs.getString(Prefs.KEY_PORT_END,
                    Prefs.DEFAULT_PORT_END);
            port_start = Integer.parseInt(port_start_pref);
            port_end = Integer.parseInt(port_end_pref);
            nb_port = port_end - port_start + 1;
            setProgress(0);
        }

        @Override
        protected void onPostExecute(Void unused) {
            // Collections.sort(ports);
            // Long[] result = ports.toArray(new Long[ports.size()]);
            // hosts_ports.set(position, result);
            if (prefs.getBoolean(Prefs.KEY_VIBRATE_FINISH,
                    Prefs.DEFAULT_VIBRATE_FINISH) == true) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(DiscoverActivity.VIBRATE);
            }
            if (ports.size() == 0) {
                makeToast(R.string.scan_noport);
            }
            stopScan();
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            if (values.length > 0) {
                if (!values[0].equals(new Long(0))) {
                    ports.add(values[0]);
                    adapter.add(values[0] + "/tcp open");
                    // Set entry icon/etc
                    // Set action here
                }
            }
            progress_current++;
            setProgress(progress_current * 10000 / nb_port);
        }
    }

    private void startScan() {
        makeToast(R.string.discover_start);
        setProgressBarVisibility(true);
        setProgressBarIndeterminateVisibility(true);
        adapter.clear();
        ports = new ArrayList<Long>();
        scanPortTask = new ScanPortTask(host);
        scanPortTask.execute();
        btn_scan.setText(R.string.btn_discover_cancel);
        btn_scan.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.cancel,
                0, 0);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scanPortTask.cancel(true);
            }
        });
    }

    private void stopScan() {
        setProgressBarVisibility(false);
        setProgressBarIndeterminateVisibility(false);
        btn_scan.setText(R.string.btn_scan);
        btn_scan.setCompoundDrawablesWithIntrinsicBounds(0,
                R.drawable.discover, 0, 0);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScan();
            }
        });
    }

    private List<String> preparePort() {
        List<String> portsChar = new ArrayList<String>();
        if (ports != null) {
            for (Long port : ports) {
                portsChar.add(String.valueOf(port) + "/tcp open");
            }
        }
        return portsChar;
    }

    public static List<String> preparePortPublic(long[] portsArray) {
        List<String> portsChar = new ArrayList<String>();
        if (portsArray != null) {
            for (int i = 0; i < portsArray.length; i++) {
                portsChar.add(String.valueOf(portsArray[i]) + "/tcp open");
            }
        }
        return portsChar;
    }

    static class ViewHolder {
        Button btn_connect;
    }

    // Custom ArrayAdapter
    private class PortsAdapter extends ArrayAdapter<String> {
        public PortsAdapter(Context context, int resource,
                int textViewresourceId, List<String> objects) {
            super(context, resource, textViewresourceId, objects);
        }

        @Override
        public View getView(final int position, View convertView,
                ViewGroup parent) {

            ViewHolder holder;
            if (convertView == null) {
                convertView = super.getView(position, convertView, parent);
                holder = new ViewHolder();
                holder.btn_connect = (Button) convertView
                        .findViewById(R.id.list_connect);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.btn_connect.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    openPortService(host, ports.get(position));
                }
            });
            return convertView;
        }
    }

    private void openPortService(String host, Long port) {
        Intent intent = null;
        String action = "";
        int portInt = (int) ((long) port);
        switch (portInt) {
            case 22:
                action = Intent.ACTION_VIEW;
                if (isPackageInstalled(this, "org.connectbot")) {
                    String user = prefs.getString(Prefs.KEY_SSH_USER,
                            Prefs.DEFAULT_SSH_USER);
                    intent = new Intent(action);
                    intent.setData(Uri.parse("ssh://" + user + "@" + host
                            + ":22/#" + user + "@" + host + ":22"));
                } else {
                    makeToast(String.format(getString(R.string.package_missing,
                            "ConnectBot")));
                }
                break;
            case 23:
                action = Intent.ACTION_VIEW;
                if (isPackageInstalled(this, "org.connectbot")) {
                    intent = new Intent(action);
                    intent.setData(Uri.parse("telnet://" + host + ":23"));
                } else {
                    makeToast(String.format(getString(R.string.package_missing,
                            "ConnectBot")));
                }
                break;
            case 80:
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://" + host + "/"));
                break;
            case 443:
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://" + host + "/"));
                break;
            default:
                makeToast(R.string.scan_noaction);
                // Use something like netcat to
                // fetch identification message of service
        }
        if (intent != null) {
            startActivity(intent);
        }
    }

    // private boolean wifiConnectedOrWarn() {
    // final NetworkInfo network_info = connMgr
    // .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    // if (network_info.getState() == NetworkInfo.State.CONNECTED) {
    // return true;
    // }
    // AlertDialog.Builder alert = new AlertDialog.Builder(
    // PortScanActivity.this);
    // alert.setMessage(R.string.wifi_disabled);
    // alert.setPositiveButton(R.string.btn_close, null);
    // alert.show();
    // return false;
    // }

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
        Toast.makeText(getApplicationContext(), (CharSequence) msg,
                Toast.LENGTH_SHORT).show();
    }

    private void makeToast(int msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
