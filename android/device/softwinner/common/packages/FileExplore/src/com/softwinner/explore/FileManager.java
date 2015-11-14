/*
    Open Manager, an open source file manager for the Android system
    Copyright (C) 2009, 2010, 2011  Joe Berria <nexesdevelopment@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.softwinner.explore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import android.content.Context;
import android.os.StatFs;
import android.util.Log;

/**
 * This class is completely modular, which is to say that it has
 * no reference to the any GUI activity. This class could be taken
 * and placed into in other java (not just Android) project and work.
 * <br>
 * <br>
 * This class handles all file and folder operations on the system.
 * This class dictates how files and folders are copied/pasted, (un)zipped
 * renamed and searched. The EventHandler class will generally call these
 * methods and have them performed in a background thread. Threading is not
 * done in this class.  
 * 
 * @author Joe Berria
 *
 */
public class FileManager {
	private static final String TAG = FileManager.class.getSimpleName();
	public  static final int ROOT_FLASH = 0;
	public  static final int ROOT_SDCARD = 1;
	public  static final int ROOT_USBHOST = 2;
	public	static final int ROOT_UNKNOWN = 3;
	
	private static final int BUFFER = 		2048;
	private static final int SORT_NONE = 	0;
	private static final int SORT_ALPHA = 	1;
	private static final int SORT_TYPE = 	2;
	
	private ArrayList<String> flashPath;
	private ArrayList<String> sdcardPath;
	private ArrayList<String> usbPath;
	private DevicePath mDevices;
	
	private boolean mShowHiddenFiles = false;
	private int mSortType = SORT_ALPHA;
	private long mDirSize = 0;
	private Stack<String> mPathStack;
	private ArrayList<String> mDirContent;
	
	public String flashList = "Sdcard";
	public String sdcardList = "External Sdcard";
	public String usbhostList = "Usb";
	
	private Context mContext ;
	/**
	 * Constructs an object of the class
	 * <br>
	 * this class uses a stack to handle the navigation of directories.
	 */
	public FileManager(Context context) {
		mDirContent = new ArrayList<String>();
		mPathStack = new Stack<String>();
		mContext = context;
		
		flashList = mContext.getResources().getString(R.string.flash);
		sdcardList = mContext.getResources().getString(R.string.extsd);
		usbhostList = mContext.getResources().getString(R.string.usbhost);
		
		mDevices = new DevicePath(context);
		flashPath = mDevices.getInterStoragePath();
		sdcardPath = mDevices.getSdStoragePath();
		usbPath = mDevices.getUsbStoragePath();
		mPathStack.push("/");
		mPathStack.push(flashList);
	}
	
	/**
	 * This will return a string of the current directory path
	 * @return the current directory
	 */
	public String getCurrentDir() {
		return mPathStack.peek();
	}
	
	/**
	 * This will return a string of the current home path.
	 * @return	the home directory
	 */
	public ArrayList<String> getHomeDir(int root_type) {
		//This will eventually be placed as a settings item
		mPathStack.clear();
		mPathStack.push("/");
		switch(root_type)
		{
			case ROOT_SDCARD:
				mPathStack.push(sdcardList);
				return mDevices.getMountedPath(sdcardPath);
				
			case ROOT_USBHOST:
				mPathStack.push(usbhostList);
				return mDevices.getMountedPath(usbPath);
				
			case ROOT_FLASH:
			default:
				mPathStack.push(flashList);
				return mDevices.getMountedPath(flashPath);
				
		}
	}
	
