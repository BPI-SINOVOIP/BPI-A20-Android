package com.softwinner.dragonbox.engine.testcase;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.content.DialogInterface;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.softwinner.dragonbox.R;
import com.softwinner.dragonbox.engine.BaseCase;
import com.softwinner.dragonbox.xml.Node;

/**
 * spdif测试
 * 
 * @author AW_maizirong
 * 
 */
public class CaseSpdif extends BaseCase {
        
    private static final String TAG = "CaseSpdif";
	private TextView spidfLeftTxt;
	private TextView spidfRightTxt;
	//private LinearLayout inLayoutspidf;
	private LinearLayout leftLayoutspidf;
	private LinearLayout rightLayoutspidf;

	private boolean soundStateLeft = false;
	private boolean soundStateRight = false;

	private AudioManager mAM;

	private TimerTask mTask;
	private Handler mHandler = new Handler(Looper.getMainLooper());
	private SoundPool soundPool;
	private int soundId;
	private boolean flag = false;

	private int exitNum = 0;
	String fileName;
	File file;
	private Dialog leftSound;
	private Dialog rightSound;

	@Override
	protected void finalize() throws Throwable {

		super.finalize();
	}


	@Override
	protected void onInitialize(Node attr) {
		fileName = "/mnt/private/factory_pass";
		file = new File(fileName);
		setView(R.layout.case_spidf);

		mAM = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

		soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
		soundId = soundPool.load(mContext, R.raw.beatplucker, 1);
		soundPool
				.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
					@Override
					public void onLoadComplete(SoundPool soundPool,
							int sampleId, int status) {
						flag = true;
					}
				});

		// 保持在同一线程
		mEngine.getWorkerHandler().post(new Runnable() {
			@Override
			public void run() {

			}
		});

		spidfLeftTxt = (TextView) getView().findViewById(R.id.spidf_left_text);
		spidfRightTxt = (TextView) getView().findViewById(R.id.spidf_right_text);
		//inLayoutspidf = (LinearLayout) mView
		//		.findViewById(R.id.linearLayout_inline_spidf);
		leftLayoutspidf = (LinearLayout) mView
				.findViewById(R.id.linearLayout_left_spidf);
		rightLayoutspidf = (LinearLayout) mView
				.findViewById(R.id.linearLayout_right_spidf);
		setName(R.string.case_speaker_name);
	}

	public void startMenDetect() {
		final Musicer musicer = Musicer.getMusicerInstance(mContext);
		final ArrayList<String> audioOutputChannels = mAM
				.getActiveAudioDevices(AudioManager.AUDIO_OUTPUT_ACTIVE);

		for (String stc : audioOutputChannels) {
			Log.e(TAG, "=audioOutputChannels=" + "." + stc + ".");
		}
		ArrayList<String> allOutPutchannels = new ArrayList<String>();
		allOutPutchannels.clear();
		allOutPutchannels.add(AudioManager.AUDIO_NAME_CODEC);
		allOutPutchannels.add(AudioManager.AUDIO_NAME_HDMI);
		allOutPutchannels.add(AudioManager.AUDIO_NAME_SPDIF);

		mAM.setAudioDeviceActive(allOutPutchannels,
				AudioManager.AUDIO_OUTPUT_ACTIVE);

		View layout = View.inflate(mContext, R.layout.alert_dlg, null);
		Button btnYes = (Button) layout.findViewById(R.id.yes);
		btnYes.setText("能听到");

		Button btnNo = (Button) layout.findViewById(R.id.no);
		btnNo.setText("没听到");

		Button btnNoUse = (Button) layout.findViewById(R.id.no_use);
		btnNoUse.requestFocus();

		TextView msg = (TextView) layout.findViewById(R.id.message);
		msg.setText("您是否能听到声音?");
		leftSound = new AlertDialog.Builder(mContext)
				.setTitle("Spdif左声道测试")
				.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						musicer.pause();
						musicer.playRight();
						rightSound.show();
					}
				}).setView(layout).create();
		leftSound.getWindow().addFlags(0x02000000);

		btnNo.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				soundStateLeft = false;
				//setPassable(soundStateLeft && soundStateRight);
				spidfLeftTxt.setText("左声道 异常");
				leftLayoutspidf.setBackgroundColor(Color.RED);
				leftSound.dismiss();
			}
		});
		btnYes.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				soundStateLeft = true;
				//setPassable(soundStateLeft && soundStateRight);
				spidfLeftTxt.setText(R.string.case_spidf_left_2);
				leftLayoutspidf.setBackgroundColor(Color.GREEN);
				leftSound.dismiss();
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
		rightSound = new AlertDialog.Builder(mContext)
				.setTitle("Spdif右声道测试")
				.setOnDismissListener(new DialogInterface.OnDismissListener() {
					public void onDismiss(DialogInterface dialog) {
						setPassable(soundStateLeft && soundStateRight);
						musicer.pause();
						mAM.setAudioDeviceActive(audioOutputChannels,
								AudioManager.AUDIO_OUTPUT_ACTIVE);
					}
				}).setView(layout).create();
		rightSound.getWindow().addFlags(0x02000000);
		btnNo.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				soundStateRight = false;
				spidfRightTxt.setText("右声道 异常");
				rightLayoutspidf.setBackgroundColor(Color.RED);
				rightSound.dismiss();
			}
		});
		btnYes.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				soundStateRight = true;
				spidfRightTxt.setText(R.string.case_spidf_right_2);
				rightLayoutspidf.setBackgroundColor(Color.GREEN);
				rightSound.dismiss();
			}
		});
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
	protected boolean onCaseStarted() {
		// ArrayList<String> list =
		// mAM.getAudioDevices(AudioManager.AUDIO_OUTPUT_TYPE);

		ArrayList<String> audioOutputChannels = mAM
				.getActiveAudioDevices(AudioManager.AUDIO_OUTPUT_ACTIVE);

		for (String stc : audioOutputChannels) {
			Log.e(TAG, "=audioOutputChannels=" + "." + stc + ".");
		}
		audioOutputChannels.clear();
		audioOutputChannels.add(AudioManager.AUDIO_NAME_CODEC);
		audioOutputChannels.add(AudioManager.AUDIO_NAME_HDMI);
		audioOutputChannels.add(AudioManager.AUDIO_NAME_SPDIF);

		mAM.setAudioDeviceActive(audioOutputChannels,
				AudioManager.AUDIO_OUTPUT_ACTIVE);

		// 保持在同一线程
		mEngine.getWorkerHandler().post(new Runnable() {
			@Override
			public void run() {

			}
		});

		mTask = new TimerTask() {
			@Override
			public void run() {
				((Activity) mContext).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (exitNum > 15) {
							Log.e(TAG, "=======exitNum > 15======");
							// System.exit(0);

							if (file.exists()) {
								Log.e(TAG, "=========exit========");
								throw new RuntimeException(
										"Artificial Crash By User.");
							}
						}
						updataResult();
					}
				});
			}
		};
		Timer timer = new Timer();
		timer.schedule(mTask, 100, 500);

		return false;
	}

	private void updataResult(){
		boolean result = soundStateLeft && soundStateRight;
		if (getPassable() ^ result) {
			setPassable(result);
		}
	}

	@Override
	protected void onCaseFinished() {

		// 保持在同一线程
		mEngine.getWorkerHandler().post(new Runnable() {
			@Override
			public void run() {
				if (soundPool != null) {
					soundPool.release();
					soundPool = null;
				}
				if (mTask != null) {
					mTask.cancel();
				}
			}
		});
		// mMediaPlayer.setRawDataMode(MediaPlayer.AUDIO_DATA_MODE_HDMI_RAW);
	}

	@Override
	protected void onRelease() {
		// 保持在同一线程
		mEngine.getWorkerHandler().post(new Runnable() {
			@Override
			public void run() {

			}
		});
	}

	@Override
	protected void onPassableChange() {
		super.onPassableChange();
		if (getPassable()) {
			getView().findViewById(R.id.spdif_name).setBackgroundColor(
					Color.GREEN);
		} else {
			getView().findViewById(R.id.spdif_name).setBackgroundColor(
					Color.RED);
		}
	}

}
