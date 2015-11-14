package com.softwinner.dragonbox.engine.testcase;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.util.Log;
import android.view.DisplayManagerAw;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Looper;
import android.os.Handler;

import com.softwinner.dragonbox.R;
import com.softwinner.dragonbox.engine.BaseCase;
import com.softwinner.dragonbox.xml.Node;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;

/**
 * CVBS测试
 * 
 * @author AW_maizirong
 */
public class CaseCvbs extends BaseCase {
        
        private static final String TAG = "CaseCvbs";
	private DisplayManagerAw mDisplayManager;
	private AudioManager mAudioManager;
	private ArrayList<String> mAudioOutputChannels;
	private TimerTask mTask;
	private boolean finishState = false;

	private TextView cvbsStateTxt;
	private TextView cvbsWatchTxt;
	private TextView cvbsSoundTxt;
	private LinearLayout inLayoutcvbs;
	private LinearLayout watchLayoutcvbs;
	private LinearLayout soundLayoutcvbs;
	private Handler mHandler = new Handler(Looper.getMainLooper());
	private boolean pluggedIn = false;
	private boolean mLeftSound = false;
	private boolean mRightSound = false;
	private Dialog leftSound;
	private Dialog rightSound;

	@Override
	protected void onInitialize(Node attr) {
		// TODO Auto-generated method stub
		setView(R.layout.case_cvbs);
		setName(R.string.case_cvbs_name);

		mDisplayManager = (DisplayManagerAw) mContext
				.getSystemService(Context.DISPLAY_SERVICE_AW);
		mAudioManager = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);
		mAudioOutputChannels = mAudioManager
				.getActiveAudioDevices(AudioManager.AUDIO_OUTPUT_ACTIVE);
		cvbsStateTxt = (TextView) getView().findViewById(R.id.cvbs_putin_text);
		cvbsWatchTxt = (TextView) getView().findViewById(R.id.cvbs_watch_text);
		cvbsSoundTxt = (TextView) getView().findViewById(R.id.cvbs_sound_text);
		inLayoutcvbs = (LinearLayout) mView
				.findViewById(R.id.linearLayout_inline_cvbs);
		watchLayoutcvbs = (LinearLayout) mView
				.findViewById(R.id.linearLayout_watch_cvbs);
		soundLayoutcvbs = (LinearLayout) mView
				.findViewById(R.id.linearLayout_sound_cvbs);
	}

	@Override
	protected boolean onCaseStarted() {

		mTask = new TimerTask() {
			@Override
			public void run() {
				((Activity) mContext).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!finishState) {
							if (mDisplayManager.getTvHotPlugStatus() == 2) {
								cvbsStateTxt.setText(R.string.case_cvbs_state);
								pluggedIn = true;
								inLayoutcvbs.setBackgroundColor(Color.GREEN);
							} else {
								cvbsStateTxt
										.setText(R.string.case_cvbs_state_null);
								pluggedIn = false;
								inLayoutcvbs.setBackgroundColor(Color.BLACK);
							}
							updataRuslt();
						}

					}
				});
			}
		};
		Timer timer = new Timer();
		timer.schedule(mTask, 100, 500);

		return false;
	}

	private void updataRuslt(){
		boolean result = pluggedIn && mLeftSound && mRightSound;
		if (getPassable() ^ result) {
			setPassable(result);
		}
	}

	private void changeToCvbs(){
		DisplayManagerAw displayManager = mDisplayManager;
		AudioManager audioManager = mAudioManager;
		ArrayList<String> audioOutputChannels = audioManager
				.getActiveAudioDevices(AudioManager.AUDIO_OUTPUT_ACTIVE);
		if (displayManager.getDisplayOutputType(0) != DisplayManagerAw.DISPLAY_OUTPUT_TYPE_TV
				|| displayManager.getDisplayOutputFormat(0) != DisplayManagerAw.DISPLAY_TVFORMAT_PAL) {
			displayManager.setDisplayOutputType(0,
					DisplayManagerAw.DISPLAY_OUTPUT_TYPE_TV,
					DisplayManagerAw.DISPLAY_TVFORMAT_PAL);
			displayManager.setDisplayParameter(0,
					DisplayManagerAw.DISPLAY_OUTPUT_TYPE_TV,
					DisplayManagerAw.DISPLAY_TVFORMAT_PAL);
			displayManager
					.setDisplayMode(DisplayManagerAw.DISPLAY_MODE_SINGLE_FB_GPU);
			Log.v(TAG, "=========1=changeDisplay()===========");
			// 设置音频输出模式
			if (audioManager == null) {
				Log.w(TAG, "audioManager is null");
				return;
			}
			audioOutputChannels.clear();
			audioOutputChannels.add(AudioManager.AUDIO_NAME_CODEC);
			audioManager.setAudioDeviceActive(audioOutputChannels,
					AudioManager.AUDIO_OUTPUT_ACTIVE);
			Log.v(TAG, "=========3=changeDisplay()===========");
		}
	}
	
	public void startMenDetect() {
		changeToCvbs();

		final Musicer musicer = Musicer.getMusicerInstance(mContext);
		// musicer.prepareLeft();
		// musicer.prepareRight();

		// -----------------------------------Right Dialog start inflate
		View layout = View.inflate(mContext, R.layout.alert_dlg, null);
		Button btnYes = (Button) layout.findViewById(R.id.yes);
		btnYes.setText("能听到");

		Button btnNo = (Button) layout.findViewById(R.id.no);
		btnNo.setText("没听到");

		Button btnNoUse = (Button) layout.findViewById(R.id.no_use);
		btnNoUse.requestFocus();

		TextView msg = (TextView) layout.findViewById(R.id.message);
		msg.setText("您是否能听到声音?");
		rightSound = new AlertDialog.Builder(mContext)
				.setTitle("Cvbs右声道测试")
				.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						// musicer.endit();
						musicer.pause();

					}
				}).setView(layout).create();
		rightSound.getWindow().addFlags(0x02000000);
		btnNo.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mRightSound = false;
				setPassable(pluggedIn && mLeftSound && mRightSound);
				cvbsSoundTxt.setText("右声道 异常");
				soundLayoutcvbs.setBackgroundColor(Color.RED);
				rightSound.dismiss();
			}
		});
		btnYes.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mRightSound = true;
				setPassable(pluggedIn && mLeftSound && mRightSound);
				cvbsSoundTxt.setText(R.string.case_cvbs_right_2);
				soundLayoutcvbs.setBackgroundColor(Color.GREEN);
				rightSound.dismiss();
			}
		});
		// -----------------------------------Right Dialog end inflate

		// -----------------------------------Left Dialog start inflate
		layout = View.inflate(mContext, R.layout.alert_dlg, null);
		btnYes = (Button) layout.findViewById(R.id.yes);
		btnYes.setText("能听到");

		btnNo = (Button) layout.findViewById(R.id.no);
		btnNo.setText("没听到");

		btnNoUse = (Button) layout.findViewById(R.id.no_use);
		btnNoUse.requestFocus();

		msg = (TextView) layout.findViewById(R.id.message);
		msg.setText("您是否能听到声音?");
		leftSound = new AlertDialog.Builder(mContext)
				.setTitle("Cvbs左声道测试")
				.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						// musicer.endit();
						musicer.pause();
						musicer.playRight();
					}
				}).setView(layout).create();
		leftSound.getWindow().addFlags(0x02000000);
		btnNo.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mLeftSound = false;
				cvbsWatchTxt.setText("左声道 异常");
				watchLayoutcvbs.setBackgroundColor(Color.RED);
				leftSound.dismiss();
				rightSound.show();
			}
		});
		btnYes.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mLeftSound = true;
				rightSound.show();
				cvbsWatchTxt.setText(R.string.case_cvbs_left_2);
				watchLayoutcvbs.setBackgroundColor(Color.GREEN);
				leftSound.dismiss();
			}
		});
		// -----------------------------------Left Dialog end inflate
		Runnable r = new Runnable() {

			@Override
			public void run() {
				musicer.waitUtilReady();
				mHandler.postDelayed(new Runnable() {

					public void run() {
						musicer.playLeft();
						leftSound.show();
					}
				}, 100);
			}
		};

		Thread thread = new Thread(r);
		thread.start();

	}

	@Override
	protected void onCaseFinished() {
		// TODO Auto-generated method stub
		// 保持在同一线程
		mEngine.getWorkerHandler().post(new Runnable() {
			@Override
			public void run() {

				if (mTask != null) {
					mTask.cancel();
				}
			}
		});
	}

	@Override
	protected void onRelease() {
		// TODO Auto-generated method stub
		if(leftSound!=null)
			leftSound.dismiss();
		if(rightSound!=null)
			rightSound.dismiss();
	}

	protected void onPassableChange() {
		mUiHandler.post(new Runnable() {
			@Override
			public void run() {
				if (getPassable()) {
					getView().findViewById(R.id.cvbs_name).setBackgroundColor(
							Color.GREEN);
					finishState = true;
				} else {
					getView().findViewById(R.id.cvbs_name).setBackgroundColor(
							Color.RED);
				}
			}
		});
	}
}
