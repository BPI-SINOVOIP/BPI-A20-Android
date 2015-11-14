package com.softwinner.TvdFileManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.content.Context;
import android.util.Log;
import jcifs.util.SmbFileOperate;
import com.softwinner.TvdFileManager.net.SambaManager;

public class FileOperate {
	private static final int BUFFER = 2048;
	private static final String TAG = "FileOperate";
	private long copySize   = 0;
	private long copyNum    = 0;
	private long deleteSize = 0;
	private long deleteNum  = 0;
	private long fileSize   = 0;
	private long fileNum    = 0;
	private boolean isCancel = false;
	private Context mContext;
	private SmbFileOperate mSmbFileOperate;

	public FileOperate() {
	}

	public FileOperate(Context context)
	{
		mContext = context;
	}
	
	public FileOperate(Context context, SmbFileOperate smbFileOperate) {
		mContext = context;
		mSmbFileOperate = smbFileOperate;
	}

	/**
	 * 
	 * @param old        the file to be copied
	 * @param newDir     the directory to move the file to
	 * @return           0:success    -1:fail
	 */
	public int copyToDirectory(String old, String newDir) {
		boolean oldInSambaDir = SambaManager.inSmbMountDir(old);
		boolean newInSambaDir = SambaManager.inSmbMountDir(newDir);
		if (isCancel) {
			return 0;
		}
		copyNum ++;
		File old_file = new File(old);
		File temp_dir = new File(newDir);
		byte[] data   = new byte[BUFFER];
		int read = 0;
		
		if (old_file.isFile() && temp_dir.isDirectory()
		        && ( newInSambaDir || temp_dir.canWrite())) {
			String file_name = old.substring(old.lastIndexOf("/"), old.length());
			String new_name  = newDir + file_name;
			File cp_file = new File(new_name);
			try {
				BufferedOutputStream o_stream = null;
				BufferedInputStream i_stream = null;
				if(!newInSambaDir) {
					o_stream = new BufferedOutputStream(new FileOutputStream(cp_file));
				} else {
					o_stream = new BufferedOutputStream(mSmbFileOperate.getOutputStream(cp_file.getAbsolutePath()));
				}

				if(!oldInSambaDir) {
				    i_stream = new BufferedInputStream(new FileInputStream(old_file));
				} else {
				    i_stream = new BufferedInputStream(mSmbFileOperate.getInputStream(old_file.getAbsolutePath()));
				}
				while ((read = i_stream.read(data, 0, BUFFER)) != -1) {
					if (isCancel) {
						return 0;
					}
					copySize += read;
					o_stream.write(data, 0, read);
				}
				o_stream.flush();
				i_stream.close();
				o_stream.close();
				
				RefreshMedia mRefresh = new RefreshMedia(mContext);
				mRefresh.notifyMediaAdd(new_name);
			}
			catch(FileNotFoundException e)
			{
				Log.d(TAG, e.getMessage());
				return -1;
			}
			catch(IOException e)
			{
				Log.d(TAG, e.getMessage());
				return -1;
			}
		} else if (old_file.isDirectory() && temp_dir.isDirectory()
				&& ( newInSambaDir || temp_dir.canWrite())) {
			String files[] = old_file.list();
			String dir = newDir + old.substring(old.lastIndexOf("/"), old.length());
			int len = files.length;
			File f = new File(dir);
			if(!f.exists())
			{
				if(!f.mkdir())
				{
					Log.d(TAG, "fail to make dir:" + dir);
					return -1;
				}
			}
			for(int i = 0; i < len; i++)
			{
				copyToDirectory(old + "/" + files[i], dir);
			}
		}
		else if(!newInSambaDir && !temp_dir.canWrite())
		{
			Log.d(TAG, "has not permission to write to " + newDir);
			return -1;
		}
		return 0;
	}
	
	/**
	 * @param filePath
	 * @param newName
	 * @return -1:newName file is exist; -2:rename fail; 0:rename success;
	 */
	public int renameTarget(String filePath, String newName)
	{
		File src = new File(filePath);
		String ext = "";
		File dest;
		
		if(src.isFile())
		{
			try
			{
				ext = filePath.substring(filePath.lastIndexOf("."), filePath.length());
			}
			catch(IndexOutOfBoundsException e)
			{
			}
		}
		if(newName.length() < 1)
		{
			return -2;
		}
		
		String temp = filePath.substring(0, filePath.lastIndexOf("/"));
		String destPath = temp + "/" + newName + ext;
		dest = new File(destPath);
		if(dest.exists()){
			return -1;
		}
		if(src.renameTo(dest))
		{
			RefreshMedia mRefresh = new RefreshMedia(mContext);
			mRefresh.notifyMediaAdd(destPath);
			mRefresh.notifyMediaDelete(filePath);
			return 0;
		}
		else return -2;
	}
	
	public boolean mkdirTarget(String parent, String newDir){
		File parentFile = new File(parent);
		File newFile = new File(parentFile,newDir);
		return newFile.mkdir();
	}
	
	public int deleteTarget(String path)
	{
		if(isCancel)
		{
			return 0;
		}
		File target = new File(path);
		if(target.exists() && target.isFile() && target.canWrite())
		{
			deleteNum ++;
			target.delete();
			RefreshMedia mRefresh = new RefreshMedia(mContext);
			mRefresh.notifyMediaDelete(path);
			return 0;
		}
		else if(target.exists() && target.isDirectory() && target.canRead())
		{
			String[] file_list = target.list();
			if(file_list != null && file_list.length == 0)
			{
				deleteNum ++;
				target.delete();
				return 0;
			}
			else if(file_list != null && file_list.length > 0)
			{
				for(int i = 0; i < file_list.length; i++)
				{
					String filePath = target.getAbsolutePath() + "/" + file_list[i];
					File temp_f = new File(filePath);
					if(temp_f.isDirectory())
					{
						deleteTarget(temp_f.getAbsolutePath());
					}
					else if(temp_f.isFile())
					{
						deleteNum ++;
						temp_f.delete();
						
						RefreshMedia mRefresh = new RefreshMedia(mContext);
						mRefresh.notifyMediaDelete(filePath);
					}
				}
			}
			if(target.exists())
			{
				if(target.delete())
				{
					return 0;
				}
			}
		}
		return -1;
	}
	
	
	
	public void scanFiles(String path)
	{
		fileSize = 0;
		fileNum  = 0;
		File file = new File(path);
		try
		{
			if(file.exists())
			{
				scanFiles(file);
			}
			else
			{
				return;
			}
		}
		catch(SecurityException e)
		{
			return;
		}
	}
	
	private void scanFiles(File file)
	{
		fileNum ++;
		if(file.isFile())
		{
			fileSize += file.length();
		}
		else if(file.isDirectory() && file.canRead())
		{
			File[] list = file.listFiles();
			for(int i = 0; i < list.length; i++)
			{
				scanFiles(list[i]);
			}
		}
	}
	
	/*   */
	public long getScanSize()
	{
		return fileSize;
	}
	
	public long getScanNum()
	{
		return fileNum;
	}
	
	public long getDeletedNum()
	{
		return deleteNum;
	}
	
	public long getCopySize()
	{
		return copySize;
	}
	
	public long getCopyNum()
	{
		return copyNum;
	}
	
	public void setCancel()
	{
		isCancel = true;
	}
	
	
	
	
	
	
}
