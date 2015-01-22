package info.lamatricexiste.network;

import info.lamatricexiste.network.Network.NetInfo;
import info.lamatricexiste.network.Utils.Prefs;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.util.Log;

public abstract class ActivityNet extends Activity {

    private final String TAG = "NetState";
    private ConnectivityManager connMgr;

    protected final static String EXTRA_WIFI = "wifiDisabled";
    protected Context ctxt;
    protected SharedPreferences prefs = null;
    protected NetInfo net = null;
    protected String info_ip_str = "";
    protected String info_in_str = "";
    protected String info_mo_str = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctxt = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
        connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        net = new NetInfo(ctxt);
    }

    @Override
    public void onResume() {
        super.onResume();
        setButtons(true);
        // Listening for network events
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(receiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    protected abstract void setInfo();

    protected abstract void setButtons(boolean disable);

    protected abstract void cancelTasks();

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            info_ip_str = "";
            info_mo_str = "";

            // Wifi state
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                    int WifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
                    //Log.d(TAG, "WifiState=" + WifiState);
                    switch (WifiState) {
                        case WifiManager.WIFI_STATE_ENABLING:
                            info_in_str = getString(R.string.wifi_enabling);
                            break;
                        case WifiManager.WIFI_STATE_ENABLED:
                            info_in_str = getString(R.string.wifi_enabled);
                            break;
                        case WifiManager.WIFI_STATE_DISABLING:
                            info_in_str = getString(R.string.wifi_disabling);
                            break;
                        case WifiManager.WIFI_STATE_DISABLED:
                            info_in_str = getString(R.string.wifi_disabled);
                            break;
                        default:
                            info_in_str = getString(R.string.wifi_unknown);
                    }
                }

                if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION) && net.getWifiInfo()) {
                    SupplicantState sstate = net.getSupplicantState();
                    //Log.d(TAG, "SupplicantState=" + sstate);
                    if (sstate == SupplicantState.SCANNING) {
                        info_in_str = getString(R.string.wifi_scanning);
                    } else if (sstate == SupplicantState.ASSOCIATING) {
                        info_in_str = getString(R.string.wifi_associating,
                                (net.ssid != null ? net.ssid : (net.bssid != null ? net.bssid
                                        : net.macAddress)));
                    } else if (sstate == SupplicantState.COMPLETED) {
                        info_in_str = getString(R.string.wifi_dhcp, net.ssid);
                    }
                }
            }

            // 3G(connected) -> Wifi(connected)
            // Support Ethernet, with ConnectivityManager.TYPE_ETHER=3
            final NetworkInfo ni = connMgr.getActiveNetworkInfo();
            if (ni != null) {
                //Log.i(TAG, "NetworkState="+ni.getDetailedState());
                if (ni.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                    int type = ni.getType();
                    //Log.i(TAG, "NetworkType="+type);
                    if (type == ConnectivityManager.TYPE_WIFI) { // WIFI
                        net.getWifiInfo();
                        if (net.ssid != null) {
                            net.getIp();
                            info_ip_str = getString(R.string.net_ip, net.ip, net.cidr, net.intf);
                            info_in_str = getString(R.string.net_ssid, net.ssid);
                            info_mo_str = getString(R.string.net_mode, getString(
                                    R.string.net_mode_wifi, net.speed, WifiInfo.LINK_SPEED_UNITS));
                            setButtons(false);
                        }
                    } else if (type == ConnectivityManager.TYPE_MOBILE) { // 3G
                        if (prefs.getBoolean(Prefs.KEY_MOBILE, Prefs.DEFAULT_MOBILE)
                                || prefs.getString(Prefs.KEY_INTF, Prefs.DEFAULT_INTF) != null) {
                            net.getMobileInfo();
                            if (net.carrier != null) {
                                net.getIp();
                                info_ip_str = getString(R.string.net_ip, net.ip, net.cidr, net.intf);
                                info_in_str = getString(R.string.net_carrier, net.carrier);
                                info_mo_str = getString(R.string.net_mode,
                                        getString(R.string.net_mode_mobile));
                                setButtons(false);
                            }
                        }
                    } else if (type == 3 || type == 9) { // ETH
                        net.getIp();
                        info_ip_str = getString(R.string.net_ip, net.ip, net.cidr, net.intf);
                        info_in_str = "";
                        info_mo_str = getString(R.string.net_mode) + getString(R.string.net_mode_eth);
                        setButtons(false);
                        Log.i(TAG, "Ethernet connectivity detected!");
                    } else {
                        Log.i(TAG, "Connectivity unknown!");
                        info_mo_str = getString(R.string.net_mode)
                                + getString(R.string.net_mode_unknown);
                    }
                } else {
                    cancelTasks();
                }
            } else {
                cancelTasks();
            }

            // Always update network info
            setInfo();
        }
    };
}
