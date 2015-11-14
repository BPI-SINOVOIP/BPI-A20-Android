package com.softwinner.dragonbox.engine.testcase;

import android.app.Activity;
import android.graphics.Color;
import android.os.SystemProperties;

import android.widget.TextView;

import com.softwinner.SystemMix;
import com.softwinner.dragonbox.R;
import com.softwinner.dragonbox.engine.BaseCase;
import com.softwinner.dragonbox.xml.Node;

/**
 * SN MAC测试
 * 
 * @author AW_maizirong
 * 
 */

public class CaseSnsn extends BaseCase {

	private static final String NAME_SN = "specialstr";
	private static final String NAME_BT_MAC = "mac_addr";
	private static final int SN_LENGTH = 7;// 设置SN长度
	String snStr;
	String BtMacStr;

	@Override
	protected void onInitialize(Node attr) {
		setView(R.layout.case_snsn);
		setName(R.string.case_snsn_name);
	}

	@Override
	protected boolean onCaseStarted() {
		final TextView tv = (TextView) getView().findViewById(R.id.snsn_info);
		final TextView tv2 = (TextView) getView().findViewById(R.id.snsn_name);
		tv2.setVisibility(8);
		snStr = getSn().trim();
		BtMacStr = getBtMac().trim();
		final String formatString = String.format(mContext.getResources()
				.getString(R.string.snsn_info), snStr, BtMacStr);
		((Activity) mContext).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				tv.setText(formatString);
			}
		});

		// 判断SN长度
		if (snStr.length() > SN_LENGTH && BtMacStr.length() == 17) {
			setPassable(true);
		}

		return false;
	}

	private String getSn() {
		String SnStr = "";
		String serialno = "";
		SnStr = SystemMix.getCmdPara(NAME_SN);
		serialno = SystemProperties.get("ro.serialno");
		if (SnStr != null) {
			return SnStr;
		} else {
			return serialno;
		}
	}

	private String getBtMac() {
		String BtMacStr = "";
		BtMacStr = SystemMix.getCmdPara(NAME_BT_MAC);
		if (BtMacStr != null) {
			return BtMacStr;
		} else {
			return "Null";
		}

	}

	@Override
	protected void onCaseFinished() {

	}

	@Override
	public String getDetailResult() {
		return "sn:" + snStr + "|" + "MAC:" + BtMacStr;
	}

	@Override
	protected void onRelease() {

	}

	protected void onPassableChange() {
		mUiHandler.post(new Runnable() {
			@Override
			public void run() {
				if (getPassable()) {
					getView().findViewById(R.id.snsn_name).setBackgroundColor(
							Color.GREEN);
				} else {
					getView().findViewById(R.id.snsn_name).setBackgroundColor(
							Color.RED);
				}
			}
		});
	}

}
