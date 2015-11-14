package com.softwinner.dragonbox.platform;

import com.android.internal.widget.LockPatternUtils;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.storage.StorageVolume;

/**
 * 平台相关接口，用于处理平台间接口不同的问题
 * @author huanglong
 *
 */
public abstract class BasePlatform {
	private static BasePlatform platform;
	public static BasePlatform getPlatform(){
		if(platform == null){
			int sdk_int = Build.VERSION.SDK_INT;
			if(sdk_int == 14 || sdk_int == 15){
				platform = new IceCreamPlatform();
			}
			if(sdk_int == 16 || sdk_int == 17){
				platform = new JellyBean();
			}
		}
		return platform;
	}
	public abstract String getExternalPath(Context context);
	public abstract String getDescription(StorageVolume volume, Context context);
	public abstract void asyncConnect(WifiManager wifimanager, Context context, Handler hander);
	public abstract int getBacklightMaximum();
	public abstract void enableLock(LockPatternUtils lockUtils, boolean enable);
	public abstract int getWindowTypeForHomeKeywrod();
}
