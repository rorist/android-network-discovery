/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network;

import info.lamatricexiste.network.Network.HostBean;
import info.lamatricexiste.network.Network.NetInfo;
import info.lamatricexiste.network.Network.RateControl;
import info.lamatricexiste.network.Utils.Prefs;

import java.lang.ref.WeakReference;
import java.net.InetAddress;

import android.content.Context;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.util.Log;

public class DnsDiscovery extends AsyncTask<Void, HostBean, Void> {

    private final String TAG = "DnsDiscovery";
    // Checks
    private int hosts_done = 0;
    private boolean doRateControl;
    private WeakReference<ActivityDiscovery> mDiscover;

    // TODO: Adaptiv value or changeable by Prefs
    protected long ip;
    protected long start;
    protected long end;
    protected long size;

    public DnsDiscovery(ActivityDiscovery discover) {
        mDiscover = new WeakReference<ActivityDiscovery>(discover);
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
                
                for(long i = start; i<end+1; i++){
                    hosts_done++;
                    
                    HostBean host = new HostBean();
                    host.ipAddress = NetInfo.getIpFromLongUnsigned(i);
                    try {
                        host.hostname = (InetAddress.getByName(host.ipAddress)).getCanonicalHostName();
                    } catch (java.net.UnknownHostException e) {
                        Log.e(TAG, e.getMessage());
                    }
                    if(host.hostname!= null && !host.hostname.equals(host.ipAddress)){

                        // Is gateway ?
                        if (discover.net.gatewayIp.equals(host.ipAddress)) {
                            host.isGateway = 1;
                            
                            // Mac Addr
                            host.hardwareAddress = discover.mHardwareAddress.getHardwareAddress(host.ipAddress);

                            // NIC vendor
                            try {
                                host.nicVendor = discover.mHardwareAddress.getNicVendor(host.hardwareAddress);
                            } catch (SQLiteDatabaseCorruptException e) {
                                Log.e(TAG, e.getMessage());
                            }
                        }
                        
                        publishProgress(host);
                    }
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
                    if (size > 0) {
                        discover.setProgress((int) (hosts_done * 10000 / size));
                    }
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
        super.onCancelled();
    }
}
