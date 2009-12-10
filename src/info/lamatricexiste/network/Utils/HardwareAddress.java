// http://standards.ieee.org/regauth/oui/oui.txt

package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class HardwareAddress {

	private final static String TAG = "HardwareAddress";
	private final String DB_PATH = "/data/data/info.lamatricexiste.network/";
	private final String DB_NAME = "oui.db";
	private Context ctxt;

	public HardwareAddress(Context context) {
		this.ctxt = context;
	}

	public static String getHardwareAddress(String ip) {
		String hw = "00:00:00:00:00:00";
		try {
			File arp = new File("/proc/net/arp");
			if (arp.exists() != false && arp.canRead()) {
				String ptrn = "^"
						+ ip.replace(".", "\\.")
						+ "\\s+0x1\\s+0x2\\s+([:0-9a-fA-F]+)\\s+\\*\\s+(tiwlan0|eth0)$";
				Pattern pattern = Pattern.compile(ptrn);
				FileReader fileReader = new FileReader(arp);
				BufferedReader bufferedReader = new BufferedReader(fileReader,
						16);
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
		return hw;
	}

	public String getNicVendor(String mac) {
		String vendor = ctxt.getString(R.string.info_nic_unknown);
		SQLiteDatabase db = openDataBase();
		String macid = mac.replace(":", "").substring(0, 6).toUpperCase();
		// Db request
		Cursor c = db.rawQuery("select vendor from oui where mac='" + macid
				+ "'", null);
		if (c.getCount() > 0) {
			c.moveToFirst();
			vendor = c.getString(c.getColumnIndex("vendor"));
		}
		c.close();
		db.close();
		return vendor;
	}

	/**
	 * MAC Database
	 */

	public SQLiteDatabase openDataBase() throws SQLException {
		if (!checkDataBase()) {
			copyDataBase();
		}
		return SQLiteDatabase.openDatabase(DB_PATH + DB_NAME, null,
				SQLiteDatabase.NO_LOCALIZED_COLLATORS);
	}

	private boolean checkDataBase() {
		try {
			File dbfile = new File(DB_PATH + DB_NAME);
			if (dbfile.exists() == true) {
				return true;
			}
		} catch (Exception e) {
			Log.d(TAG, "oui.db does not exist");
			return false;
		}
		return false;
	}

	private void copyDataBase() {
		Log.v(TAG, "Creating oui.db");
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
