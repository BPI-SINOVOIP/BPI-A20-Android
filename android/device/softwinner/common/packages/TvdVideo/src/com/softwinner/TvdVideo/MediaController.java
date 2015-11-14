/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.softwinner.TvdVideo;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.DisplayManagerAw;
//remove Animation,Modify by maizirong
//import android.view.animation.Animation;
//import android.view.animation.Animation.AnimationListener;
//import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.android.internal.policy.PolicyManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import android.os.SystemProperties;

//import com.android.internal.softwinner.config.ProductConfig;
/**
 * A view containing controls for a MediaPlayer. Typically contains the buttons
 * like "Play/Pause", "Rewind", "Fast Forward" and a progress slider. It takes
 * care of synchronizing the controls with the state of the MediaPlayer.
 * <p>
 * The way to use this class is to instantiate it programatically. The
 * MediaController will create a default set of controls and put them in a
 * window floating above your application. Specifically, the controls will float
 * above the view specified with setAnchorView(). The window will disappear if
 * left idle for three seconds and reappear when the user touches the anchor
 * view.
 * <p>
 * Functions like show() and hide() have no effect when MediaController is
 * created in an xml layout.
 * 
 * MediaController will hide and show the buttons according to these rules:
 * <ul>
 * <li>The "previous" and "next" buttons are hidden until setPrevNextListeners()
 * has been called
 * <li>The "previous" and "next" buttons are visible but disabled if
 * setPrevNextListeners() was called with null listeners
 * <li>The "rewind" and "fastforward" buttons are shown unless requested
 * otherwise by using the MediaController(Context, boolean) constructor with the
 * boolean set to false
 * </ul>
 */
public class MediaController extends FrameLayout {
	private static final String TAG = "MediaController";

	private static final int LISTITEMMAX = 6;
	private MediaPlayerControl mPlayer;
	private Context mContext;
	private View mAnchor, mDecor;
	private View mRoot, mStatus, mMediaControl, mFocusListView;
	private TextView mFileName, mCurrentTime, mEndTime;
	private String mFileNameText;
	private WindowManager mWindowManager;
	private Window mWindow;
	private boolean mShowing, mHolding;
	private boolean mDragging;
	private static final int sDefaultTimeout = 5000, SEEKSTEPTIME = 2000;
	private static final int FADE_OUT = 1;
	private static final int SHOW_PROGRESS = 2;
	private static final int SEEK_PROGRESS = 3;
	private SeekBar mProgress;
	private int mSeekTime, mSeekFlag = 0;
	private View.OnClickListener mNextListener, mPrevListener;

	private int mMediaControlFocusId, mListFoucsIndex;
	private View.OnFocusChangeListener mSubSetListener;
	private View.OnFocusChangeListener mZoomListener;
	private View.OnFocusChangeListener m3DListener;
	private View.OnFocusChangeListener mRepeatListener;
	private View.OnFocusChangeListener mTrackListener;
	private View.OnClickListener mJumpClickListener;

	private ImageButton mPauseButton;
	private ImageButton mNextButton;
	private ImageButton mPrevButton;
	private ImageButton mSubSetButton;
	private ImageButton m3DButton;
	private ImageButton mRepeatButton;
	private ImageButton mZoomButton;
	private ImageButton mJumpButton;
	private ImageButton mTrackButton;
	private ImageButton mVolumeUpBttn;
	private ImageButton mVolumeDownBttn;
	private ListView mListView, mSublistView;
	private GridView mGridView;
	private SinglechoiceListAdapter mListAdapter, mSublistAdapter;
	private SimpleAdapter mSimpleAdapter2D, mSimpleAdapter3D;
	private LinearLayout mListLayout, mSublistLayout, mGridLayout;

	@Override
	public void onFinishInflate() {
		if (mRoot != null)
			initControllerView(mRoot);
	}

	public MediaController(Context context) {
		super(context);
		mContext = context;
		initFloatingWindow();
	}

	private void initFloatingWindow() {
		mWindowManager = (WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE);
		mWindow = PolicyManager.makeNewWindow(mContext);
		mWindow.setWindowManager(mWindowManager, null, null);
		mWindow.requestFeature(Window.FEATURE_NO_TITLE);
		mDecor = mWindow.getDecorView();
		mDecor.setOnTouchListener(mTouchListener);
		mWindow.setContentView(this);
		mWindow.setBackgroundDrawableResource(android.R.color.transparent);

		// While the media controller is up, the volume control keys should
		// affect the media stream type
		mWindow.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		// mDisplay = mWindowManager.getDefaultDisplay();

		setFocusable(true);
		setFocusableInTouchMode(true);
		setHolding(false);
		setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
		requestFocus();
	}

