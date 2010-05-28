/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network;

import info.lamatricexiste.network.Network.NetInfo;
import info.lamatricexiste.network.Network.RateControl;
import info.lamatricexiste.network.Utils.Prefs;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Vibrator;

public abstract class AbstractDiscovery extends AsyncTask<Void, String, Void> {

    // private final String TAG = "AbstractDiscovery";
    private int hosts_done = 0;
    private WeakReference<ActivityDiscovery> mDiscover;
    protected RateControl mRateControl;

    // TODO: Adaptiv value or changeable by Prefs
    protected long ip;
    protected long start;
    protected long end;
    protected long size;

    public AbstractDiscovery(ActivityDiscovery discover) {
        mDiscover = new WeakReference<ActivityDiscovery>(discover);
        mRateControl = new RateControl();
    }

    abstract protected Void doInBackground(Void... params);

    abstract protected void publish(String str);

    @Override
    protected void onPreExecute() {
        final ActivityDiscovery discover = mDiscover.get();
        NetInfo net = new NetInfo(discover);
        ip = NetInfo.getUnsignedLongFromIp(net.ip);
        int shift = (32 - net.cidr);
        start = (ip >> shift << shift) + 1;
        end = (start | ((1 << shift) - 1)) - 1;
        size = (int) (end - start + 1);
        discover.setProgress(0);
    }

    @Override
    protected void onProgressUpdate(String... item) {
        final ActivityDiscovery discover = mDiscover.get();
        if(discover != null){
            if (!isCancelled()) {
                if (item[0] != null) {
                    discover.addHost(item[0], mRateControl.rate);
                }
                hosts_done++;
                discover.setProgress((int) (hosts_done * 10000 / size));
            }
        }
    }

    @Override
    protected void onPostExecute(Void unused) {
        final ActivityDiscovery discover = mDiscover.get();
        if (discover.prefs.getBoolean(Prefs.KEY_VIBRATE_FINISH, Prefs.DEFAULT_VIBRATE_FINISH) == true) {
            Vibrator v = (Vibrator) discover.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(ActivityDiscovery.VIBRATE);
        }
        discover.makeToast(R.string.discover_finished);
        discover.stopDiscovering();
    }

    @Override
    protected void onCancelled() {
        final ActivityDiscovery discover = mDiscover.get();
        discover.makeToast(R.string.discover_canceled);
        discover.stopDiscovering();
    }
}
