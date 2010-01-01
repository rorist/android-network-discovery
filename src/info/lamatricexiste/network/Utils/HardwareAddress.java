// http://standards.ieee.org/regauth/oui/oui.txt

package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class HardwareAddress {

    private final static String TAG = "HardwareAddress";
    private final String DB_PATH = "/data/data/info.lamatricexiste.network/";
    private final String DB_NAME = "oui.db";
    private String hw;
    private String ni;

    public HardwareAddress(Context ctxt, String addr) {
        hw = "00:00:00:00:00:00";
        ni = ctxt.getString(R.string.info_unknown);
        setHardwareAddress(addr);
        setNicVendor();
    }

    public String getHardwareAddress() {
        return hw;
    }

    public String getNicVendor() {
        return ni;
    }

    public void setHardwareAddress(String ip) {
        try {
            File arp = new File("/proc/net/arp");
            if (arp.exists() != false && arp.canRead()) {
                String ptrn = "^" + ip.replace(".", "\\.")
                        + "\\s+0x1\\s+0x2\\s+([:0-9a-fA-F]+)\\s+\\*\\s+(tiwlan0|eth0)$";
                Pattern pattern = Pattern.compile(ptrn);
                FileReader fileReader = new FileReader(arp);
                BufferedReader bufferedReader = new BufferedReader(fileReader, 16);
                String line;
                Matcher matcher;
                while ((line = bufferedReader.readLine()) != null) {
                    matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        hw = matcher.group(1);
                        break;
                    }
                }
                bufferedReader.close();
                fileReader.close();
            }
        } catch (Exception e) {
            Log.d(TAG, "Can't open file ARP: " + e.getMessage());
        }
    }

    public void setNicVendor() {
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
    }
}
