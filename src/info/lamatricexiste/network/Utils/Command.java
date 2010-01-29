package info.lamatricexiste.network.Utils;

import android.util.Log;

public class Command {

    static {
        try {
            System.load("/data/data/info.lamatricexiste.network/lib/libcommand.so");
        } catch (UnsatisfiedLinkError e) {
            Log.e("Command", "Cannot load libcommand.so");
        }
    }

    public static native int runCommand(String cmd);
}
