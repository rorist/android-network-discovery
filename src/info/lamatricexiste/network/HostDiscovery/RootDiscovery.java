package info.lamatricexiste.network.HostDiscovery;

import info.lamatricexiste.network.DiscoverActivity;

public class RootDiscovery extends AbstractDiscovery {

    // private final String TAG = "RootDiscovery";

    public RootDiscovery(DiscoverActivity discover) {
        super(discover);
    }

    @Override
    protected Void doInBackground(Void... params) {
        return null;
    }

    protected void publish(String str) {
        publishProgress(str);
    }
}
