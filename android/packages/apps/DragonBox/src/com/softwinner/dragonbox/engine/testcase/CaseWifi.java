package com.softwinner.dragonbox.engine.testcase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.IpAssignment;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.ProxySettings;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.ethernet.EthernetManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.DisplayManagerAw;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.os.Looper;

import com.softwinner.dragonbox.R;
import com.softwinner.dragonbox.WifiAdmin;
import com.softwinner.dragonbox.engine.BaseCase;
import com.softwinner.dragonbox.platform.BasePlatform;
import com.softwinner.dragonbox.xml.Node;

public class CaseWifi extends BaseCase implements ListView.OnItemClickListener,
		AlertDialog.OnClickListener {
        
        private static final String TAG = "CaseWifi";
	private Thread mTask;
	/* WifiManager */
	private WifiManager mWifiManager;

	/* 扫描结果 */
	private List<ScanResult> results;

	/* 布局解析器 */
	private LayoutInflater mInflater;

	/* Wifi广播的Intent过滤器 */
	private IntentFilter mFilter;

	/* Wifi Ap 扫描器 */
	private Scanner mScanner;

	/* 扫描间隔 */
	private static final int WIFI_RESCAN_INTERVAL_MS = 500;

	/* wifi状态监测间隔 */
	public static final int WIFI_STATE_INTERVAL_MS = 100;

	/* Wifi的原始状态，用于在测试完毕之后还原wifi状态 */
	private int mOriginWifiStatus;

	/* access point */
	private ArrayList<AccessPoint> accessPoint;

	/* Wifi 配置对话框 */
	private WifiDialog mWifiDialog;

	/* wifi config */
	private WifiConfiguration wifiConfig;

	private boolean findSsid = false;

	private WifiInfo mLastInfo;
	private DetailedState mLastState;

	private int maxRSSI = -65;
	private String wifiSSIDStr;
	private String wifiPWDStr;

	public static final String PASSABLE_MAX_RSSI = "maxRSSI";
	public static final String PASSABLE_WIFI_SSID = "wifiSSID";
	public static final String PASSABLE_WIFI_PWD = "wifiPWD";
	private int mPassRSSI;

	/* 保存通过DragonFire配置的网络id，它们将会在应用退出时清除 */
	private final ArrayList<Integer> mNetworkIds = new ArrayList<Integer>();

	private boolean connecting = false;

	private TextView connectTxt;
	private TextView connectStateTxt;
	private LinearLayout layoutWifi;

	private Thread mWifiStatThread;
	private boolean mWifiTestStart;
	/* wifi ap 扫描器 */
	private class Scanner extends Handler {
		private int mRetry = 0;

		void resume() {
			if (!hasMessages(0)) {
				sendEmptyMessage(0);
			}
		}

		void forceScan() {
			removeMessages(0);
			sendEmptyMessage(0);
		}

		void pause() {
			mRetry = 0;
			removeMessages(0);
		}

		@Override
		public void handleMessage(Message message) {
			if (mWifiManager.startScanActive()) {
				mRetry = 0;
			} else if (++mRetry >= 3) {
				mRetry = 0;
				return;
			}
			sendEmptyMessageDelayed(0, WIFI_RESCAN_INTERVAL_MS);
		}
	}

	private WifiAdapter mWifiAdapter;
	
	private class WifiAdapter extends BaseAdapter {

		private class ViewHolder {
			TextView ssid;
			TextView strength;
		}

		ArrayList<AccessPoint> mAps;

		public void setAccessPoints(ArrayList<AccessPoint> aps) {
			mAps = aps;
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mAps != null ? mAps.size() : 0;
		}

		@Override
		public AccessPoint getItem(int position) {
			return mAps != null ? mAps.get(position) : null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convert, ViewGroup root) {
			Log.e(TAG, "getView =====ssid=====" + getItem(position).ssid);
			if (convert == null) {
				convert = mInflater.inflate(R.layout.wifi_item, null);
				ViewHolder holder = new ViewHolder();
				holder.ssid = (TextView) convert.findViewById(R.id.ssid);
				holder.strength = (TextView) convert
						.findViewById(R.id.strength);
				convert.setTag(holder);
			}
			ViewHolder holder = (ViewHolder) convert.getTag();
			holder.ssid.setText(getItem(position).ssid);

			int level = getItem(position).rssi != Integer.MIN_VALUE ? WifiManager
					.calculateSignalLevel(getItem(position).rssi, 16) : 0;
			level = getItem(position).rssi != Integer.MIN_VALUE ? WifiManager
					.calculateSignalLevel(getItem(position).rssi, 4) : 0;

			String strength = getItem(position).rssi == Integer.MIN_VALUE ? "不在范围"
					: "" + getItem(position).rssi + " DB ";
			holder.strength.setText(strength);

			if (!getPassable() && (getItem(position).rssi != Integer.MIN_VALUE)
					&& (getItem(position).rssi >= maxRSSI) && connecting) {
				mPassRSSI = getItem(position).rssi;
				// setPassable(true);
			}

			switch (getItem(position).connectionType) {
			case AccessPoint.CONNECTION_CONNECTED:
				convert.setBackgroundColor(Color.BLACK);
				connecting = true;
				break;
			case AccessPoint.CONNECTION_CONNECTTING_FAILED:
				convert.setBackgroundColor(Color.RED);
				break;
			case AccessPoint.CONNECTION_UNKNOWN:
				convert.setBackgroundColor(Color.BLACK);
				break;
			}

			if ((getItem(position).ssid.equals(wifiSSIDStr)) && !getPassable()) {
				Log.e(TAG, "=====conneting=====");
				AccessPoint ap = mWifiAdapter.getItem(position);
				wifiConfig = getConfig(ap);
				// connect(config);
				findSsid = true;
				if(!getPassable()){
					mHandler.sendEmptyMessageDelayed(0,500); // real start test!
				}
			}
			return convert;
		}

	}

	private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent i) {
			String action = i.getAction();
			NetworkInfo networkInfo = (NetworkInfo) i
					.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
			mLastInfo = mWifiManager.getConnectionInfo();
			DetailedState state = networkInfo != null ? networkInfo
					.getDetailedState() : null;
			if (state != null) {
				mLastState = state;
			}
			Log.d(TAG,"action = " + action);
			if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)
					|| WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION
							.equals(action)
					|| WifiManager.LINK_CONFIGURATION_CHANGED_ACTION
							.equals(action)
					|| WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
				accessPoint = updateAccessPoint();
				// if(accessPoint.size() > 0){
				// setPassable(true);
				// }
				mWifiAdapter.setAccessPoints(accessPoint);

			} else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION
					.equals(action)) {

			}else if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)){
				int wifiState = i.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
				if(wifiState==WifiManager.WIFI_STATE_ENABLED){
					if(!mWifiTestStart){
						mWifiTestStart = true;
						Log.d(TAG,"wifi is enabled.let's start test");
						startWifiTest();
					}
				}
			}
		}
	};

	private class Multimap<K, V> {
		private HashMap<K, List<V>> store = new HashMap<K, List<V>>();

		/** retrieve a non-null list of values with key K */
		List<V> getAll(K key) {
			List<V> values = store.get(key);
			return values != null ? values : Collections.<V> emptyList();
		}

		void put(K key, V val) {
			List<V> curVals = store.get(key);
			if (curVals == null) {
				curVals = new ArrayList<V>(3);
				store.put(key, curVals);
			}
			curVals.add(val);
		}
	}

	private ArrayList<AccessPoint> updateAccessPoint() {
		ArrayList<AccessPoint> accessPoints = new ArrayList<AccessPoint>();
		Multimap<String, AccessPoint> apMap = new Multimap<String, AccessPoint>();

		final List<WifiConfiguration> configs = mWifiManager
				.getConfiguredNetworks();
		// 从Wifi配置中获得access point的信息
		if (configs != null) {
			for (WifiConfiguration config : configs) {
				AccessPoint accessPoint = new AccessPoint(mContext, config);
				accessPoint.update(mLastInfo, mLastState);
				accessPoints.add(accessPoint);
				apMap.put(accessPoint.ssid, accessPoint);
			}
		}

		// 从Wifi扫描结果中获得access point的信息
		final List<ScanResult> results = mWifiManager.getScanResults();
		if (results != null) {
			for (ScanResult result : results) {
				// Ignore hidden and ad-hoc networks.
				if (result.SSID == null || result.SSID.length() == 0
						|| result.capabilities.contains("[IBSS]")) {
					continue;
				}

				boolean found = false;
				for (AccessPoint accessPoint : apMap.getAll(result.SSID)) {
					if (accessPoint.update(result))
						found = true;
				}
				if (!found) {
					AccessPoint accessPoint = new AccessPoint(mContext, result);
					accessPoints.add(accessPoint);
					apMap.put(accessPoint.ssid, accessPoint);
				}
			}
		}

		// Pre-sort accessPoints to speed preference insertion
		Collections.sort(accessPoints);
		return accessPoints;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int position,
			long id) {
		AccessPoint ap = mWifiAdapter.getItem(position);
		if (ap.security == AccessPoint.SECURITY_NONE) {
			WifiConfiguration config = getConfig(ap);
			config.allowedKeyManagement.set(KeyMgmt.NONE);
			connect(config);
		} else {
			// mWifiDialog = new WifiDialog(mContext, ap, this);
			// mWifiDialog.show();
		}
	}

	private WifiConfiguration getConfig(AccessPoint ap) {
		WifiConfiguration config = new WifiConfiguration();
		config.SSID = AccessPoint.convertToQuotedString(ap.ssid);
		// config.SSID = wifiSSIDStr;
		String password = wifiPWDStr;
		switch (ap.security) {
		case AccessPoint.SECURITY_NONE:
			config.allowedKeyManagement.set(KeyMgmt.NONE);
			break;

		case AccessPoint.SECURITY_WEP:
			config.allowedKeyManagement.set(KeyMgmt.NONE);
			config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
			config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
			// String password = mWifiDialog.getPassword();

			int length = password.length();
			// WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
			if ((length == 10 || length == 26 || length == 58)
					&& password.matches("[0-9A-Fa-f]*")) {
				config.wepKeys[0] = password;
			} else {
				config.wepKeys[0] = '"' + password + '"';
			}
			break;

		case AccessPoint.SECURITY_PSK:
			config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
			password = wifiPWDStr;
			length = password.length();
			if (password.matches("[0-9A-Fa-f]{64}")) {
				config.preSharedKey = password;
			} else {
				config.preSharedKey = '"' + password + '"';
			}
			break;

		/* 暂时不支持使用EAP加密的wifi节点 */
		/*
		 * case AccessPoint.SECURITY_EAP:
		 * config.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
		 * config.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
		 * config.eap.setValue((String) mEapMethodSpinner.getSelectedItem());
		 * 
		 * config.phase2.setValue((mPhase2Spinner.getSelectedItemPosition() ==
		 * 0) ? "" : "auth=" + mPhase2Spinner.getSelectedItem());
		 * config.ca_cert.setValue((mEapCaCertSpinner.getSelectedItemPosition()
		 * == 0) ? "" : KEYSTORE_SPACE + Credentials.CA_CERTIFICATE + (String)
		 * mEapCaCertSpinner.getSelectedItem());
		 * config.client_cert.setValue((mEapUserCertSpinner
		 * .getSelectedItemPosition() == 0) ? "" : KEYSTORE_SPACE +
		 * Credentials.USER_CERTIFICATE + (String)
		 * mEapUserCertSpinner.getSelectedItem());
		 * config.private_key.setValue((mEapUserCertSpinner
		 * .getSelectedItemPosition() == 0) ? "" : KEYSTORE_SPACE +
		 * Credentials.USER_PRIVATE_KEY + (String)
		 * mEapUserCertSpinner.getSelectedItem());
		 * config.identity.setValue((mEapIdentityView.length() == 0) ? "" :
		 * mEapIdentityView.getText().toString());
		 * config.anonymous_identity.setValue((mEapAnonymousView.length() == 0)
		 * ? "" : mEapAnonymousView.getText().toString()); if
		 * (mPasswordView.length() != 0) {
		 * config.password.setValue(mPasswordView.getText().toString()); }
		 * break;
		 */

		default:
			return null;
		}
		config.proxySettings = ProxySettings.UNASSIGNED;
		config.ipAssignment = IpAssignment.UNASSIGNED;
		config.linkProperties = new LinkProperties();

		return config;
	}

	@Override
	public void onClick(DialogInterface arg0, int button) {
		// if (button == DialogInterface.BUTTON_POSITIVE) {
		// AccessPoint wifiInfo = mWifiDialog.getWifiInfo();
		// WifiConfiguration config = getConfig(wifiInfo);
		// connect(config);
		// }
	}

	private void connect(WifiConfiguration config) {
		int networkId = mWifiManager.addNetwork(config);
		mWifiManager.enableNetwork(networkId, true);
		mNetworkIds.add(networkId);
		Log.i("DB Wifi", "connect wifi!");
	}

	@Override
	protected void onInitialize(Node attr) {
		Log.d(TAG,"onInitialize");
		setName(R.string.case_wifi_name);
		setView(R.layout.case_wifi);
		ListView lv = (ListView) getView().findViewById(R.id.wifi_list);
		connectTxt = (TextView) getView().findViewById(
				R.id.case_wifi_name_connect);
		connectStateTxt = (TextView) getView().findViewById(
				R.id.case_wifi_name_connect_state);
		layoutWifi = (LinearLayout) getView().findViewById(
				R.id.linearLayout_wifi);
		mInflater = LayoutInflater.from(mContext);
		mWifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		BasePlatform.getPlatform().asyncConnect(mWifiManager, mContext,
				new Handler());
		mWifiAdapter = new WifiAdapter();
		lv.setAdapter(mWifiAdapter);
		lv.setOnItemClickListener(this);
		connecting = false;

		// 初始化Wifi的IntentFilter
		mFilter = new IntentFilter();
		mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
		mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		mFilter.addAction(WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION);
		mFilter.addAction(WifiManager.LINK_CONFIGURATION_CHANGED_ACTION);
		mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
		mWifiTestStart = false;
		// 注册Wifi广播监听器
		mContext.registerReceiver(mWifiReceiver, mFilter);
		
		// 打开wifi并保存wifi的状态
		final WifiManager wm = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		mOriginWifiStatus = wm.getWifiState();
		new Thread(new Runnable() {
			@Override
			public void run() {
				/*if ((mOriginWifiStatus == WifiManager.WIFI_STATE_DISABLED)
						|| (mOriginWifiStatus == WifiManager.WIFI_STATE_DISABLING))*/

				//wm.setWifiEnabled(true);
				if (!wm.isWifiEnabled()) {
					wm.setWifiApEnabled(null, false);
					wm.setWifiEnabled(true);
				}				
			}
		}).start();
	}

	@Override
	protected boolean onCaseStarted() {
		return false;
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			testWifi();
		}

	};

	public void testWifi() {
		mTask = new Thread() {
			@Override
			public void run() {
				((Activity) mContext).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Log.d(TAG,"passable = " + getPassable() + " findSsid = " + findSsid  + " connecting = " + connecting);
						if (!getPassable()) {
							if (findSsid) {
								connect(wifiConfig);
							}
							if (connecting) {
								WifiInfo wifiInfo = mWifiManager
										.getConnectionInfo();
								int rss = wifiInfo.getRssi();
								// wifiSSIDStr = "\"" + wifiSSIDStr + "\"";
								String ssidstr = wifiInfo.getSSID();
								ssidstr = ssidstr.substring(1,
										ssidstr.length() - 1);
								Log.d("WifiCase", "rss is " + rss
										+ " and maxrssi is " + maxRSSI
										+ ", ssidstr is" + ssidstr
										+ " , wifiSSIDStr is " + wifiSSIDStr);
								if ((rss >= maxRSSI)
										&& (ssidstr.equals(wifiSSIDStr))) {
									setPassable(true);
									connectTxt.setText("\"" + ssidstr + "\""
											+ "已连接 ");
									connectStateTxt.setText("");
									layoutWifi.setBackgroundColor(Color.GREEN);
								}

							}
						}

					}
				});
			}
		};
		mTask.start();
	}

	public void startWifiTest() {
		// 启动AP扫描器
		mScanner = new Scanner();
		mScanner.forceScan();
		onPassableChange();
	}

	@Override
	protected void onCaseFinished() {
		Log.d(TAG,"onCaseFinished");
	}

	@Override
	protected void onRelease() {
		mScanner.pause();
		mScanner = null;
		Log.d(TAG,"onRelease");
		setPassable(false);
		if(mWifiStatThread!=null)
			mWifiStatThread.stop();
		mContext.unregisterReceiver(mWifiReceiver);
		new Thread(new Runnable() {
			@Override
			public void run() {
				// mWifiManager.setWifiEnabled(mOriginWifiStatus);
				for (Integer i : mNetworkIds) {
					mWifiManager.removeNetwork(i);
				}
				//mWifiManager.setWifiEnabled(false);
				//EthernetManager.getInstance().setEnabled(true);
			}
		}).start();
		
	}

	protected void onPassableChange() {
		if (getPassable()) {
			getView().findViewById(R.id.textView1).setBackgroundColor(
					Color.GREEN);
		} else {
			getView().findViewById(R.id.textView1)
					.setBackgroundColor(Color.RED);
		}

	}

	@Override
	protected void onPassableInfo(Node node) {
		super.onPassableInfo(node);
		maxRSSI = node.getAttributeIntegerValue(PASSABLE_MAX_RSSI) * -1;
		wifiSSIDStr = node.getAttributeValue(PASSABLE_WIFI_SSID);
		wifiPWDStr = node.getAttributeValue(PASSABLE_WIFI_PWD);
		Log.e(TAG, "=================================");
		Log.e(TAG, "===maxRSSI===  " + maxRSSI);
		Log.e(TAG, "===wifiSSIDStr===  " + wifiSSIDStr);
		Log.e(TAG, "===wifiPWDStr===  " + wifiPWDStr);
		Log.e(TAG, "=================================");
		if (maxRSSI == 0) {
			maxRSSI = -65;
		}
	}

	@Override
	public String getDetailResult() {
		return "PassedRSSI:" + mPassRSSI;
	}
}
