package com.softwinner.dragonbox.engine;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析csv文件，每一行为一个list，每个单元为String
 * 
 * @author zengsc
 * @version date 2013-5-2
 */
public class CSVAnalysis {
	private enum STATE {
		/** 初始状态 */
		READY,
		/** 单元状态 */
		NORMAL,
		/** 双引号状态 */
		QUOTATION,
		/** 双引号结束状态 */
		QUOTATION_END
	}

	private STATE mState;
	public static final char QUOTATION_MARKS = '"'; // 双引号
	public static final char COMMA = ','; // 逗号
	public static final char NEWLINE = '\n'; // 换行

	public CSVAnalysis() {
	}

	/**
	 * 读取csv文件
	 */
	public List<List<String>> readCSVFile(Reader reader) throws IOException {
		List<List<String>> csv = new ArrayList<List<String>>(); // 整个csv报表
		List<String> line = null; // 一行
		StringBuffer unit = new StringBuffer();// 一个单元格
		int tmp; // 每次读单个字符
		mState = STATE.READY;
		while ((tmp = reader.read()) >= 0) {
			switch (mState) {
			case READY: // 初始状态
				if (line == null)
					line = new ArrayList<String>();
				// if (unit == null)
				// unit = new StringBuffer();
				if (tmp == QUOTATION_MARKS) { // 双引号
					unit.append((char) tmp);
					mState = STATE.QUOTATION;
				} else if (tmp == COMMA) { // 单元结束
					line.add(unit.toString());
					unit.setLength(0);
					mState = STATE.READY;
				} else if (tmp == NEWLINE) { // 行结束
					line.add(unit.toString());
					unit.setLength(0);
					csv.add(line);
					line = null;
					mState = STATE.READY;
				} else { // 其他字符
					unit.append((char) tmp);
					mState = STATE.NORMAL;
				}
				break;
			case NORMAL: // 单元状态
				if (tmp == QUOTATION_MARKS) { // 双引号
					err();
				} else if (tmp == COMMA) { // 单元结束
					line.add(unit.toString());
					unit.setLength(0);
					mState = STATE.READY;
				} else if (tmp == NEWLINE) { // 行结束
					line.add(unit.toString());
					unit.setLength(0);
					csv.add(line);
					line = null;
					mState = STATE.READY;
				} else { // 其他字符
					unit.append((char) tmp);
					mState = STATE.NORMAL;
				}
				break;
			case QUOTATION: // 双引号状态
				if (tmp == QUOTATION_MARKS) { // 双引号
					unit.append((char) tmp);
					mState = STATE.QUOTATION_END;
				} else { // 非双引号
					unit.append((char) tmp);
					mState = STATE.QUOTATION;
				}
				break;
			case QUOTATION_END: // 双引号结束状态
				if (tmp == QUOTATION_MARKS) { // 双引号
					unit.append((char) tmp);
					mState = STATE.QUOTATION;
				} else if (tmp == COMMA) { // 单元结束
					line.add(unit.toString());
					unit.setLength(0);
					mState = STATE.READY;
				} else if (tmp == NEWLINE) { // 行结束
					line.add(unit.toString());
					unit.setLength(0);
					csv.add(line);
					line = null;
					mState = STATE.READY;
				} else { // 其他字符
					err();
				}
				break;
			default:
				break;
			}
		}
		switch (mState) {
		case READY: // 初始状态
			if (line != null) {
				if (unit.length() == 0)
					line.add("");
				csv.add(line);
				line = null;
			}
			break;
		case NORMAL: // 单元状态
		case QUOTATION_END: // 双引号结束状态
			line.add(unit.toString());
			unit.setLength(0);
			csv.add(line);
			line = null;
			mState = STATE.READY;
			break;
		case QUOTATION: // 双引号状态
			err();
			break;

		default:
			break;
		}
		return csv;
	}

	/**
	 * 文件出错
	 */
	private void err() {
		throw new Error("csv file format error");
	}

	/**
	 * 将数据转成csv文件
	 */
	public void writeCSVFile(List<List<String>> csv, Writer writer)
			throws IOException {
		for (int i = 0; i < csv.size(); i++) {
			List<String> line = csv.get(i);
			if (i > 0)
				writer.write(NEWLINE);
			for (int j = 0; j < line.size(); j++) {
				String item = line.get(j);
				if (j > 0)
					writer.write(COMMA);
				writer.write(item);
			}
		}
	}
}
