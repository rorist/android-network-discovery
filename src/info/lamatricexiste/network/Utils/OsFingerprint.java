package info.lamatricexiste.network.Utils;

import info.lamatricexiste.network.HostDiscovery.HostBean;
import android.content.Context;

public class OsFingerprint {

	private final static String TAG = "OsFingerprint";
    private HostBean host;
    private Context ctxt;

	public OsFingerprint(Context ctxt, HostBean host) {
		this.ctxt = ctxt;
        this.host = host;
	}
}
