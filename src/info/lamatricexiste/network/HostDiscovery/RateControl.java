package info.lamatricexiste.network.HostDiscovery;

import java.io.InputStreamReader;
import android.util.Log;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.File;
import java.lang.Process;
import java.util.regex.Pattern;

public class RateControl {

    // TODO: Calculate a rounded up value from experiments in different networks
    // FIXME: calculate real overhead between java's ping and native ping (at
    // runtime?)
    private final String TAG = "RateControl";
    private final double RATE_BASE = 1000;
    private double rate = RATE_BASE; // Slow start
    private String[] indicator;
    private boolean indicator_discovered = false;

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public void setIndicator(String... indicator) {
        this.indicator = indicator;
    }

    public String[] getIndicator() {
        return indicator;
    }

    public boolean isIndicatorDiscovered() {
        return indicator_discovered;
    }

    public void adaptRate() {
        double response_time = 0;
        if (indicator.length > 1) {
            Log.v(TAG, "use a socket here, port=" + getIndicator()[1]);
        } else {
            indicator_discovered = true;
            if ((response_time = getAvgResponseTime(getIndicator()[0], 3)) > 0) {
                setRate(response_time);
                Log.v(TAG, "rate=" + response_time);
            }
        }
    }

    private double getAvgResponseTime(String host, int count) {
        try {
            File ping = new File("/system/bin/ping");
            if (ping.exists() == true) {
                String line;
                Matcher matcher;
                Process p = Runtime.getRuntime().exec("ping -q -n -W 2 -c " + count + " " + host);
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()), 1);
                while ((line = r.readLine()) != null) {
                    matcher = Pattern
                            .compile(
                                    "^rtt min\\/avg\\/max\\/mdev = [0-9\\.]+\\/([0-9\\.]+)\\/[0-9\\.]+\\/[0-9\\.]+ ms$")
                            .matcher(line);
                    if (matcher.matches()) {
                        return Float.parseFloat(matcher.group(1));
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't use native ping: " + e.getMessage());
        }
        return 0;
    }
}