	/**
	 * This will tell if current path is root
	 * @return	is root?
	 */
	public boolean isRoot() {
		//This will eventually be placed as a settings item
		String tmp = mPathStack.peek();
		
		if(tmp.equals(sdcardList) ||
			tmp.equals(usbhostList) ||
			tmp.equals(flashList))
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * This will tell which root we are in;
	 * @return	which root?
	 */
	public int whichRoot() {
		//This will eventually be placed as a settings item
		String tmp = mPathStack.peek();
		if(tmp.equals(flashList)){
			return ROOT_FLASH;
		}else if(tmp.equals(sdcardList)){
			return ROOT_SDCARD;
		}else if(tmp.equals(usbhostList)){
			return ROOT_USBHOST;
		}else{
			for(String st:sdcardPath){
				if(tmp.startsWith(st)){
					return ROOT_SDCARD;
				}
			}
			for(String st:flashPath){
				if(tmp.startsWith(st)){
					return ROOT_FLASH;
				}
			}
			for(String st:usbPath){
				if(tmp.startsWith(st)){
					return ROOT_USBHOST;
				}
			}
		}
		return ROOT_UNKNOWN;
	}
	
	/**
	 * This will determine if hidden files and folders will be visible to the
	 * user.
	 * @param choice	true if user is veiwing hidden files, false otherwise
	 */
	public void setShowHiddenFiles(boolean choice) {
		mShowHiddenFiles = choice;
	}
	
	/**
	 * 
	 * @param type
	 */
	public void setSortType(int type) {
		mSortType = type;
	}
	
	/**
	 * This will return a string that represents the path of the previous path
	 * @return	returns the previous path
	 */
	public ArrayList<String> getPreviousDir() {
		int size = mPathStack.size();
		
		if (size >= 2)
			mPathStack.pop();
		
		else if(size == 0)
			mPathStack.push(flashList);
		
		String st = mPathStack.peek();
		if(st.equals(flashList)){
			return mDevices.getMountedPath(flashPath);
		}else if(st.equals(sdcardList)){
			return mDevices.getMountedPath(sdcardPath);
		}else if(st.equals(usbhostList)){
			return mDevices.getMountedPath(usbPath);
		}else{
			return populate_list();
		}
	}
	
	/**
	 * 
	 * @param path
	 * @param isFullPath
	 * @return
	 */
	public ArrayList<String> getNextDir(String path) {
		if(!path.equals(mPathStack.peek())) {
			mPathStack.push(path);
		}
		if(flashList.equals(path)){
			return mDevices.getMountedPath(flashPath);
		}else if(sdcardList.equals(path)){
			return mDevices.getMountedPath(sdcardPath);
		}else if(usbhostList.equals(path)){
			return mDevices.getMountedPath(usbPath);
		}else{
			return populate_list();
		}
	}

	/**
	 * 
	 * @param old		the file to be copied
	 * @param newDir	the directory to move the file to
	 * @return
	 */
	public int copyToDirectory(String old, String newDir) {
		File old_file = new File(old);
		File temp_dir = new File(newDir);
		byte[] data = new byte[BUFFER];
		int read = 0;
		String copyName = mContext.getResources().getString(R.string.Copied);
		
		if(old_file.isFile() && temp_dir.isDirectory() && temp_dir.canWrite()){
			//������Ƶ��Լ��ĸ�Ŀ¼,��
			String new_name = "";
			if(old_file.getParent().equals(temp_dir.getAbsolutePath())){
				new_name = newDir + "/" + copyName + old.substring(old.lastIndexOf("/") + 1, old.length());
				int n = 2;
				while(new File(new_name).exists()){
					new_name = newDir + "/" + copyName + n + "_" + old.substring(old.lastIndexOf("/") + 1, old.length());
					n++;
				}
			}else{
				new_name = newDir + old.substring(old.lastIndexOf("/"), old.length());
			}
			
			
			File cp_file = new File(new_name);	
			try {
				BufferedOutputStream o_stream = new BufferedOutputStream(
												new FileOutputStream(cp_file));
				BufferedInputStream i_stream = new BufferedInputStream(
											   new FileInputStream(old_file));
				
				while((read = i_stream.read(data, 0, BUFFER)) != -1)
					o_stream.write(data, 0, read);
				
				o_stream.flush();
				i_stream.close();
				o_stream.close();
				
				RefreshMedia mRefresh = new RefreshMedia(mContext);
				mRefresh.notifyMediaAdd(new_name);
			} catch (FileNotFoundException e) {
				Log.e("FileNotFoundException", e.getMessage());
				return -1;
				
			} catch (IOException e) {
				Log.e("IOException", e.getMessage());
				return -1;
			}
			
		}else if(old_file.isDirectory() && temp_dir.isDirectory() && temp_dir.canWrite()) {
			String files[] = old_file.list();
			String dir = "copyFile";
			//������Ƶ��Լ��ĸ�Ŀ¼,��
			if(old_file.getParent().equals(temp_dir.getAbsolutePath())){
				dir = newDir + "/" + copyName + old.substring(old.lastIndexOf("/") + 1, old.length());
				int n = 2;
				while(new File(dir).exists()){
					dir = newDir + "/" + copyName + n + "_" + old.substring(old.lastIndexOf("/") + 1, old.length());
					n++;
				}
			}else{
				dir = newDir + old.substring(old.lastIndexOf("/"), old.length());
			}
			int len = files.length;
			File file = new File(dir);
			if(!file.exists()){
				if(!file.mkdir()){
					return -1;
				}
			}
			for(int i = 0; i < len; i++)
				copyToDirectory(old + "/" + files[i], dir);
			
		} else if(!temp_dir.canWrite())
			return -1;
		
		return 0;
	}
	
	/**
	 * 
	 * @param toDir
	 * @param fromDir
	 */
	public void extractZipFilesFromDir(String zipName, String toDir, String fromDir) {
		byte[] data = new byte[BUFFER];
		ZipEntry entry;
		ZipInputStream zipstream;
		
		/* create new directory for zip file */
		String org_path = fromDir + "/" + zipName;
		String dest_path = toDir + zipName.substring(0, zipName.length() - 4);
		String zipDir = dest_path + "/";
		
		new File(zipDir).mkdir();
		try {
			zipstream = new ZipInputStream(new FileInputStream(org_path));
			
			while((entry = zipstream.getNextEntry()) != null) {
				if(entry.isDirectory()) {
					String ndir = zipDir + entry.getName() + "/";
					
					new File(ndir).mkdir();
					
				} else {
					int read = 0;
					FileOutputStream out = new FileOutputStream(
												zipDir + entry.getName());
					while((read = zipstream.read(data, 0, BUFFER)) != -1)
						out.write(data, 0, read);
					
					zipstream.closeEntry();
					out.close();
					
					RefreshMedia mRefresh = new RefreshMedia(mContext);
					mRefresh.notifyMediaAdd(zipDir + entry.getName());
				}
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param zip_file
	 * @param directory
	 */
	public void extractZipFiles(String zip_file, String directory) {
		byte[] data = new byte[BUFFER];
		ZipEntry entry;
		ZipInputStream zipstream;
		
		/* create new directory for zip file */
		String path = zip_file;
		String name = path.substring(path.lastIndexOf("/") + 1, 
									 path.length() - 4);
		String zipDir = path.substring(0, path.lastIndexOf("/") +1) + 
									   name + "/";
		new File(zipDir).mkdir();
		try {
			zipstream = new ZipInputStream(new FileInputStream(path));
			
			while((entry = zipstream.getNextEntry()) != null) {
				if(entry.isDirectory()) {
					String ndir = zipDir + entry.getName() + "/";

					new File(ndir).mkdir();
					
				} else {
					int read = 0;
					FileOutputStream out = new FileOutputStream(
											zipDir + entry.getName());
					while((read = zipstream.read(data, 0, BUFFER)) != -1)
						out.write(data, 0, read);
					
					zipstream.closeEntry();
					out.close();
					
					RefreshMedia mRefresh = new RefreshMedia(mContext);
					mRefresh.notifyMediaAdd(zipDir + entry.getName());
				}
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param path
	 */
	public void createZipFile(String path) {
		File dir = new File(path);
		String[] list = dir.list();
		String name = path.substring(path.lastIndexOf("/"), path.length());
		String _path;
		
		if(!dir.canRead() || !dir.canWrite())
			return;
		
		int len = list.length;
		
		if(path.charAt(path.length() -1) != '/')
			_path = path + "/";
		else
			_path = path;
		
		try {
			ZipOutputStream zip_out = new ZipOutputStream(
									  new BufferedOutputStream(
									  new FileOutputStream(_path + name + ".zip"), BUFFER));
			
			for (int i = 0; i < len; i++)
				zip_folder(new File(_path + list[i]), zip_out);

			zip_out.close();
			
		} catch (FileNotFoundException e) {
			Log.e("File not found", e.getMessage());

		} catch (IOException e) {
			Log.e("IOException", e.getMessage());
		}
	}
	
	/**
	 * 
	 * @param filePath
	 * @param newName
	 * @return -1:newName file is exist; -2:rename fail; 0:rename success;
	 */
	public int renameTarget(String filePath, String newName) {
		File src = new File(filePath);
		String ext = "";
		File dest;
				
		if(src.isFile())
			/*get file extension*/
			ext = filePath.substring(filePath.lastIndexOf("."), filePath.length());
		
		if(newName.length() < 1)
			return -2;
	
		String temp = filePath.substring(0, filePath.lastIndexOf("/"));
		
		String destPath = temp + "/" + newName + ext;
		dest = new File(destPath);
		
		//add to make sure destPath is not exists ! 
		//Beacuse FS can not exists the same file name in the same diretory !
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
		else
			return -2;
	}
	
	/**
	 * 
	 * @param path
	 * @param name
	 * @return
	 */
	public int createDir(String path, String name) {
		int len = path.length();
		
		if(len < 1 || len < 1)
			return -1;
		
		if(path.charAt(len - 1) != '/')
			path += "/";
		
		if (new File(path+name).mkdir())
			return 0;
		
		return -1;
	}
	
	/**
	 * The full path name of the file to delete.
	 * 
	 * @param path name
	 * @return
	 */
	public int deleteTarget(String path) {
		File target = new File(path);
		
		if(target.exists() && target.isFile() && target.canWrite()) {
			target.delete();

			RefreshMedia mRefresh = new RefreshMedia(mContext);
			mRefresh.notifyMediaDelete(path);
            try
            {
                Thread.currentThread().sleep(500);
            }
            catch(Exception e) {};
            
			return 0;
		}
		
		else if(target.exists() && target.isDirectory() && target.canRead()) {
			String[] file_list = target.list();
			
			if(file_list != null && file_list.length == 0) {
				target.delete();
				try
                {
                    Thread.currentThread().sleep(500);
                }
                catch(Exception e) {};
            
				return 0;
				
			} else if(file_list != null && file_list.length > 0) {
				
				for(int i = 0; i < file_list.length; i++) {
					String filePath = target.getAbsolutePath() + "/" + file_list[i];
					File temp_f = new File(filePath);

					if(temp_f.isDirectory())
						deleteTarget(temp_f.getAbsolutePath());
					else if(temp_f.isFile())
					{
						temp_f.delete();
						
						RefreshMedia mRefresh = new RefreshMedia(mContext);
						mRefresh.notifyMediaDelete(filePath);
					}
				}
			}
			if(target.exists())
				if(target.delete()){
				    try
                    {
                        Thread.currentThread().sleep(500);
                    }
                    catch(Exception e) {};
            
					return 0;
		        }	    
		}
		return -1;
	}
	
	/**
	 * 
	 * @param name
	 * @return
	 */
	public boolean isDirectory(String name) {
		return new File(mPathStack.peek() + "/" + name).isDirectory();
	}
		
	/**
	 * converts integer from wifi manager to an IP address. 
	 * 
	 * @param des
	 * @return
	 */
	public static String integerToIPAddress(int ip) {
		String ascii_address = "";
		int[] num = new int[4];
		
		num[0] = (ip & 0xff000000) >> 24;
		num[1] = (ip & 0x00ff0000) >> 16;
		num[2] = (ip & 0x0000ff00) >> 8;
		num[3] = ip & 0x000000ff;
		 
		ascii_address = num[0] + "." + num[1] + "." + num[2] + "." + num[3];
		 
		return ascii_address;
	 }
	
	/**
	 * 
	 * @param dir
	 * @param pathName
	 * @return
	 */
	public ArrayList<String> searchInDirectory(String dir, String pathName) {
		ArrayList<String> names = new ArrayList<String>();
		search_file(dir, pathName, names);

		return names;
	}
	
	/**
	 * 
	 * @param path
	 * @return
	 */
	public long getDirSize(String path) {
		get_dir_size(new File(path));

		return mDirSize;
	}
	
	
	private static final Comparator alph = new Comparator<String>() {
		@Override
		public int compare(String arg0, String arg1) {
			return arg0.toLowerCase().compareTo(arg1.toLowerCase());
		}
	};
	
	private static final Comparator type = new Comparator<String>() {
		@Override
		public int compare(String arg0, String arg1) {
			String ext = null;
			String ext2 = null;
			
			try {
				ext = arg0.substring(arg0.lastIndexOf(".") + 1, arg0.length());
				ext2 = arg1.substring(arg1.lastIndexOf(".") + 1, arg1.length());
				
			} catch (IndexOutOfBoundsException e) {
				return 0;
			}
			
			return ext.compareTo(ext2);
		}
	};
	
	/* (non-Javadoc)
	 * this function will take the string from the top of the directory stack
	 * and list all files/folders that are in it and return that list so 
	 * it can be displayed. Since this function is called every time we need
	 * to update the the list of files to be shown to the user, this is where 
	 * we do our sorting (by type, alphabetical, etc).
	 * 
	 * @return
	 */
	private ArrayList<String> populate_list() {

		if(!mDirContent.isEmpty())
			mDirContent.clear();
		try
		{
			String path = mPathStack.peek();
			File file = new File(path); 
		
			if(file.exists() && file.canRead() && file.isDirectory()) {
//				String[] list = file.list();
				File[] fList = file.listFiles();
				boolean isPartition = false;
				if(mDevices.hasMultiplePartition(path)){
					Log.d("chen",path + " has multi partition");
					isPartition = true;
				}
				if(fList != null)
				{
					int len = fList.length;
					
					/* add files/folder to arraylist depending on hidden status */
					for (int i = 0; i < len; i++) {
						if(isPartition){
							try{
								StatFs statFs = new StatFs(fList[i].getAbsolutePath());
								int count = statFs.getBlockCount();
								Log.d("chen",fList[i].getName() + "  " + count);
								if(count == 0){
									continue;
								}
							}catch(Exception e){
								Log.d(TAG,fList[i].getName() + "  exception");
								continue;
							}
						}
						String name = fList[i].getName();
						if(!mShowHiddenFiles) {
							if(name.charAt(0) != '.')
								mDirContent.add(name);
					
						} else {
							mDirContent.add(name);
						}
					}
			
					/* sort the arraylist that was made from above for loop */
					switch(mSortType) 
					{
						case SORT_NONE:
							//no sorting needed
							break;
					
						case SORT_ALPHA:
							Object[] tt = mDirContent.toArray();
							mDirContent.clear();
					
							Arrays.sort(tt, alph);
					
							for (Object a : tt){
								mDirContent.add((String)a);
							}
							break;
					
						case SORT_TYPE:
							Object[] t = mDirContent.toArray();
							String dir = mPathStack.peek();
					
							Arrays.sort(t, type);
							mDirContent.clear();
					
							for (Object a : t){
								if(new File(dir + "/" + (String)a).isDirectory())
									mDirContent.add(0, (String)a);
								else
									mDirContent.add((String)a);
							}
							break;
					}
				}	
			} 
		}catch(Exception e)
		{
			/* clear any operate made above */
			Log.e("FileManager", "unknow exception");
			mDirContent.clear();
		}
		return mDirContent;
	}
	
	/*
	 * 
	 * @param file
	 * @param zout
	 * @throws IOException
	 */
	private void zip_folder(File file, ZipOutputStream zout) throws IOException {
		byte[] data = new byte[BUFFER];
		int read;
		
		if(file.isFile()){
			ZipEntry entry = new ZipEntry(file.getName());
			zout.putNextEntry(entry);
			BufferedInputStream instream = new BufferedInputStream(
										   new FileInputStream(file));
			
			while((read = instream.read(data, 0, BUFFER)) != -1)
				zout.write(data, 0, read);
			
			zout.closeEntry();
			instream.close();
			
		} else {
			String[] list = file.list();
			int len = list.length;
			
			for(int i = 0; i < len; i++)
				zip_folder(new File(file.getPath() +"/"+ list[i]), zout);
		}
	}
	
	/*
	 * This function will be rewritten as there is a problem getting
	 * the directory size in certain folders from root. ex /sys, /proc.
	 * The app will continue until a stack overflow. get size is fine uder the 
	 * sdcard folder.
	 * 
	 * @param path
	 */
	private void get_dir_size(File path) {
		File[] list = path.listFiles();
		int len;
		
		if(list != null) {
			len = list.length;
			
			for (int i = 0; i < len; i++) {
				if(list[i].isFile() && list[i].canRead()) {
					mDirSize += list[i].length();
					
				} else if(list[i].isDirectory() && list[i].canRead()) { 
					get_dir_size(list[i]);
				}
			}
		}
	}

	/*
	 * (non-JavaDoc)
	 * I dont like this method, it needs to be rewritten. Its hacky in that
	 * if you are searching in the root dir (/) then it is not going to be treated
	 * as a recursive method so the user dosen't have to sit forever and wait.
	 * 
	 * I will rewrite this ugly method.
	 * 
	 * @param dir		directory to search in
	 * @param fileName	filename that is being searched for
	 * @param n			ArrayList to populate results
	 */
	private void search_file(String dir, String fileName, ArrayList<String> n) {
		File root_dir = new File(dir);
		String[] list = root_dir.list();
		
		if(list != null && root_dir.canRead()) {
			int len = list.length;
			
			for (int i = 0; i < len; i++) {
				File check = new File(dir + "/" + list[i]);
				String name = check.getName();
					
				if(check.isFile() && name.toLowerCase().
										contains(fileName.toLowerCase())) {
					n.add(check.getPath());
				}
				else if(check.isDirectory()) {
					if(name.toLowerCase().contains(fileName.toLowerCase()))
						n.add(check.getPath());
					
					else if(check.canRead() && !dir.equals("/"))
						search_file(check.getAbsolutePath(), fileName, n);
				}
			}
		}
	}
}
