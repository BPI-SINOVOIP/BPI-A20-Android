package com.softwinner.dragonbox.engine.testcase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.softwinner.dragonbox.Configuration.OnActivityResult;
import com.softwinner.dragonbox.Main;
import com.softwinner.dragonbox.R;
import com.softwinner.dragonbox.engine.BaseCase;
import com.softwinner.dragonbox.engine.ReportRecordItem;
import com.softwinner.dragonbox.xml.Node;

/**
 * 用户自定义添加apk测试case
 * 
 * @author zengsc
 * @version date 2013-4-15
 */
public class CaseCustomApk extends BaseCase implements View.OnClickListener {
	private int mRequestCode;
	private String mPackage;
	private String mActivity;
	private Button mRestartButton;
	private View stateView;
	private ListView stateList;
	private ResultAdapter stateAdapter;

	@Override
	protected void onInitialize(Node attr) {
		mRequestCode = (int) (System.nanoTime() % Integer.MAX_VALUE); // 设定一个随机的请求Code
		setName(attr.getAttributeValue("name")); // 获得测试名字
		mPackage = attr.getAttributeValue("package"); // 获得包名
		mActivity = attr.getAttributeValue("activity"); // 获得Activity名
		setView(R.layout.case_custom_apk); // 获得继续
		mRestartButton = (Button) getView().findViewById(R.id.btn_redo);
		mRestartButton.setOnClickListener(this);
		stateView = getView().findViewById(R.id.case_custom_apk_state);
		stateList = (ListView) getView().findViewById(R.id.case_custom_list);
		stateAdapter = new ResultAdapter(mContext);
		stateList.setAdapter(stateAdapter);
	}

	@Override
	protected boolean onCaseStarted() {
		Main.registerActivityResult(mOnActivityResult); // 注册结果返回回调
		// 开启调用测试应用
		Intent intent = new Intent();
		ComponentName component = new ComponentName(mPackage, mActivity);
		intent.setComponent(component);
		((Activity) mContext).startActivityForResult(intent, mRequestCode);
		return false;
	}

	@Override
	protected void onCaseFinished() {

	}

	@Override
	protected void onRelease() {
		Main.unregisterActivityResult(mOnActivityResult);
	}

	@Override
	protected void onPassableChange() {
		super.onPassableChange();
		if (getPassable()) {
			stateView.setBackgroundColor(Color.GREEN);
		} else {
			stateView.setBackgroundColor(Color.RED);
		}
	}

	private Map<String, Integer> mResults;

	private OnActivityResult mOnActivityResult = new OnActivityResult() {
		public void onActivityResult(int requestCode, int resultCode,
				Intent data) {
			if (requestCode == mRequestCode
					&& resultCode != Activity.RESULT_CANCELED) {
				Iterator<String> iterator = data.getExtras().keySet()
						.iterator();
				boolean pass = true;
				mResults = new LinkedHashMap<String, Integer>();
				while (iterator.hasNext()) {
					String key = iterator.next();
					int result = data.getIntExtra(key,
							ReportRecordItem.STATE_NO_TESTED);
					mResults.put(key, result);
					if (result == ReportRecordItem.STATE_UNSUCCESS) {
						pass = false;
					}
				}
				stateAdapter.setResult(mResults);
				setPassable(pass);
			}
		};
	};

	public Map<String, Integer> getActivityResult() {
		return mResults;
	}

	@Override
	public void onClick(View v) {
		if (v == mRestartButton) {
			setResult(mResult);
			startCase();
		}
	}

	@Override
	public String getDetailResult() {
		return super.getDetailResult();
	}

	/**
	 * 显示详细结果
	 * 
	 * @author zengsc
	 * @version date 2013-4-16
	 */
	class ResultAdapter extends BaseAdapter {
		private Context mContext;
		private List<String> mKeys;
		private List<Integer> mValue;

		public ResultAdapter(Context context) {
			mContext = context;
			mKeys = new ArrayList<String>();
			mValue = new ArrayList<Integer>();
		}

		public void setResult(Map<String, Integer> result) {
			mKeys.clear();
			mValue.clear();
			Iterator<String> iterator = result.keySet().iterator();
			while (iterator.hasNext()) {
				String key = iterator.next();
				mKeys.add(key);
				mValue.add(result.get(key));
			}
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mKeys.size();
		}

		@Override
		public Integer getItem(int position) {
			if (position < 0 || position >= mValue.size())
				return -1;
			return mValue.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = new TextView(mContext);
			}
			((TextView) convertView).setText(mKeys.get(position));
			int result = getItem(position);
			if (result == ReportRecordItem.STATE_NO_TESTED) {
				convertView.setBackgroundColor(Color.BLUE);
			} else if (result == ReportRecordItem.STATE_SUCCESS) {
				convertView.setBackgroundColor(Color.GREEN);
			} else {
				convertView.setBackgroundColor(Color.RED);
			}
			return convertView;
		}
	}
}
