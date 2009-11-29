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
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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

final public class Main extends Activity {

	private final String TAG = "NetworkMain";
	// private final int DEFAULT_DISCOVER = 1;
	private final long VIBRATE = (long) 250;
	private List<String> hosts = null;
	private List<Long[]> hosts_ports = null;
	private HostsAdapter adapter;
	private ListView list;
	// private Button btn;
	private Button btn_discover;
	private Button btn_export;
	private SharedPreferences prefs = null;
	// private boolean rooted = false;
	private ConnectivityManager connMgr;
	private CheckHostsTask checkHostsTask = null;
	private ScanPortTask scanPortTask = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);

		// Get global preferences
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Discover
		btn_discover = (Button) findViewById(R.id.btn_discover);
		btn_discover.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				checkHostsTask = new CheckHostsTask();
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
				startActivity(new Intent(Main.this, Prefs.class));
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
		adapter = new HostsAdapter(this, R.layout.list, R.id.list);
		list = (ListView) findViewById(R.id.output);
		list.setAdapter(adapter);
		list.setItemsCanFocus(true);

		connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		// checkRoot();

		// Fake hosts and ports

		initList();
		addHost("10.0.10.2"); // EmulatorBridge: 10.0.2.2
		Long[] port = { (long) 80, (long) 443 };
		hosts_ports.set(0, port);

		// try {
		// File su = new File("/proc/net/arp");
		// if (su.exists() == false) {
		// Log.v(TAG, "ARP File is NOT accessible");
		// } else {
		// Log.v(TAG, "ARP File is accessible");
		// }
		// } catch (Exception e) {
		// Log.d(TAG, "Can't open file ARP: " + e.getMessage());
		// }
	}

	@Override
	public void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		registerReceiver(receiver, filter);
		networkStateChanged(new Intent());
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
			startActivity(new Intent(this, Prefs.class));
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
				// Add listeners to the Buttons
				Button btn_ports = (Button) convertView
						.findViewById(R.id.list_port);
				btn_ports.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						scanPort(position, false);
					}
				});
				Button btn_info = (Button) convertView
						.findViewById(R.id.list_info);
				btn_info.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						showHostInfo(position);
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
		if (action != null) {
			Log.d(TAG, action);
			if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				int WifiState = intent.getIntExtra(
						WifiManager.EXTRA_WIFI_STATE, -1);
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
				} else if (sstate == SupplicantState.SCANNING) {
					info_nt.setText(R.string.wifi_scanning);
				} else if (sstate == SupplicantState.ASSOCIATING) {
					info_nt.setText(R.string.wifi_associating);
				}

			}
		}

		final NetworkInfo network_info = connMgr
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (network_info != null) {
			NetworkInfo.State state = network_info.getState();
			Log.d(TAG, "netinfo=" + state + " with " + network_info.getType());
			if (state == NetworkInfo.State.CONNECTED) {
				WifiManager WifiService = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				NetInfo net = new NetInfo(WifiService);
				info_ip.setText("IP: " + net.getIp().getHostAddress());
				info_nt.setText("NT: " + net.getNetIp().getHostAddress() + "/"
						+ net.getNetCidr());
				info_id.setText("SSID: " + net.getSSID());
				setButtonOn(btn_discover);
				setButtonOn(btn_export);
			} else if (checkHostsTask != null) {
				cancelAllTasks();
			}
		} else if (checkHostsTask != null) {
			cancelAllTasks();
		}
	}

	/**
	 * Discover hosts
	 */

	private class CheckHostsTask extends DiscoveryUnicast {
		private int hosts_done = 0;

		@Override
		protected void onPreExecute() {
			WifiManager WifiService = (WifiManager) getSystemService(Context.WIFI_SERVICE);
			NetInfo net = new NetInfo(WifiService);
			int cidr = net.getNetCidr();
			ip_int = net.getIp().hashCode();
			start = (ip_int & (1 - (1 << (32 - cidr)))) + 1;
			end = (ip_int | ((1 << (32 - cidr)) - 1)) - 1;
			size = end - start + 1;
			setProgress(0);
		}

		@Override
		protected void onProgressUpdate(String... item) {
			if (!isCancelled()) {
				String host = item[0];
				if (!host.equals(new String())) {
					addHost(host);
				}
				hosts_done++;
				setProgress(hosts_done * 10000 / size);
			}
		}

		@Override
		protected void onPostExecute(Void unused) {
			if (prefs.getBoolean(Prefs.KEY_VIBRATE_FINISH,
					Prefs.DEFAULT_VIBRATE_FINISH) == true) {
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(VIBRATE);
			}
			makeToast(R.string.discover_finished);
			stopDiscovering();
		}

		@Override
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
		checkHostsTask.execute();
		btn_discover.setText(R.string.btn_discover_cancel);
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
		btn_discover.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				checkHostsTask = new CheckHostsTask();
				startDiscovering();
			}
		});
	}

	private void cancelAllTasks() {
		if (checkHostsTask != null) {
			checkHostsTask.cancel(true);
			checkHostsTask = null;
		}
		if (scanPortTask != null) {
			scanPortTask.cancel(true);
			scanPortTask = null;
		}
	}

	private void initList() {
		// setSelectedHosts(false);
		adapter.clear();
		hosts = new ArrayList<String>();
		hosts_ports = new ArrayList<Long[]>();
	}

	private void addHost(String text) {
		adapter.add(text);
		hosts.add(text);
		hosts_ports.add(null);
	}

	private void showHostInfo(int hostPosition) {
		String ip = hosts.get(hostPosition);
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = inflater.inflate(R.layout.info, null);
		// Build info dialog
		AlertDialog.Builder infoDialog = new AlertDialog.Builder(Main.this);
		infoDialog.setTitle(ip);
		// Set info values
		HardwareAddress hardwareAddress = new HardwareAddress(this);
		String macaddr = hardwareAddress.getHardwareAddress(ip);
		String nicvend = hardwareAddress.getNicVendor(macaddr);
		TextView mac = (TextView) v.findViewById(R.id.info_mac);
		TextView vendor = (TextView) v.findViewById(R.id.info_nic);
		mac.setText(macaddr);
		vendor.setText(nicvend);
		// Show dialog
		infoDialog.setView(v);
		infoDialog.setNegativeButton(R.string.btn_close, null);
		infoDialog.show();
	}

	/**
	 * Port Scan
	 */

	private class ScanPortTask extends PortScan {

		private ProgressDialog progress = null;
		private ArrayList<Long> ports = new ArrayList<Long>();
		private int progress_current = 0;

		ScanPortTask(int position, String host) {
			super(position, host);
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
			// Set progress
			progress = new ProgressDialog(Main.this);
			progress.setMessage(String.format(getString(R.string.scan_start),
					host));
			progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress.setCancelable(false);
			progress.setMax(nb_port);
			progress.show();
		}

		@Override
		protected void onPostExecute(Void unused) {
			Long[] result = ports.toArray(new Long[ports.size()]);
			hosts_ports.set(position, result);
			progress.dismiss();
			showPorts(result, position, host);
			if (prefs.getBoolean(Prefs.KEY_VIBRATE_FINISH,
					Prefs.DEFAULT_VIBRATE_FINISH) == true) {
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				v.vibrate(VIBRATE);
			}
		}

		@Override
		protected void onProgressUpdate(Long... values) {
			if (values.length > 0) {
				if (!values[0].equals(new Long(0))) {
					ports.add(values[0]);
				}
			}
			progress_current++;
			progress.setProgress(progress_current);
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			this.onPostExecute(null);
		}
	}

	private void scanPort(final int position, boolean force) {
		String host = hosts.get(position);
		Long[] ports = hosts_ports.get(position);
		if (wifiConnectedOrWarn() && (force || ports == null)) {
			scanPortTask = new ScanPortTask(position, host);
			scanPortTask.execute();
		} else if (ports != null) {
			showPorts(ports, position, host);
		}
	}

	private void showPorts(final Long[] ports, final int position,
			final String host) {
		final AlertDialog.Builder scanDone = new AlertDialog.Builder(Main.this);
		scanDone.setTitle(host).setPositiveButton(R.string.btn_rescan,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dlg, int sumthin) {
						scanPort(position, true);
					}
				}).setNegativeButton(R.string.btn_close, null);
		if (ports.length > 0) {
			scanDone.setItems(preparePort(ports),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							openPortService(host, ports[which]);
							scanDone.show();
						}
					});
		} else {
			scanDone.setMessage(R.string.scan_noport);
		}
		scanDone.show();
	}

	public static CharSequence[] preparePort(Long[] ports) {
		CharSequence[] portsChar = new CharSequence[ports.length];
		for (int i = 0; i < ports.length; i++) {
			portsChar[i] = (CharSequence) String.valueOf(ports[i])
					+ "/tcp open";
		}
		return portsChar;
	}

	private void openPortService(String host, Long port) {
		Intent intent = null;
		switch (port.hashCode()) {
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

	/**
	 * Main
	 */

	private boolean wifiConnectedOrWarn() {
		final NetworkInfo network_info = connMgr
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (network_info.getState() == NetworkInfo.State.CONNECTED) {
			return true;
		}
		AlertDialog.Builder alert = new AlertDialog.Builder(Main.this);
		alert.setMessage(R.string.wifi_disabled);
		alert.setPositiveButton(R.string.btn_close, null);
		alert.show();
		return false;
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
