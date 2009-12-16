package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.Utils.DownloadFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.util.Log;

public class UpdateNicDb {

    private final static String TAG = "UpdateNicDb";
    private final static String DB_REMOTE = "http://download.lamatricexiste.info/oui.db";
    private final static String DB_PATH = "/data/data/info.lamatricexiste.network/";
    private final static String DB_NAME = "oui.db";

    public static void remoteCopy() throws IOException {
        Log.v(TAG, "Copying oui.db remotly");
        new DownloadFile(DB_REMOTE, DB_PATH + DB_NAME);
    }

    public static void localCopy(Context ctxt) {
        Log.v(TAG, "Copying oui.db locally");
        try {
            InputStream myInput = ctxt.getAssets().open(DB_NAME);
            OutputStream myOutput = new FileOutputStream(DB_PATH + DB_NAME);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }
            myOutput.flush();
            myOutput.close();
            myInput.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

}
