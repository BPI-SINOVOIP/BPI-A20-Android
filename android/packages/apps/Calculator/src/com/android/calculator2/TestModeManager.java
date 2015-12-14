package com.android.calculator2;

import java.io.File;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;

import android.util.Log;


public class TestModeManager {

	private static final String TAG = "TestModeManager";

	public final static String TEST_MODE_KEY = "33"; // check if input value =
														// dragonFireKey,
														// startup DragongFire
														// Main Activity.
	public final static String TEST_MODE_CONFIG = "23"; // if input value = 23,
														// startup DragonFire
														// configuration
														// Activity.
	public final static String TEST_AGING_KEY = "34"; // if input value = 34,
														// startup DragonAging
														// Main Activity
	public final static String TEST_AGING_CONFIG = "24"; // if input value = 34,
														// startup DragonAging
														// configuration
														// Activity.
	public final static String TEST_DBOX_KEY = "35"; // if input value = 35,
														// startup DragonBox
														// Main Activity
	public final static String TEST_DBOX_CONFIG = "25"; // if input value = 34,
														// startup DragonAging
														// configuration
														// Activity.
	
	public final static String CONFIG_LOG_ON ="1091"; 
	public final static String CONFIG_LOG_OFF ="1090"; 
		
	/* flag for DragonFire */
	private final static String FLAG_USBHOST = "/mnt/usbhost1/DragonFire/custom_cases.xml";
	private final static String FLAG_USBHOST_CONFIG = "/mnt/usbhost1/DragonFire/";
	private final static String FLAG_EXTSD = "/mnt/sdcard/DragonFire/custom_cases.xml";
	private final static String FLAG_EXTSD_CONFIG = "/mnt/sdcard/DragonFire/";
	private final static String FLAG_SDCARD = "/mnt/sdcard/DragonFire/custom_cases.xml";
	private final static String FLAG_SDCARD_CONFIG = "/mnt/sdcard/DragonFire/";
	
	/* flag for DragonAging */
	private final static String FLAG_AGING_USBHOST = "/mnt/usbhost2/DragonBox/custom_aging_cases.xml";
	private final static String FLAG_AGING_USBHOST_CONFIG = "/mnt/usbhost2/DragonBox/";
	private final static String FLAG_AGING_EXTSD = "/mnt/sdcard/DragonBox/custom_aging_cases.xml";
	private final static String FLAG_AGING_EXTSD_CONFIG = "/mnt/sdcard/DragonBox/";
	private final static String FLAG_AGING_SDCARD = "/mnt/sdcard/DragonBox/custom_aging_cases.xml";
	private final static String FLAG_AGING_SDCARD_CONFIG = "/mnt/sdcard/DragonBox/";
	
	/* flag for DragonBox */
	private final static String FLAG_DBOX_USBHOST = "/mnt/usbhost1/DragonBox/custom_cases.xml";
	private final static String FLAG_DBOX_USBHOST_CONFIG = "/mnt/usbhost1/DragonBox/";
	private final static String FLAG_DBOX_EXTSD = "/mnt/sdcard/DragonBox/custom_cases.xml";
	private final static String FLAG_DBOX_EXTSD_CONFIG = "/mnt/sdcard/DragonBox/";
	private final static String FLAG_DBOX_SDCARD = "/mnt/sdcard/DragonBox/custom_cases.xml";
	private final static String FLAG_DBOX_SDCARD_CONFIG = "/mnt/sdcard/DragonBox/";
	
	public static boolean start(Context context, String inputKey) {
		if(inputKey.equals(TEST_MODE_KEY) || inputKey.equals(TEST_MODE_CONFIG) || inputKey.equals(TEST_AGING_KEY) ||
					inputKey.equals(TEST_AGING_CONFIG) || inputKey.equals(TEST_DBOX_KEY) || inputKey.equals(TEST_DBOX_CONFIG) ||
					inputKey.equals(CONFIG_LOG_ON) || inputKey.equals(CONFIG_LOG_OFF)) {
			boolean isStart = checkAndStart(context, inputKey);
			boolean isStartConfig = checkAndStartConfig(context, inputKey);
			boolean isStartAging = checkAndStartAging(context, inputKey);
			boolean isStratAgingConfig = checkAndStartAgingConfig(context, inputKey);
			boolean isStartDbox = checkAndStartDbox(context, inputKey);
			boolean isStartDboxConfig = checkAndStartDboxConfig(context, inputKey);
			boolean isLogOn = checkAndStartLogOn(context, inputKey);
			boolean isLogOff = checkAndStartLogOff(context, inputKey);

			Log.v(TAG, "isStart=" + isStart + " isStartConfig=" + isStartConfig + " isStartAging=" + isStartAging +
				       " isStratAgingConfig=" + isStratAgingConfig + " isStartDbox=" + isStartDbox + " isStartDboxConfig=" +
				       isStartDboxConfig + " isLogOn=" + isLogOn + " isLogOff=" + isLogOff);
			
			if(isStart || isStartConfig || isStartAging || isStratAgingConfig || isStartDbox || isStartDboxConfig || isLogOn || isLogOff) {
				return true;
			}
		}
		
		return false;
	}

