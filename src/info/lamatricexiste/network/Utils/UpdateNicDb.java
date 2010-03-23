package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.R;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class UpdateNicDb extends AsyncTask<Void, String, Void> {

    private final static String TAG = "UpdateNicDb";
    private final static String DB_REMOTE = "http://download.lamatricexiste.info/oui.db";
    private final static String DB_PATH = "/data/data/info.lamatricexiste.network/";
    private final static String DB_NAME = "oui.db";

    private Context ctxt;
    private SharedPreferences prefs;
    private int nb;

    public UpdateNicDb(final Context ctxt, final SharedPreferences prefs) {
        // TODO: Use weak Reference
        this.ctxt = ctxt;
        this.prefs = prefs;
        final AlertDialog.Builder dialog = new AlertDialog.Builder(ctxt);
        dialog.setTitle(R.string.preferences_resetdb_action);
        dialog.setPositiveButton(R.string.btn_yes, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                execute();
            }
        });
        dialog.setNegativeButton(R.string.btn_no, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                cancel(true);
            }
        });
        dialog.show();
    }

    @Override
    protected void onPreExecute() {
        ((Activity) ctxt).setProgressBarIndeterminateVisibility(true);
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            nb = countEntries();
            remoteCopy(ctxt);
        } catch (IOException e) {
            cancel(true);
        }
        return null;
    }

    private void remoteCopy(Context ctxt) throws IOException {
        Log.v(TAG, "Copying oui.db remotly");
        if (isConnected(ctxt)) {
            new DownloadFile(DB_REMOTE, DB_PATH + DB_NAME);
        }
    }

    private int countEntries() {
        int nb = 0;
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null,
                    SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            Cursor c = db.rawQuery("select count(mac) from oui", null);
            if (c.getCount() > 0) {
                c.moveToFirst();
                nb = c.getInt(0);
            }
            c.close();
            db.close();
        } catch (SQLiteException e) {
            return nb;
        }
        return nb;
    }

    private boolean isConnected(Context ctxt) {
        // TODO: Move to NetInfo and factorize with DiscoveryActivity
        NetworkInfo nfo = ((ConnectivityManager) ctxt
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (nfo != null) {
            return nfo.isConnected();
        }
        return false;
    }

    @Override
    protected void onPostExecute(Void unused) {
        ((Activity) ctxt).setProgressBarIndeterminateVisibility(false);
        Toast.makeText(
                ctxt,
                String.format(ctxt.getString(R.string.preferences_resetdb_ok),
                        (countEntries() - nb)), Toast.LENGTH_LONG).show();
        try {
            Editor edit = prefs.edit();
            edit.putInt(Prefs.KEY_RESETDB, ctxt.getPackageManager().getPackageInfo(
                    "info.lamatricexiste.network", 0).versionCode);
            edit.commit();
        } catch (NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    @Override
    protected void onCancelled() {
        Toast.makeText(ctxt, R.string.preferences_error3, Toast.LENGTH_SHORT).show();
    }
}
