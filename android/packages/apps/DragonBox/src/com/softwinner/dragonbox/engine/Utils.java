package com.softwinner.dragonbox.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import android.util.Log;

import com.softwinner.dragonbox.R;

import com.softwinner.dragonbox.engine.testcase.CaseComprehensive;
import com.softwinner.dragonbox.engine.testcase.CaseCvbs;
import com.softwinner.dragonbox.engine.testcase.CaseElthernet;
import com.softwinner.dragonbox.engine.testcase.CaseHdmi;
import com.softwinner.dragonbox.engine.testcase.CaseLed;
import com.softwinner.dragonbox.engine.testcase.CaseSdcard;
import com.softwinner.dragonbox.engine.testcase.CaseSpdif;
import com.softwinner.dragonbox.engine.testcase.CaseUsb;
import com.softwinner.dragonbox.engine.testcase.CaseVersion;
import com.softwinner.dragonbox.engine.testcase.CaseVideo;
import com.softwinner.dragonbox.engine.testcase.CaseWifi;
import com.softwinner.dragonbox.platform.BasePlatform;

public class Utils {

	public static final String APP_TAG = "DragonBox";
	public static final String version = "v2.3.0";
	static {
		Log.w(APP_TAG, "DragonBox version:" + version);
	}

	public static final String CONFIGURATION_NODE = "TestCase";

	public static final String[] ALL_CASES = { CaseComprehensive.class
			.getSimpleName() };

	public static final HashMap<String, Integer> CASE_MAP = new HashMap<String, Integer>();
	static {
		CASE_MAP.put(CaseComprehensive.class.getSimpleName(),
				R.string.case_comprehensive);
		CASE_MAP.put(CaseVideo.class.getSimpleName(), R.string.case_video_name);
		CASE_MAP.put(CaseCvbs.class.getSimpleName(), R.string.case_cvbs_name);
		CASE_MAP.put(CaseHdmi.class.getSimpleName(), R.string.case_hdmi_name);
		CASE_MAP.put(CaseElthernet.class.getSimpleName(),
				R.string.case_elthernet);
		CASE_MAP.put(CaseVersion.class.getSimpleName(),
				R.string.case_version_name);
		CASE_MAP.put(CaseWifi.class.getSimpleName(), R.string.case_wifi_name);
		CASE_MAP.put(CaseSdcard.class.getSimpleName(),
				R.string.case_sdcard_name);
		CASE_MAP.put(CaseUsb.class.getSimpleName(), R.string.case_usb_name);
		CASE_MAP.put(CaseLed.class.getSimpleName(), R.string.case_led_name);

	}

	public static BasePlatform PLATFORM_INTEFACE = BasePlatform.getPlatform();

	public static BaseCase createCase(String caseName) {
		BaseCase _case = null;
		try {
			BaseCase clazz = (BaseCase) Class.forName(
					"com.softwinner.dragonbox.engine.testcase." + caseName)
					.newInstance();
			_case = clazz;
		} catch (ClassNotFoundException e) {
			Log.w(APP_TAG, e.getMessage());
		} catch (ClassCastException e) {
			Log.w(APP_TAG, e.getMessage());
		} catch (InstantiationException e) {
			Log.w(APP_TAG, e.getMessage());
		} catch (IllegalAccessException e) {
			Log.w(APP_TAG, e.getMessage());
		}
		return _case;
	}

	public static boolean search(ArrayList<File> result, File folder,
			String[] types, boolean returnFirst) {
		File[] fileList = folder.listFiles();
		boolean b = false;
		if (fileList == null)
			return b;
		for (File file : fileList) {

			if (file.isFile()) {
				boolean tag = false;
				for (String type : types) {
					// Log.d(Utils.APP_TAG,"file:" + file.getName() + type);
					tag |= file.getName().endsWith(type);
				}
				if (tag)
					result.add(file);
				b |= tag;
			}
			if (returnFirst && result.size() > 0)
				return b;
			if (file.isDirectory()) {
				b |= search(result, file, types, returnFirst);
			}
		}
		return b;
	}

	public static Object callMethod(Object obj, String methodName,
			Object args[], Class<?>... _classType) throws SecurityException,
			NoSuchMethodException, IllegalArgumentException,
			IllegalAccessException, InvocationTargetException,
			ClassNotFoundException {
		Class<?> _class = obj.getClass();
		Method method = _class.getDeclaredMethod(methodName, _classType);
		Object object = method.invoke(obj, args);
		return object;
	}

	public static Object getFields(Class<?> _class, String fieldsName)
			throws SecurityException, NoSuchMethodException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, NoSuchFieldException {
		Field field = _class.getField(fieldsName);
		// 原来是field.getInt(_class);改成get(_class)，不知是否会出现问题
		Object object = field.get(_class);
		return object;
	}

	private static String[] CLASS_PATH_PROP = { "java.class.path",
			"java.library.path" };

	private static List<File> CLASS_PATH_ARRAY = getClassPath();

	private static List<File> getClassPath() {
		List<File> ret = new ArrayList<File>();
		String delim = ":";
		if (System.getProperty("os.name").indexOf("Windows") != -1)
			delim = ";";
		for (String pro : CLASS_PATH_PROP) {
			String str = System.getProperty(pro);
			Log.d(APP_TAG, "getProperty:" + System.getProperty(pro));
			if (str != null) {
				String[] pathes = str.split(delim);
				for (String path : pathes)
					ret.add(new File(path));
			}
		}
		return ret;
	}

	public static List<String> getClassInPackage(String pkgName) {
		List<String> ret = new ArrayList<String>();
		String rPath = pkgName.replace('.', '/') + "/";
		try {
			for (File classPath : CLASS_PATH_ARRAY) {
				if (!classPath.exists())
					continue;
				if (classPath.isDirectory()) {
					File dir = new File(classPath, rPath);
					if (!dir.exists())
						continue;
					for (File file : dir.listFiles()) {
						if (file.isFile()) {
							String clsName = file.getName();
							clsName = pkgName
									+ "."
									+ clsName
											.substring(0, clsName.length() - 6);
							ret.add(clsName);
						}
					}
				} else {
					FileInputStream fis = new FileInputStream(classPath);
					JarInputStream jis = new JarInputStream(fis, false);
					JarEntry e = null;
					while ((e = jis.getNextJarEntry()) != null) {
						String eName = e.getName();
						if (eName.startsWith(rPath) && !eName.endsWith("/")) {
							ret.add(eName.replace('/', '.').substring(0,
									eName.length() - 6));
						}
						jis.closeEntry();
					}
					jis.close();
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return ret;
	}

	public static void getCaseList(String packageName) throws IOException {
		List<String> cls = getClassInPackage(packageName);
		for (String s : cls) {
			System.out.println(s);
		}
	}
}
