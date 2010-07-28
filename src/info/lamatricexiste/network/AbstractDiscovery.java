package info.lamatricexiste.network;

import info.lamatricexiste.network.Network.HostBean;
import info.lamatricexiste.network.Network.NetInfo;
import info.lamatricexiste.network.Utils.Prefs;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Vibrator;

public abstract class AbstractDiscovery extends AsyncTask<Void, HostBean, Void> {

    // private final String TAG = "DnsDiscovery";

    protected int hosts_done = 0;
    protected WeakReference<ActivityDiscovery> mDiscover;

    // TODO: Adaptiv value or changeable by Prefs
    protected long ip;
    protected long start;
    protected long end;
    protected long size;

    public AbstractDiscovery(ActivityDiscovery discover) {
        mDiscover = new WeakReference<ActivityDiscovery>(discover);
    }

    abstract protected Void doInBackground(Void... params);

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
