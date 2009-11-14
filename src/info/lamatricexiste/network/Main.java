package info.lamatricexiste.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

final public class Main extends Activity {

	private final String TAG = "NetworkMain";
	// private final int DEFAULT_DISCOVER = 1;
	private final int NB_PORTS = 1024;
	private final long VIBRATE = (long) 250;
	private List<String> hosts = null;
	private List<CharSequence[]> hosts_ports = null;
	private HostsAdapter adapter;
	private ListView list;
	// private Button btn;
	private Button btn_discover;
	private Button btn_export;
	// private SharedPreferences prefs = null;
	private boolean discovering = false;
	private WifiManager WifiService;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Send Request
		// btn = (Button) findViewById(R.id.btn);
		// btn.setOnClickListener(new View.OnClickListener() {
		// public void onClick(View v) {
		// sendPacket();
		// }
		// });

		// Discover
		btn_discover = (Button) findViewById(R.id.btn1);
		btn_discover.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
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

		// Wifi Settings
		Button btn2 = (Button) findViewById(R.id.btn2);
		btn2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
			}
		});

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
		adapter = new HostsAdapter(this, R.layout.list, R.id.list);
		list = (ListView) findViewById(R.id.output);
		list.setAdapter(adapter);
		list.setItemsCanFocus(true);

		WifiService = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
	}

	@Override
	public void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
		filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
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
		new MenuInflater(getApplication()).inflate(R.menu.options, menu);
		return (super.onCreateOptionsMenu(menu));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.settings) {
			// startActivity(new Intent(this, Prefs.class));
			return true;
		}
		return (super.onOptionsItemSelected(item));
	}

	// Custom ArrayAdapter
	private class HostsAdapter extends ArrayAdapter<String> {
		public HostsAdapter(Context context, int resource,
				int textViewresourceId) {
			super(context, resource, textViewresourceId);
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			convertView = super.getView(position, convertView, parent);
			if (convertView != null) {
				Button btn_ports = (Button) convertView
						.findViewById(R.id.list_port);
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
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		public void onReceive(Context ctxt, Intent intent) {
			String a = intent.getAction();
			Log.d(TAG, "Receive broadcasted " + a);
			if (a.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				setWifiState(intent);
			} else if (a.equals(WifiManager.NETWORK_IDS_CHANGED_ACTION)
					|| a.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)
					|| a.equals(WifiManager.RSSI_CHANGED_ACTION)
					|| a.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
					|| a
							.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
					|| a.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) {
				setWifiInfo();
			}
		}
	};

	private void setWifiInfo() {
		TextView info_ip = (TextView) findViewById(R.id.info_ip);
		TextView info_nt = (TextView) findViewById(R.id.info_nt);
		TextView info_id = (TextView) findViewById(R.id.info_id);
		WifiInfo wifiInfo = WifiService.getConnectionInfo(); // TODO: User
		// NetworkInfo
		// class
		SupplicantState sstate = wifiInfo.getSupplicantState();
		NetworkInfo net = new NetworkInfo(WifiService);

		info_ip.setText("");
		info_id.setText("");
		setButtonOff(btn_discover);
		setButtonOff(btn_export);
		switch (sstate) {
		case SCANNING:
			info_nt.setText(R.string.wifi_scanning);
			break;
		case ASSOCIATED:
		case ASSOCIATING:
			String ssid = net.getSSID();
			if (ssid != null) {
				info_nt.setText(String.format(
						getString(R.string.wifi_associating_ap), ssid));
			} else {
				info_nt.setText(R.string.wifi_associating);
			}
			break;
		case COMPLETED:
			if (discovering == false) {
				setButtonOn(btn_discover);
				setButtonOn(btn_export);
			}
			info_ip.setText("IP: " + net.getIp().getHostAddress());
			info_nt.setText("NT: " + net.getNetIp().getHostAddress() + "/"
					+ net.getNetCidr());
			info_id.setText("SSID: " + net.getSSID());
			break;
		}
	}

	private void setWifiState(Intent intent) {
		TextView info_ip = (TextView) findViewById(R.id.info_ip);
		TextView info_nt = (TextView) findViewById(R.id.info_nt);
		TextView info_id = (TextView) findViewById(R.id.info_id);

		info_ip.setText("");
		info_id.setText("");
		int WifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
		setButtonOff(btn_discover);
		setButtonOff(btn_export);
		switch (WifiState) {
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
	 * Discover hosts
	 */

	private class CheckHostsTask extends AsyncTask<Void, String, Void>
			implements Observer {

		private int hosts_done = 0;
		private int hosts_size;

		protected Void doInBackground(Void... v) {
			NetworkInfo net = new NetworkInfo(WifiService);
			int cidr = net.getNetCidr();
			int ip_int = net.getIp().hashCode();
			int start = (ip_int & (1 - (1 << (32 - cidr)))) + 1;
			int end = (ip_int | ((1 << (32 - cidr)) - 1)) - 1;
			hosts_size = end - start;

			DiscoveryUnicast discover = new DiscoveryUnicast(this);
			discover.run(ip_int, start, end);

			return null;
		}

		protected void onCancelled() {
		}

		protected void onProgressUpdate(String... item) {
			String host = item[0];
			if (host != null) {
				addHost(host);
				hosts.add(host);
				hosts_ports.add(null);
			}
			hosts_done++;
			if (hosts_done == hosts_size) {
				stopDiscovering();
			}
		}

		public void update(Observable observable, Object data) {
			publishProgress((String) data);
		}
	}

	private void startDiscovering() {
		discovering = true;
		setButtonOff(btn_discover);
		setButtonOff(btn_export);
		makeToast(R.string.discover_start);
		initList();
		final CheckHostsTask task = new CheckHostsTask();
		task.execute();

		// btn_discover.setText("Cancel");
		// btn_discover.setOnClickListener(new View.OnClickListener() {
		// public void onClick(View v) {
		// task.cancel(true);
		// }
		// });
	}

	private void stopDiscovering() {
		discovering = false;
		setButtonOn(btn_discover);
		setButtonOn(btn_export);
		makeToast(R.string.discover_finished);
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(VIBRATE);
		// btn_discover.setText("Discover");
		// btn_discover.setOnClickListener(new View.OnClickListener() {
		// public void onClick(View v) {
		// startDiscovering();
		// }
		// });
	}

	/**
	 * Port Scan
	 */

	private class ScanPortTask extends AsyncTask<Void, Void, Void> implements
			Observer {
		private int position;
		private String host;
		private ProgressDialog progress = null;
		private ArrayList<CharSequence> ports = new ArrayList<CharSequence>();
		private int progress_current = 0;

		protected void onPreExecute() {
			progress = new ProgressDialog(Main.this);
			progress.setMessage(String.format(getString(R.string.scan_start),
					host));
			progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress.setCancelable(false);
			progress.setMax(NB_PORTS);
			progress.show();
		}

		protected Void doInBackground(Void... v) {
			PortScan scan = new PortScan();
			scan.scan(this, host);
			return null;
		}

		protected void onPostExecute(Void unused) {
			CharSequence[] result = ports
					.toArray(new CharSequence[ports.size()]);
			hosts_ports.set(position, result);
			progress.dismiss();
			showPorts(result, position, host);
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(VIBRATE);
		}

		public void setInfo(int position, String host) {
			this.position = position;
			this.host = host;
		}

		public void update(Observable observable, Object data) {
			CharSequence port = (CharSequence) data;
			if (port != null) {
				ports.add(port);
			}
			progress_current++;
			progress.setProgress(progress_current);
		}
	}

	private void scanPort(final int position, final String host, boolean force) {
		CharSequence[] ports = hosts_ports.get(position);
		if (force || ports == null) {
			ScanPortTask task = new ScanPortTask();
			task.setInfo(position, host);
			task.execute();
		} else {
			showPorts(ports, position, host);
		}
	}

	private void showPorts(final CharSequence[] ports, final int position,
			final String host) {
		AlertDialog.Builder scanDone = new AlertDialog.Builder(Main.this);
		scanDone.setTitle(host).setPositiveButton("Rescan",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dlg, int sumthin) {
						scanPort(position, host, true);
					}
				}).setNegativeButton("Close", null);
		if (ports.length > 0) {
			scanDone.setItems(ports, null);
		} else {
			scanDone.setMessage(R.string.scan_noport);
		}
		scanDone.show();
	}

	/**
	 * Main
	 */

	// private void sendPacket(){
	// CheckBox cb = (CheckBox) findViewById(R.id.repeat); //FIXME: This is bad
	// final boolean repeat = cb.isChecked();
	// final CharSequence[] items = {"Ping (ICMP)","Samba exploit"};
	// setButtonOff(btn);
	// @SuppressWarnings("unused")
	// AlertDialog dialog = new AlertDialog.Builder(this)
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

	private void export() {
		final Export e = new Export(Main.this, hosts, hosts_ports);
		String file = e.getFileName();

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.file, null);
		final EditText txt = (EditText) v.findViewById(R.id.export_file);
		txt.setText(file);

		AlertDialog.Builder getFileName = new AlertDialog.Builder(Main.this);
		getFileName.setTitle("Choose file destination");
		getFileName.setView(v);
		getFileName.setPositiveButton("Save",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dlg, int sumthin) {
						try {
							e.writeToSd((txt.getText()).toString());
							makeToast(R.string.export_finished);
						} catch (IOException e) {
							makeToast("Error: " + e.getMessage());
							export();
						}
					}
				});
		getFileName.show();
	}

	private void initList() {
		// setSelectedHosts(false);
		adapter.clear();
		hosts = new ArrayList<String>();
		hosts_ports = new ArrayList<CharSequence[]>();
	}

	// private void updateList() {
	// adapter.clear();
	// listHosts();
	// }
	//
	// private void listHosts() {
	// for (String h : hosts) {
	// addHost(h);
	// }
	// }

	private void addHost(String text) {
		adapter.add(text);
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

	private void makeToast(String msg) {
		Toast.makeText(getApplicationContext(), (CharSequence) msg,
				Toast.LENGTH_SHORT).show();
	}

	private void makeToast(int msg) {
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}

	private void setButtonOff(Button b) {
		b.setClickable(false);
		b.setEnabled(false);
	}

	private void setButtonOn(Button b) {
		b.setClickable(true);
		b.setEnabled(true);
	}
}
