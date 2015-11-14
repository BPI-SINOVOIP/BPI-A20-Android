package com.softwinner.agingdragonbox;

import com.softwinner.agingdragonbox.engine.DFEngine;
import com.softwinner.agingdragonbox.engine.LogcatHelper;

import android.app.Application;

public class DragonBoxApp extends Application {

	public DFEngine mEngine;

	@Override
	public void onCreate() {
		super.onCreate();
		LogcatHelper.getInstance(this).start();
	}

	public void setEngine(DFEngine engine) {
		mEngine = engine;
	}

	public DFEngine getEngine() {
		return mEngine;
	}
}
