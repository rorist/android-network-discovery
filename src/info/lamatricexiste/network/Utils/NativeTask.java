package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.DiscoverActivity;
import info.lamatricexiste.network.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import android.os.AsyncTask;
import android.util.Log;

public class NativeTask extends AsyncTask<Void, Void, Void> {
    private final String TAG = "NativeTask";
    private final String DAEMON = "scand";
    private String path;
    private WeakReference<DiscoverActivity> mDiscover;

    public static native int runCommand(String command);

    public NativeTask(DiscoverActivity discover) {
        mDiscover = new WeakReference<DiscoverActivity>(discover);
        final DiscoverActivity d = mDiscover.get();
        path = d.getFilesDir().getParent() + "/bin/";

        try {
            System.loadLibrary("command");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    protected void onPreExecute() {
        createDir(path);
        copyFile(path + DAEMON, R.raw.scand);
        // setPermissions(path + DAEMON);
    }

    @Override
    protected Void doInBackground(Void... params) {
        String file = path + DAEMON;
        Log.d(TAG, "command returned: " + runCommand(file));
        // String[] cmds = { "chmod 755 " + file, "chown root " + file, file };
        // execute(cmds);
        return null;
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

    // private void setPermissions(String file) {
    // try {
    // Runtime.getRuntime().exec("chmod 0755 " + file);
    // } catch (IOException e) {
    // Log.e(TAG + ":setPermissions", e.getMessage());
    // }
    // }

    // private void execute(String command) {
    // try {
    // Runtime.getRuntime().exec(command);
    // } catch (SecurityException e) {
    // Log.e(TAG + ":execute", e.getMessage());
    // } catch (IOException e) {
    // Log.e(TAG + "execute", e.getMessage());
    // }
    // // Check if the process is running fine
    // }

    // Found here: http://code.google.com/p/market-enabler/wiki/ShellCommands
    // private List<String> execute(String[] commands) {
    // List<String> res = new ArrayList<String>();
    // try {
    // Process process = Runtime.getRuntime().exec("su");
    // DataOutputStream os = new DataOutputStream(process.getOutputStream());
    // DataInputStream osRes = new DataInputStream(process.getInputStream());
    // for (String single : commands) {
    // os.writeBytes(single + "\n");
    // os.flush();
    // res.add(osRes.readLine());
    // Log.i(TAG, res.get(res.size()));
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
