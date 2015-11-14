package com.softwinner.dragonbox.platform;

import java.lang.reflect.InvocationTargetException;

import com.android.internal.widget.LockPatternUtils;
import com.softwinner.dragonbox.engine.Utils;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.storage.StorageVolume;
import android.view.WindowManager;

public class IceCreamPlatform extends BasePlatform {

	@Override
	public String getExternalPath(Context context) {
		return null;
	}
	
	@Override
	public String getDescription(StorageVolume volume, Context context) {
		Object args[] = new Object[0];
		try {
			return (String)Utils.callMethod(volume, "getDescription", args);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public void asyncConnect(WifiManager wifimanager, Context context,
			Handler handler) {
		Object args[] = new Object[2];
		args[0] = context;
		args[1] = handler;
		try {
			Utils.callMethod(wifimanager, "asyncConnect", args, Context.class,
					Handler.class);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public int getBacklightMaximum() {
		Integer maximum = 255;
		try {
			Class<?> _class = Class.forName("android.os.Power");
			maximum = (Integer) Utils.getFields(_class, "BRIGHTNESS_ON");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		return maximum;
	}

	@Override
	public void enableLock(LockPatternUtils lockUtils,boolean enable) {
		lockUtils.clearLock(false);
		lockUtils.setLockScreenDisabled(enable);
	}

	@Override
	public int getWindowTypeForHomeKeywrod() {
		return WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG;
	}

}