	private OnTouchListener mTouchListener = new OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				if (mShowing) {
					hide();
				}
			}
			return false;
		}
	};

	public void setMediaPlayer(MediaPlayerControl player) {
		mPlayer = player;
		updatePausePlay();
	}

	/**
	 * Set the view that acts as the anchor for the control view. This can for
	 * example be a VideoView, or your Activity's main view.
	 * 
	 * @param view
	 *            The view to which to anchor the controller when it is visible.
	 */
	public void setAnchorView(View view) {
		mAnchor = view;

		FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);

		removeAllViews();
		View v = makeControllerView();
		View statusView = makeStatusView();
		addView(v, frameParams);
		addView(statusView, frameParams);
	}

	/**
	 * Create the view that holds the widgets that control playback. Derived
	 * classes can override this to create their own.
	 * 
	 * @return The controller view.
	 * @hide This doesn't work as advertised
	 */
	protected View makeStatusView() {
		LayoutInflater inflate = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mStatus = inflate.inflate(R.layout.media_status, null);

		initStatusView(mStatus);

		return mStatus;
	}

	private void initStatusView(View v) {
		mFileName = (TextView) v.findViewById(R.id.file_name);
		mFileName.setText(mFileNameText);
		mCurrentTime = (TextView) v.findViewById(R.id.time_current);
		mEndTime = (TextView) v.findViewById(R.id.time);
		mProgress = (SeekBar) v.findViewById(R.id.mediacontroller_progress);
		if (mProgress != null) {
			mProgress.setOnSeekBarChangeListener(mSeekListener);
			mProgress.setMax(1000);
		}
	}

	/**
	 * Create the view that holds the widgets that control playback. Derived
	 * classes can override this to create their own.
	 * 
	 * @return The controller view.
	 * @hide This doesn't work as advertised
	 */
	protected View makeControllerView() {
		LayoutInflater inflate = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mRoot = inflate.inflate(R.layout.media_controller, null);

		initControllerView(mRoot);

		return mRoot;
	}

	private void initControllerView(View v) {
		Log.w(TAG,
				"*********** initControllerView *************************************");
		mMediaControl = v.findViewById(R.id.media_control);

		m3DButton = (ImageButton) v.findViewById(R.id.mode3D);
		if (m3DButton != null) {
			m3DButton.setOnClickListener(mButtonClickListener);
			m3DButton.setOnFocusChangeListener(m3DListener);

			// String chipType = SystemProperties.get(ProductConfig.CHIP_TYPE);
			// if(chipType == null){
			// Log.w(TAG, "Fail in getting the value of property " +
			// ProductConfig.CHIP_TYPE);
			// chipType = ProductConfig.CHIP_TYPE_DEFAULT;
			// }
			// Log.d(TAG, "property " + ProductConfig.CHIP_TYPE + " is " +
			// chipType);
			// if(chipType.equals(ProductConfig.CHIP_TYPE_A10S)){
			// m3DButton.setVisibility(View.GONE);
			// }else {
			m3DButton.setVisibility(View.VISIBLE);
			// }
		}
		mRepeatButton = (ImageButton) v.findViewById(R.id.repeat);
		if (mRepeatButton != null) {
			mRepeatButton.setOnClickListener(mButtonClickListener);
			mRepeatButton.setOnFocusChangeListener(mRepeatListener);
		}
		mTrackButton = (ImageButton) v.findViewById(R.id.track);
		if (mTrackButton != null) {
			mTrackButton.setOnClickListener(mButtonClickListener);
			mTrackButton.setOnFocusChangeListener(mTrackListener);
		}
		mPrevButton = (ImageButton) v.findViewById(R.id.prev);
		if (mPrevButton != null) {
			mPrevButton.setOnClickListener(mPrevListener);
			mPrevButton.setOnFocusChangeListener(mOnButtonFocusChangeListener);
		}
		mPauseButton = (ImageButton) v.findViewById(R.id.pause);
		if (mPauseButton != null) {
			mPauseButton.requestFocus();
			mPauseButton.setOnClickListener(mPauseListener);
			mPauseButton.setOnFocusChangeListener(mOnButtonFocusChangeListener);
		}
		mNextButton = (ImageButton) v.findViewById(R.id.next);
		if (mNextButton != null) {
			mNextButton.setOnClickListener(mNextListener);
			mNextButton.setOnFocusChangeListener(mOnButtonFocusChangeListener);
		}
		mZoomButton = (ImageButton) v.findViewById(R.id.zoom);
		if (mZoomButton != null) {
			mZoomButton.setOnClickListener(mButtonClickListener);
			mZoomButton.setOnFocusChangeListener(mZoomListener);
		}
		mJumpButton = (ImageButton) v.findViewById(R.id.jump);
		if (mJumpButton != null) {
			mJumpButton.setOnClickListener(mJumpClickListener);
			mJumpButton.setOnFocusChangeListener(mJumpFocusChangeListener);
		}
		mSubSetButton = (ImageButton) v.findViewById(R.id.subset);
		if (mSubSetButton != null) {
			mSubSetButton.setOnClickListener(mButtonClickListener);
			mSubSetButton.setOnFocusChangeListener(mSubSetListener);
		}
		mVolumeUpBttn = (ImageButton) v.findViewById(R.id.volume_up);
		if (mVolumeUpBttn != null) {
			mVolumeUpBttn.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					sendKeyIntent(KeyEvent.KEYCODE_VOLUME_UP);
				}

			});
			mVolumeUpBttn
					.setOnFocusChangeListener(mOnButtonFocusChangeListener);
		}
		mVolumeDownBttn = (ImageButton) v.findViewById(R.id.volume_down);
		if (mVolumeDownBttn != null) {
			mVolumeDownBttn.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					sendKeyIntent(KeyEvent.KEYCODE_VOLUME_DOWN);
				}

			});
			mVolumeDownBttn
					.setOnFocusChangeListener(mOnButtonFocusChangeListener);
		}

		initGridViewParam(v);

		mListView = (ListView) v.findViewById(R.id.list);
		if (mListAdapter == null) {
			mListAdapter = new SinglechoiceListAdapter(mContext);
		}
		mListView.setOnItemClickListener(mOnItemClickListener);
		mListView.setOnFocusChangeListener(mListFocusChangeListener);
		mListView.setOnKeyListener(mOnListKeyListener);
		mListView.setAdapter(mListAdapter);

		mSublistView = (ListView) v.findViewById(R.id.sublist);
		if (mSublistAdapter == null) {
			mSublistAdapter = new SinglechoiceListAdapter(mContext);
		}
		mSublistView.setOnItemClickListener(mOnSubItemClickListener);
		mSublistView.setAdapter(mSublistAdapter);

		mListLayout = (LinearLayout) v.findViewById(R.id.list_layout);
		mSublistLayout = (LinearLayout) v.findViewById(R.id.sublist_layout);
		mListLayout.setVisibility(GONE);
		mSublistLayout.setVisibility(GONE);

		resetListLayoutHeight();
	}

	private void sendKeyIntent(int keycode) {
		final int keyCode = keycode;
		// to avoid deadlock, start a thread to perform operations
		Thread sendKeyDelay = new Thread() {
			public void run() {
				try {
					int count = 1;
					if (keyCode == KeyEvent.KEYCODE_BACK)
						count = 2;

					IWindowManager wm = IWindowManager.Stub
							.asInterface(ServiceManager.getService("window"));
					for (int i = 0; i < count; i++) {
						Thread.sleep(100);
						long now = SystemClock.uptimeMillis();
						// if(!mOnPause) {
						KeyEvent keyDown = new KeyEvent(now, now,
								KeyEvent.ACTION_DOWN, keyCode, 0);
						InputManager.getInstance().injectInputEvent(keyDown,
								InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
						// wm.injectKeyEvent(keyDown, false);

						KeyEvent keyUp = new KeyEvent(now, now,
								KeyEvent.ACTION_UP, keyCode, 0);
						InputManager.getInstance().injectInputEvent(keyUp,
								InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
						// wm.injectKeyEvent(keyUp, false);
						// }
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				// catch(RemoteException ex){
				//
				// }

			}
		};
		sendKeyDelay.start();
	}

	private void initGridViewParam(View v) {
		mGridLayout = (LinearLayout) v.findViewById(R.id.grid_layout);
		mGridView = (GridView) v.findViewById(R.id.gridview);
		mGridView.setOnItemClickListener(mOnSubItemClickListener);
		/* 2D grid view init */
		String[] text2D = mContext.getResources().getStringArray(
				R.array.screen_3d_2d_entries);
		int[] id2D = { R.drawable.grid_left_right, R.drawable.grid_up_down,
				R.drawable.grid_custom };
		ArrayList<HashMap<String, Object>> lstImageItem2D = new ArrayList<HashMap<String, Object>>();
		for (int i = 0; i < Math.min(text2D.length, id2D.length); i++) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("ItemImage", id2D[i]);
			map.put("ItemText", text2D[i]);
			lstImageItem2D.add(map);
		}
		mSimpleAdapter2D = new SimpleAdapter(mContext, lstImageItem2D,
				R.layout.grid_item, new String[] { "ItemImage", "ItemText" },
				new int[] { R.id.ItemImage, R.id.ItemText });
		/* 3D grid view init */
		String[] text3D = mContext.getResources().getStringArray(
				R.array.screen_3d_3d_entries);
		int[] id3D = { R.drawable.grid_left_right, R.drawable.grid_up_down,
				R.drawable.grid_interlace, R.drawable.grid_line_interlace };// ,
																			// R.drawable.grid_row_interlace};
		ArrayList<HashMap<String, Object>> lstImageItem3D = new ArrayList<HashMap<String, Object>>();
		for (int i = 0; i < Math.min(text3D.length, id3D.length); i++) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			map.put("ItemImage", id3D[i]);
			map.put("ItemText", text3D[i]);
			lstImageItem3D.add(map);
		}
		mSimpleAdapter3D = new SimpleAdapter(mContext, lstImageItem3D,
				R.layout.grid_item, new String[] { "ItemImage", "ItemText" },
				new int[] { R.id.ItemImage, R.id.ItemText });
	}

	private void resetListLayoutHeight() {
		LayoutInflater inflate = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View listItem = inflate.inflate(R.layout.list_item, null);
		listItem.measure(
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		int itemHeight = listItem.getMeasuredHeight();
		int dividerHeight = mListView.getDividerHeight();
		int listLayoutHeight = (itemHeight + dividerHeight) * LISTITEMMAX
				- dividerHeight;
		RelativeLayout.LayoutParams ll = (android.widget.RelativeLayout.LayoutParams) mListLayout
				.getLayoutParams();
		ll.height = listLayoutHeight;
	}

	/**
	 * set listview data
	 * 
	 */
	public void setListViewData(int mediaControlFocusId, int selectedItem,
			String[] listData) {
		setHolding(true);

		mMediaControlFocusId = mediaControlFocusId;
		mListAdapter.setSelectIcon(R.drawable.selected);
		mListAdapter.setListItems(Arrays.asList(listData));
		mListAdapter.notifyDataSetChanged(selectedItem);
		mListLayout.setVisibility(VISIBLE);
		/* hide the sublist */
		mSublistLayout.setVisibility(GONE);
		if (mGridLayout.getVisibility() == VISIBLE) {
			mGridLayout.setVisibility(GONE);
		}
		/* set listview margin left */
		int marginLeft = getListViewMarginLeft(mediaControlFocusId);
		RelativeLayout.LayoutParams ll = (android.widget.RelativeLayout.LayoutParams) mListLayout
				.getLayoutParams();
		ll.leftMargin = marginLeft;
		/* reset listlayout height to fit listview items */

		/* set listview animation */
		int lHeight = ll.height;
		//remove Animation,Modify by maizirong
		//Animation translateAnimation = new TranslateAnimation(0, 0, lHeight, 0);
		//translateAnimation.setDuration(500);
		//mListView.startAnimation(translateAnimation);
		mListView.setVisibility(VISIBLE);
		mListView.setNextFocusDownId(mediaControlFocusId);
		mRoot.invalidate();
	}

	private int getListViewMarginLeft(int id) {
		ImageButton button = (ImageButton) findViewById(id);
		return button.getLeft();

		// int l = button.getLeft();
		// int w = button.getWidth();
		// int listW = mListView.getWidth();
		// int dispW = mDisplay.getWidth();
		//
		// int marginLeft = l + w/2 - listW/2;
		// if(marginLeft < 0) {
		// marginLeft = 0;
		// } else if(marginLeft + listW > dispW) {
		// marginLeft = dispW - listW;
		// }
		//
		// return marginLeft;
	}

	/**
	 * set listview data
	 * 
	 */
	public void setSublistViewData(int sublistFoucsIndex, int selectedItem,
			String[] listData) {
		mListFoucsIndex = sublistFoucsIndex;
		mSublistAdapter.setSelectIcon(R.drawable.selected);
		mSublistAdapter.setListItems(Arrays.asList(listData));
		mSublistAdapter.notifyDataSetChanged(selectedItem);
		/* set sublistview margin top */
		int marginTop = getSublistViewTopMargin(listData.length);
		LinearLayout.LayoutParams ll = (android.widget.LinearLayout.LayoutParams) mSublistLayout
				.getLayoutParams();
		ll.topMargin = marginTop;
		mSublistLayout.setLayoutParams(ll);
		if (mGridLayout.getVisibility() == VISIBLE) {
			mGridLayout.setVisibility(GONE);
		}
		mSublistLayout.setVisibility(VISIBLE);
		int lWidth = ll.width;
		/* set listview margin left */
		int marginLeft = getListViewMarginLeft(mMediaControlFocusId);
		RelativeLayout.LayoutParams listPara = (android.widget.RelativeLayout.LayoutParams) mListLayout
				.getLayoutParams();
		listPara.leftMargin = marginLeft - ll.width;
		//remove Animation,Modify by maizirong
		/* set listview animation */
		//Animation translateAnimation = new TranslateAnimation(lWidth, 0, 0, 0);
		//translateAnimation.setDuration(500);
		mSublistView.requestFocus();
		mSublistView.setVisibility(VISIBLE);
		mRoot.invalidate();
	}

	private int getSublistViewTopMargin(int itemCount) {
		int marginTop = 0;
		View focusListView = mFocusListView;
		int bottomRemain = mListView.getMeasuredHeight()
				- focusListView.getTop();
		int sublistH = (itemCount - 1) * mListView.getDividerHeight()
				+ itemCount * focusListView.getMeasuredHeight();
		int layoutH = findViewById(R.id.list_layout).getHeight();
		if (sublistH < bottomRemain) {
			marginTop = layoutH - bottomRemain;
		} else if (sublistH < layoutH) {
			marginTop = layoutH - sublistH;
		}

		return marginTop;
	}

	public void setGridViewData(int sublistFoucsIndex, int selectedItem) {
		mListFoucsIndex = sublistFoucsIndex;

		if (sublistFoucsIndex == 0) {
			mGridView.setAdapter(mSimpleAdapter2D);
		} else {
			mGridView.setAdapter(mSimpleAdapter3D);
		}
		if (mSublistLayout.getVisibility() == VISIBLE) {
			mSublistLayout.setVisibility(GONE);
		}
		/* set gridview margin top */
		int marginTop = getGridViewTopMargin(sublistFoucsIndex);
		LinearLayout.LayoutParams ll = (android.widget.LinearLayout.LayoutParams) mGridLayout
				.getLayoutParams();
		ll.topMargin = marginTop;
		/* set gridview margin left */
		int marginLeft = getListViewMarginLeft(mMediaControlFocusId);
		RelativeLayout.LayoutParams listPara = (android.widget.RelativeLayout.LayoutParams) mListLayout
				.getLayoutParams();
		listPara.leftMargin = marginLeft - ll.width;

		mGridView.requestFocus();
		mGridLayout.setVisibility(VISIBLE);
	}

	private int getGridViewTopMargin(int sublistFoucsIndex) {
		int marginTop = 0;
		int gridViewHeight;
		View focusListView = mFocusListView;

		int layoutH = findViewById(R.id.list_layout).getHeight();
		mGridView.measure(
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
				MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
		gridViewHeight = mGridView.getMeasuredHeight();
		if (sublistFoucsIndex != 0) {
			gridViewHeight *= 2;
		}
		int bottomRemain = mListView.getMeasuredHeight()
				- focusListView.getTop();
		if (gridViewHeight < bottomRemain) {
			marginTop = layoutH - bottomRemain;
		} else {
			marginTop = layoutH - gridViewHeight;
		}

		return marginTop;
	}

	/**
	 * Show the controller on screen. It will go away automatically after 3
	 * seconds of inactivity.
	 */
	public void show() {
		show(sDefaultTimeout);
	}

	/**
	 * Disable pause or seek buttons if the stream cannot be paused or seeked.
	 * This requires the control interface to be a MediaPlayerControlExt
	 */
	private void disableUnsupportedButtons() {
		try {
			if (mPauseButton != null && mPlayer != null && !mPlayer.canPause()) {
				mPauseButton.setEnabled(false);
			}
		} catch (IncompatibleClassChangeError ex) {
			// We were given an old version of the interface, that doesn't have
			// the canPause/canSeekXYZ methods. This is OK, it just means we
			// assume the media can be paused and seeked, and so we don't
			// disable
			// the buttons.
		}
	}

	/**
	 * Show the controller on screen. It will go away automatically after
	 * 'timeout' milliseconds of inactivity.
	 * 
	 * @param timeout
	 *            The timeout in milliseconds. Use 0 to show the controller
	 *            until hide() is called.
	 */
	public void show(int timeout) {

		if (!mShowing && mAnchor != null) {
			// setProgress();
			if (mPauseButton != null) {
				mPauseButton.requestFocus();
			}

			if (mPauseButton != null) {
				mPauseButton.requestFocus();
			}
			disableUnsupportedButtons();

			int[] anchorpos = new int[2];
			mAnchor.getLocationOnScreen(anchorpos);

			WindowManager.LayoutParams p = new WindowManager.LayoutParams();
			// p.gravity = Gravity.TOP;
			p.width = mAnchor.getWidth();
			p.height = LayoutParams.MATCH_PARENT;
			p.x = 0;
			p.y = 0;// anchorpos[1] + mAnchor.getHeight() - p.height;
			p.format = PixelFormat.TRANSLUCENT;
			p.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
			p.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
			p.token = null;
			p.windowAnimations = 0; // android.R.style.DropDownAnimationDown;
			mWindowManager.addView(mDecor, p);

			mMediaControl.measure(
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			//remove Animation,Modify by maizirong
			//Animation translateAnimation = new TranslateAnimation(0, 0,mMediaControl.getMeasuredHeight(), 0);
			//translateAnimation.setDuration(500);
			//mMediaControl.startAnimation(translateAnimation);
			mMediaControl.setVisibility(VISIBLE);

			mStatus.measure(
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			//remove Animation,Modify by maizirong
			//Animation statusTranslateAnimation = new TranslateAnimation(0, 0,0 - mStatus.getMeasuredHeight(), 0);
			//statusTranslateAnimation.setDuration(500);
			//mStatus.startAnimation(statusTranslateAnimation);
			mStatus.setVisibility(VISIBLE);

			mShowing = true;
		}
		updatePausePlay();
		mHandler.sendEmptyMessage(SHOW_PROGRESS);

		Message msg = mHandler.obtainMessage(FADE_OUT);
		if (timeout != 0) {
			mHandler.removeMessages(FADE_OUT);
			mHandler.sendMessageDelayed(msg, timeout);
		}
	}

	public boolean isShowing() {
		return mShowing;
	}

	/* if the widget need keep state or not */
	public void setHolding(boolean hold) {
		mHolding = hold;
	}

	public int getMediaControlFocusId() {
		return mMediaControlFocusId;
	}

	public void setMediaControlFocusId(int id) {
		mMediaControlFocusId = id;
	}

	/**
	 * Remove the controller from the screen.
	 */
	public void hide() {
		if (mAnchor == null)
			return;

		if (mHolding) {
			Message msg = mHandler.obtainMessage(FADE_OUT);
			mHandler.removeMessages(FADE_OUT);
			mHandler.sendMessageDelayed(msg, sDefaultTimeout);
			return;
		}

		if (mShowing) {
			try {
				mMediaControl
						.measure(MeasureSpec.makeMeasureSpec(0,
								MeasureSpec.UNSPECIFIED), MeasureSpec
								.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
				//remove Animation,Modify by maizirong 
				//Animation translateAnimation = new TranslateAnimation(0, 0, 0,mMediaControl.getMeasuredHeight());
				//translateAnimation.setDuration(500);
				//mMediaControl.startAnimation(translateAnimation);
				mMediaControl.setVisibility(GONE);

				mStatus.measure(
						MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
						MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
				//remove Animation,Modify by maizirong
				//Animation statusTranslateAnimation = new TranslateAnimation(0,0, 0, 0 - mStatus.getMeasuredHeight());
				//statusTranslateAnimation.setDuration(500);
				//statusTranslateAnimation.setAnimationListener(mAnimationListener);
				//mStatus.startAnimation(statusTranslateAnimation);
				mStatus.setVisibility(GONE);
				mWindowManager.removeViewImmediate(mDecor);
				mShowing = false;

				mHandler.removeMessages(SHOW_PROGRESS);
				mHandler.removeMessages(SEEK_PROGRESS);

			} catch (IllegalArgumentException ex) {
				Log.w(TAG, "already removed");
			}
		}
	}

	//remove Animation,Modify by maizirong
	/*private AnimationListener mAnimationListener = new AnimationListener() {

		@Override
		public void onAnimationStart(Animation arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onAnimationRepeat(Animation arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onAnimationEnd(Animation arg0) {
			// TODO Auto-generated method stub
			mWindowManager.removeViewImmediate(mDecor);
			mShowing = false;
		}
	};*/

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int pos;
			switch (msg.what) {
			case FADE_OUT:
				hide();
				break;
			case SHOW_PROGRESS:
				if (mSeekFlag == 0) {
					pos = setProgress();
					// if (mShowing && mPlayer.isPlaying()) {
					if (!mDragging && mShowing) {
						msg = obtainMessage(SHOW_PROGRESS);
						sendMessageDelayed(msg, 1000 - (pos % 1000));
					}
				}
				break;
			case SEEK_PROGRESS:
				if (!mDragging && mShowing && mSeekFlag != 0) {
					if (mSeekFlag > 0) { // FF
						mSeekTime += SEEKSTEPTIME;
					} else { // FR
						mSeekTime -= SEEKSTEPTIME;
					}
					seekVideo();
					msg = obtainMessage(SEEK_PROGRESS);
					sendMessageDelayed(msg, 100);
				}
			}
		}
	};

	private String stringForTime(int timeMs) {
		int totalSeconds = timeMs / 1000;

		int seconds = totalSeconds % 60;
		int minutes = (totalSeconds / 60) % 60;
		int hours = totalSeconds / 3600;

		if (hours > 0) {
			return String.format("%d:%02d:%02d", hours, minutes, seconds)
					.toString();
		} else {
			return String.format("%02d:%02d", minutes, seconds).toString();
		}
	}

	public void setFilePathTextView(String filePath) {
		mFileNameText = filePath;
		int index = filePath.lastIndexOf('/');
		if (index >= 0) {
			mFileNameText = filePath.substring(index + 1);
		}
		if (mStatus != null && mFileName != null) {
			mFileName.setText(mFileNameText);
		}
	}

	private int setProgress() {
		if (mPlayer == null || mDragging) {
			return 0;
		}
		int position = mPlayer.getCurrentPosition();
		int duration = mPlayer.getDuration();
		duration = duration == -1 ? 0 : duration;
		if (mProgress != null) {
			if (duration > 0) {
				// use long to avoid overflow
				long pos = 1000L * position / duration;
				mProgress.setProgress((int) pos);
			}
			int percent = mPlayer.getBufferPercentage();
			mProgress.setSecondaryProgress(percent * 10);
		}
		if (mEndTime != null)
			mEndTime.setText(stringForTime(duration));
		if (mCurrentTime != null)
			mCurrentTime.setText(stringForTime(position));

		return position;
	}

	private void seekVideo() {
		int pos = mPlayer.getCurrentPosition();
		pos += mSeekTime; // milliseconds
		int duration = mPlayer.getDuration();
		if (pos > 0 && pos < duration) {
			mPlayer.seekTo(pos);
			setProgress();
			// show(sDefaultTimeout);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			if (isShowing()) {
				hide();
			} else {
				show(sDefaultTimeout);
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int keyCode = event.getKeyCode();
		if (event.getRepeatCount() == 0
				&& event.isDown()
				&& (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
						|| keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE)) {
			doPauseResume();
			show(sDefaultTimeout);
			if (mPauseButton != null) {
				mPauseButton.requestFocus();
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
				|| keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
			if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
					&& event.getAction() == KeyEvent.ACTION_DOWN) {
				OnMediaPrevClickListener();
			} else if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
					&& event.getAction() == KeyEvent.ACTION_DOWN) {
				OnMediaNextClickListener();
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
				|| keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
					if (mSeekFlag != 1) {
						mHandler.sendEmptyMessage(SEEK_PROGRESS);
						mSeekTime = 0;
						mSeekFlag = 1;
					}
				} else {
					if (mSeekFlag != -1) {
						mHandler.sendEmptyMessage(SEEK_PROGRESS);
						mSeekTime = 0;
						mSeekFlag = -1;
					}
				}
			} else {
				mSeekFlag = 0;
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_SUBTITLE) { // subtitle key
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				OnMediaSubtitleKeyListener();
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_AUDIO) { // audio key
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				OnMediaAudioKeyListener();
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_LOOP) { // repeat key
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				OnMediaRepeatClickListener();
			}
		} else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				OnMediaStopClickListener();
			}

			return true;
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
				|| keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			// don't show the controls for volume adjustment
			return super.dispatchKeyEvent(event);
		} else if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (event.getAction() == KeyEvent.ACTION_UP) {
				if (mHolding) {
					/* hide the sublist view */
					if (mSublistLayout.getVisibility() == VISIBLE) {
						mListView.requestFocus();
						mSublistLayout.setVisibility(GONE);
						int marginLeft = getListViewMarginLeft(mMediaControlFocusId);
						RelativeLayout.LayoutParams ll = (android.widget.RelativeLayout.LayoutParams) mListLayout
								.getLayoutParams();
						ll.leftMargin = marginLeft;
						return true;
					}
					/* hide the grid view */
					if (mGridLayout.getVisibility() == VISIBLE) {
						mListView.requestFocus();
						mGridLayout.setVisibility(GONE);
						int marginLeft = getListViewMarginLeft(mMediaControlFocusId);
						RelativeLayout.LayoutParams ll = (android.widget.RelativeLayout.LayoutParams) mListLayout
								.getLayoutParams();
						ll.leftMargin = marginLeft;
						return true;
					}

					setHolding(false);
					mListLayout.setVisibility(GONE);
					mPauseButton.requestFocus();
					return true;
				}
				hide();
			}

			return true;
		} else {
			show(sDefaultTimeout);
		}

		return super.dispatchKeyEvent(event);
	}

	View.OnKeyListener mOnListKeyListener = new OnKeyListener() {

		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			// TODO Auto-generated method stub
			if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
					&& event.getAction() == KeyEvent.ACTION_DOWN
					&& (mMediaControlFocusId == R.id.subset
							&& mSublistLayout.getVisibility() != VISIBLE || mMediaControlFocusId == R.id.mode3D
							&& mGridLayout.getVisibility() != VISIBLE)) {
				int position = mListView.getSelectedItemPosition();
				if (position >= 0 && position < mListAdapter.getCount()) {
					mFocusListView = mListView.getSelectedView();
					mListAdapter.notifyDataSetChanged(position);
					mOnListDataChanged.OnListDataChangedListener(
							mMediaControlFocusId, position);
				}
			}

			return false;
		}
	};

	OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long id) {
			// TODO Auto-generated method stub
			mFocusListView = arg1;
			mListAdapter.notifyDataSetChanged(position);
			mOnListDataChanged.OnListDataChangedListener(mMediaControlFocusId,
					position);
		}

	};

	OnItemClickListener mOnSubItemClickListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> v, View item, int position,
				long id) {
			// TODO Auto-generated method stub
			if (v instanceof ListView) {
				mSublistAdapter.notifyDataSetChanged(position);
			} else if (v instanceof GridView) {
				item.setSelected(true);
			}
			mOnListDataChanged.OnSublistDataChangedListener(mListFoucsIndex,
					position);
		}

	};

	private View.OnClickListener mPauseListener = new View.OnClickListener() {
		public void onClick(View v) {
			setMediaControlFocusId(R.id.pause);
			doPauseResume();
			show(sDefaultTimeout);
		}
	};

	private void updatePausePlay() {
		if (mRoot == null || mPauseButton == null)
			return;

		if (mPlayer.isPlaying()) {
			mPauseButton.setImageResource(R.drawable.media_button_pause);
		} else {
			mPauseButton.setImageResource(R.drawable.media_button_play);
		}
	}

	private void doPauseResume() {
		if (mPlayer.isPlaying()) {
			mPlayer.pause();
		} else {
			mPlayer.start();
		}
		updatePausePlay();
	}

	private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		public void onStartTrackingTouch(SeekBar bar) {
			show(3600000);

			mDragging = true;
			mHandler.removeMessages(SHOW_PROGRESS);
		}

		public void onProgressChanged(SeekBar bar, int progress,
				boolean fromuser) {
			if (!fromuser) {
				// We're not interested in programmatically generated changes to
				// the progress bar's position.
				return;
			}

			long duration = mPlayer.getDuration();
			long newposition = (duration * progress) / 1000L;
			mPlayer.seekTo((int) newposition);
			if (mCurrentTime != null)
				mCurrentTime.setText(stringForTime((int) newposition));
		}

		public void onStopTrackingTouch(SeekBar bar) {
			mDragging = false;
			setProgress();
			show(sDefaultTimeout);

			mHandler.sendEmptyMessage(SHOW_PROGRESS);
		}
	};

	@Override
	public void setEnabled(boolean enabled) {
		if (mMediaControl.getVisibility() != View.GONE) {
			if (mPauseButton != null) {
				mPauseButton.setEnabled(enabled);
			}
			if (mNextButton != null) {
				mNextButton.setEnabled(enabled);
			}
			if (mPrevButton != null) {
				mPrevButton.setEnabled(enabled);
			}
			if (mSubSetButton != null) {
				mSubSetButton.setEnabled(enabled);
			}
			if (mZoomButton != null) {
				mZoomButton.setEnabled(enabled);
			}
			if (m3DButton != null) {
				m3DButton.setEnabled(enabled);
			}
			if (mRepeatButton != null) {
				if (enabled & mRepeatListener != null) {
					mRepeatButton.setVisibility(VISIBLE);
					mRepeatButton.setEnabled(true);
				} else {
					mRepeatButton.setVisibility(GONE);
				}
			}
			if (mTrackButton != null) {
				mTrackButton.setEnabled(enabled);
			}
			if (mPrevButton != null) {
				mPrevButton.setEnabled(enabled);
			}
			disableUnsupportedButtons();
		}

		super.setEnabled(enabled);
	}

	View.OnFocusChangeListener mListFocusChangeListener = new View.OnFocusChangeListener() {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			// TODO Auto-generated method stub
			if (hasFocus) {
				if (mSublistLayout.getVisibility() == VISIBLE) {
					mSublistLayout.setVisibility(GONE);
				}
				if (mGridLayout.getVisibility() == VISIBLE) {
					mGridLayout.setVisibility(GONE);
				}
				/* set listview margin left */
				int marginLeft = getListViewMarginLeft(mMediaControlFocusId);
				RelativeLayout.LayoutParams ll = (android.widget.RelativeLayout.LayoutParams) mListLayout
						.getLayoutParams();
				ll.leftMargin = marginLeft;
			}
		}

	};

	View.OnFocusChangeListener mJumpFocusChangeListener = new View.OnFocusChangeListener() {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			// TODO Auto-generated method stub
			if (hasFocus) {
				setHolding(false);
				setMediaControlFocusId(v.getId());
				mListLayout.setVisibility(GONE);
				mSublistLayout.setVisibility(GONE);
				mGridLayout.setVisibility(GONE);
			}
		}

	};

	View.OnFocusChangeListener mOnButtonFocusChangeListener = new View.OnFocusChangeListener() {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			// TODO Auto-generated method stub

			if (!hasFocus) {
				return;
			}
			/* hide or show the listview and sublistview */
			setHolding(false);
			setMediaControlFocusId(v.getId());
			mListLayout.setVisibility(GONE);
			mSublistLayout.setVisibility(GONE);
		}
	};

	View.OnClickListener mButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			v.requestFocus();
		}
	};

	public void setJumpListener(View.OnClickListener jump) {
		mJumpClickListener = jump;
		if (mRoot != null && mJumpButton != null) {
			mJumpButton.setOnClickListener(mJumpClickListener);
		}
	}

	public void setSubsetListener(View.OnFocusChangeListener set) {
		mSubSetListener = set;
		if (mRoot != null && mSubSetButton != null) {
			mSubSetButton.setOnFocusChangeListener(mSubSetListener);
		}
	}

	public void setPrevNextListeners(View.OnClickListener next,
			View.OnClickListener prev) {
		mNextListener = next;
		mPrevListener = prev;

		if (mRoot != null) {
			if (mNextButton != null) {
				mNextButton.setOnClickListener(mNextListener);
				mNextButton.setEnabled(mNextListener != null);
			}

			if (mPrevButton != null) {
				mPrevButton.setOnClickListener(mPrevListener);
				mPrevButton.setEnabled(mPrevListener != null);
			}
		}
	}

	public void setZoomListener(View.OnFocusChangeListener zoom) {
		mZoomListener = zoom;
		if (mRoot != null && mZoomButton != null) {
			mZoomButton.setOnFocusChangeListener(mZoomListener);
		}
	}

	public void set3DListener(View.OnFocusChangeListener mode3D) {
		m3DListener = mode3D;
		if (mRoot != null && m3DButton != null) {
			m3DButton.setOnFocusChangeListener(m3DListener);
		}
	}

	public void setRepeatListener(View.OnFocusChangeListener repeat) {
		mRepeatListener = repeat;
		if (mRoot != null && mRepeatButton != null) {
			mRepeatButton.setOnFocusChangeListener(mRepeatListener);
		}
	}

	public void setTrackListener(View.OnFocusChangeListener track) {
		mTrackListener = track;
		if (mRoot != null && mTrackButton != null) {
			mTrackButton.setOnFocusChangeListener(mTrackListener);
		}
	}

	public void OnMediaAudioKeyListener() {
		mOnListDataChanged.OnMediaAudioKeyClickListener();
	}

	public void OnMediaSubtitleKeyListener() {
		mOnListDataChanged.OnMediaSubtitleKeyClickListener();
	}

	public void OnMediaPrevClickListener() {
		mOnListDataChanged.OnMediaPrevKeyClickListener();
	}

	public void OnMediaNextClickListener() {
		mOnListDataChanged.OnMediaNextKeyClickListener();
	}

	public void OnMediaStopClickListener() {
		if (mShowing) {
			mPauseButton.requestFocus();
			if (mListLayout.getVisibility() == VISIBLE) {
				mListLayout.setVisibility(GONE);
				setHolding(false);
				if (mSublistLayout.getVisibility() == VISIBLE) {
					mSublistLayout.setVisibility(GONE);
				}
				if (mGridLayout.getVisibility() == VISIBLE) {
					mGridLayout.setVisibility(GONE);
				}
			}
			mHandler.removeMessages(SHOW_PROGRESS);
			mHandler.removeMessages(SEEK_PROGRESS);
			mWindowManager.removeView(mDecor);
			mShowing = false;
		}
		mOnListDataChanged.OnMediaStopKeyClickListener();
	}

	public void OnMediaRepeatClickListener() {
		mOnListDataChanged.OnMediaRepeatKeyClickListener();
	}

	/**
	 * Register a callback to be invoked when the media source is ready for
	 * playback.
	 * 
	 */
	public interface OnListDataChanged {
		void OnListDataChangedListener(int mediaControlFocusId,
				int selectedIterm);

		void OnSublistDataChangedListener(int listFoucsIndex, int selectedIterm);

		void OnMediaPrevKeyClickListener();

		void OnMediaNextKeyClickListener();

		void OnMediaStopKeyClickListener();

		void OnMediaSubtitleKeyClickListener();

		void OnMediaAudioKeyClickListener();

		void OnMediaRepeatKeyClickListener();
	}

	private OnListDataChanged mOnListDataChanged;

	public void setOnListDataChanged(OnListDataChanged onListDataChanged) {
		mOnListDataChanged = onListDataChanged;
	}

	public interface MediaPlayerControl {
		void start();

		void pause();

		int getDuration();

		int getCurrentPosition();

		void seekTo(int pos);

		boolean isPlaying();

		int getBufferPercentage();

		boolean canPause();

		boolean canSeekBackward();

		boolean canSeekForward();
	}
}

class AlwaysMarqueeTextView extends TextView {

	public AlwaysMarqueeTextView(Context context) {
		super(context);
	}

	public AlwaysMarqueeTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AlwaysMarqueeTextView(Context context, AttributeSet attrs,
			int defStyle) {

		super(context, attrs, defStyle);
	}

	@Override
	public boolean isFocused() {
		return true;
	}
}