	private static boolean checkAndStart(Context context, String inputKey) {
		boolean b = false;
		if (inputKey.equals(TEST_MODE_KEY) && (new File(FLAG_USBHOST).exists() || new File(FLAG_EXTSD).exists() || new File(FLAG_SDCARD).exists())) {
			Intent i = new Intent();
			ComponentName component = new ComponentName(
					"com.softwinner.dragonfire",
					"com.softwinner.dragonfire.Main");
			i.setComponent(component);
			try {
				context.startActivity(i);
				b = true;
			} catch (Exception e) {
			}
		}
		return b;
	}

	private static boolean checkAndStartConfig(Context context, String inputKey) {
		boolean b = false;
		if (inputKey.equals(TEST_MODE_CONFIG) && (new File(FLAG_USBHOST_CONFIG).exists() || new File(FLAG_EXTSD_CONFIG).exists()
				|| new File(FLAG_SDCARD_CONFIG).exists())) {
			Intent i = new Intent();
			ComponentName component = new ComponentName(
					"com.softwinner.dragonfire",
					"com.softwinner.dragonfire.Configuration");
			i.setComponent(component);
			try {
				context.startActivity(i);
				b = true;
			} catch (Exception e) {
			}
		}
		return b;
	}

	private static boolean checkAndStartAging(Context context, String inputKey) {
		boolean b = false;
		if (inputKey.equals(TEST_AGING_KEY) && (new File(FLAG_AGING_USBHOST).exists() || new File(FLAG_AGING_EXTSD).exists() || new File(FLAG_AGING_SDCARD).exists())) {
			Intent i = new Intent();
			ComponentName component = new ComponentName(
					"com.softwinner.agingdragonbox",
					"com.softwinner.agingdragonbox.Main");
			i.setComponent(component);
			try {
				context.startActivity(i);
				b = true;
			} catch (Exception e) {
			}
		}
		return b;
	}

	private static boolean checkAndStartAgingConfig(Context context, String inputKey) {
		boolean b = false;
		if (inputKey.equals(TEST_AGING_CONFIG) && (new File(FLAG_AGING_USBHOST_CONFIG).exists() || new File(FLAG_AGING_EXTSD_CONFIG).exists()
				|| new File(FLAG_AGING_SDCARD_CONFIG).exists())) {
			Intent i = new Intent();
			ComponentName component = new ComponentName(
					"com.softwinner.agingdragonbox",
					"com.softwinner.agingdragonbox.Configuration");
			i.setComponent(component);
			try {
				context.startActivity(i);
				b = true;
			} catch (Exception e) {
			}
		}
		return b;
	}

	private static boolean checkAndStartDbox(Context context, String inputKey) {
		boolean b = false;
		if (inputKey.equals(TEST_DBOX_KEY) && (new File(FLAG_DBOX_USBHOST).exists() || 
				new File(FLAG_DBOX_EXTSD).exists() || new File(FLAG_DBOX_SDCARD).exists())) {

			Log.d(TAG, "checkAndStartDbox()");
			
			Intent i = new Intent();
			ComponentName component = new ComponentName(
					"com.softwinner.dragonbox",
					"com.softwinner.dragonbox.Main");
			i.setComponent(component);
			try {
				context.startActivity(i);
				b = true;
			} catch (Exception e) {
			}
		}
		return b;
	}

	private static boolean checkAndStartDboxConfig(Context context, String inputKey) {
		boolean b = false;
		if (inputKey.equals(TEST_DBOX_CONFIG) && (new File(FLAG_DBOX_USBHOST_CONFIG).exists() || 
				new File(FLAG_DBOX_EXTSD_CONFIG).exists() || new File(FLAG_DBOX_SDCARD_CONFIG).exists())) {

			Log.d(TAG, "checkAndStartDboxConfig()");

			Intent i = new Intent();
			ComponentName component = new ComponentName(
					"com.softwinner.dragonbox",
					"com.softwinner.dragonbox.Configuration");
			i.setComponent(component);
			try {
				context.startActivity(i);
				b = true;
			} catch (Exception e) {
			}
		}
		return b;
	}

	private static boolean checkAndStartLogOn(Context context, String inputKey) {
		boolean b = false;
		if (inputKey.equals(CONFIG_LOG_ON)) {

			Log.d(TAG, "checkAndLogOn()");
			
			SystemProperties.set("persist.sys.log_kernel", "true");
			SystemProperties.set("persist.sys.log_main", "true");
			SystemProperties.set("persist.sys.log_radio", "true");

			Log.d(TAG, "propery is " + SystemProperties.get("persist.sys.log_kernel"));

			b = true;
		}
		
		return b;
	}

	private static boolean checkAndStartLogOff(Context context, String inputKey) {
		boolean b = false;
		if (inputKey.equals(CONFIG_LOG_OFF)) {

			Log.d(TAG, "checkAndLogOff()");
			
			SystemProperties.set("persist.sys.log_kernel", "false");
			SystemProperties.set("persist.sys.log_main", "false");
			SystemProperties.set("persist.sys.log_radio", "false");

			b = true;
		}
		
		return b;
	}

}

