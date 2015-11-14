package com.softwinner.TvdFileManager.net;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

import org.apache.http.conn.util.InetAddressUtils;

import jcifs.netbios.NbtAddress;
import jcifs.smb.NtStatus;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import java.net.UnknownHostException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;
import jcifs.smb.WinError;
import jcifs.util.LocalNetwork;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.NetworkInfo.DetailedState;
import android.net.ethernet.EthernetDevInfo;
import android.net.ethernet.EthernetManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.softwinner.SystemMix;

import com.softwinner.TvdFileManager.R;
import com.softwinner.TvdFileManager.R.drawable;
import com.softwinner.TvdFileManager.R.id;
import com.softwinner.TvdFileManager.R.layout;
import com.softwinner.TvdFileManager.R.string;
import com.softwinner.TvdFileManager.R.style;

public class SambaManager
{
	private Context mContext;
	private ArrayList<SmbFile> mWorkgroupList = null;
	private ArrayList<SmbFile> mServiceList = null;
	private ArrayList<SmbFile> mShareList = null;
	private ArrayList<String> mMountedPointList = null;
	private HashMap<String, String> mMap = null;

	private SmbFile[] mSmbList = null;
	private SmbLoginDB mLoginDB = null;
	public static File mountRoot = null;
	private ProgressDialog pr_dialog;

	private static final boolean V_TAG = true;
	private final static String TAG = "SambaManager";

	public static HashMap<Integer, String> badAddress = new HashMap<Integer, String> ();
	public static int index = 0;

	public SambaManager(Context context)
	{
		mContext = context;
		mWorkgroupList = new ArrayList<SmbFile>();
		mServiceList = new ArrayList<SmbFile>();
		mShareList = new ArrayList<SmbFile>();
		mMountedPointList = new ArrayList<String>();
		mMap = new HashMap<String, String>();
		mLoginDB = new SmbLoginDB(context);

		mountRoot = mContext.getDir("share", 0);
		index = 0;
		badAddress.clear();
		initSambaProp();
	}

