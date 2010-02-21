// Inspired from AOSP in frameworks/base/services/java/com/android/server/Installer.java
// Nmap for ARM: http://rmccurdy.com/nmap.sh

package info.lamatricexiste.network.HostDiscovery;

import info.lamatricexiste.network.DiscoverActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

public class RootDiscovery extends AbstractDiscovery {

    private final String TAG = "RootDiscovery";
    private LocalSocket mSocket;
    private InputStream mIn;
    private OutputStream mOut;
    private byte buf[] = new byte[1024];

    // private int buflen = 0;

    public RootDiscovery(DiscoverActivity discover) {
        super(discover);
    }

    @Override
    protected Void doInBackground(Void... params) {
        /*
         * Wireless Interfaces: HTC Magic has tiwlan0 Nexus One has eth0
         */

        if (!connect()) {
            Log.e(TAG, "connection failed");
            return null;
        }
        if (!writeCommand("discover eth0 " + start + " " + end)) {
            Log.e(TAG, "write command failed!");
            return null;
        }
        // Log.i(TAG,"send: '"+cmd+"'");
        // if (readReply()) {
        // String s = new String(buf, 0, buflen);
        // // Log.i(TAG,"recv: '"+s+"'");
        // return s;
        // } else {
        // // Log.i(TAG,"fail");
        // return "-1";
        // }
        disconnect();

        return null;
    }

    protected void publish(String str) {
        publishProgress(str);
    }

    private boolean connect() {
        if (mSocket != null) {
            return true;
        }
        Log.i(TAG, "connecting...");
        try {
            mSocket = new LocalSocket();
            LocalSocketAddress address = new LocalSocketAddress("scand",
                    LocalSocketAddress.Namespace.RESERVED);
            mSocket.connect(address);
            mIn = mSocket.getInputStream();
            mOut = mSocket.getOutputStream();
        } catch (IOException ex) {
            disconnect();
            return false;
        }
        return true;
    }

    private void disconnect() {
        Log.i(TAG, "disconnecting...");
        try {
            if (mSocket != null)
                mSocket.close();
        } catch (IOException ex) {
        }
        try {
            if (mIn != null)
                mIn.close();
        } catch (IOException ex) {
        }
        try {
            if (mOut != null)
                mOut.close();
        } catch (IOException ex) {
        }
        mSocket = null;
        mIn = null;
        mOut = null;
    }

    private boolean writeCommand(String _cmd) {
        byte[] cmd = _cmd.getBytes();
        int len = cmd.length;
        if ((len < 1) || (len > 1024))
            return false;
        buf[0] = (byte) (len & 0xff);
        buf[1] = (byte) ((len >> 8) & 0xff);
        try {
            mOut.write(buf, 0, 2);
            mOut.write(cmd, 0, len);
        } catch (IOException ex) {
            Log.e(TAG, "write error");
            disconnect();
            return false;
        }
        return true;
    }

}
