package com.softwinner.dragonbox.engine.testcase;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.os.SystemProperties;

import android.util.Log;
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

public class CaseResult extends BaseCase {
	private TimerTask mTask;

	@Override
	protected void onInitialize(Node attr) {
		setView(R.layout.case_snsn);
		setName(R.string.case_snsn_name);
	}

	@Override
	protected boolean onCaseStarted() {
		final TextView tv = (TextView) getView().findViewById(R.id.snsn_info);
		mTask = new TimerTask() {
			@Override
			public void run() {
				((Activity) mContext).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (mEngine.getCaseSize() > 0) {
							if (allPassed()) {
								tv.setText("OK");
								tv.setTextSize(100);
								tv.setTextColor(Color.GREEN);
							} else {
								tv.setText("No OK");
								tv.setTextSize(100);
								tv.setTextColor(Color.RED);
							}
						}
					}
				});
			}
		};
		Timer timer = new Timer();
		timer.schedule(mTask, 100, 500);

		return false;
	}

	private boolean allPassed() {
		int n = mEngine.getCaseSize();
		if (n == 0)
			return false;
		boolean ret = true;
		for (int i = 0; i < n; i++) {
			ret &= mEngine.getCase(i).getResult() == BaseCase.RESULT_PASSED;
		}
		return ret;
	}

	@Override
	protected void onCaseFinished() {

	}

	@Override
	protected void onRelease() {

	}

	protected void onPassableChange() {
		mUiHandler.post(new Runnable() {
			@Override
			public void run() {
				if (getPassable()) {

				} else {

				}
			}
		});
	}

}
