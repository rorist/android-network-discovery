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

import android.content.Intent;
import android.util.Log;

public class RootDaemon {
    private final String TAG = "RootDaemon";
    private final String DAEMON = "scand";
    private WeakReference<DiscoverActivity> mDiscover;

    public RootDaemon(DiscoverActivity discover) {
        mDiscover = new WeakReference<DiscoverActivity>(discover);
    }

    public void start() {
        if ((new File(getDir() + DAEMON)).exists() == true) {
            installDaemon();
            restart();
            // return;
        }
        startDaemon();
    }

    public void killDaemon() {
        execute("su -c 'killall -9 " + DAEMON + "'");
    }

    private void installDaemon() {
        createDir(getDir());
        copyFile(getDir() + DAEMON, R.raw.scand);
        execute("su -c 'chmod 755 " + getDir() + DAEMON + "'");
        execute("su -c 'chown root " + getDir() + DAEMON + "'");
    }

    private void startDaemon() {
        execute("su -c " + getDir() + DAEMON);
    }

    private void restart() {
        final DiscoverActivity d = mDiscover.get();
        Intent intent = d.getIntent();
        d.startActivity(intent);
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
