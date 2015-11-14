package com.softwinner.dragonbox.engine.testcase;

import android.graphics.Color;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.softwinner.dragonbox.R;
import android.os.Looper;
import android.os.Handler;

import com.softwinner.dragonbox.engine.BaseCase;
import com.softwinner.dragonbox.xml.Node;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;

public class CaseLed extends BaseCase {

	private TextView ledTxt;
	private LinearLayout ledLayout;

	private Button leftbtn;
	private Button rightbtn;
	private Button stopbtn;
	private Musicer mc;
	private Dialog alertDialog;
	private Handler mHandler = new Handler(Looper.getMainLooper());

	@Override
	protected void onInitialize(Node attr) {
		setView(R.layout.case_led);
		setName(R.string.case_led_name);
		// mc = new Musicer(mContext);
		// mc.prepareLeft();
		// mc.prepareRight();

		ledTxt = (TextView) getView().findViewById(R.id.led_info_text);

		ledLayout = (LinearLayout) mView
				.findViewById(R.id.linearLayout_info_led);

	}

	public void startMenDetect() {

		// -----------------------------------Dialog start inflate
		View layout = View.inflate(mContext, R.layout.alert_dlg, null);
		Button btnYes = (Button) layout.findViewById(R.id.yes);
		btnYes.setText("是的");

		Button btnNo = (Button) layout.findViewById(R.id.no);
		btnNo.setText("没有");

		Button btnNoUse = (Button) layout.findViewById(R.id.no_use);
		btnNoUse.requestFocus();

		TextView msg = (TextView) layout.findViewById(R.id.message);
		msg.setText("LED灯是否处于亮的状态？");

		alertDialog = new AlertDialog.Builder(mContext)
				.setTitle("LED测试").setView(layout).create();
		alertDialog.getWindow().addFlags(0x02000000);
		btnNo.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setPassable(false);
				alertDialog.dismiss();
			}
		});
		btnYes.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setPassable(true);
				alertDialog.dismiss();
			}
		});
		// -----------------------------------Left Dialog end inflate
		mHandler.postDelayed(new Runnable() {

			public void run() {
				alertDialog.show();
			}
		}, 500);
	}

	@Override
	protected boolean onCaseStarted() {

		return false;
	}

	@Override
	protected void onCaseFinished() {

	}

	@Override
	protected void onRelease() {
		if(alertDialog!=null)
			alertDialog.dismiss();
	}

	protected void onPassableChange() {
		mUiHandler.post(new Runnable() {
			@Override
			public void run() {
				if (getPassable()) {
					ledTxt.setText(R.string.case_led_state);
					ledLayout.setBackgroundColor(Color.GREEN);
					getView().findViewById(R.id.led_name).setBackgroundColor(
							Color.GREEN);
				} else {
					ledTxt.setText(R.string.case_led_state_fail);
					ledLayout.setBackgroundColor(Color.RED);
					getView().findViewById(R.id.led_name).setBackgroundColor(
							Color.RED);
				}
			}
		});
	}

}
