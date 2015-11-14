package com.softwinner.dragonbox.platform;

import java.lang.reflect.InvocationTargetException;

import android.content.Context;
import android.os.Build;

/**
 * android.view.DisplayManager在版本4.2中改名为android.view.DisplayManagerAw
 * @author Zengsc
 * 
 */
public class DisplayManagerPlatform {
	private Class<?> mClass;
	private Context mContext;
	private Object mManager;

	public DisplayManagerPlatform(Context context) {
		mContext = context;
		int sdk_int = Build.VERSION.SDK_INT;
		if (sdk_int <= 16) {
			mManager = mContext.getSystemService(Context.DISPLAY_SERVICE);
			try {
				mClass = Class.forName("android.view.DisplayManager");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		} else if (sdk_int == 17) {
			mManager = mContext.getSystemService(Context.WINDOW_SERVICE);
			try {
				mClass = Class.forName("android.view.DisplayManagerAw");
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			if (mClass == null) {
				try {
					mClass = Class.forName("android.view.DisplayManager");
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		if (mClass ==null)
			return;
		try {
			EXTRA_HDMISTATUS = mClass.getField("EXTRA_HDMISTATUS").get(mManager).toString();
			DISPLAY_OUTPUT_TYPE_VGA = mClass.getField("DISPLAY_OUTPUT_TYPE_VGA").getInt(mManager);
			DISPLAY_OUTPUT_TYPE_TV = mClass.getField("DISPLAY_OUTPUT_TYPE_TV").getInt(mManager);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
	}

	public int getHdmiHotPlugStatus() {
		int status = 0;
		try {
			status = (Integer) mClass.getDeclaredMethod("getHdmiHotPlugStatus").invoke(mManager);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return status;
	}

	public int getDisplayOutputType(int mDisplay) {
		int result = 0;
		try {
			result = (Integer) mClass.getDeclaredMethod("getDisplayOutputType", Integer.class).invoke(mManager,
					mDisplay);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Use this method to get the default Display object.
	 * 
	 * @return default Display object
	 */
	public String EXTRA_HDMISTATUS;
	public int DISPLAY_OUTPUT_TYPE_VGA;
	public int DISPLAY_OUTPUT_TYPE_TV;
}
