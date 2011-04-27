/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network;

import info.lamatricexiste.network.Network.HardwareAddress;
import info.lamatricexiste.network.Network.HostBean;
import info.lamatricexiste.network.Network.NetInfo;
import info.lamatricexiste.network.Network.RateControl;
import info.lamatricexiste.network.Network.Reachable;
import info.lamatricexiste.network.Utils.Prefs;
import info.lamatricexiste.network.Utils.Save;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jcifs.netbios.NbtAddress;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.util.Log;

public class DefaultDiscovery extends AbstractDiscovery {

    private final String TAG = "DefaultDiscovery";
    private final static long TIMEOUT_SHUTDOWN = 10;
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
                        long pt_backward = ip;
                        long pt_forward = ip + 1;
                        long size_hosts = size - 1;

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
                    if (!mPool.awaitTermination(TIMEOUT_SHUTDOWN, TimeUnit.SECONDS)) {
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
                // Arp Check #1 //FIXME: need to do a req to the host before ?
                if(!NetInfo.NOMAC.equals(HardwareAddress.getHardwareAddress(host))){
                    Log.e(TAG, "found using arp #1 "+host);
                    publish(host);
                    return;
                }
                // Native InetAddress check
                if (h.isReachable(getRate())) {
                    Log.e(TAG, "found using InetAddress ping "+host);
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
                    Log.v(TAG, "used Network.Reachable object, "+host+" port=" + port);
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

    private void publish(final String addr) {
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
                host.hardwareAddress = HardwareAddress.getHardwareAddress(addr);

                // NIC vendor
                host.nicVendor = HardwareAddress.getNicVendor(host.hardwareAddress);

                // Is gateway ?
                if (discover.net.gatewayIp.equals(host.ipAddress)) {
                    host.deviceType = HostBean.TYPE_GATEWAY;
                }

                // FQDN
                // Static
                if ((host.hostname = Save.getCustomName(host.hardwareAddress)) == null) {
                    // DNS
                    if (discover.prefs.getBoolean(Prefs.KEY_RESOLVE_NAME,
                            Prefs.DEFAULT_RESOLVE_NAME) == true) {
                        try {
                            host.hostname = (InetAddress.getByName(addr)).getCanonicalHostName();
                        } catch (UnknownHostException e) {
                            Log.e(TAG, e.getMessage());
                        }
                    }
                    // TODO: NETBIOS
                    try {
                        host.hostname = NbtAddress.getByName(addr).getHostName();
                    } catch (UnknownHostException e) {
                        Log.i(TAG, e.getMessage());
                    }
                }
            }
        }

        publishProgress(host);
    }
}
