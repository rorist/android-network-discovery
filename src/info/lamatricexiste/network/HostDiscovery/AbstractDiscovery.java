package info.lamatricexiste.network.HostDiscovery;

import info.lamatricexiste.network.DiscoverActivity;
import info.lamatricexiste.network.R;
import info.lamatricexiste.network.Utils.NetInfo;
import info.lamatricexiste.network.Utils.Prefs;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Vibrator;

public abstract class AbstractDiscovery extends AsyncTask<Void, String, Void> {

    // private final String TAG = "AbstractDiscovery";
    private int hosts_done = 0;
    private WeakReference<DiscoverActivity> mDiscover;
    protected RateControl mRateControl;

    // TODO: Adaptiv value or changeable by Prefs
    protected long ip;
    protected long start;
    protected long end;
    protected long size = 0;

    public AbstractDiscovery(DiscoverActivity discover) {
        mDiscover = new WeakReference<DiscoverActivity>(discover);
        mRateControl = new RateControl();
    }

    abstract protected Void doInBackground(Void... params);

    abstract protected void publish(String str);

    @Override
    protected void onPreExecute() {
        final DiscoverActivity discover = mDiscover.get();
        NetInfo net = new NetInfo(discover);
        ip = NetInfo.getUnsignedLongFromIp(net.getIp());
        int shift = (32 - net.getNetCidr());
        start = (ip >> shift << shift) + 1;
        end = (start | ((1 << shift) - 1)) - 1;
        size = (int) (end - start + 1);
        discover.setProgress(0);
    }

    @Override
    protected void onProgressUpdate(String... item) {
        final DiscoverActivity discover = mDiscover.get();
        if (!isCancelled()) {
            if (item[0] != null) {
                discover.addHost(item[0], mRateControl.getRate());
            }
            hosts_done++;
            discover.setProgress((int) (hosts_done * 10000 / size));
        }
    }

    @Override
    protected void onPostExecute(Void unused) {
        final DiscoverActivity discover = mDiscover.get();
        if (discover.prefs.getBoolean(Prefs.KEY_VIBRATE_FINISH, Prefs.DEFAULT_VIBRATE_FINISH) == true) {
            Vibrator v = (Vibrator) discover.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(DiscoverActivity.VIBRATE);
        }
        discover.makeToast(R.string.discover_finished);
        discover.stopDiscovering();
    }

    @Override
    protected void onCancelled() {
        final DiscoverActivity discover = mDiscover.get();
        discover.makeToast(R.string.discover_canceled);
        discover.stopDiscovering();
    }
}
