package com.softwinner.dragonbox.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;

import android.os.Handler;
import android.os.Message;

/*
 LOGCAT说明
 options include:
 -s              Set default filter to silent.(默认设置过滤器)
 Like specifying filterspec '*:s'
 -f <filename>   Log to file. Default to stdout(输出到日志文件)
 -r [<kbytes>]   Rotate log every kbytes. (16 if unspecified). Requires -f
 -n <count>      Sets max number of rotated logs to <count>, default 4
 -v <format>     Sets the log print format, where <format> is one of:

 brief process tag thread raw time threadtime long

 -c              clear (flush) the entire log and exit(清除日志)
 -d              dump the log and then exit (don't block)(获取日志)
 -t <count>      print only the most recent <count> lines (implies -d)
 -g              get the size of the log's ring buffer and exit(获取日志的大小)
 -b <buffer>     Request alternate ring buffer, 'main', 'system', 'radio'
 or 'events'. Multiple -b parameters are allowed and the
 results are interleaved. The default is -b main -b system.
 -B              output the log in binary
 filterspecs are a series of
 <tag>[:priority]
 where <tag> is a log component tag (or * for all) and priority is:
 V    Verbose
 D    Debug
 I    Info
 W    Warn
 E    Error
 F    Fatal
 S    Silent (supress all output)

 '*' means '*:d' and <tag> by itself means <tag>:v
 设置日志（见下面的格式打印格式
 -v 格式		例
 brief		W/tag ( 876): message
 process		W( 876) message (tag)
 tag			W/tag : message
 thread		W( 876:0x37c) message
 raw			message
 time		09-08 05:40:26.729 W/tag ( 876): message
 threadtime	09-08 05:40:26.729 876 892 W tag : message
 long		[ 09-08 05:40:26.729 876:0x37c W/tag ] message
 */
/**
 * 获得logcat信息
 * 
 * @author zengsc
 * @version date 2013-5-7
 */
public class LogcatHelper {
	public static final int MSG_LOGCAT = 0;
	private final String BEGIN = "--------- beginning of /dev/log/main";

	private QueueBuffer mBuffer;

	public LogcatHelper(int size) {
		mBuffer = new QueueBuffer(size);
		mBuffer.add("begin logcat");
	}

	/**
	 * 获取logcat最新信息，并用Message返回
	 * 
	 * @param handler
	 *            返回消息LogcatHelper.MSG_LOGCAT
	 */
	public void makeLogcat(Handler handler) {
		Process mLogcatProc = null;
		BufferedReader reader = null;
		try {
			// 获取logcat日志信息
			mLogcatProc = Runtime.getRuntime().exec(
					new String[] { "logcat", "-d", "*:D" });
			Runtime.getRuntime().exec(new String[] { "logcat", "-c" }); // 清空logcat信息
			reader = new BufferedReader(new InputStreamReader(
					mLogcatProc.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				if (BEGIN.equals(line))
					continue;
				mBuffer.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		if (handler != null) {
			Message msg = Message.obtain();
			msg.what = MSG_LOGCAT;
			msg.obj = getLogcat();
			handler.sendMessage(msg);
		}
	}

	/**
	 * 取得logcat字符串
	 */
	public String getLogcat() {
		List<Object> list = mBuffer.getQueue();
		if (list.size() == 0)
			return "Failed to Get Logcat!";
		StringBuilder buffer = new StringBuilder(list.size() * 16);
		Iterator<?> it = list.iterator();
		while (it.hasNext()) {
			Object next = it.next();
			if (next != this) {
				buffer.append(next);
			} else {
				buffer.append("(this Collection)");
			}
			if (it.hasNext()) {
				buffer.append("\n");
			}
		}
		return buffer.toString();
	}
}