	private BroadcastReceiver mNetStatusReveicer;
	private void initSambaProp(){
		jcifs.Config.setProperty("jcifs.encoding", "GBK");
		jcifs.Config.setProperty("jcifs.util.loglevel", "0");
		LocalNetwork.setCurIpv4("172.16.10.66");
		//start to obser network state

		mNetStatusReveicer = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				NetworkInfo info;
				LinkProperties link;
				Log.d("Samba", "Networks action:" + action);
				if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
					info = (NetworkInfo)intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
					link = (LinkProperties)intent.getParcelableExtra(WifiManager.EXTRA_LINK_PROPERTIES);
					if(info.getDetailedState() == DetailedState.CONNECTED){
						for(LinkAddress l:link.getLinkAddresses()){
							LocalNetwork.setCurIpv4(l.getAddress().getHostAddress());
							return;
						}
					}
				}else if(action.equals(EthernetManager.NETWORK_STATE_CHANGED_ACTION)){
					EthernetManager ethMng = EthernetManager.getInstance();
					ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
					boolean ethConnected = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET).isConnected();
					info = (NetworkInfo)intent.getParcelableExtra(EthernetManager.EXTRA_NETWORK_INFO);
					link = (LinkProperties)intent.getParcelableExtra(EthernetManager.EXTRA_LINK_PROPERTIES);
					int event = intent.getIntExtra(EthernetManager.EXTRA_ETHERNET_STATE, EthernetManager.EVENT_CONFIGURATION_SUCCEEDED);
					if(event == EthernetManager.EVENT_CONFIGURATION_SUCCEEDED && ethConnected){
						for(LinkAddress l:link.getLinkAddresses()){
							LocalNetwork.setCurIpv4(l.getAddress().getHostAddress());
							return;
						}

					}
				}
			}
		};
		IntentFilter filter = new IntentFilter();
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		filter.addAction(EthernetManager.NETWORK_STATE_CHANGED_ACTION);
		mContext.registerReceiver(mNetStatusReveicer, filter);

	}

	/* Search neighborhood, return results by callback */
	public void startSearch(final String smbUrl, final OnSearchListener ls)
	{
		Log.d(TAG, "search smb:" + smbUrl);

		/* Search wait dialog */
		pr_dialog = showProgressDialog(R.drawable.icon, mContext.getResources().getString(R.string.search),
				null, ProgressDialog.STYLE_SPINNER, true);
		pr_dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				ls.onFinish(true);
			}
		});
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				SmbFile smbFile = null;
				try{
					if(smbUrl.equals("smb://"))
					{
						/* Search all workgroup */
						smbFile = new SmbFile("smb://");

					}
					else if(isSambaWorkgroup(smbUrl))
					{
						/* Search all server */
						smbFile = getSambaWorkgroup(smbUrl);
					}
					else if(isSambaServices(smbUrl))
					{
						/* Search all shared folder */
						smbFile = getSambaService(smbUrl);
					}
					else if(isSambaShare(smbUrl))
					{
						/* mount shared folder */
						smbFile = getSambaShare(smbUrl);
					}else{
						smbFile = new SmbFile(smbUrl);
					}
					boolean ret = startLogin(smbFile, ls);

					final SmbFile f = smbFile;
					pr_dialog.cancel();
					if(!ret)
					{
						((Activity) mContext).runOnUiThread(new Runnable() {
							@Override
							public void run() {
								createLoginDialog(f, ls);
							}
						});

					}
				}catch(MalformedURLException e){
					mSmbList = null;
					pr_dialog.cancel();
					Log.d(TAG, e.getMessage());
					e.printStackTrace();
				}
			}
		});
		thread.start();
	}

	private ProgressDialog showProgressDialog(int icon, String title, String message, int style, final boolean cancelable)
	{
		ProgressDialog pr_dialog = null;
		pr_dialog = new ProgressDialog(mContext);
		pr_dialog.setProgressStyle(style);
		pr_dialog.setIcon(icon);
		pr_dialog.setTitle(title);
		pr_dialog.setIndeterminate(false);
		pr_dialog.setCancelable(cancelable);
		if(message != null)
		{
			pr_dialog.setMessage(message);
		}
		pr_dialog.show();
		pr_dialog.getWindow().setLayout(600, 300);
		return pr_dialog;
	}
	private final static int TRY_TIME = 5;
	SmbFile[] smbFiles = new SmbFile[TRY_TIME];
	private int netStatus = NtStatus.NT_STATUS_UNSUCCESSFUL;

	private SmbFile[] listFiles(final SmbFile smbFile, final ArrayList<SmbFile> list, final OnSearchListener ls) {
		SmbFile[] smbList;

		SmbFileFilter filter = new SmbFileFilter() {
			@Override
			public boolean accept(SmbFile file) throws SmbException {
				if(pr_dialog.isShowing())
				{
					if(!file.getPath().endsWith("$") && !file.getPath().endsWith("$/")){
						list.add(file);
						Log.d(TAG, "child:" + file.getPath());
						Log.d(TAG, file.getURL().getHost().toString());
						ls.onReceiver(file.getPath());
						return true;
					}
				}
				return false;
			}
		};
		for(int i = 0; i < TRY_TIME; i++) {
		try {
			smbFiles[i] = new SmbFile("smb:////".equals(smbFile.getPath())? "smb://" : smbFile.getPath());
			if(filter != null)
				smbList = smbFiles[i].listFiles(filter);
			else
				smbList = smbFiles[i].listFiles();
			/**
			 * When got the file list, close the session.
 			 * shanxiaoxi@allwinnertech.com
			 */
			String myAddress = smbFiles[i].getMyAddress();
			Log.d(TAG, "Disconnect in general");
			//samba.doDisconnect();
			if(smbList != null){
				netStatus = NtStatus.NT_STATUS_OK;
			}else{
				netStatus = NtStatus.NT_STATUS_UNSUCCESSFUL;
			}
			return smbList;
//    			break;
		} catch (SmbException e) {
			netStatus = e.getNtStatus();
			e.printStackTrace();
			Log.d(TAG, "Disconnect in excpetion");
			try {
			    badAddress.put(index++, smbFiles[i].getMyAddress());
			 } catch (UnknownHostException e1) {
			   netStatus = NtStatus.NT_STATUS_UNSUCCESSFUL;
			   return null;

			}
//    		  samba.doDisconnect();
			continue;

		} catch (UnknownHostException e1) {
			   netStatus = NtStatus.NT_STATUS_UNSUCCESSFUL;
			   return null;
		}catch(MalformedURLException e){
		    break;
		}
		}
		return null;
	}

	private int addFileList(SmbFile samba, final ArrayList<SmbFile> list, final OnSearchListener ls)
	{
		Log.d(TAG, "start search " + samba.getPath());

		SmbFile[] smbList = listFiles(samba, list, ls);

		Log.d(TAG, String.format("search end, return %x, means net status %s, win status %s", netStatus,
				statusToStr(netStatus), SmbException.getMessageByWinerrCode(netStatus)));
		return netStatus;
	}

	private String statusToStr(int statusCode){
		for(int i = 0; i < NtStatus.NT_STATUS_CODES.length; i++){
			if(statusCode == NtStatus.NT_STATUS_CODES[i]){
				return NtStatus.NT_STATUS_MESSAGES[i];
			}
		}
		return null;
	}

	private boolean login(SmbFile samba,final ArrayList<SmbFile> list, final OnSearchListener ls)
	{
		int ret = addFileList(samba, list, ls);
		switch(ret)
		{
		case NtStatus.NT_STATUS_OK:
			return true;
		case NtStatus.NT_STATUS_ACCESS_DENIED:
		case WinError.ERROR_ACCESS_DENIED:
			NtlmPasswordAuthentication ntlm = getLoginDataFromDB(samba);
			String path = samba.getPath();
			if(ntlm != null)
			{
				try {
					samba = new SmbFile(path, ntlm);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					Log.e(TAG, e.getMessage());
					e.printStackTrace();
					return false;
				}
			}
			Log.d("Samba", String.format("ntlm domain=%s, usr=%s, psw=%s", ntlm.getDomain(),
					ntlm.getUsername(), ntlm.getPassword()));
			int r = addFileList(samba, list, ls);
			if(r == NtStatus.NT_STATUS_OK)
				return true;
			else
			{
				mLoginDB.delete(path);
				return false;
			}
		default:
			Log.e(TAG, String.format("other error code: %x",ret));
			if(!ls.onFinish(false)){
				showMessage(R.string.access_fail);
			}
		}
		return true;
	}

	private void showMessage(final int resId)
	{
		((Activity)mContext).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try{
					Toast.makeText(mContext, resId, Toast.LENGTH_SHORT).show();
				}catch (Exception e) {
				}
			}
		});
	}

	/**
	 *
	 * @param samba
	 * @param ls
	 * @return true: login success
	 * 		   false: login failed
	 */
	public boolean startLogin(SmbFile samba,final OnSearchListener ls)
	{
		NtlmPasswordAuthentication ntlm = null;
		//fix me:when create samba with url 'smb://', the value of samba.getPath() is 'smb:////'
		if("smb:////".equals(samba.getPath())){
			// first clear all cached info
			mServiceList.clear();
			mMountedPointList.clear();
			mMap.clear();
			mWorkgroupList.clear();
			return login(samba, mWorkgroupList, ls);
		}
		int type;
		try {
			type = samba.getType();
		} catch (SmbException e1) {
			// TODO Auto-generated catch block
			Log.e(TAG, e1.getMessage());
			e1.printStackTrace();
			return false;
		}
		Log.d(TAG, "getType " + type);
		try{
			switch(type)
			{
			case SmbFile.TYPE_WORKGROUP:
				mServiceList.clear();
				return login(samba, mServiceList, ls);
			case SmbFile.TYPE_SERVER:
				mShareList.clear();
				return login(samba, mShareList, ls);
			case SmbFile.TYPE_SHARE:
				/* If have logged in, go into mounted point folder directly */
				String mountPoint = getSambaMountedPoint(samba.getPath());
				if(mountPoint != null)
				{
					ls.onReceiver(mountPoint);
					return true;
				}
				/* if have not logged in, first try login in without username and password. */
				String serverName = samba.getServer();
				Log.d(TAG, "server name of " + samba.getPath() + " is " + serverName);
				NbtAddress addr = NbtAddress.getByName(serverName);
				Log.d(TAG, "nbt address is " + addr.getHostAddress());
				mountPoint = createNewMountedPoint(samba.getPath().substring(0, samba.getPath().length() - 1));
				umountSmb(mountPoint);

				int ret = mountSmb(samba.getPath(), mountPoint, "", "", addr.getHostAddress());
				if(ret == 0)
				{
					mMountedPointList.add(mountPoint);
					mMap.put(samba.getPath(), mountPoint);
					ls.onReceiver(mountPoint);
					return true;
				}
				/* If logged in without password and username failed, then get login information
				 * from database, try again.
				 */
				else
				{
					String path = samba.getPath();
					ntlm = getLoginDataFromDB(samba);
					if(ntlm != null)
					{
						samba = new SmbFile(path, ntlm);
					}else{
						String pPath = samba.getParent();
						SmbFile parent;
						parent = new SmbFile(pPath, (NtlmPasswordAuthentication)samba.getPrincipal());
						ntlm = getLoginDataFromDB(parent);
						if(ntlm != null){
							//or use username and password of its's server
							samba = new SmbFile(path, ntlm);
						}else{
							return false;
						}
					}

					ret = mountSmb(samba.getPath(), mountPoint, ntlm.getUsername(), ntlm.getPassword(), addr.getHostAddress());
					if(ret == 0){
						mMountedPointList.add(mountPoint);
						mMap.put(samba.getPath(), mountPoint);
						ls.onReceiver(mountPoint);
						return true;
					}
					/*
					 * If failed again, delete login information from database,
					 * and return false
					 */
					else {
						mLoginDB.delete(path);
						return false;
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return false;
	}

	public NtlmPasswordAuthentication getLoginDataFromDB(SmbFile file)
	{
		if(file == null)
			return null;
		final int SMB_PATH_COLUME = 0;
		final int DOMAIN_COLUME = 1;
		final int USERNAME_COLUME = 2;
		final int PASSWORD_COLUME = 3;
		String[] columns = null;
		String selection = SmbLoginDB.SMB_PATH + "=?";
		String selectionArgs[] = {file.getPath()};
		String domain = null;
		String username = null;
		String password = null;
		NtlmPasswordAuthentication ntlm = null;
		Cursor cr = mLoginDB.query(columns, selection, selectionArgs, null);
		if(cr != null)
		{
			try
			{
				while (cr.moveToNext()) {
					domain = cr.getString(DOMAIN_COLUME);
					Log.d(TAG,"------------------get ntlm ------------------");
					Log.d(TAG,"fileDom  " + ((NtlmPasswordAuthentication)file.getPrincipal()).getDomain());
					Log.d(TAG,"path     " + cr.getString(SMB_PATH_COLUME));
					Log.d(TAG,"domain   " + domain);
					Log.d(TAG,"username " + cr.getString(USERNAME_COLUME));
					Log.d(TAG,"password " + cr.getString(PASSWORD_COLUME));
					Log.d(TAG,"------------------end ntlm ------------------");
					if(domain != null && domain.equals(((NtlmPasswordAuthentication)file.getPrincipal()).getDomain()));
					{
						username = cr.getString(USERNAME_COLUME);
						password = cr.getString(PASSWORD_COLUME);
						ntlm = new NtlmPasswordAuthentication(domain, username, password);
						break;
					}
				}
			}finally
			{
				cr.close();
				cr = null;
			}
		}
		return ntlm;
	}

	private void createLoginDialog(final SmbFile samba, final OnSearchListener ls)
	{
		final Dialog dg = new Dialog(mContext, R.style.menu_dialog);
		dg.setCancelable(true);
		LayoutInflater infrater = LayoutInflater.from(mContext);
		View v = infrater.inflate(R.layout.login_dialog, null);
		dg.setContentView(v);
		final EditText account = (EditText) v.findViewById(R.id.account);
		final EditText password = (EditText) v.findViewById(R.id.password);
		Button ok = (Button) v.findViewById(R.id.login_ok);
		ok.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				dg.dismiss();
				/* Starting login */
				final ProgressDialog pdg = showProgressDialog(R.drawable.icon, mContext.getResources().getString(R.string.login),
						null, ProgressDialog.STYLE_SPINNER, true);
				Thread thread = new Thread(new Runnable() {

					@Override
					public void run() {
						try{
							// TODO Auto-generated method stub
							String inputStr = account.getEditableText().toString();
							String domain = null;
							String user = null;
							int i = inputStr.indexOf("\\");
							if(i > 0){
								domain = inputStr.substring(0, i);
								user = inputStr.substring(i + 1, inputStr.length());
							}else{
								domain = ((NtlmPasswordAuthentication)samba.getPrincipal()).getDomain();
								user = inputStr;
							}
							NtlmPasswordAuthentication ntlm = new NtlmPasswordAuthentication(domain,
									user, password.getEditableText().toString());
							SmbFile smbfile;
							Log.d(TAG, String.format("create dialog: ntlm domain=%s, usr=%s, psw=%s", ntlm.getDomain(),
									ntlm.getUsername(), ntlm.getPassword()));
							smbfile = new SmbFile(samba.getPath(), ntlm);
							int type = samba.getType();
							switch (type)
							{
							/**
							 * if the type is Samba Server, then list all shared folders
							 */
							case SmbFile.TYPE_SERVER:
								SmbFile[] shareList = smbfile.listFiles();
								if(shareList == null)
								{
									showMessage(R.string.login_fail);
								}
								else
								{
									addLoginMessage(ntlm, smbfile.getPath());
									mShareList.clear();
									for(SmbFile file:shareList)
									{
										if(!file.getPath().endsWith("$")){
											mShareList.add(file);
											ls.onReceiver(file.getPath());
										}
									}
									ls.onFinish(true);
								}
								break;
							/**
							 * If the type is Samba shared folder, then mount it and go into the directory
							 */
							case SmbFile.TYPE_SHARE:
								//for test
								String smbPath = samba.getPath();
								smbPath = smbPath.substring(0, smbPath.length() - 1);
								String mountedPoint = createNewMountedPoint(smbPath);
								String serverName = samba.getServer();
								Log.d(TAG, "server name of " + samba.getPath() + " is " + serverName);
								NbtAddress addr = NbtAddress.getByName(serverName);
								Log.d(TAG, "nbt address is " + addr.getHostAddress());
								int success = mountSmb(smbPath, mountedPoint, ntlm.getUsername(), ntlm.getPassword(), addr.getHostAddress());
								/* Mount success,list all sub file  */
								if(success == 0)
								{
									addLoginMessage(ntlm, smbPath);
									mMountedPointList.add(mountedPoint);
									mMap.put(samba.getPath(), mountedPoint);
									ls.onReceiver(mountedPoint);
									ls.onFinish(true);
								}
								else
								{
									showMessage(R.string.login_fail);
								}
								break;
							}
							pdg.dismiss();
						}catch (Exception e) {
							e.printStackTrace();
							pdg.dismiss();
						}
					}

				});
				thread.start();
			}
		});
		Button cancel = (Button) v.findViewById(R.id.login_cancel);
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				dg.dismiss();
			}
		});

		Window dialogWindow = dg.getWindow();
		WindowManager.LayoutParams lp = dialogWindow.getAttributes();
		lp.width = 300;
		dialogWindow.setAttributes(lp);
		dg.show();
	}

	/**
	 * Add login information to database
	 * @param ntlm
	 * @param smbPath
	 */
	public void addLoginMessage(NtlmPasswordAuthentication ntlm, String smbPath)
	{
		Log.d(TAG, "-------------add login message------------");
		Log.d(TAG, "path     " + smbPath + (smbPath.endsWith("/")? "" : "/"));
		Log.d(TAG, "domain   " + ntlm.getDomain());
		Log.d(TAG, "username " + ntlm.getUsername());
		Log.d(TAG, "password " + ntlm.getPassword());
		Log.d(TAG, "-------------end adding-------------------");
		mLoginDB.insert(smbPath + (smbPath.endsWith("/")? "" : "/"), ntlm.getDomain(),
				ntlm.getUsername(), ntlm.getPassword());
	}

	/**
	 * Umount all mounted point
	 */
	public void clear()
	{
		for(String mountPoint:mMountedPointList)
		{
			umountSmb(mountPoint);
		}
		mMountedPointList.clear();
		mLoginDB.closeDB();
		mContext.unregisterReceiver(mNetStatusReveicer);
	}

	public boolean isSambaServices(String path)
	{
		String[] attrs = path.split("\\|");
		// if path format is "smb|192.168.1.105"
		if (attrs.length == 2 && attrs[0].equals("smb")) {
			path = attrs[1];
			SmbFile file = null;
			boolean found = false;
			try {
				if(LocalNetwork.getIpType(path) != LocalNetwork.IP_TYPE_UNKNOWN){
					path = "smb://" + path + "/";
				}
				for(int i = 0; i < mServiceList.size(); i++)
				{
					if(mServiceList.get(i).getPath().equals(path))
					{
						found = true;
						return true;
					}
				}
				if (!found) {
					file = new SmbFile(path);
					mServiceList.add(file);
					return true;
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(LocalNetwork.getIpType(path) != LocalNetwork.IP_TYPE_UNKNOWN){
			path = "smb://" + path + "/";
		}
		for(int i = 0; i < mServiceList.size(); i++)
		{
			if(mServiceList.get(i).getPath().equals(path))
			{
				return true;
			}
		}
		return false;
	}

	public boolean isSambaShare(String path)
	{
		for(int i = 0; i < mShareList.size(); i++)
		{
			if(mShareList.get(i).getPath().equals(path))
			{
				return true;
			}
		}
		return false;
	}

	public boolean isSambaWorkgroup(String path)
	{
		for(int i = 0; i < mWorkgroupList.size(); i++)
		{
			if(mWorkgroupList.get(i).getPath().equals(path))
			{
				return true;
			}
		}
		return false;
	}

	public SmbFile getSambaWorkgroup(String sambaFile)
	{
		for(int i = 0; i < mWorkgroupList.size(); i++)
		{
			SmbFile file = mWorkgroupList.get(i);
			if(file.getPath().equals(sambaFile))
			{
				return file;
			}
		}
		return null;
	}

	public SmbFile getSambaService(String sambaFile)
	{
		String[] attrs = sambaFile.split("\\|");
		if (attrs.length == 2 && attrs[0].equals("smb")) {
			sambaFile = attrs[1];
		}
		if(LocalNetwork.getIpType(sambaFile) != LocalNetwork.IP_TYPE_UNKNOWN){
			sambaFile = "smb://" + sambaFile + "/";
		}
		for(int i = 0; i < mServiceList.size(); i++)
		{
			SmbFile file = mServiceList.get(i);
			if(file.getPath().equals(sambaFile))
			{
				return file;
			}
		}
		return null;
	}

	public SmbFile getSambaShare(String sambaFile)
	{
		for(int i = 0; i < mShareList.size(); i++)
		{
			SmbFile file = mShareList.get(i);
			if(file.getPath().equals(sambaFile))
			{
				return file;
			}
		}
		return null;
	}
	public ArrayList<SmbFile> getAllSmbWorkgroup()
	{
		return (ArrayList<SmbFile>) mWorkgroupList.clone();
	}

	public ArrayList<SmbFile> getAllSmbServices()
	{
		return (ArrayList<SmbFile>) mServiceList.clone();
	}

	/**
	 * Get current all shared folder
	 * @return
	 */
	public ArrayList<SmbFile> getAllSmbShared()
	{
		return (ArrayList<SmbFile>) mShareList.clone();
	}

	/**
	 * Create smb mounted point on local disk
	 * @param path
	 * @return
	 */
	public static String createNewMountedPoint(String path)
	{
		String mountedPoint = null;

		/* for test */
		path = path.replaceFirst("smb://", "smb_");
		path = path.replace("/", "_");
		mountedPoint = path;
		File file = new File(mountRoot,mountedPoint);
		Log.d(TAG, "mounted create:  " + file.getPath());
		if(!file.exists())
		{
			try {
				file.mkdir();
			} catch (Exception e) {
				Log.e(TAG, "create " + mountedPoint + " fail");
				e.printStackTrace();
			}
		}
		return file.getAbsolutePath();
	}

	private String getSambaMountedPoint(String samba)
	{
		String mountedPoint = null;
		mountedPoint = mMap.get(samba);
		return mountedPoint;
	}

	public String getSmbPathFromMountedPoint(String mountPoint) {
		Iterator iterator = mMap.keySet().iterator();
		while(iterator.hasNext()) {
			Object key = iterator.next();
			if(mMap.get(key).equals(mountPoint)) {
				return (String)key;
			}
		}
		return null;
	}

	public boolean isSambaMountedPoint(String mountedPoint)
	{
		Log.d(TAG,"mountedPoint   " + mountedPoint);
		if(mMountedPointList.size() == 0)
		{
			Log.d(TAG,"list is 0");
		}
		for(String item:mMountedPointList)
		{
			Log.d(TAG,"list.....  " + item);
			if(item.equals(mountedPoint))
			{
				return true;
			}
		}
		return false;
	}

	public String getSambaMountedPointFromPath(String path)
	{
		if(mMountedPointList.size() == 0)
		{
			Log.d(TAG,"list is 0");
		}
		for(String item:mMountedPointList)
		{
			Log.d(TAG,"list.....  " + item);
			if(path.startsWith(item))
			{
				return item;
			}
		}
		return null;
	}
	public interface OnLoginFinishListenner
	{
		void onLoginFinish(String mountedPoint);
	}

	/**
	 * Mount smb shared folder on local disk
	 * @param source
	 * @param target
	 * @param username
	 * @param password
	 * @param ip
	 * @return
	 */
	private int mountSmb(String source, String target, String username, String password, String ip){
		if(source.endsWith("/")){
            source = source.substring(0, source.length() - 1);
		}
		int begin = source.lastIndexOf("smb:") + "smb:".length();
		String src = source.substring(begin, source.length());
		String mountPoint = target;
		String fs = "cifs";
		int flag = 64;
		String sharedName = src.substring(src.lastIndexOf("/") + 1, src.length());
		String server = src.substring(0, src.lastIndexOf("/"));
		String unc = String.format("%s\\%s", server, sharedName);
		String ver = "1";

		String options = String.format("unc=%s,ver=%s,user=%s,pass=%s,ip=%s,iocharset=%s",
				unc, ver, username, password, ip, "utf8");
		int ret = SystemMix.Mount(src, mountPoint, fs, flag, options);
		Log.d(TAG, "------------------------");
		Log.d(TAG, "src            " + src);
		Log.d(TAG, "mountPoint     " + mountPoint);
		Log.d(TAG, "fs             " + fs);
		Log.d(TAG, "flag           " + flag);
		Log.d(TAG, "options        " + options);
		Log.d(TAG, "ret            " + ret);
		Log.d(TAG, "------------------------");
		return ret;
	}

	/**
	 * Umount smb shared folder from local disk
	 * @param target
	 * @return
	 */
	public int umountSmb(String target){
		int ret = SystemMix.Umount(target);
		if (ret == 0) {
			for (int i = 0; i < mMountedPointList.size(); i++) {
				if (mMountedPointList.get(i).equals(target)) {
					mMap.clear();
					mMountedPointList.remove(i);
					break;
				}
			}
		}
		Log.d(TAG, "Umount:" + target + " ret:" + ret);
		return ret;
	}
	public static boolean inSmbMountDir(String pathName) {
	    return pathName.startsWith(mountRoot.getAbsolutePath() + "/smb");
	}
}
