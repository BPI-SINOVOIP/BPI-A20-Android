/**
 * you can find the path of sdcard,flash and usbhost in here
 * @author chenjd
 * @email chenjd@allwinnertech.com
 * @data 2011-8-10
 */
package com.softwinner.TvdFileManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Set;

import jcifs.util.LocalNetwork;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;
import android.os.storage.IMountService;
import android.os.ServiceManager;
/**
 * manager of flash,sdcard,usbhost
 * @author chenjd
 *
 */
public class DeviceManager{
	
	private static String TAG = "DeviceManager";
	
	private ArrayList<String> localDevicesList;
	private ArrayList<String> sdDevicesList;
	private ArrayList<String> usbDevicesList;
	private ArrayList<String> sataDevicesList;
	private ArrayList<String> internalDevicesList;
	private ArrayList<String> mountedDevicesList;
	private Context mContext;
	private StorageManager manager;

	public DeviceManager(Context mContext)
	{
		this.mContext = mContext;
		/* ��ȡ���豸�б� */
		localDevicesList = new ArrayList<String>();
		String[] volumeList;
		manager = (StorageManager)mContext.getSystemService(Context.STORAGE_SERVICE);
		volumeList = manager.getVolumePaths();
		for(int i = 0; i < volumeList.length; i ++)
		{
			localDevicesList.add(volumeList[i]);
		}
        
        /* ��ȡ�ڲ��洢�豸·���б� */
        internalDevicesList = new ArrayList<String>();
        internalDevicesList.add(Environment.getExternalStorageDirectory().getPath());
        
        sdDevicesList = new ArrayList<String>();
        usbDevicesList = new ArrayList<String>();
        sataDevicesList = new ArrayList<String>();
        String path;
        for(int i = 0; i < localDevicesList.size(); i++)
        {
        	path = localDevicesList.get(i);
        	if(!path.equals(Environment.getExternalStorageDirectory().getPath()))
        	{
        		if(path.contains("sd"))
        		{
        			/* ��ȡSD���豸·���б� */
        			sdDevicesList.add(path);
        		}
        		else if(path.contains("usb"))
        		{
        			/* ��ȡUSB�豸·���б� */
        			usbDevicesList.add(path);
        		}
        		else if(path.contains("sata"))
        		{
        			/* ��ȡsata�豸·���б� */
        			sataDevicesList.add(path);
        		}
        	}
        }
	}
	public boolean isLocalDevicesRootPath(String path)
	{
		for(int i = 0; i < localDevicesList.size(); i++)
		{
			if(path.equals(localDevicesList.get(i)))
				return true;
		}
		return false;
	}
	
	/**
	 * ��ȡ���豸���б�
	 * @return
	 */
	public ArrayList<String> getLocalDevicesList()
	{
		return (ArrayList<String>) localDevicesList.clone();
	}
	
	/**
	 * ��ȡ��ǰ�����ص��豸�б�
	 */
	public ArrayList<String> getMountedDevicesList()
	{
		String state;
		ArrayList<String> mountedDevices = new ArrayList<String>();
		try 
		{
	        for(int i = 0; i < localDevicesList.size(); i++)
	        {
	            state = manager.getVolumeState(localDevicesList.get(i));
	           	if(state.equals(Environment.MEDIA_MOUNTED))
	           	{
	           		mountedDevices.add(localDevicesList.get(i));
	           	}
	        }
	    } catch (Exception rex) 
	    {
	    }
	    return mountedDevices;
	}
	
	public boolean isInterStoragePath(String path)
	{
		if(internalDevicesList.contains(path))
		{
			return true;
		}
		return false;
	}
	
	public boolean isSdStoragePath(String path)
	{
		if(sdDevicesList.contains(path))
		{
			return true;
		}
		return false;
	}
	
	public boolean isUsbStoragePath(String path)
	{
		if(usbDevicesList.contains(path))
		{
			return true;
		}
		return false;
	}
	
