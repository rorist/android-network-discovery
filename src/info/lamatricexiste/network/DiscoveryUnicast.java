package info.lamatricexiste.network;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import android.os.AsyncTask;
import android.util.Log;

public class DiscoveryUnicast extends AsyncTask<Void, String, Void> {

    private final String TAG = "DiscoveryUnicast";
    private final int TIMEOUT_REACH = 1000;
    private final int nTHREADS = 64;
    private int pt_forward;
    private int pt_backward;
    private int pt_move = 2; // 1=backward 2=forward

    protected ExecutorService pool;
    protected int ip;
    protected int start;
    protected int end;
    protected int size = 0;

    protected Void doInBackground(Void... params) {
        Log.v(TAG, "start=" + NetInfo.getIpFromIntInverted(start) + " ("
                + start + "), end=" + NetInfo.getIpFromIntInverted(end) + " ("
                + end + "), length=" + size);
        pool = Executors.newFixedThreadPool(nTHREADS);

        // gateway
        launch(start);

        // hosts
        pt_backward = ip - 1;
        pt_forward = ip + 1;
        for (int i = 0; i < size - 2; i++) {
            if (pt_move == 1) {
                if (pt_backward > start) {
                    launch(pt_backward);
                    pt_backward--;
                }
                pt_move = 2;
            } else if (pt_move == 2) {
                if (pt_forward <= end) {
                    launch(pt_forward);
                    pt_forward++;
                }
                pt_move = 1;
            }
        }

        pool.shutdown();
        try {
            pool.awaitTermination(3600L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Got Interrupted");
        }

        return null;
    }

    private void launch(int i) {
        String ip = NetInfo.getIpFromIntInverted(i);
        CheckRunnable r = new CheckRunnable(ip);
        pool.execute(r);
    }

    private class CheckRunnable implements Runnable {
        String host;

        CheckRunnable(String host) {
            this.host = host;
        }

        public void run() {
            Reachable r = new Reachable();
            try {
                InetAddress h = InetAddress.getByName(host);
                if (h.isReachable(TIMEOUT_REACH) || r.request(h)) {
                    publishProgress(host);
                } else {
                    publishProgress(new String());
                }
            } catch (IOException e) {
                publishProgress(new String());
                Log.e(TAG, e.getMessage());
            }
        }
    }

}
