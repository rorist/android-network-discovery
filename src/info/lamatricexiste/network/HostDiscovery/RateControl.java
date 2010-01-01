package info.lamatricexiste.network.HostDiscovery;

import java.io.InputStreamReader;
import android.util.Log;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.File;
import java.lang.Process;
import java.util.regex.Pattern;

public class RateControl {

    private final String TAG = "RateControl";
    private final double RATE_BASE = 500;
    // TODO: Calculate a rounded up value from experiments in different networks
    // FIXME: calculate real overhead between java's ping and native ping (at
    // runtime?)
    private double rate_mult = 1.5; // Slow start
    private String[] indicator;
    private boolean indicator_discovered = false;

    public RateControl() {
    }

    public double getRate() {
        return (RATE_BASE * rate_mult);
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

    public double adaptRate() {
        double response_time = 0;
        if (indicator.length > 1) {
            Log.v(TAG, "use a socket here, port=" + getIndicator()[1]);
        } else {
            if ((response_time = getAvgResponseTime(getIndicator()[0], 5)) > 0) {
                setRateMult(rate_mult * response_time / RATE_BASE); // TODO: is
                                                                    // it
                                                                    // accurate
                                                                    // in all
                                                                    // situations
                                                                    // ?
                Log.v(TAG, "rate=" + getRate() + ", ping=" + response_time);
                // indicator_discovered = true;
            }
        }
        return getRate();
    }

    private void setRateMult(double rate_mult) {
        this.rate_mult = rate_mult;
    }

    private double getAvgResponseTime(String host, int count) {
        try {
            File ping = new File("/system/bin/ping");
            if (ping.exists() == true) {
                String line;
                Matcher matcher;
                Process p = Runtime.getRuntime().exec("ping -c " + count + " " + host);
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
