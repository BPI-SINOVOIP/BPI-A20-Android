package com.softwinner.dragonbox;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ExitReceiver extends BroadcastReceiver {
	private final static String TAG = "ExitReceiver";
	private final static String ACTION_EXIT = "com.softwinner.dragonbox.ACTION_EXIT";
	public void onReceive(Context context, Intent intent) {
		if(ACTION_EXIT.equals(intent.getAction())){
			Log.d(TAG,"receive exit command");
			System.exit(0);
		}
	}
}
