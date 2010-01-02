// http://standards.ieee.org/regauth/oui/oui.txt

package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class HardwareAddress {

    private final String TAG = "HardwareAddress";
    private final String DB_PATH = "/data/data/info.lamatricexiste.network/";
    private final String DB_NAME = "oui.db";

    public String getHardwareAddress(String ip) {
        String hw = "00:00:00:00:00:00";
        try {
            FileReader fileReader = new FileReader(new File("/proc/net/arp"));
            String ptrn = "^" + ip.replace(".", "\\.")
                    + "\\s+0x1\\s+0x2\\s+([:0-9a-fA-F]+)\\s+\\*\\s+(tiwlan0|eth0)$";
            Pattern pattern = Pattern.compile(ptrn);
            BufferedReader bufferedReader = new BufferedReader(fileReader, 16);
            String line;
            Matcher matcher;
            while ((line = bufferedReader.readLine()) != null) {
                matcher = pattern.matcher(line);
                if (matcher.matches()) {
                    hw = matcher.group(1);
                }
            }
            bufferedReader.close();
            fileReader.close();
        } catch (IOException e) {
            Log.d(TAG, "Can't open file ARP: " + e.getMessage());
        }
        return hw;
    }

    public String getNicVendor(Context ctxt, String hw) {
        String ni = ctxt.getString(R.string.info_unknown);
        SQLiteDatabase db = SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null,
                SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        String macid = hw.replace(":", "").substring(0, 6).toUpperCase();
        // Db request
        Cursor c = db.rawQuery("select vendor from oui where mac='" + macid + "'", null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            ni = c.getString(c.getColumnIndex("vendor"));
        }
        c.close();
        db.close();
        return ni;
    }
}
