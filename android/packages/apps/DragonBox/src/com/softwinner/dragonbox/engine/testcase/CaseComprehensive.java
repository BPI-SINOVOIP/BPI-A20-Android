package com.softwinner.dragonbox.engine.testcase;

import java.util.ArrayList;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.softwinner.dragonbox.R;
import com.softwinner.dragonbox.engine.BaseCase;
import com.softwinner.dragonbox.engine.BaseConfiguration;
import com.softwinner.dragonbox.xml.Node;

/**
 * 综合测试
 * 
 * @author huanglong
 * 
 */
public class CaseComprehensive extends BaseCase implements
		BaseCase.OnPassableChangeListener {

	private static final String TAG = CaseComprehensive.class.getSimpleName();

	private LinearLayout mLeft;
	private LinearLayout mRight;
	private LinearLayout mMidLeft;
	private LinearLayout mMidRight;
	private ArrayList<BaseCase> mBaseList;

	private CaseHdmi mCaseHdmi;
	private CaseCvbs mCaseCvbs;
	private CaseLed mCaseLed;
	private CaseWifi mCaseWifi;
	private CaseElthernet mCaseEthernet;
	private CaseSpdif mCaseSpdif;

	@Override
	protected void onInitialize(Node node) {
		boolean hasVideo = false;
		boolean hasVersion = false;
		boolean hasEltherner = false;
		boolean hasHdmi = false;
		boolean hasWifi = false;
		boolean hasSpidf = false;

		boolean hasCvbs = false;
		boolean hasSdcard = false;
		boolean hasLed = false;
		boolean hasUsb = false;

		boolean hasResult = false;

		Node nVersion = null;
		Node nEltherner = null;
		Node nHdmi = null;
		Node nWifi = null;

		Node nCvbs = null;
		Node nSdcard = null;
		Node nLed = null;
		Node nUsb = null;

		Node nVideo = null;
		Node nSpidf = null;

		Node nResult = null;

		if (node != null) {
			int nNode = node.getNNodes();
			for (int i = 0; i < nNode; i++) {
				String nodeName = node.getNode(i).getName();
				if (DEBUG) {
					Log.v(TAG, "initialize the case " + nodeName);
				}
				if (CaseVersion.class.getSimpleName().equals(nodeName)) {
					hasVersion = true;
					nVersion = node.getNode(i);
				} else if (CaseElthernet.class.getSimpleName().equals(nodeName)) {
					hasEltherner = true;
					nEltherner = node.getNode(i);
				} else if (CaseHdmi.class.getSimpleName().equals(nodeName)) {
					hasHdmi = true;
					nHdmi = node.getNode(i);
				} else if (CaseWifi.class.getSimpleName().equals(nodeName)) {
					hasWifi = true;
					nWifi = node.getNode(i);
				} else if (CaseCvbs.class.getSimpleName().equals(nodeName)) {
					hasCvbs = true;
					nCvbs = node.getNode(i);
				} else if (CaseSdcard.class.getSimpleName().equals(nodeName)) {
					hasSdcard = true;
					nSdcard = node.getNode(i);
				} else if (CaseLed.class.getSimpleName().equals(nodeName)) {
					hasLed = true;
					nLed = node.getNode(i);
				} else if (CaseUsb.class.getSimpleName().equals(nodeName)) {
					hasUsb = true;
					nUsb = node.getNode(i);
				} else if (CaseVideo.class.getSimpleName().equals(nodeName)) {
					hasVideo = true;
					nVideo = node.getNode(i);
				} else if (CaseSpdif.class.getSimpleName().equals(nodeName)) {
					hasSpidf = true;
					nSpidf = node.getNode(i);
				}
			}
		}

		mBaseList = new ArrayList<BaseCase>();
		setView(R.layout.case_comprehensive);
		setName(R.string.case_comprehensive);
		mLeft = (LinearLayout) getView().findViewById(R.id.left);
		mRight = (LinearLayout) getView().findViewById(R.id.right);
		mMidLeft = (LinearLayout) getView().findViewById(R.id.mid_left);
		mMidRight = (LinearLayout) getView().findViewById(R.id.mid_right);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.weight = 1;
		lp.gravity = Gravity.CENTER;
		// 初始化版本测试
		if (hasVersion) {
			BaseCase case_version = new CaseVersion();
			case_version.setEngine(mEngine);
			mBaseList.add(case_version);
			case_version.initialize(mContext, nVersion);
			case_version.addPassableChangeListener(this);
			case_version.getView().setBackgroundResource(
					R.drawable.comprehensive_background);
			mMidRight.addView(case_version.getView(), lp);
		}

		if (hasEltherner) {
			CaseElthernet case_eltherner = new CaseElthernet();
			case_eltherner.setEngine(mEngine);
			mBaseList.add(case_eltherner);
			case_eltherner.initialize(mContext, nEltherner);
			case_eltherner.addPassableChangeListener(this);
			case_eltherner.getView().setBackgroundResource(
					R.drawable.comprehensive_background);
			mRight.addView(case_eltherner.getView(), lp);
			mCaseEthernet = case_eltherner;
		}

		// 初始化Hdmi测试，add by AW_maizirong
		if (hasHdmi) {
			CaseHdmi case_hdmi = new CaseHdmi();
			case_hdmi.setEngine(mEngine);
			mBaseList.add(case_hdmi);
			case_hdmi.initialize(mContext, nHdmi);
			case_hdmi.addPassableChangeListener(this);
			case_hdmi.getView().setBackgroundResource(
					R.drawable.comprehensive_background);
			mMidRight.addView(case_hdmi.getView(), lp);
			mCaseHdmi = case_hdmi;
		}

		// 初始化wifi
		if (hasWifi) {
			CaseWifi case_wifi = new CaseWifi();
			case_wifi.setEngine(mEngine);
			mBaseList.add(case_wifi);
			case_wifi.initialize(mContext, nWifi);
			case_wifi.addPassableChangeListener(this);
			mRight.addView(case_wifi.getView(), lp);
			case_wifi.getView().setBackgroundResource(
					R.drawable.comprehensive_background);
			mCaseWifi = case_wifi;
		}

		// 初始化cvbs spdif测试，add by AW_maizirong
		if (hasCvbs) {
			CaseCvbs case_cvbs = new CaseCvbs();
			case_cvbs.setEngine(mEngine);
			mBaseList.add(case_cvbs);
			case_cvbs.initialize(mContext, nCvbs);
			case_cvbs.addPassableChangeListener(this);
			case_cvbs.getView().setBackgroundResource(
					R.drawable.comprehensive_background);
			mMidRight.addView(case_cvbs.getView(), lp);
			mCaseCvbs = case_cvbs;
		}

		// 初始化Video测试，add by AW_maizirong
		if (hasSdcard) {
			BaseCase case_sdcard = new CaseSdcard();
			case_sdcard.setEngine(mEngine);
			mBaseList.add(case_sdcard);
			case_sdcard.initialize(mContext, nSdcard);
			case_sdcard.addPassableChangeListener(this);
			case_sdcard.getView().setBackgroundResource(
					R.drawable.comprehensive_background);
			mRight.addView(case_sdcard.getView(), lp);
		}

		if (hasLed) {
			CaseLed case_led = new CaseLed();
			case_led.setEngine(mEngine);
			mBaseList.add(case_led);
			case_led.initialize(mContext, nLed);
			case_led.addPassableChangeListener(this);
			case_led.getView().setBackgroundResource(
					R.drawable.comprehensive_background);
			mMidRight.addView(case_led.getView(), lp);
			mCaseLed = case_led;
		}

		// 初始化memory
		if (hasUsb) {
			BaseCase case_usb = new CaseUsb();
			case_usb.setEngine(mEngine);
			mBaseList.add(case_usb);
			case_usb.initialize(mContext, nUsb);
			case_usb.addPassableChangeListener(this);
			case_usb.getView().setBackgroundResource(
					R.drawable.comprehensive_background);
			mRight.addView(case_usb.getView(), lp);
		}

		if (hasVideo) {
			BaseCase caseVideo = new CaseVideo();
			caseVideo.setEngine(mEngine);
			mBaseList.add(caseVideo);
			caseVideo.initialize(mContext, nUsb);
			caseVideo.addPassableChangeListener(this);
			caseVideo.getView().setBackgroundResource(
					R.drawable.comprehensive_background);
			mLeft.addView(caseVideo.getView(), lp);
		}

		if (hasResult) {
			BaseCase caseResult = new CaseResult();
			caseResult.setEngine(mEngine);
			mBaseList.add(caseResult);
			caseResult.initialize(mContext, nUsb);
			caseResult.addPassableChangeListener(this);
			caseResult.getView().setBackgroundResource(
					R.drawable.comprehensive_background);
			caseResult.getView().setMinimumHeight(100);
			mLeft.addView(caseResult.getView(), lp);
		}

		if (hasSpidf) {
			CaseSpdif case_spidf = new CaseSpdif();
			case_spidf.setEngine(mEngine);
			mBaseList.add(case_spidf);
			case_spidf.initialize(mContext, nUsb);
			case_spidf.addPassableChangeListener(this);
			case_spidf.getView().setBackgroundResource(
					R.drawable.comprehensive_background);
			mRight.addView(case_spidf.getView(), lp);
			mCaseSpdif = case_spidf;
		}


		if (mLeft.getChildCount() == 0) {
			mLeft.setVisibility(View.GONE);
		}
		if (mMidLeft.getChildCount() == 0) {
			mMidLeft.setVisibility(View.GONE);
		}
		if (mRight.getChildCount() == 0) {
			mRight.setVisibility(View.GONE);
		}
		if (mMidRight.getChildCount() == 0) {
			mRight.setVisibility(View.GONE);
		}
	}

	@Override
	protected boolean onCaseStarted() {
		for (BaseCase _case : mBaseList) {
			_case.startCase();
			if (_case instanceof CaseHdmi) {
				((CaseHdmi) _case).startMenDetect();
			}
		}
		return false;
	}

	@Override
	protected void onCaseFinished() {
		for (BaseCase _case : mBaseList) {
			_case.setResult(mResult);
		}
	}

	@Override
	protected void onRelease() {
		for (BaseCase _case : mBaseList) {
			_case.release();
		}
		mBaseList.clear();
	}

	boolean testElthernet = false;
	boolean testCvbs = false;
	boolean testLed = false;
	boolean testSpidf = false;

	@Override
	public void onPassableChange(BaseCase _case, boolean passable) {
		if (!passable) {
			setPassable(passable);
		} else {
			boolean allPassed = true;
			for (BaseCase __case : mBaseList) {
				allPassed &= __case.getPassable();
			}
			if (allPassed)
				setPassable(allPassed);
		}
		Log.d(TAG, "===============" + _case);
		if (_case instanceof CaseWifi && mCaseEthernet != null// && passable
				&& !testElthernet) {
			mCaseEthernet.startElthernetTest();
			testElthernet = true;
		}

		if (_case instanceof CaseHdmi && mCaseSpdif != null// && passable
				&& !testSpidf) {
			mCaseSpdif.startMenDetect();
			testSpidf = true;
		}

		if (_case instanceof CaseSpdif && mCaseCvbs != null// && passable
				&& !testCvbs) {
			mCaseCvbs.startMenDetect();
			testCvbs = true;
		}
		if (_case instanceof CaseCvbs && mCaseLed != null// && passable
				&& !testLed) {
			mCaseLed.startMenDetect();
			testLed = true;
		}

	}

	@Override
	public String getDetailResult() {
		String result = "" + super.getDetailResult();
		for (BaseCase __case : mBaseList) {
			if (!__case.getPassable()) {
				result += __case.getName() + ":" + __case.getDetailResult()
						+ ";";
			}
		}
		return result;
	}

	@Override
	protected BaseConfiguration generateConfiguration() {
		return new ComphsConfiguration();
	}

	private class ComphsConfiguration extends BaseConfiguration {

		private CheckBox enableVersion;
		private CheckBox enableElthernet;
		private CheckBox enableHdmi;
		private CheckBox enableWifi;

		private CheckBox enableCvbs;
		private CheckBox enableSdcard;
		private CheckBox enableLed;
		private CheckBox enableUsb;

		private CheckBox enableVideo;

		private boolean hasVersion = true;
		private boolean hasElthernet = true;
		private boolean hasHdmi = true;
		private boolean hasWifi = true;

		private boolean hasCvbs = true;
		private boolean hasSdcard = true;
		private boolean hasLed = true;
		private boolean hasUsb = true;

		private boolean hasVideo = true;

		// 版本测试
		private EditText passableFirewareEdit;
		private Button passableVersionDetail;
		private EditText passableMaxRSSIEdit;
		private EditText passableWIFISSIDEdit;
		private EditText passableWIFIPwdEdit;

		private String passableFireware;
		private String passableDisplay;
		private String passableMinCpu;
		private String passableMaxCpu;
		private String passableModel;

		private boolean autoGet = false;

		private String maxRSSI;
		private String wifiSSID;
		private String wifiPwd;

		private View titleView1;
		private View contentView1;

		@Override
		public void onInitializeForConfig(Node node) {
			setName(R.string.case_comprehensive);
			// 加载配置信息
			if (node != null) {
				int nNode = node.getNNodes();

				hasVersion = false;
				hasElthernet = false;
				hasHdmi = false;
				hasWifi = false;

				hasCvbs = false;
				hasSdcard = false;
				hasLed = false;
				hasUsb = false;

				hasVideo = false;

				for (int i = 0; i < nNode; i++) {
					String nodeName = node.getNode(i).getName();
					if (CaseVersion.class.getSimpleName().equals(nodeName)) {
						hasVersion = true;
						Node passNode = node.getNode(i).getNode("Passable");
						passableFireware = passNode
								.getAttributeValue(CaseVersion.PASSABLE_FIREWARE);
					} else if (CaseHdmi.class.getSimpleName().equals(nodeName)) {
						hasHdmi = true;
					} else if (CaseVideo.class.getSimpleName().equals(nodeName)) {
						hasVideo = true;
					} else if (CaseCvbs.class.getSimpleName().equals(nodeName)) {
						hasCvbs = true;
					} else if (CaseElthernet.class.getSimpleName().equals(
							nodeName)) {
						hasElthernet = true;
					} else if (CaseWifi.class.getSimpleName().equals(nodeName)) {
						hasWifi = true;
						maxRSSI = node.getNode(i).getAttributeValue(
								CaseWifi.PASSABLE_MAX_RSSI);

						Node passNode = node.getNode(i).getNode("Passable");
						wifiSSID = passNode
								.getAttributeValue(CaseWifi.PASSABLE_WIFI_SSID);
						wifiPwd = passNode
								.getAttributeValue(CaseWifi.PASSABLE_WIFI_PWD);

						if (passNode != null) {
							maxRSSI = passNode
									.getAttributeValue(CaseWifi.PASSABLE_MAX_RSSI);
						}
					} else if (CaseSdcard.class.getSimpleName()
							.equals(nodeName)) {
						hasSdcard = true;
					} else if (CaseLed.class.getSimpleName().equals(nodeName)) {
						hasLed = true;
					} else if (CaseUsb.class.getSimpleName().equals(nodeName)) {
						hasUsb = true;
					}
				}
			}

			// 初始化用于配置的View
			mConfigView = View.inflate(mContext,
					R.layout.case_comprehensive_config, null);

			enableVersion = (CheckBox) mConfigView
					.findViewById(R.id.enable_version);
			enableWifi = (CheckBox) mConfigView.findViewById(R.id.enable_wifi);
			enableSdcard = (CheckBox) mConfigView
					.findViewById(R.id.enable_sdcard);
			enableHdmi = (CheckBox) mConfigView.findViewById(R.id.enable_hdmi);
			enableLed = (CheckBox) mConfigView.findViewById(R.id.enable_led);
			enableCvbs = (CheckBox) mConfigView.findViewById(R.id.enable_cvbs);
			enableElthernet = (CheckBox) mConfigView
					.findViewById(R.id.enable_elthernet);
			enableUsb = (CheckBox) mConfigView.findViewById(R.id.enable_usb);

			enableVideo = (CheckBox) mConfigView
					.findViewById(R.id.enable_video);

			enableVersion.setChecked(hasVersion);
			enableWifi.setChecked(hasWifi);
			enableLed.setChecked(hasLed);
			enableHdmi.setChecked(hasHdmi);
			enableUsb.setChecked(hasUsb);
			enableCvbs.setChecked(hasCvbs);
			enableElthernet.setChecked(hasElthernet);
			enableSdcard.setChecked(hasSdcard);
			enableVideo.setChecked(hasVideo);

			// enableVersion.requestFocus();

			passableVersionDetail = (Button) mConfigView
					.findViewById(R.id.auto_get);
			passableVersionDetail
					.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							autoGet = true;
							passableVersionDetail
									.setText(R.string.case_comprehensive_config_passable_version_ok);
						}
					});
			passableFirewareEdit = (EditText) mConfigView
					.findViewById(R.id.edit_fireware);

			passableMaxRSSIEdit = (EditText) mConfigView
					.findViewById(R.id.edit_max_rssi);

			passableWIFISSIDEdit = (EditText) mConfigView
					.findViewById(R.id.config_wifi_ssid_edittext);

			passableWIFIPwdEdit = (EditText) mConfigView
					.findViewById(R.id.config_wifi_pwd_edittext);

			if (hasVersion) {
				passableFirewareEdit.setText(passableFireware);
			}

			if (hasWifi) {
				passableMaxRSSIEdit.setText(maxRSSI);
				passableWIFISSIDEdit.setText(wifiSSID);
				passableWIFIPwdEdit.setText(wifiPwd);
			}

			titleView1 = mConfigView.findViewById(R.id.textView1);
			contentView1 = mConfigView.findViewById(R.id.linearLayout1);
			titleView1.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View arg0) {
					if (contentView1.getVisibility() == View.VISIBLE)
						contentView1.setVisibility(View.GONE);
					else
						contentView1.setVisibility(View.VISIBLE);
				}
			});
		}

		public String getConfigInfo() {
			StringBuilder sb = new StringBuilder();

			sb.append(mContext
					.getString(R.string.case_comprehensive_config_prefix));
			if (hasVersion) {
				sb.append(mContext.getString(R.string.case_version_name));
			}

			if (hasWifi) {
				sb.append(mContext.getString(R.string.case_wifi_name));
			}
			if (hasHdmi) {
				sb.append(mContext.getString(R.string.case_hdmi_name));
			}
			if (hasVideo) {
				sb.append(mContext.getString(R.string.case_video_name));
			}

			if (hasCvbs) {
				sb.append(mContext.getString(R.string.case_cvbs_name));
			}
			if (hasElthernet) {
				sb.append(mContext.getString(R.string.case_elthernet_name));
			}
			if (hasSdcard) {
				sb.append(mContext.getString(R.string.case_sdcard_name));
			}
			if (hasUsb) {
				sb.append(mContext.getString(R.string.case_usb_name));
			}
			if (hasLed) {
				sb.append(mContext.getString(R.string.case_led_name));
			}

			return sb.toString();
		}

		@Override
		protected void onGetConfigNode(Node node) {
			if (hasVersion) {

				Node child = new Node(CaseVersion.class.getSimpleName());
				Node passable = new Node(PASSABLE_NODE_NAME);
				if (passableFireware != null) {
					passable.addAttribute(CaseVersion.PASSABLE_FIREWARE,
							passableFireware);
				}
				if (passableDisplay != null) {
					passable.addAttribute(CaseVersion.PASSABLE_DISPLAY,
							passableDisplay);
				}
				if (passableMaxCpu != null && passableMinCpu != null) {
					passable.addAttribute(CaseVersion.PASSABLE_CPU,
							passableMaxCpu + CaseVersion.PASSABLE_CPU_SPLIT
									+ passableMinCpu);
				}
				if (passableModel != null) {
					passable.addAttribute(CaseVersion.PASSABLE_MODEL,
							passableModel);
				}
				child.addNode(passable);
				node.addNode(child);
			}
			if (hasWifi) {
				Node child = new Node(CaseWifi.class.getSimpleName());
				Node passable = new Node(PASSABLE_NODE_NAME);
				if (maxRSSI != null) {
					passable.addAttribute(CaseWifi.PASSABLE_MAX_RSSI, maxRSSI);
				}
				if (wifiSSID != null) {
					passable.addAttribute(CaseWifi.PASSABLE_WIFI_SSID, wifiSSID);
				}
				if (wifiPwd != null) {
					passable.addAttribute(CaseWifi.PASSABLE_WIFI_PWD, wifiPwd);
				}
				child.addNode(passable);
				node.addNode(child);
			}
			if (hasHdmi) {
				node.addNode(new Node(CaseHdmi.class.getSimpleName()));
			}
			if (hasVideo) {
				node.addNode(new Node(CaseVideo.class.getSimpleName()));
			}
			if (hasCvbs) {
				node.addNode(new Node(CaseCvbs.class.getSimpleName()));
			}
			if (hasElthernet) {
				node.addNode(new Node(CaseElthernet.class.getSimpleName()));
			}
			if (hasSdcard) {
				node.addNode(new Node(CaseSdcard.class.getSimpleName()));
			}
			if (hasLed) {
				node.addNode(new Node(CaseLed.class.getSimpleName()));
			}
			if (hasUsb) {
				node.addNode(new Node(CaseUsb.class.getSimpleName()));
			}

		}

		@Override
		public void saveConfig() {
			hasVersion = enableVersion.isChecked();

			hasWifi = enableWifi.isChecked();
			hasHdmi = enableHdmi.isChecked();

			hasCvbs = enableCvbs.isChecked();
			hasElthernet = enableElthernet.isChecked();
			hasUsb = enableUsb.isChecked();
			hasLed = enableLed.isChecked();
			hasSdcard = enableSdcard.isChecked();
			hasVideo = enableVideo.isChecked();

			if (autoGet) {
				// 保存版本信息
				passableFireware = passableFirewareEdit.getText().toString();
				passableDisplay = Build.DISPLAY;
				passableMinCpu = CpuManager.getMinCpuFreq();
				passableMaxCpu = CpuManager.getMaxCpuFreq();
				passableModel = Build.MODEL;
			}

			String fireware = passableFirewareEdit.getText().toString();
			if (!TextUtils.isEmpty(fireware))
				passableFireware = fireware;

			String rssi = passableMaxRSSIEdit.getText().toString();
			if (rssi != null && rssi.length() > 0) {
				maxRSSI = rssi;
			} else {
				maxRSSI = "65";
			}

			String wifissid = passableWIFISSIDEdit.getText().toString();
			if (wifissid != null && !wifissid.equals("")) {
				wifiSSID = wifissid;
			}
			String wifipwd = passableWIFIPwdEdit.getText().toString();
			if (wifipwd != null && !wifipwd.equals("")) {
				wifiPwd = wifipwd;
			}

		}

	}

}
