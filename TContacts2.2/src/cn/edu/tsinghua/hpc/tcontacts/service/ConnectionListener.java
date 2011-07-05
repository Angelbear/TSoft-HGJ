package cn.edu.tsinghua.hpc.tcontacts.service;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class ConnectionListener extends PhoneStateListener {
	private Context context;
	private boolean isConnected;

	public boolean isConnected() {
		WifiManager mWifiMgr = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		return mWifiMgr.isWifiEnabled() || isConnected; // || isConnecteds
	}

	public ConnectionListener(Context context) {
		isConnected = false;
		this.context = context;
	}

	@Override
	public void onDataConnectionStateChanged(int state) {
		switch (state) {
		case TelephonyManager.DATA_CONNECTED:
			isConnected = true;
			break;
		case TelephonyManager.DATA_DISCONNECTED:
			isConnected = false;
			break;
		default:
			break;
		}
	}
}