	public boolean isSataStoragePath(String path)
	{
		if(sataDevicesList.contains(path))
		{
			return true;
		}
		return false;
	}
	
	public ArrayList<String> getSdDevicesList()
	{
		return (ArrayList<String>) sdDevicesList.clone();
	}
	
	public ArrayList<String> getUsbDevicesList()
	{
		return (ArrayList<String>) usbDevicesList.clone();
	}
	
	public ArrayList<String> getInternalDevicesList()
	{
		return (ArrayList<String>) internalDevicesList.clone();
	}
	
	public ArrayList<String> getSataDevicesList()
	{
		return (ArrayList<String>) sataDevicesList.clone();
	}
	
	public boolean hasMultiplePartition(String dPath)
	{
		try
		{
			File file = new File(dPath);
			String minor = null;
			String major = null;
			for(int i = 0; i < localDevicesList.size(); i++)
			{
				if(dPath.equals(localDevicesList.get(i)))
				{
					String[] list = file.list();
					/********
					 *add by hechuanlong 2013-08-20  start{{----------------------------
					 *fix a bug when list.length==0 the device be a multiple partition
					 ********/
					 if(0 == list.length)
					 {
					 		return false;
					 }
					/********
					 *------------------------}}end
					 *********/
					for(int j = 0; j < list.length; j++)
					{
						/* ���Ŀ¼������������"���豸��:���豸��"(��ǰ��������������),�򷵻�false */
						int lst = list[j].lastIndexOf("_");
						if(lst != -1 && lst != (list[j].length() -1))
						{
							major = list[j].substring(0, lst);
							minor = list[j].substring(lst + 1, list[j].length());
							try
							{
							
								Integer.valueOf(major);
								Integer.valueOf(minor);
							}
							catch(NumberFormatException e)
							{
								/* ������ַ������ܱ�����Ϊ����,���˳� */
								return false;
							}
						}
						else 
						{
							return false;
						}
					}
					return true;
				}
			}
			return false;
		}
		catch(Exception e)
		{
			Log.e(TAG, "hasMultiplePartition() exception e");
			return false;
		}
	}
	
	
	public ArrayList<String> getNetDeviceList(){
		SharedPreferences pref = mContext.getSharedPreferences("Device", 0);
		String list = pref.getString("Net", null);
		if(list != null){
			String[] split = list.split(",");
			if(split != null){
				ArrayList<String> devList = new ArrayList<String>();
				for(int i = 0; i < split.length; i++){
					devList.add(split[i]);
				}
				return devList;
			}
		}
		return null;
	}
	
	public void saveNetDevice(String devPath){
		if(devPath == null){
			return;
		}
		SharedPreferences pref = mContext.getSharedPreferences("Device", 0);
		SharedPreferences.Editor editor = pref.edit();
		String list = pref.getString("Net", null);
		if(list == null){
			editor.putString("Net", devPath);
		}else{
			list = list + "," + devPath;
			editor.putString("Net", list);
		}
		editor.commit();
	}
	
	public void delNetDevice(String devPath){
		if(devPath == null){
			return;
		}
		ArrayList<String> list = getNetDeviceList();
		list.remove(devPath);
		String st = null;
		for(int i = 0; i < list.size(); i++){
			if(st == null){
				st = list.get(i);
			}else{
				st = st + "," + list.get(i);
			}
		}
		SharedPreferences pref = mContext.getSharedPreferences("Device", 0);
		SharedPreferences.Editor editor = pref.edit();
		editor.putString("Net", st);
		editor.commit();
	}
	
	public boolean isNetStoragePath(String path){
		if (path.startsWith("nfs|") || path.startsWith("smb|")) {
			Log.d(TAG, path + " is net storage");
			return true;
		}
		if(LocalNetwork.getIpType(path) != LocalNetwork.IP_TYPE_UNKNOWN){
			return true;
		}
		return false;
	}
}


