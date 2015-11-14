package com.softwinner.agingdragonbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.app.Activity;
import com.softwinner.agingdragonbox.Main;
import android.os.SystemProperties;
import android.content.ComponentName;
import java.io.File;
public class UsbListen extends BroadcastReceiver {    
	private BroadcastReceiver mReceiver;
	String extsd_path = new String("/mnt/extsd/DragonBox/");
    String usbhost0_path = new String("/mnt/usbhost0/DragonBox/custom_aging_cases.xml");
	String usbhost1_path = new String("/mnt/usbhost1/DragonBox/custom_aging_cases.xml");
	String usbhost2_path = new String("/mnt/usbhost2/DragonBox/custom_aging_cases.xml");
	String usbhost3_path = new String("/mnt/usbhost3/DragonBox//custom_aging_cases.xml");
	public void onReceive(Context context, Intent intent) {
        Log.i("myLoger"," Receive SDCard Mount/UnMount!");
        if(isDirExist(extsd_path)||isDirExist(usbhost1_path)||isDirExist(usbhost0_path)||isDirExist(usbhost2_path)||isDirExist(usbhost3_path))
        {
            
            String TestPackageName = SystemProperties.get("ro.sw.agingtestapkpackage", "no");
            String TestClassName = SystemProperties.get("ro.sw.agingtestapkclass", "no");
            intent = new Intent();
            intent.setComponent(new ComponentName(TestPackageName,TestClassName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        
        }
	}

    public boolean isDirExist(String path){  
    File dir = new File(path);
      if (dir.exists()) 
         return true;
      else return false;
   }
   
} 
