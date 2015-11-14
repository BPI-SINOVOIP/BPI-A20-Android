package com.softwinner.dragonbox;

import com.softwinner.dragonbox.engine.DFEngine;

import android.app.Application;

public class DragonBoxApp extends Application {

	public DFEngine mEngine;

	@Override
	public void onCreate() {
		super.onCreate();
	}

	public void setEngine(DFEngine engine) {
		mEngine = engine;
	}

	public DFEngine getEngine() {
		return mEngine;
	}
}
