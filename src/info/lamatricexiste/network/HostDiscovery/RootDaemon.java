package info.lamatricexiste.network.HostDiscovery;

import info.lamatricexiste.network.DiscoverActivity;
import info.lamatricexiste.network.R;
import info.lamatricexiste.network.Utils.Command;
import info.lamatricexiste.network.Utils.DownloadFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import android.util.Log;

public class RootDaemon {
    private final String TAG = "RootDaemon";
    private final String DAEMON = "scand";
    private WeakReference<DiscoverActivity> mDiscover;

    public RootDaemon(DiscoverActivity discover) {
        mDiscover = new WeakReference<DiscoverActivity>(discover);
    }

    public void start() {
        String dir = getDir();
        Log.d(TAG, "dir=" + dir); // FIXME: Remove me after debug
        if ((new File(dir + DAEMON)).exists() == false) {
            installDaemon(dir);
            restart();
            return;
        }
        startDaemon(dir);
    }

    public void killDaemon() {
        execute("su -c 'killall -9 " + DAEMON + "'");
    }

    private void installDaemon(String dir) {
        createDir(dir);
        copyFile(dir + DAEMON, R.raw.scand);
        execute("su -c 'chmod 755 " + dir + DAEMON + "; chown root " + dir + DAEMON + "'");
    }

    private void startDaemon(String dir) {
        execute("su -c " + dir + DAEMON);
    }

    private void restart() {
        final DiscoverActivity d = mDiscover.get();
        d.startActivity(d.getIntent());
        // d.finish();
    }

    private String getDir() {
        final DiscoverActivity d = mDiscover.get();
        return d.getFilesDir().getParent() + "/bin/";
    }

    private void createDir(String dirname) {
        File dir = new File(dirname);
        if (dir.exists() == false) {
            dir.mkdir();
        }
    }

    private void copyFile(String file, int resource) {
        final DiscoverActivity discover = mDiscover.get();
        try {
            InputStream in = discover.getResources().openRawResource(resource);
            OutputStream out = new FileOutputStream(file);
            final ReadableByteChannel inputChannel = Channels.newChannel(in);
            final WritableByteChannel outputChannel = Channels.newChannel(out);
            DownloadFile.fastChannelCopy(inputChannel, outputChannel);
        } catch (IOException e) {
            Log.e(TAG + ":copyFile", e.getMessage());
        }
    }

    private void execute(final String cmd) {
        int ret = Command.runCommand(cmd);
        Log.i(TAG, "cmd=" + cmd + ", ret=" + ret);
    }
    // private void execute(final String[] cmds) {
    // new Thread(new Runnable() {
    // public void run() {
    // try {
    // Process process = Runtime.getRuntime().exec("su");
    // DataOutputStream os = new DataOutputStream(process.getOutputStream());
    // for (String cmd : cmds) {
    // Log.v(TAG, "run=" + cmd);
    // os.writeBytes(cmd + "\n");
    // }
    // os.writeBytes("exit\n");
    // os.flush();
    // os.close();
    // process.waitFor();
    // } catch (IOException e) {
    // Log.e(TAG + ":execute", e.getMessage());
    // } catch (InterruptedException e) {
    // Log.e(TAG + ":execute", e.getMessage());
    // }
    // }
    // }).start();
    // }
}
