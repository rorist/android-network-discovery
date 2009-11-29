// http://standards.ieee.org/regauth/oui/oui.txt

package info.lamatricexiste.network;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class HardwareAddress {

	private final static String TAG = "HardwareAddress";
	private final static String DB_PATH = "/data/data/info.lamatricexiste.network/";
	private final static String DB_NAME = "oui.db";
	private Context ctxt;

	public HardwareAddress(Context context) {
		this.ctxt = context;
		if (!checkDataBase()) {
			copyDataBase();
		}
	}

	public SQLiteDatabase openDataBase() throws SQLException {
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
