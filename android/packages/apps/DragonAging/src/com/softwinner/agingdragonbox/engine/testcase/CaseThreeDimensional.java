package com.softwinner.agingdragonbox.engine.testcase;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.SystemProperties;

import android.view.ViewGroup;
import android.widget.TextView;


import com.softwinner.agingdragonbox.R;
import com.softwinner.agingdragonbox.engine.BaseCase;
import com.softwinner.agingdragonbox.engine.testcase.ThreeDimensionalView;
import com.softwinner.agingdragonbox.xml.Node;



/**
 * 3D测试
 * 
 * @author zhengxiangna
 * 
 */

public class CaseThreeDimensional extends BaseCase {
	private ThreeDimensionalView mThreeDimensionalView;
	private ViewGroup iewGroup;
	@Override
	protected void onInitialize(Node attr) {
		setView(R.layout.case_threedimensional);
		setName(R.string.case_memory_name);
		mThreeDimensionalView = new ThreeDimensionalView(mContext);
		iewGroup = (ViewGroup)getView().findViewById(R.id.myViewGroup);
	}

	@Override
	protected boolean onCaseStarted() {
		iewGroup.addView(mThreeDimensionalView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		return false;
	}

	

	@Override
	protected void onCaseFinished() {

	}

	@Override
	protected void onRelease() {

	}

}
