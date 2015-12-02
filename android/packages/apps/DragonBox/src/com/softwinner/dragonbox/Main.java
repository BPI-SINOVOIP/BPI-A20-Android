package com.softwinner.dragonbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import android.R.integer;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.SharedPreferences; 
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.content.BroadcastReceiver;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.SystemProperties;
import android.os.SystemClock;
import android.text.Html;
import android.util.Log;
import android.view.DisplayManagerAw;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowManager;

import com.android.internal.os.storage.ExternalStorageFormatter;
import com.softwinner.dragonbox.Configuration.OnActivityResult;
import com.softwinner.dragonbox.platform.BasePlatform;
import com.softwinner.dragonbox.engine.BaseCase;
import com.softwinner.dragonbox.engine.DFEngine;
import com.softwinner.dragonbox.engine.LockLayer;
import com.softwinner.dragonbox.engine.UICallback;
import com.softwinner.dragonbox.engine.Utils;
import com.softwinner.dragonbox.engine.testcase.Musicer;

public class Main extends Activity implements UICallback, View.OnClickListener,
		AdapterView.OnItemClickListener {
	/** Called when the activity is first created. */
	
	private static final String TAG = "DragonBox";
	private static final int STATE_TEST_CASE = 0;
	private static final int STATE_SHOW_RESULT = 1;
	private static final int STATE_SHOW_CASE_LIST = 2;
	public static final String useLockButton = "useLockButton";
	public static final boolean DEVELOPER_MODE = false;
	private ViewGroup mCaseContent;

	private DFEngine mEngine;

	public static final int ACTION_BAR_EXIT = 1;
	public static final int ACTION_BAR_ALL_LIST = 2;
	public static final int ACTION_BAR_AUDIO = 3;
	public static final int ACTION_BAR_VIDEO = 4;

	private int mCurrentState = STATE_TEST_CASE;
	LockLayer lockLayer;
	View mCurrentLockView;

	private boolean testCvbs = false;
	private boolean testHdmi = false;

	/*
	 * private View mPassed; private View mPrevious; private View mFailed;
	 * private View mSkips;
	 */

	private int mPosition;
	private AlertDialog mResultDialog;

	public static final int FLAG_DISMISS_HOMEKEY = 0x02000000;

	// FIXME
	public static SoundPool SOUNDPOOL_LEFT;
	public static int SOUNDPOOL_LEFT_ID;
	public static SoundPool SOUNDPOOL_RIGHT;
	public static int SOUNDPOOL_RIGHT_ID;
	
	private ResetReceiver mResetReceiver;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getWindow().addFlags(FLAG_DISMISS_HOMEKEY);
		mResetReceiver = new ResetReceiver();
		mResetReceiver.registerAction(this);
		switchView();
		if (DEVELOPER_MODE) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
					.detectAll().penaltyLog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectAll().penaltyLog().penaltyDeath().build());
		}
		this.getWindow().setFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
		// R.layout.main_title);
		mCaseContent = (ViewGroup) findViewById(R.id.case_content);
		lockLayer = new LockLayer(this);
		long t = System.currentTimeMillis();
		resetFactory();
		mEngine = new DFEngine(this);
		mEngine.setUICallback(this);
		Log.v(Utils.APP_TAG,
				"initial the engine cost " + (System.currentTimeMillis() - t)
						+ "ms");
		DragonBoxApp app = (DragonBoxApp) getApplication();
		app.setEngine(mEngine);
		mEngine.startNextCase();

	}
	class ResetReceiver extends BroadcastReceiver {
		private final static String ACTION_RESET = "com.softwinner.dragonbox.ACTION_RESET";
		private long lastResetTick = 0;
		private long currentResetTick = 0;
		public void registerAction(Context context){   
			Log.d(TAG,"ResetReceiver registerAction");
	        IntentFilter filter = new IntentFilter();   
	        filter.addAction(ACTION_RESET);       
	        context.registerReceiver(this, filter);   
	    }  
		@Override
		public void onReceive(Context context, Intent intent) {
			if(ACTION_RESET.equals(intent.getAction())){
				lastResetTick = currentResetTick;
				currentResetTick = SystemClock.elapsedRealtime();
				Log.d(TAG,"lastResetTick = " + lastResetTick + "currentResetTick = " + currentResetTick + "c-l = " +(currentResetTick - lastResetTick));
				if(currentResetTick - lastResetTick>3000){
					Log.d(TAG,"receive reset command");
					resetDragonBox();
				}
			}
		}
	}
	public void resetDragonBox(){
		Log.d(TAG,"resetDragonBox");
		if(mResultDialog!=null)
			mResultDialog.dismiss();
		if(mEngine!=null)
			mEngine.release();
		Musicer musicer = Musicer.getMusicerInstance(this);
		if (musicer != null) {
			musicer.release();
		}
		resetFactory();
		lockLayer = new LockLayer(this);
		long t = System.currentTimeMillis();
		mEngine = new DFEngine(this);
		mEngine.setUICallback(this);
		Log.v(Utils.APP_TAG,
				"initial the engine cost " + (System.currentTimeMillis() - t)
						+ "ms");
		mCurrentState = STATE_TEST_CASE;
		testCvbs = false;
		testHdmi = false;
		DragonBoxApp app = (DragonBoxApp) getApplication();
		app.setEngine(mEngine);
		mEngine.startNextCase();
	}
	
	private void killMyProcess() {
		ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		Log.v(TAG, "----getPackageName----" + getPackageName());
		am.killBackgroundProcesses(getPackageName());
		Log.v(TAG, "---------killMyself---------");
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		if (mEngine != null) {
			mEngine.release();
		}
		finish();
		killMyProcess();
		Log.v(TAG, "---------onStop()---------");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG, "---------onResume()---------");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.v(TAG, "---------onPause()---------");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mEngine != null) {
			mEngine.release();
		}

		Musicer musicer = Musicer.getMusicerInstance(this);
		if (musicer != null) {
			musicer.release();
		}
		finish();
		killMyProcess();
		Log.v(TAG, "---------onDestroy()---------");
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
				|| keyCode == KeyEvent.KEYCODE_VOLUME_UP
				|| keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) {
			return super.onKeyDown(keyCode, event);
		}
		return false;
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		int order = 0;
		/*
		 * if (mCurrentState == STATE_TEST_CASE) { MenuItem exit = menu.add(0,
		 * ACTION_BAR_EXIT, order++, R.string.result_exit);
		 * exit.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		 * 
		 * MenuItem allCase = menu.add(0, ACTION_BAR_ALL_LIST, order++,
		 * R.string.action_bar_all_list);
		 * allCase.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		 * 
		 * MenuItem audioSwitch = menu.add(0, ACTION_BAR_AUDIO, order++,
		 * R.string.action_bar_audio);
		 * audioSwitch.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		 * 
		 * MenuItem videoSwitch = menu.add(0, ACTION_BAR_VIDEO, order++,
		 * R.string.action_bar_video);
		 * videoSwitch.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS); } else
		 * if (mCurrentState == STATE_SHOW_CASE_LIST) { MenuItem exit =
		 * menu.add(0, ACTION_BAR_EXIT, order++, R.string.result_exit);
		 * exit.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS); }
		 */
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case ACTION_BAR_EXIT:
			int result = mEngine.getCurCase().getResult();
			try {
				mEngine.setCurCaseResult(result);
			} catch (Exception e) {
			}
			finish();
			return true;
		case ACTION_BAR_ALL_LIST:
			mCurrentState = STATE_SHOW_CASE_LIST;
			result = mEngine.getCurCase().getResult();
			mEngine.setCurCaseResult(result);
			switchView();
			return true;
		case ACTION_BAR_AUDIO:
			switchAudio();
			return true;
		case ACTION_BAR_VIDEO:
			switchVideo();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void switchVideo() {
		// TODO Auto-generated method stub
		Log.v(TAG, "===========switchVideo===========");
	}

	private void switchAudio() {
		// TODO Auto-generated method stub
		Log.v(TAG, "===========switchAudio===========");

		AudioManager mAudioManager = (AudioManager) this
				.getSystemService(Context.AUDIO_SERVICE);
		ArrayList<String> list = mAudioManager
				.getAudioDevices(AudioManager.AUDIO_OUTPUT_TYPE);

		for (String st : list) {
			Log.v(TAG, st);

		}

		// mAudioManager.setAudioDeviceActive(list,
		// AudioManager.AUDIO_OUTPUT_ACTIVE);
	}

	private void switchToMain() {
		setContentView(R.layout.main);
		if (mCaseContent != null) {
			mCaseContent.removeAllViews();
		}
		mCaseContent = (ViewGroup) findViewById(R.id.case_content);
		/*
		 * mPassed = findViewById(R.id.passed);
		 * mPassed.setOnClickListener(this); mFailed =
		 * findViewById(R.id.failed); mFailed.setOnClickListener(this);
		 * mPrevious = findViewById(R.id.previous);
		 * mPrevious.setOnClickListener(this); mSkips = findViewById(R.id.skip);
		 * mSkips.setOnClickListener(this);
		 */
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

	private void switchToResult() {
		ResultsAdapter mResultAdapter = new ResultsAdapter();
		boolean result = allPassed();
		setResult(result ? 1 : 0);
		setContentView(R.layout.result);
		findViewById(R.id.uninstall).setOnClickListener(this);
		findViewById(R.id.recovery).setOnClickListener(this);
		findViewById(R.id.btn_exit).setOnClickListener(this);
		ListView lv = (ListView) findViewById(R.id.result_list);
		// lv.setOnItemClickListener(this);
		lv.setAdapter(mResultAdapter);
		setTitle(R.string.result_output);
	}

	//
	public ArrayList<HashMap<String, Object>> resultData;

	public class ResultsAdapter extends BaseAdapter {
		public ResultsAdapter() {
			try {
				CsvUtil test = new CsvUtil("/DragonBox/FATPLog.csv");
				resultData = test.getDate();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		@Override
		public int getCount() {
			return resultData.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			if (convertView == null) {
				convertView = View.inflate(Main.this, R.layout.result_item,
						null);
			}
			TextView tv = (TextView) convertView;
			tv.setText(resultData.get(position).keySet() + ":"
					+ resultData.get(position).values());
			String result = tv.getText().toString();
			if (result.indexOf(String.valueOf("FAIL")) == -1) {
				tv.setBackgroundColor(Color.GREEN);
			} else {
				tv.setBackgroundColor(Color.RED);
			}

			return convertView;
		}
	}

	private void switchToList() {
		setContentView(R.layout.all_case_list);
		ListView lv = (ListView) findViewById(R.id.list);
		lv.setOnItemClickListener(this);
		lv.setAdapter(mAdapter);
		setTitle(R.string.title_all_list);
		findViewById(R.id.btn_return).setOnClickListener(this);
	}

	private void switchView() {
		switch (mCurrentState) {
		case STATE_TEST_CASE:
			switchToMain();
			break;
		case STATE_SHOW_RESULT:
			// switchToResult();
			break;
		case STATE_SHOW_CASE_LIST:
			switchToList();
			break;
		}
		invalidateOptionsMenu();
	}

	@Override
	public void setCaseContent(View v) {
		if (v != null) {
			if (useLockButton.equals(v.getTag())) {
			}
			mCaseContent.removeAllViews();
			mCaseContent.addView(v);
			// setTitle(Html.fromHtml(mEngine.getTitle()));
		}
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (useLockButton.equals(v.getTag())) {
			// lockLayer.unlockTop();
		}
		switch (id) {
		case R.id.passed:
			passedit();
			break;
		case R.id.failed:
			failedit();
			break;
		case R.id.previous:
			mEngine.startPreCase();
			break;
		case R.id.recovery:
			final CheckBox cb = new CheckBox(this);
			cb.setText(getResources().getString(R.string.format_sd));
			new AlertDialog.Builder(this)
					.setTitle(R.string.reset_device)
					.setView(cb)
					.setPositiveButton(com.android.internal.R.string.ok,
							new OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									if (cb.isChecked()) {
										Intent intent;
										intent = new Intent(
												ExternalStorageFormatter.FORMAT_AND_FACTORY_RESET);
										intent.setComponent(ExternalStorageFormatter.COMPONENT_NAME);
										startService(intent);
									} else {
										sendBroadcast(new Intent(
												"android.intent.action.MASTER_CLEAR"));
									}
								}
							})
					.setNeutralButton(com.android.internal.R.string.no, null)
					.create().show();
			break;
		case R.id.uninstall:
			// Intent uninstall;
			// Uri packageURI = Uri.parse("package:" +
			// "com.softwinner.dragonbox");
			// uninstall = new
			// Intent("android.intent.action.UNINSTALL_PACKAGE",packageURI);
			// startActivity(uninstall);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setPositiveButton(com.android.internal.R.string.ok,
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface arg0, int arg1) {
							try {
								mEngine.release();
							} catch (Exception e) {
								e.printStackTrace();
							}
							// try{
							// Runtime.getRuntime().exec("rm /system/app/DragonPhone*.apk");
							// Runtime.getRuntime().exec("pm uninstall -k com.softwinner.dragonphone");
							// Runtime.getRuntime().exec("rm /system/app/DragonBox*.apk");
							// Runtime.getRuntime().exec("pm uninstall -k com.softwinner.dragonbox");
							// }catch(IOException e){
							// e.printStackTrace();
							// }
							RootCmd.execRootCmdSilent("rm /system/app/DragonPhone*.apk");
							RootCmd.execRootCmdSilent("pm uninstall -k com.softwinner.dragonphone");
							RootCmd.execRootCmdSilent("rm /system/app/DragonBox*.apk");
							RootCmd.execRootCmdSilent("pm uninstall -k com.softwinner.dragonbox");
						}
					}).setTitle(R.string.uninstall_dragonbox_description)
					.create().show();
			break;
		case R.id.btn_exit:
			finish();
			break;
		/*
		 * case R.id.skip: mEngine.setCurCaseResult(BaseCase.RESULT_RESET); if
		 * (STATE_TEST_CASE == mCurrentState) { mEngine.startNextCase(); } else
		 * { switchView(); } break;
		 */
		case R.id.btn_return:
			mCurrentState = STATE_TEST_CASE;
			switchView();
			mEngine.startCurCaseAtFlowing();
			break;
		}
	}

	private void failedit() {
		// TODO Auto-generated method stub
		if (STATE_TEST_CASE == mCurrentState) {
			mEngine.setCurCaseResult(BaseCase.RESULT_UNPASSED);
			mEngine.startNextCase();
		} else if (STATE_SHOW_CASE_LIST == mCurrentState) {
			mEngine.getCase(mPosition).setResult(BaseCase.RESULT_UNPASSED);
			switchView();
		} else if (STATE_SHOW_RESULT == mCurrentState) {
			mEngine.setCurCaseResult(BaseCase.RESULT_UNPASSED);
			switchView();
		}
	}

	private void passedit() {
		// TODO Auto-generated method stub
		if (STATE_TEST_CASE == mCurrentState) {
			mEngine.setCurCaseResult(BaseCase.RESULT_PASSED);
			mEngine.startNextCase();
		} else if (STATE_SHOW_CASE_LIST == mCurrentState) {
			mEngine.getCase(mPosition).setResult(BaseCase.RESULT_PASSED);
			switchView();
		} else if (STATE_SHOW_RESULT == mCurrentState) {
			mEngine.setCurCaseResult(BaseCase.RESULT_PASSED);
			switchView();
		}
	}

	@Override
	public void onCaseCompleted() {
		mCurrentState = STATE_SHOW_RESULT;
		// switchView();
		if (mEngine.getCaseSize() > 0) {
			writeFactory();
			if (isRunSn()) {
				String TestPackageName = SystemProperties.get(
						"ro.sw.snapkpackage", "no");
				String TestClassName = SystemProperties.get("ro.sw.snapkclass",
						"no");
				Intent intent = new Intent();
				intent.setComponent(new ComponentName(TestPackageName,
						TestClassName));
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				this.startActivity(intent);
			} else {
				showResultDialog();
			}
		} else {
			System.out.print("OPEN XML ERROR");
		}
	}

	public boolean isRunSn() {
		String extsd_sn_path = new String(
				"/mnt/sdcard/DragonBox/custom_sn_cases.xml");
		String usbhost0_sn_path = new String(
				"/mnt/usbhost0/DragonBox/custom_sn_cases.xml");
		String usbhost1_sn_path = new String(
				"/mnt/usbhost1/DragonBox/custom_sn_cases.xml");
		String usbhost2_sn_path = new String(
				"/mnt/usbhost2/DragonBox/custom_sn_cases.xml");
		String usbhost3_sn_path = new String(
				"/mnt/usbhost3/DragonBox/custom_sn_cases.xml");
		if ((isDirExist(extsd_sn_path) || isDirExist(usbhost0_sn_path)
				|| isDirExist(usbhost1_sn_path) || isDirExist(usbhost2_sn_path) || isDirExist(usbhost3_sn_path))) {
			return true;
		} else {
			return false;
		}
	}

	public boolean isDirExist(String path) {
		File dir = new File(path);
		if (dir.exists())
			return true;
		else
			return false;
	}

	private void showResultDialog() {
		// View view = View.inflate(this, R.layout.finish_view, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.finish_title)
				// .setView(view)
                .setMessage("测试已通过！")
				.setCancelable(false)/*
				.setPositiveButton(R.string.finish_uninstall,
						new OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								try {
									mEngine.release();
								} catch (Exception e) {
									e.printStackTrace();
								}
								RootCmd.execRootCmdSilent("rm /system/app/DragonPhone*.apk");
								RootCmd.execRootCmdSilent("pm uninstall -k com.softwinner.dragonphone");
								RootCmd.execRootCmdSilent("rm /system/app/DragonBox*.apk");
								RootCmd.execRootCmdSilent("pm uninstall -k com.softwinner.dragonbox");
							}
						})*/
				/*
				 * .setNegativeButton(R.string.finish_hide, new
				 * OnClickListener() {
				 * 
				 * @Override public void onClick(DialogInterface dialog, int
				 * arg1) { dialog.dismiss(); } })
				 */
				.setPositiveButton(R.string.finish_exit, new OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						finish();
					}

				});
		mResultDialog = builder.create();
		mResultDialog.getWindow().addFlags(0x02000000);
		Button changeBtn;
		mResultDialog.show();
		changeBtn = (Button) mResultDialog.getWindow().findViewById(
				android.R.id.button2);
		changeBtn.requestFocus();

	}

	private void changeDisplay() {
		showResultDialog();
		Log.v(TAG, "=========0=changeDisplay()===========");
		// int DISPLAY_OUTPUT_TYPE_TV = 2;
		// int DISPLAY_OUTPUT_TYPE_HDMI = 3;
		// int DISPLAY_TVFORMAT_PAL = 11;
		// int DISPLAY_TVFORMAT_NTSC = 14;
		// int DISPLAY_TVFORMAT_720P_50HZ = 4;
		// int DISPLAY_TVFORMAT_720P_60HZ = 5;
		DisplayManagerAw displayManager = (DisplayManagerAw) getSystemService(Context.DISPLAY_SERVICE_AW);
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
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
				Log.w("hehe", "audioManager is null");
				return;
			}
			audioOutputChannels.clear();
			audioOutputChannels.add(AudioManager.AUDIO_NAME_CODEC);
			audioManager.setAudioDeviceActive(audioOutputChannels,
					AudioManager.AUDIO_OUTPUT_ACTIVE);
			testCvbs = true;
			Log.v(TAG, "=========3=changeDisplay()===========");

		} else {
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
			testHdmi = true;
			Log.v(TAG, "=========5=changeDisplay()===========");
		}
		if (testCvbs && testHdmi) {
			writeFactory();
		}

	}

	private void writeFactory() {
        SharedPreferences sharedPreferences = getSharedPreferences("factory_pass", Context.MODE_WORLD_READABLE);  
        Editor editor = sharedPreferences.edit();  
        editor.putString("pass", "1");  
        editor.commit();  
	}

	private void resetFactory() {
        SharedPreferences sharedPreferences = getSharedPreferences("factory_pass", Context.MODE_WORLD_READABLE);  
        Editor editor = sharedPreferences.edit();  
        editor.putString("pass", "0");  
        editor.commit();  
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position,
			long id) {
		if (mCurrentState == STATE_SHOW_CASE_LIST) {
			switchToMain();
			mPosition = position;
			mEngine.startCaseAtFreedom(position);
			// mPrevious.setVisibility(View.GONE);
		} else if (mCurrentState == STATE_SHOW_RESULT) {
			switchToMain();
			mEngine.startCaseAtReviews(position);
			// mPrevious.setVisibility(View.GONE);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	private ResultAdapter mAdapter = new ResultAdapter();

	private class ResultAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mEngine.getCaseSize();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convert, ViewGroup root) {

			if (convert == null) {
				convert = View.inflate(Main.this, R.layout.result_item, null);

			}
			TextView tv = (TextView) convert;
			String resultStr = ":";
			BaseCase task = getItem(position);

			if (task.getResult() == BaseCase.RESULT_RESET) {
				AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
						AbsListView.LayoutParams.MATCH_PARENT, 1);
				convert.setLayoutParams(lp);
				convert.setVisibility(View.GONE);
			} else {
				AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
						AbsListView.LayoutParams.MATCH_PARENT,
						AbsListView.LayoutParams.WRAP_CONTENT);
				convert.setLayoutParams(lp);
				convert.setVisibility(View.VISIBLE);
			}

			switch (task.getResult()) {
			case BaseCase.RESULT_PASSED:
				tv.setBackgroundColor(Color.GREEN);
				resultStr += getResources().getString(R.string.result_passed);
				break;
			case BaseCase.RESULT_RESET:
				tv.setBackgroundColor(Color.BLUE);
				resultStr += getResources().getString(R.string.result_reset);
				break;
			case BaseCase.RESULT_UNPASSED:
				tv.setBackgroundColor(Color.RED);
				resultStr += getResources().getString(R.string.result_failed);
				break;
			}
			tv.setText(task.getName() + resultStr);
			return convert;
		}

		@Override
		public BaseCase getItem(int index) {
			return mEngine.getCase(index);
		}

	}

	@Override
	public void setUiVisible(int component, int visible) {
		switch (component) {
		case UI_COMPONENT_BTN_PASSED:
			// mPassed.setVisibility(visible);
			if (visible == 0) {
				passedit();
			}
			break;
		case UI_COMPONENT_BTN_FAILED:
			// mFailed.setVisibility(visible);
			break;
		}
	}

	/*
	 * private void startToNext() { // TODO Auto-generated method stub new
	 * AlertDialog.Builder(this) .setTitle("?")
	 * .setPositiveButton(R.string.result_passed, new OnClickListener() {
	 * 
	 * @Override public void onClick(DialogInterface arg0, int arg1) {
	 * passedit(); } }) .setNegativeButton(R.string.result_failed, new
	 * OnClickListener() {
	 * 
	 * @Override public void onClick(DialogInterface arg0, int arg1) {
	 * failedit(); } }).show();
	 * 
	 * }
	 */

	private static final ArrayList<OnActivityResult> mActivityResult = new ArrayList<OnActivityResult>();

	public static final void registerActivityResult(OnActivityResult callback) {
		if (!mActivityResult.contains(callback)) {
			mActivityResult.add(callback);
		}
	}

	public static final void unregisterActivityResult(OnActivityResult callback) {
		mActivityResult.remove(callback);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		for (OnActivityResult ar : mActivityResult) {
			ar.onActivityResult(requestCode, resultCode, data);
		}
	};
}
