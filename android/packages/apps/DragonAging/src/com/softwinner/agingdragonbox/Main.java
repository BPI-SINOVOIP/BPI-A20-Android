package com.softwinner.agingdragonbox;

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
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.StrictMode;
import android.text.Html;
import android.util.Log;
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

import com.android.internal.os.storage.ExternalStorageFormatter;
import com.softwinner.agingdragonbox.Configuration.OnActivityResult;
import com.softwinner.agingdragonbox.engine.BaseCase;
import com.softwinner.agingdragonbox.engine.DFEngine;
import com.softwinner.agingdragonbox.engine.LockLayer;
import com.softwinner.agingdragonbox.engine.UICallback;
import com.softwinner.agingdragonbox.engine.Utils;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

public class Main extends Activity implements UICallback, View.OnClickListener,
		AdapterView.OnItemClickListener {
	/** Called when the activity is first created. */

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

	/*
	 * private View mPassed; private View mPrevious; private View mFailed;
	 * private View mSkips;
	 */

	private int mPosition;
	private BroadcastReceiver mReceiver;
	public static final int FLAG_DISMISS_HOMEKEY = 0x02000000;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getWindow().addFlags(FLAG_DISMISS_HOMEKEY);
		switchView();
		if (DEVELOPER_MODE) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
					.detectAll().penaltyLog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
					.detectAll().penaltyLog().penaltyDeath().build());
		}
		/*
		 * ActivityManager am = (ActivityManager)
		 * getSystemService(Context.ACTIVITY_SERVICE);
		 * List<RunningAppProcessInfo> rp = am.getRunningAppProcesses(); for
		 * (int i = 0; i < rp.size(); i++) { Log.v("process","process:" +
		 * rp.get(i).processName + ",pid=" + rp.get(i).pid + ",uid=" +
		 * rp.get(i).uid); }
		 */

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
		// R.layout.main_title);
		mCaseContent = (ViewGroup) findViewById(R.id.case_content);
		lockLayer = new LockLayer(this);
		long t = System.currentTimeMillis();
		mEngine = new DFEngine(this);
		mEngine.setUICallback(this);
		Log.v(Utils.APP_TAG,
				"initial the engine cost " + (System.currentTimeMillis() - t)
						+ "ms");
		DragonBoxApp app = (DragonBoxApp) getApplication();
		app.setEngine(mEngine);
		mEngine.startNextCase();

}

	private void killMyProcess() {
		ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		Log.v("maizirong", "----getPackageName----" + getPackageName());
		am.killBackgroundProcesses(getPackageName());
		Log.v("maizirong", "---------killMyself---------");
	}
	//private BroadcastReceiver mReceiver;

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		if (mEngine != null) {
			mEngine.release();
		}
		finish();
		killMyProcess();
		Log.v("maizirong", "---------onStop()---------");
	}
      
	@Override
	protected void onResume() {
		super.onResume();
		Log.v("maizirong", "---------onResume()---------");
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.v("maizirong", "---------onPause()---------");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mEngine != null) {
			mEngine.release();
		}
		finish();
		killMyProcess();
		Log.v("maizirong", "---------onDestroy()---------");
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
		System.out.println("===========switchVideo===========");
	}

	private void switchAudio() {
		// TODO Auto-generated method stub
		System.out.println("===========switchAudio===========");

		AudioManager mAudioManager = (AudioManager) this
				.getSystemService(Context.AUDIO_SERVICE);
		ArrayList<String> list = mAudioManager
				.getAudioDevices(AudioManager.AUDIO_OUTPUT_TYPE);

		for (String st : list) {
			System.out.println(st);

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
							RootCmd.execRootCmdSilent("pm uninstall -k com.softwinner.agingdragonbox");
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
		if (mEngine.getCaseSize() > 0)
			showResultDialog();
		else
			System.out.print("OPEN XML ERROR");
		finish();
	}

	private void showResultDialog() {
		// View view = View.inflate(this, R.layout.finish_view, null);
		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle(R.string.finish_title)
				// .setView(view)
				.setCancelable(false)
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
						})
				.setNegativeButton(R.string.finish_change,
						new OnClickListener() {
							@Override
							public void onClick(DialogInterface arg0, int arg1) {
								
							}
						})
				.setNeutralButton(R.string.finish_exit, new OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						finish();
					}

				});
		AlertDialog dialog = builder.create();
		Button changeBtn;
		dialog.show();
		changeBtn = (Button) dialog.getWindow().findViewById(
				android.R.id.button2);
		changeBtn.requestFocus();

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
