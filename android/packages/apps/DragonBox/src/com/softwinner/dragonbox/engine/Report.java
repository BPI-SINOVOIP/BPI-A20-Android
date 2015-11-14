package com.softwinner.dragonbox.engine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.os.Environment;

import com.softwinner.dragonbox.engine.testcase.CaseCustomApk;
import com.softwinner.dragonbox.engine.testcase.CaseElthernet;
import com.softwinner.dragonbox.engine.testcase.CaseHdmi;
import com.softwinner.dragonbox.engine.testcase.CaseLed;
import com.softwinner.dragonbox.engine.testcase.CaseMemory;
import com.softwinner.dragonbox.engine.testcase.CaseSdcard;
import com.softwinner.dragonbox.engine.testcase.CaseUsb;
import com.softwinner.dragonbox.engine.testcase.CaseWifi;
import com.softwinner.dragonbox.engine.testcase.CaseCvbs;
import com.softwinner.dragonbox.engine.testcase.CaseSpdif;
import com.softwinner.dragonbox.engine.testcase.CaseVersion;
import com.softwinner.dragonbox.engine.testcase.CaseVideo;

/**
 * modify by zengsc
 * 
 * @version date 2013-5-6
 */
public class Report {
	private final String mLogcatPath = Environment
			.getExternalStorageDirectory().getPath() + "/DragonBox/logcat.txt";
	private File logcatFile;
	private String mPath;
	private File reportFile;
	private CSVAnalysis mCSVAnalysis;
	private List<List<String>> reportItems;
	private ReportRecordItem currentReportItem;
	private LogcatHelper mLogcatHelper;

	public Report(String filePath, String deviceId) {
		mCSVAnalysis = new CSVAnalysis();
		mPath = filePath;
		currentReportItem = new ReportRecordItem(TITLES.length);
		currentReportItem.setDeviceId(deviceId);
		mLogcatHelper = new LogcatHelper(500);
	}

