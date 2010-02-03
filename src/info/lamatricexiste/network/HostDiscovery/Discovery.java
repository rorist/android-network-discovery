package info.lamatricexiste.network.HostDiscovery;

import info.lamatricexiste.network.DiscoverActivity;
import info.lamatricexiste.network.R;
import info.lamatricexiste.network.Utils.NetInfo;
import info.lamatricexiste.network.Utils.Prefs;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Vibrator;
import android.util.Log;

public class Discovery extends AbstractDiscovery {

    private final String TAG = "Discovery";
    private final int TIMEOUT_REACH = 1000;
    private final int mRateMult = 50; // Number of hosts between Rate Checks
    private int mRateCnt = 0;
    private int pt_move = 2; // 1=backward 2=forward
    private Reachable mReachable;
    private ExecutorService mPool;
    private SharedPreferences mPrefsMgr;

    public Discovery(DiscoverActivity discover) {
        super(discover);
        mReachable = new Reachable();
        mPrefsMgr = discover.prefs;
    }

    protected void publish(String str) {
        publishProgress(str);
        mRateCnt++;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Log.v(TAG, "start=" + NetInfo.getIpFromLongUnsigned(start) + " (" + start + "), end="
                + NetInfo.getIpFromLongUnsigned(end) + " (" + end + "), length=" + size);
        mPool = Executors.newFixedThreadPool(Integer.parseInt(mPrefsMgr.getString(
                Prefs.KEY_NTHREADS, Prefs.DEFAULT_NTHREADS)));

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
            mPool.awaitTermination(3600L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Got Interrupted");
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        mPool.shutdownNow();
        super.onCancelled();
    }

    private void launch(long i) {
        mPool.execute(new CheckRunnable(NetInfo.getIpFromLongUnsigned(i)));
    }

    private class CheckRunnable implements Runnable {
        private String host;

        CheckRunnable(String host) {
            this.host = host;
        }

        public void run() {
            try {
                Thread.sleep((int) mRateControl.getRate());
                InetAddress h = InetAddress.getByName(host);
                // Rate control check
                if (mRateControl.isIndicatorDiscovered() && mRateCnt % mRateMult == 0) {
                    mRateControl.adaptRate();
                }
                // Native InetAddress check
                if (h.isReachable(TIMEOUT_REACH)) {
                    publish(host);
                    if (!mRateControl.isIndicatorDiscovered()) {
                        mRateControl.setIndicator(host);
                        mRateControl.adaptRate();
                    }
                    return;
                }
                // Custom check
                int port = -1;
                if ((port = mReachable.isReachable(h)) > -1) {
                    Log.v(TAG, "used Reachable object, port=" + port);
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
            } catch (InterruptedException e) {
            }
        }
    }
}
