package com.softwinner.agingdragonbox.engine.testcase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.softwinner.agingdragonbox.R;
import com.softwinner.agingdragonbox.engine.BaseCase;
import com.softwinner.agingdragonbox.xml.Node;

import android.R.attr;
import android.R.integer;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.storage.StorageManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.Toast;
import com.softwinner.Gpio;
import android.media.MediaPlayer.OnErrorListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import com.softwinner.agingdragonbox.Thread_var;

public class CaseMemory extends BaseCase {

	private static final String TAG = CaseMemory.class.getSimpleName();
	public static final String PASSABLE_MIN_CAP = "minCap";
	private StorageManager mStorageManager;
	private ArrayAdapter<String> mAdapter;
	private int mMinCapInMB = -1;
	private long mAvailSize = 0; // 用来返回结果的，直接加上一个变量
	private final static String CMD_PATH = "/data/data/com.softwinner.agingdragonbox/cache/memtester";

	static final boolean DEBUG = false;
	protected ViewGroup mStage;
	private boolean mIsRunning = false;
	private boolean mResult = false;
	private ViewGroup myViewGroup;
	private int memSize = 128;
	private int repeat;
	private SharedPreferences mSharedPreferences;
	private SharedPreferences.Editor mEditor;
	private final static int HANDLER_UPDATE_OUTPUT = 0;
	private final static int HANDLER_FINISHED = 1;
	private final static int HANDLER_PROGRESS = 2;
	private ProgressBar mProgressBar;
	private TextView mResultTextView;
	private TextView outputWindow;
	private boolean ThreadExit_ddr;
	private boolean ThreadExit;
	StringBuilder sb = new StringBuilder();
	String cmd;
	private boolean isShow;
	private int MAX_INFO_LINE = 500;
	private int currentLine = 0;

	private static int LedTime = 500;
	Thread browseThread = null;
	Thread_var thread_var = new Thread_var();

