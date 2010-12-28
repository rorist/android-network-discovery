/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

package info.lamatricexiste.network;

import info.lamatricexiste.network.Network.NetInfo;
import android.util.Log;

public class AsyncDiscovery extends AbstractDiscovery {

    private final String TAG = "AsyncDiscovery";

    // private boolean doRateControl;
    // private RateControl mRateControl;

    public AsyncDiscovery(ActivityDiscovery discover) {
        super(discover);
        // mRateControl = new RateControl();
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (mDiscover != null) {
            final ActivityDiscovery discover = mDiscover.get();
            if (discover != null) {
                Log.v(TAG, "start=" + NetInfo.getIpFromLongUnsigned(start) + " (" + start
                        + "), end=" + NetInfo.getIpFromLongUnsigned(end) + " (" + end
                        + "), length=" + size);

            }
        }
        return null;
    }
}
