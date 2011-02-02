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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.util.Log;

public class DefaultDiscovery extends AbstractDiscovery {

    private final String TAG = "DefaultDiscovery";
    private final int mRateMult = 5; // Number of alive hosts between Rate
    private int pt_move = 2; // 1=backward 2=forward
    private ExecutorService mPool;
    private boolean doRateControl;
    private RateControl mRateControl;

    public DefaultDiscovery(ActivityDiscovery discover) {
        super(discover);
        mRateControl = new RateControl();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (mDiscover != null) {
            final ActivityDiscovery discover = mDiscover.get();
            if (discover != null) {
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

                try {
                    if (ip <= end && ip >= start) {
                        Log.i(TAG, "Back and forth scanning");
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
                    } else {
                        Log.i(TAG, "Sequencial scanning");
                        for (long i = start; i <= end; i++) {
                            launch(i);
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
    protected void onCancelled() {
        if (mPool != null) {
            mPool.shutdownNow();
        }
        super.onCancelled();
    }

    private void launch(long i) {
        mPool.execute(new CheckRunnable(NetInfo.getIpFromLongUnsigned(i)));
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
                if (doRateControl && mRateControl.indicator != null && hosts_done % mRateMult == 0) {
                    mRateControl.adaptRate();
                }
                // Native InetAddress check
                if (h.isReachable(getRate())) {
                    publish(host);
                    // Set indicator and get a rate
                    if (doRateControl && mRateControl.indicator == null) {
                        mRateControl.indicator = host;
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
                    return;
                }
                publish((String) null);

            } catch (IOException e) {
                publish((String) null);
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private void publish(String addr) {
        hosts_done++;

        if (addr == null) {
            publishProgress((HostBean) null);
            return;
        }

        HostBean host = new HostBean();
        host.ipAddress = addr;
        host.responseTime = getRate();

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
