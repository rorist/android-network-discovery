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

public class DiscoveryUnicast extends AsyncTask<Void, String, Void> {

    private final String TAG = "DiscoveryUnicast";
    private final int TIMEOUT_REACH = 1000;
    private final int DISCOVER_RATE = 0;
    private int pt_move = 2; // 1=backward 2=forward
    private Reachable reachable;

    protected SharedPreferences prefsMgr;
    protected ExecutorService pool;
    protected long ip;
    protected long start;
    protected long end;
    protected int size = 0;

    protected Void doInBackground(Void... params) {
        Log.v(TAG, "start=" + NetInfo.getIpFromLongUnsigned(start) + " (" + start + "), end="
                + NetInfo.getIpFromLongUnsigned(end) + " (" + end + "), length=" + size);
        pool = Executors.newFixedThreadPool(Integer.parseInt(prefsMgr.getString(Prefs.KEY_NTHREADS,
                Prefs.DEFAULT_NTHREADS)));
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
        String ip = NetInfo.getIpFromLongUnsigned(i);
        pool.execute(new CheckRunnable(ip));
    }

    private class CheckRunnable implements Runnable {
        private String host;

        CheckRunnable(String host) {
            this.host = host;
        }

        public void run() {
            try {
                InetAddress h = InetAddress.getByName(host);
                // Native InetAddress check
                if (h.isReachable(TIMEOUT_REACH)) {
                    publishProgress(host);
                    return;
                }
                // Custom check
                if (reachable.isReachable(h)) {
                    publishProgress(host);
                    return;
                }
                publishProgress((String) null);

            } catch (IOException e) {
                publishProgress((String) null);
                Log.e(TAG, e.getMessage());
            }
        }
    }

    // private float getHostResponseTime(String host) {
    // try {
    // File ping = new File("/system/bin/ping");
    // if (ping.exists() == true) {
    // String line;
    // Matcher matcher;
    // Process p = Runtime.getRuntime().exec("ping -c 2 " + host);
    // BufferedReader r = new BufferedReader(new
    // InputStreamReader(p.getInputStream()), 1);
    // while ((line = r.readLine()) != null) {
    // matcher = Pattern
    // .compile(
    // "^rtt min\\/avg\\/max\\/mdev = ([0-9\\.]+)\\/[0-9\\.]+\\/[0-9\\.]+\\/[0-9\\.]+ ms$")
    // .matcher(line);
    // if (matcher.matches()) {
    // return Float.parseFloat(matcher.group(1));
    // }
    // }
    // }
    // } catch (Exception e) {
    // Log.e(TAG, "Can't use native ping: " + e.getMessage());
    // }
    // return 0;
    // }
}
