package com.softwinner.dragonbox.engine.testcase;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.DisplayManagerAw;
import android.media.AudioManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

import com.softwinner.dragonbox.R;
import com.softwinner.dragonbox.engine.BaseCase;
import com.softwinner.dragonbox.xml.Node;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

public class CaseHdmi extends BaseCase {
        
        private final static String TAG = "CaseHdmi";
	private DisplayManagerAw mDisplayManager;
	private AudioManager mAudioManager;
	private TimerTask mTask;

	private TextView hdmiStateTxt;
	private TextView hdmiSoundTxt;

	private LinearLayout inLayouthdmi;
	private LinearLayout soundLayouthdmi;
	private boolean finishState = false;
	private Handler mHandler = new Handler(Looper.getMainLooper());

	private boolean detectPass = false;
	private boolean soundPass = false;
	private Dialog alertDialog;

	@Override
	protected void onInitialize(Node attr) {

		mDisplayManager = (DisplayManagerAw) mContext
				.getSystemService(Context.DISPLAY_SERVICE_AW);
		mAudioManager = (AudioManager) mContext
				.getSystemService(Context.AUDIO_SERVICE);
		// TODO Auto-generated method stub
		setView(R.layout.case_hdmi);
		setName(R.string.case_hdmi_name);
		hdmiStateTxt = (TextView) getView().findViewById(R.id.hdmi_putin_text);
		hdmiSoundTxt = (TextView) getView().findViewById(R.id.hdmi_sound_text);
		inLayouthdmi = (LinearLayout) mView
				.findViewById(R.id.linearLayout_inline_hdmi);
		soundLayouthdmi = (LinearLayout) mView
				.findViewById(R.id.linearLayout_sound_hdmi);
	}

	@Override
	public void setPassable(boolean passable) {
		// 防止重复setPassable
		if (mTask != null) {
			mTask.cancel();
		}
		super.setPassable(passable);
		Log.v(TAG, "setPassable finish.");
	}

	@Override
	protected boolean onCaseStarted() {
		// TODO Auto-generated method stub

		mTask = new TimerTask() {
			@Override
			public void run() {
				((Activity) mContext).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!finishState) {
							if (mDisplayManager.getHdmiHotPlugStatus() == 1) {
								hdmiStateTxt.setText(R.string.case_hdmi_state);
								inLayouthdmi.setBackgroundColor(Color.GREEN);
								detectPass = true;
								//setPassable(detectPass && soundPass);
							} else {
								hdmiStateTxt
										.setText(R.string.case_hdmi_state_null);
								inLayouthdmi.setBackgroundColor(Color.RED);
								detectPass = false;
								//setPassable(detectPass && soundPass);
							}
							boolean result = detectPass && soundPass;
							if (getPassable() ^ result) {
								setPassable(result);
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

/*	private void changeToHdmi() {
		// TODO Auto-generated method stub

		if (mDisplayManager.getDisplayOutputType(0) != DisplayManagerAw.DISPLAY_OUTPUT_TYPE_HDMI) {
			mDisplayManager.setDisplayOutputType(0,
					DisplayManagerAw.DISPLAY_OUTPUT_TYPE_HDMI,
					DisplayManagerAw.DISPLAY_TVFORMAT_720P_50HZ);
			mDisplayManager
					.setDisplayMode(DisplayManagerAw.DISPLAY_MODE_SINGLE_FB_GPU);
			Log.v(TAG, "=========4=changeDisplay()===========");
		}
	}*/
	
	private void changeToHdmi() {
		DisplayManagerAw displayManager = mDisplayManager;
		AudioManager audioManager = mAudioManager;
		ArrayList<String> audioOutputChannels = audioManager
				.getActiveAudioDevices(AudioManager.AUDIO_OUTPUT_ACTIVE);
		if (mDisplayManager.getDisplayOutputType(0) != DisplayManagerAw.DISPLAY_OUTPUT_TYPE_HDMI) {
			displayManager.setDisplayOutputType(0,
					DisplayManagerAw.DISPLAY_OUTPUT_TYPE_HDMI,
					DisplayManagerAw.DISPLAY_TVFORMAT_720P_50HZ);
			displayManager.setDisplayParameter(0,
					DisplayManagerAw.DISPLAY_OUTPUT_TYPE_HDMI,
					DisplayManagerAw.DISPLAY_TVFORMAT_720P_50HZ);
			displayManager
					.setDisplayMode(DisplayManagerAw.DISPLAY_MODE_SINGLE_FB_GPU);
			Log.v(TAG, "=========4=changeDisplay()===========");
			if (audioManager == null) {
				Log.w("hehe", "audioManager is null");
				return;
			}
			audioOutputChannels.clear();
			audioOutputChannels.add(AudioManager.AUDIO_NAME_HDMI);
			audioManager.setAudioDeviceActive(audioOutputChannels,
					AudioManager.AUDIO_OUTPUT_ACTIVE);
			Log.v(TAG, "=========5=changeDisplay()===========");
		}	
	}

	public void startMenDetect() {

		changeToHdmi();
		final Musicer musicer = Musicer.getMusicerInstance(mContext);

		// -----------------------------------Dialog start inflate
		View layout = View.inflate(mContext, R.layout.alert_dlg, null);
		Button btnYes = (Button) layout.findViewById(R.id.yes);
		btnYes.setText("能听到");

		Button btnNo = (Button) layout.findViewById(R.id.no);
		btnNo.setText("没听到");

		Button btnNoUse = (Button) layout.findViewById(R.id.no_use);
		btnNoUse.requestFocus();

		TextView msg = (TextView) layout.findViewById(R.id.message);
		msg.setText("您是否能听到声音?");
		
		if(alertDialog!=null){
			alertDialog.dismiss();
			alertDialog = null;
		}
		alertDialog = new AlertDialog.Builder(mContext)
				.setTitle("声音测试")
				.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						// musicer.endit();
						musicer.pause();
					}
				}).setView(layout).create();
		alertDialog.getWindow().addFlags(0x02000000);
		btnYes.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				soundPass = true;
				hdmiSoundTxt.setText(R.string.case_hdmi_sound);
				soundLayouthdmi.setBackgroundColor(Color.GREEN);
				setPassable(detectPass && soundPass);
				alertDialog.dismiss();
			}
		});
		btnNo.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				soundPass = false;
				setPassable(detectPass && soundPass);
				hdmiSoundTxt.setText(R.string.case_hdmi_sound_null);
				soundLayouthdmi.setBackgroundColor(Color.RED);
				alertDialog.dismiss();
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
						alertDialog.show();
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
		if(alertDialog!=null){
			Log.d(TAG,"dismiss alertDialog");
			alertDialog.dismiss();
		}	
		if(mTask!=null)
			mTask.cancel();
	}

	protected void onPassableChange() {
		mUiHandler.post(new Runnable() {
			@Override
			public void run() {
				if (getPassable()) {
					getView().findViewById(R.id.hdmi_name).setBackgroundColor(
							Color.GREEN);
					finishState = true;
				} else {
					getView().findViewById(R.id.hdmi_name).setBackgroundColor(
							Color.RED);
				}
			}
		});
	}

}
