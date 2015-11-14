package com.softwinner.dragonbox.engine.testcase;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.http.conn.util.InetAddressUtils;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.ethernet.EthernetManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.softwinner.dragonbox.R;
import com.softwinner.dragonbox.WifiAdmin;
import com.softwinner.dragonbox.engine.BaseCase;
import com.softwinner.dragonbox.xml.Node;

public class CaseElthernet extends BaseCase {

        private static final String TAG = "CaseElthernet";
	TextView stateView;

	WifiManager wifiManager;
	EthernetManager mEthManager;
	boolean defaultWifiEnable;
	boolean isTestingCaseRunning = false;
	private WifiAdmin mWifiAdmin;

	LinearLayout layout1;
	LinearLayout layout2;

	TextView inlineStateTxT;
	TextView ipStateTxT;

	@Override
	protected void onInitialize(Node attr) {
		this.setView(R.layout.case_elthernet);
		this.setName(R.string.case_elthernet);
		stateView = (TextView) mView.findViewById(R.id.elthernet_name);
		inlineStateTxT = (TextView) mView
				.findViewById(R.id.case_elthernet_linestate_text);
		ipStateTxT = (TextView) mView.findViewById(R.id.case_elthernet_ip_text);
		wifiManager = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);
		mEthManager = EthernetManager.getInstance();
		layout1 = (LinearLayout) mView
				.findViewById(R.id.linearLayout_rj45_inline);
		layout2 = (LinearLayout) mView.findViewById(R.id.linearLayout_rj45_ip);

		mMaxFailedTimes = 0;
	}

	@Override
	protected boolean onCaseStarted() {
		// mEthManager=(EthernetManager)
		// mContext.getSystemService(Context.ETHERNET_SERVICE);

		return false;
	}

	public void startElthernetTest() {
		final WifiManager wm = (WifiManager) mContext
				.getSystemService(Context.WIFI_SERVICE);	    
		isTestingCaseRunning = true;

		defaultWifiEnable = wifiManager.isWifiEnabled();
		if (defaultWifiEnable) {
			wifiManager.setWifiEnabled(false);
		}
	 for (int i = 0; i < 100; i++) {
	    if(wm.getWifiState() != WifiManager.WIFI_STATE_DISABLED) {
	        try {
	            Thread.sleep(CaseWifi.WIFI_STATE_INTERVAL_MS);
	        } catch (InterruptedException e) {
				        e.printStackTrace();
			      }
			  } else {
			      break;
			  }
		}
		// inetAddressInfo.setText("正在测试中...");
		if(wm.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {
		    mHandler.sendEmptyMessageDelayed(0, 1000);
	 } else {
	    Log.e("zjh","cannot disable wifi before ethernet test start!!!");
	 }
 }
	@Override
	protected void onCaseFinished() {

		if (defaultWifiEnable) {
			wifiManager.setWifiEnabled(true);
		}

		isTestingCaseRunning = false;
		mHandler.removeMessages(0);

	}

	@Override
	protected void onRelease() {

	}

	private void testElthernet() {
	 if(mEthManager.getState() != EthernetManager.ETHERNET_STATE_ENABLED) {
		    mEthManager.setEnabled(true);
		    for (int i = 0; i < 100; i++) {
	        if(mEthManager.getState() != EthernetManager.ETHERNET_STATE_ENABLED) {
	            try {
	                Thread.sleep(CaseWifi.WIFI_STATE_INTERVAL_MS);
	            } catch (InterruptedException e) {
				            e.printStackTrace();
			          }
			      } else {
			          break;
			      }
		    }
		}
		if(mEthManager.getState() != EthernetManager.ETHERNET_STATE_ENABLED) {
		    Log.e("zjh", "cannot enable ethernet before test!!!");
//		    return;
		}
		Log.e(TAG, "====CheckLink====" + mEthManager.CheckLink("eth0"));
		if (mEthManager.CheckLink("eth0") == 1) {
			inlineStateTxT.setText(R.string.case_elthernet_inline_2);
			layout1.setBackgroundColor(Color.GREEN);
		}

		ConnectivityManager connectivityManager = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = connectivityManager
				.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);

		if (netInfo.isAvailable()) {
			Log.i("dragonbox", "ethernet network is available");
			Enumeration<NetworkInterface> networkInterfaces = null;
			try {
				networkInterfaces = NetworkInterface.getNetworkInterfaces();
			} catch (SocketException e) {
				e.printStackTrace();
			}

			if (networkInterfaces != null) {
				NetworkInterface network = null;

				while (networkInterfaces.hasMoreElements()) {
					Log.i("dragonbox", "network interface");
					network = networkInterfaces.nextElement();
					Enumeration<InetAddress> inetAddresses = network
							.getInetAddresses();

					InetAddress inetAddress = null;
					while (inetAddresses != null
							&& inetAddresses.hasMoreElements()) {
						inetAddress = inetAddresses.nextElement();
						Log.i("dragonbox",
								"get a inetAddress:"
										+ inetAddress.getHostAddress());
						if (network.getName().equalsIgnoreCase(
								new String("eth0"))) {
							if (!inetAddress.isLoopbackAddress()
									&& InetAddressUtils
											.isIPv4Address(inetAddress
													.getHostAddress())) {
								// inetAddressInfo.setText("IP: "+
								// inetAddress.getHostAddress());
								// inetAddressInfo.setText("IP地址获取成功");
								ipStateTxT
										.setText(R.string.case_elthernet_ipaddress_notest_2);
								layout2.setBackgroundColor(Color.GREEN);
								setPassable(true);
								break;
							}
						}
					}
				}
			}
		} else {
			setPassable(false);
			Log.i("dragonbox", "ethernet network is inavailable");
		}
		if (!getPassable() && isTestingCaseRunning) {
			mHandler.sendEmptyMessageDelayed(0, 500);
		}
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			testElthernet();
		}

	};

	@Override
	protected void onPassableChange() {
		super.onPassableChange();
		if (getPassable()) {
			stateView.setBackgroundColor(Color.GREEN);
			mEthManager.setEnabled(false);
			mWifiAdmin = new WifiAdmin(mContext);
			mWifiAdmin.openWifi();
		} else {
			stateView.setBackgroundColor(Color.RED);
		}
	}

	@Override
	public String getDetailResult() {
		return "retrytimes:" + mFailedTimes;
	}

}
