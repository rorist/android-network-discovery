package info.lamatricexiste.network.Utils;

import android.util.Log;

public class NativeTask {
    static {
        try {
            Log.i("NativeTask", "Trying to load libscan.so");
            System.loadLibrary("scan");
        }
        catch (UnsatisfiedLinkError ule) {
            Log.e("NativeTask", "WARNING: Could not load libscan.so");
        }
    }
    public static native void scan();
}
