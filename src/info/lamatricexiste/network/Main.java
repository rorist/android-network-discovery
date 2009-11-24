package info.lamatricexiste.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

final public class Main extends Activity {

	private final String TAG = "NetworkMain";
	// private final int DEFAULT_DISCOVER = 1;
	private final long VIBRATE = (long) 250;
	private List<String> hosts = null;
	private List<CharSequence[]> hosts_ports = null;
	private HostsAdapter adapter;
	private ListView list;
	// private Button btn;
	private Button btn_discover;
	private Button btn_export;
	// private SharedPreferences prefs = null;
	private ConnectivityManager connMgr;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
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
		btn_discover = (Button) findViewById(R.id.btn_discover);
		btn_discover.setText(R.string.btn_discover);
		btn_discover.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startDiscovering();
			}
		});

		// Export
		btn_export = (Button) findViewById(R.id.btn_export);
		btn_export.setText(R.string.btn_export);
		btn_export.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				export();
			}
		});

		// Wifi Settings
		Button btn_wifi = (Button) findViewById(R.id.btn_wifi);
		btn_wifi.setOnClickListener(new View.OnClickListener() {
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

		connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	@Override
	public void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
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

	// @Override
	// public boolean onCreateOptionsMenu(Menu menu) {
	// new MenuInflater(getApplication()).inflate(R.menu.options, menu);
	// return (super.onCreateOptionsMenu(menu));
	// }

	// @Override
	// public boolean onOptionsItemSelected(MenuItem item) {
	// if (item.getItemId() == R.id.settings) {
	// // startActivity(new Intent(this, Prefs.class));
	// return true;
	// }
	// return (super.onOptionsItemSelected(item));
	// }

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
						scanPort(position, false);
					}
				});
			}
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

		String action = intent.getAction();
		Log.d(TAG, action);
		if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
			int WifiState = intent
					.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
			Log.d(TAG, "WifiState=" + WifiState);
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

		if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
			WifiManager WifiService = (WifiManager) this
					.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiInfo = WifiService.getConnectionInfo();
			SupplicantState sstate = wifiInfo.getSupplicantState();
			Log.d(TAG, "SSTATE=" + sstate);
			if (sstate == SupplicantState.COMPLETED) {
				info_nt.setText(R.string.wifi_dhcp);
			}

		}
		final NetworkInfo network_info = connMgr.getActiveNetworkInfo();
		if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)
				&& network_info != null) {
			NetworkInfo.State state = network_info.getState();
			Log.d(TAG, "netinfo=" + state + " with " + network_info.getType());
			if (network_info.getType() == ConnectivityManager.TYPE_WIFI
					&& state == NetworkInfo.State.CONNECTED) {
				WifiManager WifiService = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				NetInfo net = new NetInfo(WifiService);
				info_ip.setText("IP: " + net.getIp().getHostAddress());
				info_nt.setText("NT: " + net.getNetIp().getHostAddress() + "/"
						+ net.getNetCidr());
				info_id.setText("SSID: " + net.getSSID());
				setButtonOn(btn_discover);
				setButtonOn(btn_export);
			}
		}
	}

	/**
	 * Discover hosts
	 */

	private class CheckHostsTask extends DiscoveryUnicast {
		private int hosts_done = 0;

		protected void onPreExecute() {
			WifiManager WifiService = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			NetInfo net = new NetInfo(WifiService);
			int cidr = net.getNetCidr();
			ip_int = net.getIp().hashCode();
			start = (ip_int & (1 - (1 << (32 - cidr)))) + 1;
			end = (ip_int | ((1 << (32 - cidr)) - 1)) - 1;
			size = end - start;
		}

		protected void onProgressUpdate(String... item) {
			if (!isCancelled()) {
				String host = item[0];
				if (!host.equals(new String())) {
					addHost(host);
					hosts.add(host);
					hosts_ports.add(null);
				}
				hosts_done++;
				setProgress(hosts_done * 10000 / size);
			}
		}

		protected void onPostExecute(Void unused) {
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(VIBRATE);
			makeToast(R.string.discover_finished);
			stopDiscovering();
		}

		protected void onCancelled() {
			pool.shutdownNow();
			makeToast(R.string.discover_canceled);
			stopDiscovering();
		}
	}

	private void startDiscovering() {
		makeToast(R.string.discover_start);
		setProgressBarVisibility(true);
		setProgressBarIndeterminateVisibility(true);
		initList();
		final CheckHostsTask task = new CheckHostsTask();
		task.execute();
		btn_discover.setText("Cancel");
		btn_discover.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				task.cancel(true);
			}
		});
	}

	private void stopDiscovering() {
		setProgressBarVisibility(false);
		setProgressBarIndeterminateVisibility(false);
		btn_discover.setText("Discover");
		btn_discover.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startDiscovering();
			}
		});
	}

	/**
	 * Port Scan
	 */

	private class ScanPortTask extends PortScan {

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

		protected void onPostExecute(Void unused) {
			CharSequence[] result = ports
					.toArray(new CharSequence[ports.size()]);
			hosts_ports.set(position, result);
			progress.dismiss();
			showPorts(result, position, host);
			Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(VIBRATE);
		}

		protected void onProgressUpdate(String... values) {
			if (values.length > 0) {
				if (!values[0].equals(new String())) {
					ports.add(values[0]);
				}
			}
			progress_current++;
			progress.setProgress(progress_current);
		}
	}

	private void scanPort(final int position, boolean force) {
		String host = hosts.get(position);
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
		scanDone.setTitle(host).setPositiveButton(R.string.btn_rescan,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dlg, int sumthin) {
						scanPort(position, true);
					}
				}).setNegativeButton(R.string.btn_close, null);
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
		getFileName.setTitle(R.string.export_choose);
		getFileName.setView(v);
		getFileName.setPositiveButton(R.string.export_save,
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
