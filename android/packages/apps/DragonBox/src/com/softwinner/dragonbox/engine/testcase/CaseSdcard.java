package com.softwinner.dragonbox.engine.testcase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.softwinner.dragonbox.R;
import com.softwinner.dragonbox.engine.BaseCase;
import com.softwinner.dragonbox.platform.BasePlatform;
import com.softwinner.dragonbox.xml.Node;

/**
 * SN MAC测试
 * 
 * @author AW_maizirong
 * 
 */

public class CaseSdcard extends BaseCase {
        
        private static final String TAG = "CaseSdcard";
	private StorageManager mStorageManager;
	private MemoryInfo mMemInfo;

	private long mAvailSize = 0; // 用来返回结果的，直接加上一个变量
	private TextView infoTxt;
	private TextView writeTxt;
	private LinearLayout infoLayout;
	private LinearLayout writeLayout;

	private boolean hasSD = false;

	private BroadcastReceiver mStorageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (DEBUG)
				Log.v(TAG, "get a new action:" + action);
			mMemInfo.init(context);
			outputSD();
		}

	};

	public class MemoryInfo {
		String noDevice;
		String mFormat;
		StorageVolume[] storageVolumes;
		boolean extsdState = false;

		void init(Context context) {
			noDevice = context.getResources().getString(
					R.string.case_storage_no_device);
			mFormat = context.getResources().getString(
					R.string.case_sdcard_info);
			StorageVolume[] volumes = mStorageManager.getVolumeList();
			storageVolumes = new StorageVolume[0];

			for (StorageVolume volume : volumes) {
				Log.v(TAG,
						volume.getPath()
								+ "|"
								+ mStorageManager.getVolumeState(volume
										.getPath()));
				if (!Environment.MEDIA_MOUNTED.equals(mStorageManager
						.getVolumeState(volume.getPath()))) {
					continue;
				}
				addVolume(volume);
			}
			extsdState = hasExtSd(volumes);
		}

		boolean hasExtSd(StorageVolume[] volumes) {
			boolean st = false;
			for (StorageVolume volume : volumes) {
				if (volume.getPath().equals("/mnt/sdcard")
						&& mStorageManager.getVolumeState(volume.getPath())
								.equals("mounted")) {
					st = true;
				}
			}
			return st;
		}

		void addVolume(StorageVolume volume) {
			int originSize = storageVolumes.length;
			StorageVolume[] volumes = new StorageVolume[originSize + 1];
			for (int i = 0; i < originSize; i++) {
				volumes[i] = storageVolumes[i];
			}
			volumes[originSize] = volume;
			storageVolumes = volumes;
		}

		String toOneString() {
			String str;
			String rstr = "未检测到设备";
			if (storageVolumes == null || storageVolumes.length == 0) {
				str = noDevice;
				return "未检测到设备";
			}
			for (int i = 0; i < storageVolumes.length; i++) {
				StorageVolume volume = storageVolumes[i];
				Log.e(TAG, "==volume.getPath()==" + volume.getPath());
				if (volume.getPath().equals("/mnt/sdcard")) {
					Log.e(TAG, "=======extsd=========");

					StatFs stat = new StatFs(volume.getPath());
					long blockSize = stat.getBlockSize();
					long totalBlocks = stat.getBlockCount();
					long availableBlocks = stat.getAvailableBlocks();

					long totalSize = totalBlocks * blockSize;
					long availSize = availableBlocks * blockSize;
					if (mAvailSize == 0)
						mAvailSize = availSize / 1024 / 1024;
					String description = BasePlatform.getPlatform()
							.getDescription(volume, mContext);

					// rstr = String.format(mFormat,
					// description,toSize(totalSize));
					rstr = description;
					// boolean passable = true;
					Log.e(TAG, "=Environment======="
							+ Environment.getExternalStorageDirectory()
									.getPath());
					Log.e(TAG, "=storageVolumes[i].getPath()======="
							+ storageVolumes[i].getPath());

					if (totalSize > 0 && availSize > 0) {
						mAvailSize = availSize / 1024 / 1024;
						rstr = "TF 卡 已插入";
						infoLayout.setBackgroundColor(Color.GREEN);
						hasSD = true;

					}
					if (write()) {
						writeTxt.setText("TF 卡 数据读写成功");
						writeLayout.setBackgroundColor(Color.GREEN);
					}
					if (hasSD && write()) {
						setPassable(true);
					}
				}

			}
			return rstr;
		}
	}

	private boolean write() {
		boolean state = false;
		String fileName = "/mnt/sdcard/factoryRun";
		String str = "JustForTest.";
		String str0 = "";
		try {
			FileOutputStream fout = new FileOutputStream(fileName);
			byte[] bytes = str.getBytes();
			fout.write(bytes);
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			InputStream in = new FileInputStream(fileName);
			byte b[] = new byte[1024];
			int len = 0;
			int temp = 0; // 所有读取的内容都使用temp接收
			while ((temp = in.read()) != -1) { // 当没有读取完时，继续读取
				b[len] = (byte) temp;
				len++;
			}
			in.close();
			str0 = new String(b, 0, len);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (str.equals(str0)) {
			state = true;
			System.out.print("111");
			File dir = new File(fileName);
			if (dir.exists())
				dir.delete();
		}
		return state;

	}

	public static String toSize(long size) {
		String s = toGb(size);
		if (s.startsWith("0.")) {
			s = toMb(size);
			if (s.startsWith("0.")) {
				s = toKb(size);
			}
		}
		return s;
	}

	public static String toKb(long mbyte) {
		String kb = String.format("%.2f Kb ", (double) mbyte / 1024);
		return kb;
	}

	public static String toMb(long mbyte) {
		String mb = String.format("%.2f Mb ", (double) mbyte / (1024 * 1024));
		return mb;
	}

	public static String toGb(long mbyte) {
		String gb = String.format("%.2f Gb ", (double) mbyte
				/ (1000 * 1000 * 1000));
		// 准确来说，应该除以(1024 * 1024 * 1024)
		return gb;
	}

	@Override
	protected void onInitialize(Node attr) {
		setView(R.layout.case_sdcard);
		setName(R.string.case_sdcard_name);
		infoTxt = (TextView) mView.findViewById(R.id.sdcard_info_text);
		writeTxt = (TextView) mView.findViewById(R.id.sdcard_write_text);
		infoLayout = (LinearLayout) mView.findViewById(R.id.linearLayout_info);
		writeLayout = (LinearLayout) mView
				.findViewById(R.id.linearLayout_write);
	}

	private void outputSD() {
		MemoryInfo memoryInfo = new MemoryInfo();
		memoryInfo.init(mContext);
		mMemInfo = memoryInfo;
		infoTxt.setText(memoryInfo.toOneString());
		Log.e(TAG, "===============" + memoryInfo.toOneString());
	}

	@Override
	protected boolean onCaseStarted() {
		// ListView list = (ListView) getView().findViewById(R.id.memory_info);
		// mAdapter = new ArrayAdapter<String>(mContext, R.layout.mid_text);
		// list.setAdapter(mAdapter);

		mStorageManager = (StorageManager) mContext
				.getSystemService(Context.STORAGE_SERVICE);
		// mAdapter.addAll(memoryInfo.toOneString());
		// mAdapter.notifyDataSetChanged();
		outputSD();
		IntentFilter filter = new IntentFilter();
		filter.addDataScheme("file");
		filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_REMOVED);
		mContext.registerReceiver(mStorageReceiver, filter);
		return false;
	}

	@Override
	protected void onCaseFinished() {
		mContext.unregisterReceiver(mStorageReceiver);
	}

	@Override
	protected void onRelease() {

	}

	@Override
	protected void onPassableChange() {
		super.onPassableChange();
		if (getPassable()) {
			getView().findViewById(R.id.sdcard_name).setBackgroundColor(
					Color.GREEN);

		} else {
			getView().findViewById(R.id.sdcard_name).setBackgroundColor(
					Color.RED);
		}
	}

	@Override
	protected void onPassableInfo(Node node) {
		super.onPassableInfo(node);
	}

	@Override
	public String getDetailResult() {
		return "size:" + mAvailSize;
	}

}
