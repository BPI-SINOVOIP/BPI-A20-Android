package com.softwinner.dragonbox.engine.testcase;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class CpuManager {

	// 获取CPU最大频率（单位KHZ）
	// "/system/bin/cat" 命令行
	// "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq" 存储最大频率的文件的路径
	public static String getMaxCpuFreq() {
		String result = "";
		try {
			FileReader fr = new FileReader(
					"/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
			BufferedReader br = new BufferedReader(fr);
			String text = br.readLine();
			result = text.trim();
			result.trim();
			int freq = Integer.parseInt(result) / 1000 ;
			result = Integer.toString(freq);
			fr.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			result = "N/A";
		} catch (NumberFormatException e){
			result = "N/A";
		}
		return result.trim();
	}

	// 获取CPU最小频率（单位KHZ）
	public static String getMinCpuFreq() {
		String result = "";
		try {
			FileReader fr = new FileReader(
					"/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq");
			BufferedReader br = new BufferedReader(fr);
			String text = br.readLine();
			result = text.trim();
			result.trim();
			int freq = Integer.parseInt(result) / 1000 ;
			result = Integer.toString(freq);
			fr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			result = " N/A ";
		} catch (IOException ex) {
			ex.printStackTrace();
			result = " N/A ";
		} catch (NumberFormatException e){
			result = " N/A ";
		}
		return result.trim();
	}

	// 实时获取CPU当前频率（单位KHZ）
	public static String getCurCpuFreq() {
		String result = "N/A";
		try {
			FileReader fr = new FileReader(
					"/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq");
			BufferedReader br = new BufferedReader(fr);
			String text = br.readLine();
			result = text.trim();
			result.trim();
			int freq = Integer.parseInt(result) / 1000 ;
			result = Integer.toString(freq);
			fr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			result = "N/A";
		} catch (IOException e) {
			e.printStackTrace();
			result = "N/A";
		} catch (NumberFormatException e){
			result = "N/A";
		}
		return result;
	}

	// 获取CPU名字
	public static String getCpuName() {
		try {
			FileReader fr = new FileReader("/proc/cpuinfo");
			BufferedReader br = new BufferedReader(fr);
			String text = br.readLine();
			String[] array = text.split(":\\s+", 2);
			for (int i = 0; i < array.length; i++) {
			}
			fr.close();
			return array[1];
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
