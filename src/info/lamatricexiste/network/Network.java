package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class Network extends Service {
	private final String TAG = "NetworkService";
	public final static String ACTION_SENDHOST = "info.lamatricexiste.network.SENDHOST";
	public final static String ACTION_FINISH = "info.lamatricexiste.network.FINISH";
	public final static String ACTION_UPDATELIST = "info.lamatricexiste.network.UPDATELIST";
	public final static String ACTION_TOTALHOSTS = "info.lamatricexiste.network.TOTALHOSTS";
	public final static int TIMEOUT_REACH = 600;
	private final long UPDATE_INTERVAL = 60000; // 1mn
	public static int WifiState = -1;
	private WifiManager WifiService = null;
	private Timer timer = new Timer();
	private List<InetAddress> hosts = new ArrayList<InetAddress>();
	// private SharedPreferences prefs = null;
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		public void onReceive(Context ctxt, Intent intent) {
			String a = intent.getAction();
			if (a.equals(Network.ACTION_SENDHOST)) {
				String h = intent.getExtras().getString("addr");
				try {
					InetAddress i = InetAddress.getByName(h);
					if (!hosts.contains(i)) {
						hosts.add(i);
					}
				} catch (UnknownHostException e) {
					Log.e(TAG, e.getMessage());
				}
			} else if (a.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
				WifiState = intent
						.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
			}
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Network.ACTION_SENDHOST);
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		registerReceiver(receiver, filter);
		// prefs = PreferenceManager.getDefaultSharedPreferences(this);
	}

	@Override
	public void onDestroy() {
		stopService();
		unregisterReceiver(receiver);
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		WifiService = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		// sendBroadcast(new Intent(ACTION_UPDATELIST));
		return mBinder;
	}

	private void startService(final List<InetAddress> hosts_send,
			final int request) {
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				launchRequest(hosts_send, request);
			}
		}, 0, UPDATE_INTERVAL);
	}

	private void stopService() {
		if (timer != null)
			timer.cancel();
	}

	/**
	 * Runnable requests
	 */

	private void onUpdate(int method) {
		NetworkInfo net = new NetworkInfo(WifiService);
		hosts = new ArrayList<InetAddress>();
		// TODO: handler multiple methods
		if (net.isWifiEnabled()) {
			switch (method) {
			case 1:
				DiscoveryUnicast run = new DiscoveryUnicast();
				run.setVar(this, net.getIp(), net.getNetIp(), net
						.getBroadcastIp(), net.getNetmask(), net.getNetCidr());
				new Thread(run).start();
				break;
			default:
				Log.v(TAG, "No discovery method selected!");
			}
		}
	}

	private void launchRequest(List<InetAddress> hosts_send, int request) {
		NetworkInfo net = new NetworkInfo(WifiService);
		if (net.isWifiEnabled()) {
			for (InetAddress h : hosts_send) {
				Thread t = new Thread(getRunnable(h, request));
				t.start();
			}
			sendBroadcast(new Intent(ACTION_FINISH)); // TODO: Move to
														// SendSmbNegotiate and
														// so on
		}
	}

	private Runnable getRunnable(final InetAddress host, int request) {
		switch (request) {
		case 0:
			SendSmbNegotiate smb = new SendSmbNegotiate();
			smb.setHost(host);
			return (Runnable) smb;
		default:
			return new Runnable() {
				public void run() {
					try {
						host.isReachable(TIMEOUT_REACH);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
		}
	}

	/**
	 * Interface binder
	 */

	private final NetworkInterface.Stub mBinder = new NetworkInterface.Stub() {

		public void inSearchReachableHosts(int method)
				throws DeadObjectException {
			Log.v(TAG, "inSearchReachableHosts");
			onUpdate(method);
		}

		public List<String> inGetHosts() throws RemoteException {
			Log.v(TAG, "inGetHosts");
			return hostsToStr();
		}

		public void inSendPacket(List<String> hosts_send, int request,
				boolean repeat) {
			Log.v(TAG, "inSendPacket");
			List<InetAddress> hosts_receive = hostsFromStr(hosts_send);
			if (repeat) {
				startService(hosts_receive, request);
			} else {
				stopService();
			}
			launchRequest(hosts_receive, request);
		}
	};

	/**
	 * Hosts to/from String
	 */

	private List<String> hostsToStr() {
		List<String> hosts_str = new ArrayList<String>();
		for (InetAddress h : hosts) {
			hosts_str.add(h.getHostAddress());
		}
		return hosts_str;
	}

	private List<InetAddress> hostsFromStr(List<String> hosts_str) {
		List<InetAddress> hosts_new = new ArrayList<InetAddress>();
		for (String h : hosts_str) {
			try {
				hosts_new.add(InetAddress.getByName(h));
			} catch (UnknownHostException e) {
				Log.e(TAG, e.getMessage());
			}
		}
		return hosts_new;
	}
}
