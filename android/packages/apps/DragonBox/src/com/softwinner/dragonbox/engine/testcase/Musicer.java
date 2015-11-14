package com.softwinner.dragonbox.engine.testcase;

import java.io.File;
import java.util.ArrayList;
import java.util.TimerTask;
import java.util.logging.Logger;

import com.softwinner.dragonbox.Main;
import com.softwinner.dragonbox.R;

import android.R.integer;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.widget.Toast;

public class Musicer {
        
    private final static String TAG = "Musicer";	
	private static Musicer instance;
	
	AudioManager mAM;
	TimerTask mTask;
	SoundPool mSoundPool;
	int soundIdLeft;
	int soundIdRight;
	int streamIdLeft;
	int streamIdRight;
	boolean flagLeft = false;
	boolean flagRight = false;
	boolean mSoundLeftPlayed = false;
	boolean mSoundRightPlayed = false;
	Context mContext;

	public static Musicer getMusicerInstance(Context context){
		if (instance == null){
			instance = new Musicer(context);
		}
		return instance;
	}
	
	private Musicer(Context context) {
		mContext = context;
		mAM = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
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
		int maxVolume = mAM.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mAM.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
		prepare();
		
	}

	private void prepare(){
		mSoundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
		mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool,
					int sampleId, int status) {
				Log.v("soundpool", "load  sample-" + sampleId + " complete!");
				if (sampleId == soundIdLeft){
					flagLeft = true;
				}else if(sampleId == soundIdRight){
					flagRight = true;
				}
				
			}
		});
		soundIdLeft = mSoundPool.load(mContext, R.raw.beatplucker, 1);
		soundIdRight = mSoundPool.load(mContext, R.raw.loveflute, 1);
	}
	
//	void prepareLeft() {
//
//		soundPoolLeft = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
//		soundIdLeft = soundPoolLeft.load(mContext, R.raw.beatplucker, 1);
//		//FIXME 
////		soundPoolLeft = Main.SOUNDPOOL_LEFT;
////		soundIdLeft = Main.SOUNDPOOL_LEFT_ID;
//		soundPoolLeft
//				.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
//					@Override
//					public void onLoadComplete(SoundPool soundPool,
//							int sampleId, int status) {
//						Log.v("soundpool", "load  left complete!");
//						flagLeft = true;
//					}
//				});
//	}
//
//	void prepareRight() {
//
//		soundPoolRight = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
//		soundIdRight = soundPoolRight.load(mContext, R.raw.loveflute, 1);
////		//FIXME
////		soundPoolRight = Main.SOUNDPOOL_RIGHT;
////		soundIdRight = Main.SOUNDPOOL_RIGHT_ID;
//		soundPoolRight
//				.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
//					@Override
//					public void onLoadComplete(SoundPool soundPool,
//							int sampleId, int status) {
//						Log.v("soundpool", "load  right complete!");
//						flagRight = true;
//					}
//				});
//	}

	void playLeft() {
		if (mSoundLeftPlayed){
			mSoundPool.resume(streamIdLeft);
			return;
		}
		Log.v("soundpool open", "soundPoolLeft:" + flagLeft);
		if (flagLeft) {
			streamIdLeft = mSoundPool.play(soundIdLeft, 1.0f, 0.0f, 1, -1, 1.0f);
			mSoundLeftPlayed = true;
		} else {
			Toast.makeText(mContext, R.string.case_spdif_loading,
					Toast.LENGTH_SHORT).show();
		}
	}

	void playRight() {
		if (mSoundRightPlayed){
			mSoundPool.resume(streamIdRight);
			return;
		}
		// TODO Auto-generated method stub
		Log.v("soundpool open", "soundPoolRight:" + flagRight);
		if (flagRight) {
			streamIdRight = mSoundPool.play(soundIdRight, 0.0f, 1.0f, 1, -1, 1.0f);
			mSoundRightPlayed = true;
		} else {
			Toast.makeText(mContext, R.string.case_spdif_loading,
					Toast.LENGTH_SHORT).show();
		}
	}

	void pause() {
//		if (soundPoolLeft != null) {
//			soundPoolLeft.release();
//			soundPoolLeft = null;
//		}
//		if (soundPoolRight != null) {
//			soundPoolRight.release();
//			soundPoolRight = null;
//		}
//		prepareLeft();
//		prepareRight();
		Log.v("soundpool", "stop()");
//		mSoundPool.pause(soundIdLeft);
		//pause all stream.
		mSoundPool.autoPause();
//		mSoundPool.autoResume();
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	public void waitUtilReady() {
		while(flagLeft == false || flagRight == false){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.v("soundpool", "waitting for res! flagLeft = " + flagLeft + "and flagRight" + flagRight);
		}
		
	}


	public void release(){
		mSoundPool.release();
		instance = null;
	}
}