	// 读取报表中以保存的记录
	public boolean load() throws ReportFormatException {
		// reportFile = new File(mPath);
		// BufferedReader reader = null;
		// if (reportFile.exists()) {
		// try {
		// reader = new BufferedReader(new FileReader(reportFile));
		// reportItems = mCSVAnalysis.readCSVFile(reader);
		// if (reportItems.size() > 0) {
		// // 当前报表记录序号
		// currentReportItem.setNo(reportItems.size() + "");
		// // customApk 初始化，保存时需要把title重新设置
		// List<String> titles = reportItems.get(0);
		// for (int i = TITLES.length; i < titles.size(); i++) {
		// customApkResults.put(titles.get(i),
		// ReportRecordItem.STATE_NO_TESTED);
		// currentReportItem.setNoTested(i);
		// }
		// } else {
		// reportItems.add(getTitles()); // 标题
		// // 新报表文件，当前序号为1
		// currentReportItem.setNo("1");
		// }
		// } catch (IOException e) {
		// e.printStackTrace();
		// return false;
		// } finally {
		// if (reader != null) {
		// try {
		// reader.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		// }
		// } else {
		// try {
		// reportFile.createNewFile();
		// reportItems = new ArrayList<List<String>>();
		// reportItems.add(getTitles()); // 标题
		// // 新报表文件，当前序号为1
		// currentReportItem.setNo("1");
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		// reportItems.add(currentReportItem.getReportRecord());
		// return true;
		reportFile = new File(mPath);
		if (!reportFile.getParentFile().exists())
			reportFile.getParentFile().mkdirs();
		if (!reportFile.exists()) {
			try {
				reportFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		logcatFile = new File(mLogcatPath);
		if (!logcatFile.getParentFile().exists())
			logcatFile.getParentFile().mkdirs();
		if (!logcatFile.exists()) {
			try {
				logcatFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		reportItems = new ArrayList<List<String>>();
		reportItems.add(getTitles()); // 标题
		// 新报表文件，当前序号为1
		currentReportItem.setNo("1");
		reportItems.add(currentReportItem.getReportRecord());
		return true;
	}

	public synchronized boolean save() {
		reportItems.set(0, getTitles());
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(reportFile));
			byte bom[] = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
			writer.write(new String(bom));
			mCSVAnalysis.writeCSVFile(reportItems, writer);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		/** 开启线程用于监听log输出的信息 **/
		new Thread(new Runnable() {
			@Override
			public void run() {
				mLogcatHelper.makeLogcat(null);
				BufferedWriter writer = null;
				try {
					writer = new BufferedWriter(new FileWriter(logcatFile));
					writer.write(mLogcatHelper.getLogcat());
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (writer != null) {
						try {
							writer.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}).start();
		return true;
	}

	// 用来保存日志handler
	// private Handler mSaveHandler = new Handler() {
	// @Override
	// public void handleMessage(Message msg) {
	// switch (msg.what) {
	// case LogcatHelper.MSG_LOGCAT:
	// currentReportItem.setResult(TITLES.length - 1, (String)msg.obj);
	// saveLog();
	// break;
	//
	// default:
	// break;
	// }
	// }
	// };
	// private void saveLog() {
	// reportItems.set(0, getTitles());
	// BufferedWriter writer = null;
	// try {
	// writer = new BufferedWriter(new FileWriter(reportFile));
	// byte bom[] = {(byte)0xEF, (byte)0xBB, (byte)0xBF};
	// writer.write(new String(bom));
	// mCSVAnalysis.writeCSVFile(reportItems, writer);
	// } catch (IOException e) {
	// e.printStackTrace();
	// } finally {
	// if (writer != null) {
	// try {
	// writer.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	// }
	// }

	private List<String> getTitles() {
		List<String> titles = new ArrayList<String>();
		for (String tmp : TITLES) {
			titles.add(tmp);
		}
		Iterator<String> iterator = customApkResults.keySet().iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			titles.add(key);
		}
		return titles;
	}

	// public synchronized boolean save() {
	// if (reportFile == null || !reportFile.exists()) {
	// return false;
	// }
	//
	// BufferedWriter writer = null;
	// try {
	// writer = new BufferedWriter(new FileWriter(reportFile));
	// saveTitleToReport(writer);
	// Iterator<ReportRecordItem> iterator = this.reportItems.iterator();
	// ReportRecordItem item = null;
	// while(iterator.hasNext()) {
	// item = iterator.next();
	//
	// writer.newLine();
	// writer.write(item.getReportRecord());
	// }
	// writer.flush();
	// } catch (IOException e) {
	// e.printStackTrace();
	// return false;
	// } finally {
	// if(writer != null) {
	// try {
	// writer.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	// }
	// return true;
	// }

	// private void saveTitleToReport(BufferedWriter writer) throws IOException
	// {
	// for(int i = 0; i < TITLES.length; i++) {
	// writer.write(TITLES[i]);
	//
	// //输出各字段以逗号为分隔符
	// if(i != TITLES.length -1) {
	// writer.write(",");
	// }
	// }
	// Iterator<String> iterator = customApkResults.keySet().iterator();
	// while(iterator.hasNext()) {
	// String key = iterator.next();
	// writer.write(",");
	// writer.write(key);
	// }
	// }

	// 解析报表中一行记录
	// private void getReport(BufferedReader reader) throws IOException,
	// ReportFormatException {
	// String buffer;
	// int recordSize = 0;
	//
	// //读第一行标题栏
	// buffer = reader.readLine();
	// String[] titles = buffer.split(",");
	// for (int i = TITLES.length; i < titles.length; i++) {
	// customApkResults.put(titles[i], ReportRecordItem.STATE_NO_TESTED);
	// currentReportItem.setNoTested(i - 2);
	// }
	//
	// //开始解析测试结果
	// while((buffer = reader.readLine()) != null) {
	// Log.i("report", "-------read line------ ");
	// ReportRecordItem item = new ReportRecordItem(TITLES.length - 2);
	// item.parserReportRecord(buffer);
	// reportItems.add(item);
	// recordSize++;
	// }
	// //当前报表记录序号等于报表最后一行记录序号 + 1
	// currentReportItem.setNo(String.valueOf(recordSize + 1));
	// }

	public void setTestCase(BaseCase _case) {
		Class<?> testcase = _case.getClass();
		boolean passed = _case.getPassable();
		int index = -1;
		String testcaseTitle = null;

		if (testcase == CaseVersion.class) {
			testcaseTitle = VERSION;
		} else if (testcase == CaseSdcard.class) {
			testcaseTitle = SDCARD;
		} else if (testcase == CaseWifi.class) {
			testcaseTitle = WIFI;
		} else if (testcase == CaseUsb.class) {
			testcaseTitle = USB;
		} else if (testcase == CaseLed.class) {
			testcaseTitle = LED;
		} else if (testcase == CaseCvbs.class) {
			testcaseTitle = CVBS;
		} else if (testcase == CaseHdmi.class) {
			testcaseTitle = HDMI;
		} else if (testcase == CaseElthernet.class) {
			testcaseTitle = Elthernet;
		} else if (testcase == CaseVideo.class) {
			testcaseTitle = VIDEO;
		} else if (testcase == CaseCustomApk.class) {
			CaseCustomApk custom = (CaseCustomApk) _case;
			Map<String, Integer> results = custom.getActivityResult();
			if (results == null)
				return;
			customApkResults.putAll(results);
			Iterator<String> iterator = customApkResults.keySet().iterator();
			// 在后面加上customApkResults
			for (int i = 0; iterator.hasNext(); i++) {
				String key = iterator.next();
				int pass = customApkResults.get(key);
				index = i + TITLES.length;
				if (pass == 1)
					this.currentReportItem.setResult(index, true,
							_case.getDetailResult());
				else if (pass == 0)
					this.currentReportItem.setResult(index, false,
							_case.getDetailResult());
				else if (pass == -1)
					this.currentReportItem.setNoTested(index);
			}
			return;
		}
		index = -1;
		for (int i = 0; i < TITLES.length; i++) {
			if (testcaseTitle != null && testcaseTitle.equals(TITLES[i])) {
				index = i;
				break;
			}
		}
		if (index == -1) {
			return;
		}
		this.currentReportItem
				.setResult(index, passed, _case.getDetailResult());
	}

	public final static String VERSION = "VERSION";
	public final static String MEMORY = "MEMORY";
	public final static String WIFI = "WIFI";
	public final static String SPDIF = "SPDIF";
	public final static String VIDEO = "VIDEO";
	public final static String Elthernet = "Elthernet";
	public final static String CVBS = "CVBS";
	public final static String HDMI = "HDMI";
	public final static String LED = "LED";
	public final static String USB = "USB";
	public final static String SDCARD = "SDCARD";

	public final static String[] TITLES = new String[] { "NO", "DEVICE_ID",
			VERSION, WIFI, Elthernet, CVBS, HDMI, LED, USB, SDCARD, VIDEO };

	public static Map<String, Integer> customApkResults = new LinkedHashMap<String, Integer>();
}
