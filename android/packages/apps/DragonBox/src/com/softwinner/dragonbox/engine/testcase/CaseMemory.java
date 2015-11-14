package com.softwinner.dragonbox.engine.testcase;

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
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.softwinner.dragonbox.R;
import com.softwinner.dragonbox.engine.BaseCase;
import com.softwinner.dragonbox.platform.BasePlatform;
import com.softwinner.dragonbox.xml.Node;

public class CaseMemory extends BaseCase {

	private static final String TAG = CaseMemory.class.getSimpleName();
	public static final String PASSABLE_MIN_CAP = "minCap";

	private StorageManager mStorageManager;
	private ArrayAdapter<String> mAdapter;
	private MemoryInfo mMemInfo;

	private int mMinCapInMB = -1;
	private long mAvailSize = 0; // 用来返回结果的，直接加上一个变量

	private BroadcastReceiver mStorageReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (DEBUG)
				Log.v(TAG, "get a new action:" + action);
			mMemInfo.init(context);
			mAdapter.clear();
			mAdapter.addAll(mMemInfo.toArrayString());
			mAdapter.notifyDataSetChanged();
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
					R.string.case_memory_info);
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
				if (volume.getPath().equals("/mnt/extsd")
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

		String[] toArrayString() {
			if (storageVolumes == null || storageVolumes.length == 0) {
				String[] str = new String[1];
				str[0] = noDevice;
				return str;
			}
			String rstr[] = new String[storageVolumes.length];
			for (int i = 0; i < storageVolumes.length; i++) {
				StorageVolume volume = storageVolumes[i];

				StatFs stat = new StatFs(volume.getPath());
				long blockSize = stat.getBlockSize();
				long totalBlocks = stat.getBlockCount();
				long availableBlocks = stat.getAvailableBlocks();

				long totalSize = totalBlocks * blockSize;
				long availSize = availableBlocks * blockSize;
				if (mAvailSize == 0)
					mAvailSize = availSize / 1024 / 1024;
				String description = BasePlatform.getPlatform().getDescription(
						volume, mContext);
				rstr[i] = String.format(mFormat, "" + description, ""
						+ toSize(totalSize), "" + toSize(availSize));
				// boolean passable = true;
				if ((Environment.getExternalStorageDirectory().getPath()
						.equals(storageVolumes[i].getPath()))
						&& mMinCapInMB != -1) {
					mAvailSize = availSize / 1024 / 1024;
					Log.v(TAG, "" + mMinCapInMB + "|"
							+ (availSize / 1024 / 1024));
					if (mMinCapInMB <= (availSize / 1024 / 1024) && extsdState) {
						setPassable(true);
					} else {
						setPassable(false);
					}
				}

			}
			return rstr;
		}
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
				/ (1024 * 1024 * 1024));
		return gb;
	}

	@Override
	protected void onInitialize(Node attr) {
		setView(R.layout.case_memory);
		setName(R.string.case_memory_name);

	}

	@Override
	protected boolean onCaseStarted() {
		ListView list = (ListView) getView().findViewById(R.id.memory_info);
		mAdapter = new ArrayAdapter<String>(mContext, R.layout.mid_text);
		list.setAdapter(mAdapter);
		mStorageManager = (StorageManager) mContext
				.getSystemService(Context.STORAGE_SERVICE);

		MemoryInfo memoryInfo = new MemoryInfo();
		memoryInfo.init(mContext);
		mMemInfo = memoryInfo;
		mAdapter.addAll(memoryInfo.toArrayString());
		mAdapter.notifyDataSetChanged();

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
			getView().findViewById(R.id.textView1).setBackgroundColor(
					Color.GREEN);
		} else {
			getView().findViewById(R.id.textView1)
					.setBackgroundColor(Color.RED);
		}
	}

	@Override
	protected void onPassableInfo(Node node) {
		super.onPassableInfo(node);
		mMinCapInMB = node.getAttributeIntegerValue(PASSABLE_MIN_CAP);
		if (mMinCapInMB == -1) {
			setPassable(true);
		}
	}

	@Override
	public String getDetailResult() {
		return "size:" + mAvailSize;
	}
}
