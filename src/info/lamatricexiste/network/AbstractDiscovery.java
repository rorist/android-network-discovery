package info.lamatricexiste.network;

import info.lamatricexiste.network.Network.HostBean;
import info.lamatricexiste.network.Utils.Prefs;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Vibrator;

public abstract class AbstractDiscovery extends AsyncTask<Void, HostBean, Void> {

    //private final String TAG = "AbstractDiscovery";

    protected int hosts_done = 0;
    final protected WeakReference<ActivityDiscovery> mDiscover;

    protected long ip;
    protected long start = 0;
    protected long end = 0;
    protected long size = 0;

    public AbstractDiscovery(ActivityDiscovery discover) {
        mDiscover = new WeakReference<ActivityDiscovery>(discover);
    }

    public void setNetwork(long ip, long start, long end) {
        this.ip = ip;
        this.start = start;
        this.end = end;
    }

    abstract protected Void doInBackground(Void... params);

    @Override
    protected void onPreExecute() {
        size = (int) (end - start + 1);
        if (mDiscover != null) {
            final ActivityDiscovery discover = mDiscover.get();
            if (discover != null) {
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
                    if (host[0] != null) {
                        discover.addHost(host[0]);
                    }
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
