package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.DiscoverActivity;
import info.lamatricexiste.network.R;

import java.io.DataOutputStream;
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
        String[] cmds = { "chmod 755 " + file, "chown root " + file, file };
        try {
            execute(cmds);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
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

    void execute(String[] cmds) throws Exception {
        Process process = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(process.getOutputStream());
        for (String cmd : cmds) {
            Log.v(TAG, "run=" + cmd);
            os.writeBytes(cmd + "\n");
        }
        os.writeBytes("exit\n");
        os.flush();
        os.close();
        process.waitFor();
    }
}
