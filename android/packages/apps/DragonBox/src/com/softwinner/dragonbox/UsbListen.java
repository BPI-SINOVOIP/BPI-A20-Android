package com.softwinner.dragonbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.app.Activity;
import com.softwinner.dragonbox.Main;
import android.os.SystemProperties;
import android.os.SystemClock;
import android.content.ComponentName;
import java.io.File;

public class UsbListen extends BroadcastReceiver {
	private BroadcastReceiver mReceiver;
        private static final String TAG = "UsbListen";
	String extsd_path = new String(
	        "/mnt/extsd/DragonBox/custom_cases.xml");
	String usbhost0_path = new String(
			"/mnt/usbhost0/DragonBox/custom_cases.xml");
	String usbhost1_path = new String(
			"/mnt/usbhost1/DragonBox/custom_cases.xml");
	String usbhost2_path = new String(
			"/mnt/usbhost2/DragonBox/custom_cases.xml");
	String usbhost3_path = new String(
			"/mnt/usbhost3/DragonBox/custom_cases.xml");

	String aging_extsd_path = new String(
			"/mnt/extsd/DragonBox/custom_aging_cases.xml");
	String aging_usbhost0_path = new String(
			"/mnt/usbhost0/DragonBox/custom_aging_cases.xml");
	String aging_usbhost1_path = new String(
			"/mnt/usbhost1/DragonBox/custom_aging_cases.xml");
	String aging_usbhost2_path = new String(
			"/mnt/usbhost2/DragonBox/custom_aging_cases.xml");
	String aging_usbhost3_path = new String(
			"/mnt/usbhost3/DragonBox/custom_aging_cases.xml");

    String extsd_sn_path = new String(
	       "/mnt/extsd/DragonBox/custom_sn_cases.xml");
    String usbhost0_sn_path = new String(
	       "/mnt/usbhost0/DragonBox/custom_sn_cases.xml");
	String usbhost1_sn_path = new String(
	       "/mnt/usbhost1/DragonBox/custom_sn_cases.xml");
	String usbhost2_sn_path = new String(
	       "/mnt/usbhost2/DragonBox/custom_sn_cases.xml");
	String usbhost3_sn_path = new String(
	       "/mnt/usbhost3/DragonBox/custom_sn_cases.xml");
		   
	String extsd_proof_path = new String(
	       "/mnt/extsd/DragonBox/custom_proof_cases.xml");
    String usbhost0_proof_path = new String(
	       "/mnt/usbhost0/DragonBox/custom_proof_cases.xml");
	String usbhost1_proof_path = new String(
	       "/mnt/usbhost1/DragonBox/custom_proof_cases.xml");
	String usbhost2_proof_path = new String(
	       "/mnt/usbhost2/DragonBox/custom_proof_cases.xml");
	String usbhost3_proof_path = new String(
	       "/mnt/usbhost3/DragonBox/custom_proof_cases.xml");
		   
	public void onReceive(Context context, Intent intent) {
		Log.i("myLoger", " Receive SDCard Mount/UnMount!");

		Boolean dragonboxing = false;
		Boolean aginging = false;
		Boolean sn = false;
		Boolean proof = false;
		Boolean bootcompleted = false;
		for(int i=0;i<10;i++){
			bootcompleted = SystemProperties.getInt("sys.boot_completed",0)==1?true:false;
			if(bootcompleted)
				break;
			SystemClock.sleep(500);
			Log.d(TAG,"wait for boot completed");
		}
		if ((isDirExist(extsd_path) || isDirExist(usbhost1_path)
				|| isDirExist(usbhost0_path) || isDirExist(usbhost2_path) || isDirExist(usbhost3_path))) {
			dragonboxing = true;
			Log.i(TAG, "======dragonboxing======");
		}

		if ((isDirExist(aging_extsd_path) || isDirExist(aging_usbhost0_path)
				|| isDirExist(aging_usbhost1_path)
				|| isDirExist(aging_usbhost2_path) || isDirExist(aging_usbhost3_path))) {
			aginging = true;
			Log.i(TAG, "======aging======");
		}
		
		if ((isDirExist(extsd_sn_path) || isDirExist(usbhost0_sn_path)
				|| isDirExist(usbhost1_sn_path)
				|| isDirExist(usbhost2_sn_path) || isDirExist(usbhost3_sn_path))) {
			sn = true;
			Log.i(TAG, "======aging======");
		}
		
		if ((isDirExist(extsd_proof_path) || isDirExist(usbhost0_proof_path)
				|| isDirExist(usbhost1_proof_path)
				|| isDirExist(usbhost2_proof_path) || isDirExist(usbhost3_proof_path))) {
			proof = true;
			Log.i(TAG, "======aging======");
		}

		if (dragonboxing && !aginging) {

			String TestPackageName = SystemProperties.get(
					"ro.sw.testapkpackage", "no");
			String TestClassName = SystemProperties.get("ro.sw.testapkclass",
					"no");
			intent = new Intent();
			intent.setComponent(new ComponentName(TestPackageName,
					TestClassName));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		}
		
		if(aginging){
		    String TestPackageName = SystemProperties.get(
					"ro.sw.agingtestapkpackage", "no");
			String TestClassName = SystemProperties.get("ro.sw.agingtestapkclass",
					"no");
			intent = new Intent();
			intent.setComponent(new ComponentName(TestPackageName,
					TestClassName));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		    
		}
		
		if(sn&& !aginging&& !dragonboxing)
		{
		  	String TestPackageName = SystemProperties.get(
					"ro.sw.snapkpackage", "no");
			String TestClassName = SystemProperties.get("ro.sw.snapkclass",
					"no");
			intent = new Intent();
			intent.setComponent(new ComponentName(TestPackageName,
					TestClassName));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);  
	    }
	    
	    if(proof&& !aginging&& !dragonboxing)
		{
		  	String TestPackageName = SystemProperties.get(
					"ro.sw.snapkpackage", "no");
			String TestClassName = SystemProperties.get("ro.sw.proofapkclass",
					"no");
			intent = new Intent();
			intent.setComponent(new ComponentName(TestPackageName,
					TestClassName));
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);  
	    }
	    
	    
	    
	}

	public boolean isDirExist(String path) {
		File dir = new File(path);
		if (dir.exists())
			return true;
		else
			return false;
	}

}
