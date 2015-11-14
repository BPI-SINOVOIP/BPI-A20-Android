package com.softwinner.agingdragonbox.engine;

import android.view.View;

public interface UICallback {

	public static final int UI_COMPONENT_BTN_PASSED = 1;
	public static final int UI_COMPONENT_BTN_FAILED = 2;

	public void setCaseContent(View v);

	public void onCaseCompleted();

	public void setUiVisible(int component, int visible);
}
