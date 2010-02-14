package info.lamatricexiste.network.HostDiscovery;

import java.io.File;

import android.util.Log;

public abstract class AbstractRoot {
    
    private final static String TAG = "AbstractRoot";
    private final static String BIN = "/system/bin/su";

    public static boolean checkRoot() {
        try {
            File su = new File(BIN);
            if (su.exists() == false) {
                Log.d(TAG, "Can't obtain root: No su binary found");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Can't obtain root: " + e.getMessage());
            return false;
        }
        return true;
    }
}