	private Handler myHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HANDLER_UPDATE_OUTPUT:
				refeshOutputWindow();
				break;
			}
		}
	};

	public void initCMDFile() {

		File cmdFile = new File(CMD_PATH);

		if (!cmdFile.exists()) {
			Log.d(TAG, " create memtester file on cache.");
			// copy memtest from assets of app to cache directory.
			InputStream is = null;
			FileOutputStream fos = null;
			try {
				is = mContext.getAssets().open("memtester");
				fos = new FileOutputStream(cmdFile);

				byte[] buff = new byte[2048];
				int length = 0;
				while ((length = is.read(buff)) != -1) {
					fos.write(buff, 0, length);
				}
				fos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		// change memtest in cache permission

		Process p = null;
		DataOutputStream dos = null;

		try {
			// p = Runtime.getRuntime().exec("su");
			//
			// dos = new DataOutputStream(p.getOutputStream());
			//
			// dos.writeBytes("chmod 777 " + CMD_PATH);
			// dos.flush();

			Runtime.getRuntime().exec("chmod 6755 " + CMD_PATH);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (dos != null) {
				try {
					dos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void refeshOutputWindow() {
		outputWindow.setText(sb.toString());
		if (isShow) {
			myHandler.sendEmptyMessageDelayed(HANDLER_UPDATE_OUTPUT, 1000);
		}
	}

	class TestRunnable implements Runnable {

		@Override
		public void run() {
			if (memSize <= 0) {
				return;
			}
			initCMDFile();
			sb.setLength(0);
			myHandler.sendEmptyMessage(HANDLER_UPDATE_OUTPUT);
			BufferedWriter dos = null;
			BufferedReader dis = null;

			Process p = null;

			try {
				p = Runtime.getRuntime().exec("/system/xbin/su");

				cmd = String.format("%s %dm %d\n", CMD_PATH, 128, repeat);

				dos = new BufferedWriter(new OutputStreamWriter(
						p.getOutputStream()));
				dis = new BufferedReader(new InputStreamReader(
						p.getInputStream()));

				Log.d(TAG, "ddr test start........");

				// sb.append(dis.readLine());
				Log.v("maizirong", "=========run cmd=========" + cmd);
				dos.write(cmd);
				dos.flush();
				sb.append("start memtest\n");
				String line = null;
				Log.d(TAG, "ddr test start11........");

				// Record start time of testing to log file.
				SimpleDateFormat sdf = new SimpleDateFormat(
						"yyyy-MM-dd hh:mm:ss");
				String timeStr = sdf
						.format(new Date(System.currentTimeMillis()));
				String spliteLine = "------------------------------------------------------------------\n";
				String testInfo = timeStr + "    " + cmd + "\n";

				while ((line = dis.readLine()) != null) {
					Log.d(TAG, "ddr test start22........");
					if (currentLine >= MAX_INFO_LINE) {
						currentLine = 0;
						sb.setLength(0);

					}

					if (line.length() > 15
							&& "Stuck Address".equals(line.substring(2, 15))) {
						Log.d(TAG, "ddr test start33........");
						if (line.endsWith("ok")) {
							Log.d(TAG, "ddr test ok........");
							sb.append("  Stuck Address		: ok");

						} else {
							thread_var.ThreadExit_ddr = false;
							sb.append("  Stuck Address		: failed");
							Log.d(TAG,
									"ddr test ........Stuck Address		: failed");
						}
					} else if (line.length() > 14
							&& "Random Value".equals(line.substring(2, 14))) {
						Log.d(TAG, "ddr test start44........");
						if (line.endsWith("ok")) {

							sb.append("  Random Value		: ok");
						} else {
							thread_var.ThreadExit_ddr = false;
							sb.append("  Random Value		: failed");
							Log.d(TAG,
									"ddr test ........Random Value		: failed");
						}
					} else if (line.length() > 14
							&& "Checkerboard".equals(line.substring(2, 14))) {
						Log.d(TAG, "ddr test start55........");
						if (line.endsWith("ok")) {
							sb.append("  Checkerboard		: ok");
						} else {
							thread_var.ThreadExit_ddr = false;
							sb.append("  Checkerboard		: failed");
							Log.d(TAG,
									"ddr test ........Checkerboard		: failed");
						}
					} else if (line.length() > 12
							&& "Solid Bits".equals(line.substring(2, 12))) {
						Log.d(TAG, "ddr test start66........");
						if (line.endsWith("ok")) {
							sb.append("  Solid Bits		: ok");
						} else {
							thread_var.ThreadExit_ddr = false;
							sb.append("  Solid Bits		: failed");
							Log.d(TAG, "ddr test ........Solid Bits		: failed");
						}
					} else if (line.length() > 18
							&& "Block Sequential".equals(line.substring(2, 18))) {
						Log.d(TAG, "ddr test start77........");
						if (line.endsWith("ok")) {
							sb.append("  Block Sequential		: ok");
						} else {
							thread_var.ThreadExit_ddr = false;
							sb.append("  Block Sequential		: failed");
							Log.d(TAG, "ddr test ........Solid Bits		: failed");
						}

					} else if (line.length() > 12
							&& "Bit Spread".equals(line.substring(2, 12))) {
						Log.d(TAG, "ddr test start88........");
						if (line.endsWith("ok")) {
							sb.append("  Bit Spread		: ok");
						} else {
							thread_var.ThreadExit_ddr = false;
							sb.append("  Bit Spread		: failed");
							Log.d(TAG, "ddr test ........Bit Spread		: failed");
						}

					} else if (line.length() > 10
							&& "Bit Flip".equals(line.substring(2, 10))) {
						Log.d(TAG, "ddr test start99........");
						if (line.endsWith("ok")) {
							sb.append("  Bit Flip		: ok");
						} else {
							thread_var.ThreadExit_ddr = false;
							sb.append("  Bit Flip		: failed");
							Log.d(TAG, "ddr test ........Bit Flip		: failed");
						}

					} else if (line.length() > 14
							&& "Walking Ones".equals(line.substring(2, 14))) {
						Log.d(TAG, "ddr test start100........");
						if (line.endsWith("ok")) {
							sb.append("  Walking Ones		: ok");
						} else {
							thread_var.ThreadExit_ddr = false;
							sb.append("  Walking Ones		: failed");
							Log.d(TAG,
									"ddr test ........Walking Ones		: failed");
						}
					} else if (line.length() > 16
							&& "Walking Zeroes".equals(line.substring(2, 16))) {
						Log.d(TAG, "ddr test start110........");
						if (line.endsWith("ok")) {
							sb.append("  Walking Zeroes		: ok");
						} else {
							thread_var.ThreadExit_ddr = false;
							sb.append("  Walking Zeroes		: failed");
							Log.d(TAG,
									"ddr test ........Walking Zeroes		: failed");
						}
					} else {
						sb.append(line);
					}

					sb.append("\n");
					if (line.startsWith("Done")) {
						break;
					}
					currentLine++;
					Log.e(TAG, "current = " + currentLine);
				}
				Log.i(TAG, "ddr test end.");
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (dos != null) {
					try {

						dos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				if (dis != null) {
					try {
						dis.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				if (p != null) {
					p.destroy();
				}

				// memSize = 0;
				repeat = 1;
				cmd = null;
			}
		}

	};

	@Override
	protected void onInitialize(Node attr) {
		setView(R.layout.memory_test);
		setName(R.string.case_memory_name);
		outputWindow = (TextView) getView().findViewById(R.id.output_window);
	}

	@Override
	protected boolean onCaseStarted() {
		Log.i(TAG, "start MemoryTest");
		mIsRunning = true;
		mResult = false;
		isShow = true;
		new Thread(new TestRunnable()).start();
		startCtrlLedThread();
		return false;
	}

	@Override
	protected void onCaseFinished() {
		// mContext.unregisterReceiver(mStorageReceiver);
	}

	@Override
	protected void onRelease() {

	}

	private void chanceLedStatus(int status) {
		char portType = 'h';
		int portNum = 20;
		Gpio.writeGpio(portType, portNum, status);
	}

	private void startCtrlLedThread() {
		browseThread = new Thread() {
			public void run() {
				try {
					ThreadExit = thread_var.ThreadExit;
					ThreadExit_ddr = thread_var.ThreadExit_ddr;
					while (ThreadExit_ddr && ThreadExit) {
						if (LedTime <= 0) {
							LedTime = 500;
						}
						chanceLedStatus(0);
						Thread.sleep(LedTime);
						chanceLedStatus(1);
						Thread.sleep(LedTime);
						ThreadExit = thread_var.ThreadExit;
						ThreadExit_ddr = thread_var.ThreadExit_ddr;
					}
					chanceLedStatus(0);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		};
		browseThread.start();
	}
}
