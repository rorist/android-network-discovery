/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network;

import info.lamatricexiste.network.Network.HostBean;
import info.lamatricexiste.network.Network.NetInfo;
import info.lamatricexiste.network.Network.RateControl;
import info.lamatricexiste.network.Network.Reachable;
import info.lamatricexiste.network.Utils.Prefs;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.util.Log;

public class DefaultDiscovery extends AsyncTask<Void, HostBean, Void> {

	private final String TAG = "DefaultDiscovery";
	private final int mRateMult = 5; // Number of alive hosts between Rate
	// Checks
	private int pt_move = 2; // 1=backward 2=forward
	private ExecutorService mPool;
	private int hosts_done = 0;
	private boolean doRateControl;
	private WeakReference<ActivityDiscovery> mDiscover;
	protected RateControl mRateControl;

	// TODO: Adaptiv value or changeable by Prefs
	protected long ip;
	protected long start;
	protected long end;
	protected long size;

	public DefaultDiscovery(ActivityDiscovery discover) {
		mDiscover = new WeakReference<ActivityDiscovery>(discover);
		mRateControl = new RateControl();
	}

	@Override
	protected void onPreExecute() {
        // FIXME: Move that in ActivityDiscovey
		if (mDiscover != null) {
			final ActivityDiscovery discover = mDiscover.get();
			if (discover != null) {
				NetInfo net = new NetInfo(discover);
				ip = NetInfo.getUnsignedLongFromIp(net.ip);
				int shift = (32 - net.cidr);
				start = (ip >> shift << shift) + 1;
				end = (start | ((1 << shift) - 1)) - 1;
				size = (int) (end - start + 1);
				discover.setProgress(0);
				doRateControl = discover.prefs.getBoolean(Prefs.KEY_RATECTRL_ENABLE,
						Prefs.DEFAULT_RATECTRL_ENABLE);
			}
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		if (mDiscover != null) {
			final ActivityDiscovery discover = mDiscover.get();
			if (discover != null) {
				Log.v(TAG, "start=" + NetInfo.getIpFromLongUnsigned(start) + " (" + start
						+ "), end=" + NetInfo.getIpFromLongUnsigned(end) + " (" + end
						+ "), length=" + size);
				mPool = Executors.newFixedThreadPool(Integer.parseInt(discover.prefs.getString(
						Prefs.KEY_NTHREADS, Prefs.DEFAULT_NTHREADS)));
				// mPool = Executors.newCachedThreadPool();
				// mPool =
				// Executors.newScheduledThreadPool(Integer.parseInt(discover.prefs.getString(
				// Prefs.KEY_NTHREADS, Prefs.DEFAULT_NTHREADS)));

				try {
					// gateway
					launch(start);
					// hosts
					long pt_backward = ip - 1;
					long pt_forward = ip + 1;
					long size_hosts = size - 2;

					for (int i = 0; i < size_hosts; i++) {
						// Set pointer if of limits
						if (pt_backward <= start) {
							pt_move = 2;
						} else if (pt_forward > end) {
							pt_move = 1;
						}
						// Move back and forth
						if (pt_move == 1) {
							launch(pt_backward);
							pt_backward--;
							pt_move = 2;
						} else if (pt_move == 2) {
							launch(pt_forward);
							pt_forward++;
							pt_move = 1;
						}
					}
					mPool.shutdown();
					if (!mPool.awaitTermination(3600L, TimeUnit.SECONDS)) {
						mPool.shutdownNow();
					}
				} catch (InterruptedException e) {
					Log.e(TAG, "Got Interrupted");
					Thread.currentThread().interrupt();
				}
			}
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(HostBean... host) {
		if (mDiscover != null) {
			final ActivityDiscovery discover = mDiscover.get();
			if (discover != null) {
				if (!isCancelled()) {
					discover.addHost(host[0]);
					discover.setProgress((int) (hosts_done * 10000 / size));
				}
			}
		}
	}

	@Override
	protected void onPostExecute(Void unused) {
		if (mDiscover != null) {
			final ActivityDiscovery discover = mDiscover.get();
			if (discover != null) {
				if (discover.prefs.getBoolean(Prefs.KEY_VIBRATE_FINISH,
						Prefs.DEFAULT_VIBRATE_FINISH) == true) {
					Vibrator v = (Vibrator) discover.getSystemService(Context.VIBRATOR_SERVICE);
					v.vibrate(ActivityDiscovery.VIBRATE);
				}
				discover.makeToast(R.string.discover_finished);
				discover.stopDiscovering();
			}
		}
	}

	@Override
	protected void onCancelled() {
		if (mDiscover != null) {
			final ActivityDiscovery discover = mDiscover.get();
			if (discover != null) {
				discover.makeToast(R.string.discover_canceled);
				discover.stopDiscovering();
			}
		}
		if (mPool != null) {
			mPool.shutdownNow();
		}
		super.onCancelled();
	}

	private void launch(long i) {
		mPool.execute(new CheckRunnable(NetInfo.getIpFromLongUnsigned(i)));
		// mPool.scheduleAtFixedRate(new
		// CheckRunnable(NetInfo.getIpFromLongUnsigned(i)), 50,
		// getRate(), TimeUnit.MILLISECONDS);
	}

	private int getRate() {
		if (doRateControl) {
			return mRateControl.rate;
		}

		if (mDiscover != null) {
			final ActivityDiscovery discover = mDiscover.get();
			if (discover != null) {
				return Integer.parseInt(discover.prefs.getString(Prefs.KEY_TIMEOUT_DISCOVER,
						Prefs.DEFAULT_TIMEOUT_DISCOVER));
			}
		}
		return 1;
	}

	private class CheckRunnable implements Runnable {
		private String host;

		CheckRunnable(String host) {
			this.host = host;
		}

		public void run() {
			try {
				InetAddress h = InetAddress.getByName(host);
				// Rate control check
				if (doRateControl && mRateControl.is_indicator_discovered
						&& hosts_done % mRateMult == 0) {
					mRateControl.adaptRate();
				}
				// Native InetAddress check
				if (h.isReachable(getRate())) {
					publish(host);
					if (doRateControl && !mRateControl.is_indicator_discovered) {
						mRateControl.indicator = new String[] { host };
						mRateControl.adaptRate();
					}
					return;
				}
				// Custom check
				int port;
				// TODO: Get ports from options
				if ((port = Reachable.isReachable(h, getRate())) > -1) {
					Log.v(TAG, "used Network.Reachable object, port=" + port);
					publish(host);
					// if (!mRateControl.isIndicatorDiscovered()) {
					// mRateControl.setIndicator(host,
					// String.valueOf(port));
					// mRateControl.adaptRate();
					// discover_rate = mRateControl.getRate();
					// }
					return;
				}
				publish((String) null);

			} catch (IOException e) {
				publish((String) null);
				Log.e(TAG, e.getMessage());
				// } catch (InterruptedException e) {
				// Log.i(TAG, "InterruptedException");
			}
		}
	}

	private void publish(String addr) {
		hosts_done++;

		if (addr == null) {
			return;
		}

		int rtt = getRate();
		HostBean host = new HostBean();
		host.ipAddress = addr;
		host.responseTime = rtt;
		Log.v(TAG, "rtt=" + rtt);

		if (mDiscover != null) {
			final ActivityDiscovery discover = mDiscover.get();
			if (discover != null) {
				// Mac Addr
				host.hardwareAddress = discover.mHardwareAddress.getHardwareAddress(addr);

				// NIC vendor
				try {
					host.nicVendor = discover.mHardwareAddress.getNicVendor(host.hardwareAddress);
				} catch (SQLiteDatabaseCorruptException e) {
					Log.e(TAG, e.getMessage());
					Editor edit = discover.prefs.edit();
					edit.putInt(Prefs.KEY_RESET_NICDB, Prefs.DEFAULT_RESET_NICDB);
					edit.commit();
				}

				// Is gateway ?
				if (discover.net.gatewayIp.equals(host.ipAddress)) {
					host.isGateway = 1;
				}

				// FQDN
				if (discover.prefs.getBoolean(Prefs.KEY_RESOLVE_NAME, Prefs.DEFAULT_RESOLVE_NAME) == true) {
					try {
						host.hostname = (InetAddress.getByName(addr)).getCanonicalHostName();
						// if (NbtAddress.getByName(addr).isActive()) {
						// UniAddress test = new UniAddress(addr);
						// if (test != null) {
						// try {
						// Log.i(TAG, "netbios=" +
						// test.getHostName().toString());
						// } catch (ClassCastException e) {
						// Log.e(TAG, e.getMessage());
						// e.printStackTrace();
						// }
						// }
						// }
					} catch (UnknownHostException e) {
					}
				}
			}
		}

		publishProgress(host);
	}
}
