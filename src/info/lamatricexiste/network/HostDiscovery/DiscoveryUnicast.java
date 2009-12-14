package info.lamatricexiste.network.HostDiscovery;

import info.lamatricexiste.network.Utils.NetInfo;
import info.lamatricexiste.network.Utils.Prefs;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

public class DiscoveryUnicast extends AsyncTask<Void, InetAddress, Void> {

    private final String TAG = "DiscoveryUnicast";
    private final int TIMEOUT_REACH = 1000;
    private final int DISCOVER_RATE = 0;
    private int pt_move = 2; // 1=backward 2=forward
    private Reachable reachable; // TODO: This is a test of reuse (less GC)

    protected SharedPreferences prefsMgr;
    protected ExecutorService pool;
    protected long ip;
    protected long start;
    protected long end;
    protected int size = 0;

    protected Void doInBackground(Void... params) {
        Log.v(TAG, "start=" + NetInfo.getIpFromLongInverted(start) + " ("
                + start + "), end=" + NetInfo.getIpFromLongInverted(end) + " ("
                + end + "), length=" + size);
        pool = Executors.newFixedThreadPool(Integer.parseInt(prefsMgr
                .getString(Prefs.KEY_NTHREADS, Prefs.DEFAULT_NTHREADS)));
        reachable = new Reachable();

        try {
            // gateway
            launch(start);

            // hosts
            long pt_backward = ip - 1;
            long pt_forward = ip + 1;
            int size_hosts = size - 2;

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

            pool.shutdown();
            pool.awaitTermination(3600L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Got Interrupted");
        }

        return null;
    }

    private void launch(long i) throws InterruptedException {
        Thread.sleep(DISCOVER_RATE);
        String ip = NetInfo.getIpFromLongInverted(i);
        CheckRunnable r = new CheckRunnable(ip);
        pool.execute(r);
    }

    private class CheckRunnable implements Runnable {
        private String host;

        CheckRunnable(String host) {
            this.host = host;
        }

        public void run() {
            try {
                InetAddress h = InetAddress.getByName(host);
                if (h.isReachable(TIMEOUT_REACH) || reachable.request(h)) {
                    publishProgress(h);
                } else {
                    publishProgress((InetAddress)null);
                }
            } catch (IOException e) {
                publishProgress((InetAddress)null);
                Log.e(TAG, e.getMessage());
            }
        }
    }
}
