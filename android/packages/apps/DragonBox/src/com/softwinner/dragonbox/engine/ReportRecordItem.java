package com.softwinner.dragonbox.engine;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;

/**
 * modify by zengsc
 * 
 * @version date 2013-5-6
 */
public class ReportRecordItem {
	private final int NO = 0;
	private final int DEVICEID = 1;

	private List<String> mResults;

	public final static int STATE_UNSUCCESS = 0; // The test case not passed.
	public final static int STATE_SUCCESS = 1; // The test case passed.
	public final static int STATE_NO_TESTED = -1; // The test case had not start
													// testing.
	private final static String UNSUCCESS = "FAIL";
	private final static String SUCCESS = "PASS";
	private final static String NO_TEST = "NA";

	public ReportRecordItem(List<String> record) {
		mResults = record;
	}

	public ReportRecordItem(int testcaseSize) {
		mResults = new ArrayList<String>(testcaseSize);
		for (int i = 0; i < testcaseSize; i++) {
			setResult(i, NO_TEST);
		}
	}

	public String getNo() {
		return mResults.get(NO);
	}

	public void setNo(String no) {
		setResult(NO, no);
	}

	public String getDeviceId() {
		return mResults.get(DEVICEID);
	}

	public void setDeviceId(String deviceId) {
		setResult(DEVICEID, deviceId);
	}

	private final String QUOTATION_MARKS = "\"";
	private final String COMMA = ",";
	private final String NEWLINE = "\n";
	private final String DOUBLE_QUOTATION_MARKS = "\"\"";

	/**
	 * 设置某些测试结果及详细
	 */
	public void setResult(int position, String result) {
		// 防止List越界
		int size = mResults.size();
		if (position >= size) {
			for (int i = size; i <= position; i++) {
				mResults.add(NO_TEST);
			}
		}
		if (result == null)
			result = "";
		System.out.println("Set Result " + position + ":"
				+ (result.length() > 50 ? result.substring(0, 50) : result));
		boolean flag = false;
		if (result.contains(QUOTATION_MARKS)) {
			result = result.replace(QUOTATION_MARKS, DOUBLE_QUOTATION_MARKS);
			flag = true;
		}
		if (result.contains(COMMA)) {
			flag = true;
		}
		if (result.contains(NEWLINE)) {
			flag = true;
		}
		if (flag) {
			result = QUOTATION_MARKS + result + QUOTATION_MARKS;
		}
		mResults.set(position, result);
	}

	/**
	 * 返回结果
	 */
	public List<String> getReportRecord() {
		return mResults;
	}

	/**
	 * 设置某些测试结果及详细
	 */
	public void setResult(int position, boolean result, String detail) {
		if (position == NO || position == DEVICEID || position < 0)
			throw new Error("index out of range");
		String record = result ? SUCCESS : UNSUCCESS;
		if (!TextUtils.isEmpty(detail))
			record += "|" + detail;
		setResult(position, record);
	}

	/**
	 * 重置某项测试
	 */
	public void setNoTested(int position) {
		setResult(position, NO_TEST);
	}
}
