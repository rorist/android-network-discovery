package info.lamatricexiste.network.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import android.content.Context;
import android.util.Log;

public class NativeTask {
    private final String TAG = "NativeTask";
    private final String DAEMON = "scand";
    private final String APP_PATH = "/data/data/info.lamatricexiste.network/";
    private Context ctxt;

    public NativeTask(Context ctxt) {
        this.ctxt = ctxt;
    }

    public void install() {
        copyFile(DAEMON);
        setPermissions(DAEMON);
        execute();
    }

    private void copyFile(String filename) {
        try {
            InputStream in = ctxt.getAssets().open(filename);
            OutputStream out = new FileOutputStream(APP_PATH + filename);
            final ReadableByteChannel inputChannel = Channels.newChannel(in);
            final WritableByteChannel outputChannel = Channels.newChannel(out);
            DownloadFile.fastChannelCopy(inputChannel, outputChannel);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void setPermissions(String filename) {
        try {
            Runtime.getRuntime().exec("chmod 777 " + APP_PATH + filename);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void execute() {
        try {
            Runtime.getRuntime().exec("su -c " + APP_PATH + DAEMON);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    // Found here: http://code.google.com/p/market-enabler/wiki/ShellCommands
    // private List<String> execute(String[] commands) {
    // List<String> res = null;
    // try {
    // Process process = Runtime.getRuntime().exec("su");
    // DataOutputStream os = new DataOutputStream(process.getOutputStream());
    // DataInputStream osRes = new DataInputStream(process.getInputStream());
    // for (String single : commands) {
    // os.writeBytes(single + "\n");
    // os.flush();
    // res.add(osRes.readLine());
    // }
    // os.writeBytes("exit\n");
    // os.flush();
    // process.waitFor();
    // } catch (IOException e) {
    // } catch (InterruptedException e) {
    // }
    // return res;
    // }
}
