package com.softwinner.pppoe;

import java.util.List;
import java.util.regex.Pattern;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.ethernet.EthernetDevInfo;
import android.net.ethernet.EthernetManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import android.os.SystemProperties;

public class Loader extends BroadcastReceiver {

	private static final boolean DEBUG = true;
	private static final String TAG = "PPPoE.Loader";
	public static final String ACTION_STATE_CHANGE = "com.softwinner.pppoe.ACTION_STATE_CHANGE";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (DEBUG) Log.d(TAG, "receive : " + action);
		boolean autoConn = Settings.Secure.getInt(context.getContentResolver(),
				Settings.Secure.PPPOE_AUTO_CONN, 0) != 0 ? true : false;
		boolean enable = Settings.Secure.getInt(context.getContentResolver(),
				Settings.Secure.PPPOE_ENABLE, 0) != 0 ? true : false;

		if (DEBUG) {
			Log.d(TAG, "*************PPPoE Debug Info****************");
			Log.d(TAG, "autoConn:\t" + autoConn);
			Log.d(TAG, "enable:\t" + enable);
			Log.d(TAG, "net.pppoe.interface:\t" + SystemProperties.get("net.pppoe.interface"));
			Log.d(TAG, "net.pppoe.ppp-exit:\t" + SystemProperties.get("net.pppoe.ppp-exit"));
			Log.d(TAG, "net.pppoe.reason:\t" + SystemProperties.get("net.pppoe.reason"));
			Log.d(TAG, "init.svc.pppoe:\t" + SystemProperties.get("init.svc.pppoe"));
			Log.d(TAG, "*********************************************");
		}

		if (ACTION_STATE_CHANGE.equals(action)) {
			if (enable) {
				if(DEBUG) Log.d(TAG, "User enable PPPoE.");
				PPPoEService.mTry = PPPoEService.MAX_CONN_TRY_TIMES;
				Intent startIntent = new Intent(PPPoEService.ACTION_START_PPPOE);
				context.startService(startIntent);
			} else if (PPPoEService.isConnect()) {
				if(DEBUG) Log.d(TAG, "User disable PPPoE.");
				PPPoEService.stop();
			}
		}

		if (Intent.ACTION_BOOT_COMPLETED.equals(action) && autoConn && enable) {
			Intent startIntent = new Intent(PPPoEService.ACTION_START_PPPOE);
			context.startService(startIntent);
		}

		String iface = Settings.Secure.getString(context.getContentResolver(),
				Settings.Secure.PPPOE_INTERFACE);

		if (iface == null) {
			return;
		}

		boolean isEthernet = isEthernetInterface(context, iface);
		if (enable && autoConn && !isEthernet
				&& WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
			final NetworkInfo networkInfo = (NetworkInfo) intent
					.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			boolean wifiConnected = networkInfo != null
					&& networkInfo.isConnected();
			if (wifiConnected) {
				Log.d(TAG, "receive the wifi state change");
				Intent startIntent = new Intent(PPPoEService.ACTION_START_PPPOE);
				context.startService(startIntent);
			}
		}

		if (enable && autoConn && isEthernet
				&& EthernetManager.ETHERNET_LINKED_ACTION.equals(action)) {
			PPPoEService.mTry = PPPoEService.MAX_CONN_TRY_TIMES;
			String loginInfo[] = PPPoEService.readLoginInfo().split(
					Pattern.quote("*"));
			String ipppoeface = Settings.Secure.getString(
					context.getContentResolver(),
					Settings.Secure.PPPOE_INTERFACE);
			if (loginInfo != null && loginInfo.length == 2
					&& ipppoeface != null) {
				loginInfo[0] = loginInfo[0].replace('\"', ' ');
				loginInfo[0] = loginInfo[0].trim();
				try {
                    PPPoEService.mTry = PPPoEService.MAX_CONN_TRY_TIMES;
                    PPPoEService.connect(ipppoeface, loginInfo[0]);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (DEBUG) Log.d(TAG, "Receive the Ethernet Linked Action");
		} else if (isEthernet
				&& EthernetManager.ETHERNET_DISLINKED_ACTION.equals(action)) {
			if (DEBUG) Log.d(TAG, "Receive the Ethernet DisLinked Action");
			PPPoEService.stop();
		}
	}

	private boolean isEthernetInterface(Context context, String iface) {
		if (iface == null)
			return false;
		EthernetManager ethernetManager = EthernetManager.getInstance();
		List<EthernetDevInfo> devices = ethernetManager.getDeviceNameList();
		if(devices == null)
			return false;
		for (EthernetDevInfo device : devices) {
			if (iface.equals(device.getIfName())) {
				return true;
			}
		}
		return false;
	}
}
