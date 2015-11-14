package com.softwinner.dragonbox.engine.testcase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;
import android.app.Activity;

import com.softwinner.dragonbox.R;
import com.softwinner.dragonbox.engine.BaseCase;
import com.softwinner.dragonbox.engine.Utils;
import com.softwinner.dragonbox.xml.Node;

public class CaseVersion extends BaseCase {

	private static final String FILENAME_PROC_VERSION = "/proc/version";

	// private static final String FILENAME_TPFW_VERSION =
	// "/sys/bus/i2c/devices/1-0038/ftstpfwver";

	public static final String PASSABLE_FIREWARE = "fireware";
	public static final String PASSABLE_DISPLAY = "display";
	public static final String PASSABLE_CPU = "cpu";
	public static final String PASSABLE_MODEL = "model";
	public static final String PASSABLE_CPU_SPLIT = ";";
	// public static final String PASSABLE_TPFW = "TPFW";

	private String mPresetFireware;
	private String mPresetDisplay;
	private String mPresetMinFreq;
	private String mPresetMaxFreq;
	private String mPresetModel;
	// private String mPresetTPFW;

	private TimerTask mTask;

	private String readLine(String filename) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(filename),
				256);
		try {
			return reader.readLine();
		} finally {
			reader.close();
		}
	}

	private String getCpuInfo() {
		return new StringBuilder().append(CpuManager.getCurCpuFreq())
				.append("mHz\\").append(CpuManager.getMaxCpuFreq())
				.append("mHz\\").append(CpuManager.getMinCpuFreq())
				.append("mHz").toString();
	}

	/*
	 * private String getTPFWVer() { File file = new
	 * File(FILENAME_TPFW_VERSION); BufferedReader reader = null; String TPFWstr
	 * = ""; try { reader = new BufferedReader(new FileReader(file)); TPFWstr =
	 * reader.readLine(); } catch (IOException e) { e.printStackTrace(); }
	 * finally { if (reader != null) try { reader.close(); } catch (IOException
	 * e) { e.printStackTrace(); } } return TPFWstr; }
	 */

	@SuppressWarnings("unused")
	private String getFormattedKernelVersion() {
		String procVersionStr;

		try {
			procVersionStr = readLine(FILENAME_PROC_VERSION);

			final String PROC_VERSION_REGEX = "\\w+\\s+" + /* ignore: Linux */
			"\\w+\\s+" + /* ignore: version */
			"([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
			"\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /*
														 * group 2:
														 * (xxxxxx@xxxxx
														 * .constant)
														 */
			"\\((?:[^(]*\\([^)]*\\))?[^)]*\\)\\s+" + /* ignore: (gcc ..) */
			"([^\\s]+)\\s+" + /* group 3: #26 */
			"(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
			"(.+)"; /* group 4: date */

			Pattern p = Pattern.compile(PROC_VERSION_REGEX);
			Matcher m = p.matcher(procVersionStr);

			if (!m.matches()) {
				Log.e(Utils.APP_TAG, "Regex did not match on /proc/version: "
						+ procVersionStr);
				return "Unavailable";
			} else if (m.groupCount() < 4) {
				Log.e(Utils.APP_TAG,
						"Regex match on /proc/version only returned "
								+ m.groupCount() + " groups");
				return "Unavailable";
			} else {
				return (new StringBuilder(m.group(1)).append("\n")
						.append(m.group(2)).append(" ").append(m.group(3))
						.append("\n").append(m.group(4))).toString();
			}
		} catch (IOException e) {
			Log.e(Utils.APP_TAG,
					"IO Exception when getting kernel version for Device Info screen",
					e);

			return "Unavailable";
		}
	}

	@Override
	protected void onInitialize(Node attr) {
		setView(R.layout.case_version);
		setName(R.string.case_version_name);
		mMaxFailedTimes = 0;
	}

	@Override
	protected boolean onCaseStarted() {
		final TextView tv = (TextView) getView()
				.findViewById(R.id.version_info);
		mTask = new TimerTask() {
			@Override
			public void run() {
				String fireware = Build.FIRMWARE;
				String board = Build.MODEL;
				String version = Build.DISPLAY;
				String cpu = getCpuInfo();
				// String tpfw = getTPFWVer();
				final String formatString = String.format(mContext
						.getResources().getString(R.string.case_version_info),
						fireware, board, version, cpu);
				((Activity) mContext).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						tv.setText(formatString);
					}
				});
			}
		};
		Timer timer = new Timer();
		timer.schedule(mTask, 100, 500);
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (!getPassable()) {
					boolean passable = true;
					if (mPresetFireware != null && !mPresetFireware.equals("")) {
						passable &= mPresetFireware.equals(Build.FIRMWARE);
					}
					if (mPresetDisplay != null && !mPresetDisplay.equals("")) {
						passable &= mPresetDisplay.equals(Build.DISPLAY);
					}
					if (mPresetMinFreq != null && !mPresetMinFreq.equals("")) {
						passable &= mPresetMinFreq.equals(CpuManager
								.getMinCpuFreq());
					}
					if (mPresetMaxFreq != null && !mPresetMaxFreq.equals("")) {
						passable &= mPresetMaxFreq.equals(CpuManager
								.getMaxCpuFreq());
					}
					if (mPresetModel != null && !mPresetModel.equals("")) {
						passable &= mPresetModel.equals(Build.MODEL);
					}
					/*
					 * if (mPresetTPFW != null && !mPresetTPFW.equals("")) {
					 * passable &= mPresetTPFW.equals(getTPFWVer()); }
					 */
					setPassable(passable);
				}
			}
		}).start();
		return false;
	}

	@Override
	protected void onCaseFinished() {
		mTask.cancel();
	}

	@Override
	protected void onRelease() {

	}

	@Override
	protected void onPassableInfo(Node node) {
		super.onPassableInfo(node);
		String fireware = node.getAttributeValue(PASSABLE_FIREWARE);
		mPresetFireware = fireware;
		String display = node.getAttributeValue(PASSABLE_DISPLAY);
		mPresetDisplay = display;
		String cpu = node.getAttributeValue(PASSABLE_CPU);
		String freq[] = cpu.split(PASSABLE_CPU_SPLIT);
		// String tpfw = node.getAttributeValue(PASSABLE_TPFW);
		// mPresetTPFW = tpfw;
		if (freq.length == 2) {
			mPresetMaxFreq = freq[0];
			mPresetMinFreq = freq[1];
		}
		String model = node.getAttributeValue(PASSABLE_MODEL);
		mPresetModel = model;
	}

	protected void onPassableChange() {
		mUiHandler.post(new Runnable() {
			@Override
			public void run() {
				if (getPassable()) {
					getView().findViewById(R.id.version_name)
							.setBackgroundColor(Color.GREEN);
				} else {
					getView().findViewById(R.id.version_name)
							.setBackgroundColor(Color.RED);
				}
			}
		});
	}

	@Override
	public String getDetailResult() {
		return null;
	}
}
