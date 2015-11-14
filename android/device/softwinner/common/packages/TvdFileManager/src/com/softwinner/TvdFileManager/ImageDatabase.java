package com.softwinner.TvdFileManager;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

public class ImageDatabase
{
	private static final String APP_DIR_NAME = ".fileManager";
	private String APP_DIR;
	private static final String DB_NAME = "fileManager.db";
	
	private static final String IMAGE_TABLE = "image";
	public  static final String ORIG_PATH  = "orignalPath";
	public  static final String THUMB_PATH = "thumbPath";
	public  static final String CREATE_TIME = "created_time";
	public  static final String KEY_ID = "_id";
	public  static final String KEY_NAME = "name";
	public  static final String TABLE_CREATE = "CREATE TABLE IF NOT EXISTS " + IMAGE_TABLE 
				+ " (" + ORIG_PATH + " TEXT," + THUMB_PATH + " TEXT," + CREATE_TIME + " INTEGER)";
	private SQLiteDatabase imageDatabase;
	private Context mContext;
	
	
	private String TAG = "ImageDatabase";
	public ImageDatabase(Context context)
	{
		mContext = context;
		init();
		APP_DIR = mContext.getDir(APP_DIR_NAME, 0).getAbsolutePath(); 
	}
	public String getAppDir()
	{
		return APP_DIR;
	}
	
	/* Ϊ�˷�ֹ���ļ����ʱ����������ɾ�������ļ��������ݣ��������ݿ���ʴ���ÿ�β���ǰ��Ҫ���г�ʼ��  */
	private void init()
	{
		File dir = mContext.getDir(APP_DIR_NAME, 0);
		File dbFile = new File(dir, DB_NAME);
		String dbPath = dbFile.getAbsolutePath();
		if(imageDatabase == null)
		{
			createDB(dbPath);
		}
		else
		{
			
			if(!dbFile.exists())
			{
				createDB(dbPath);
			}
		}
	}
	
	private void createDB(String path)
	{
		//�򿪻򴴽�ָ����ͼƬ���ݿ�
		
		imageDatabase = SQLiteDatabase.openOrCreateDatabase(path, null);
		imageDatabase.execSQL(TABLE_CREATE);
		Log.d(TAG, "open database success");
	}
	
	/**
	 * ����һ������
	 * @param origPath     :ԴͼƬ�ļ�·��
	 * @param thumbPath    :����ͼ�ļ�·��
	 */
	public void insert(String origPath, String thumbPath)
	{
		init();
		File thumb = new File(thumbPath);
		long time  = thumb.lastModified();
		ContentValues cv = new ContentValues();
		cv.put(ORIG_PATH, origPath);
		cv.put(THUMB_PATH, thumbPath);
		cv.put(CREATE_TIME, time);
		imageDatabase.insert(IMAGE_TABLE, null, cv);
	}
	
	/**
	 * ɾ�����ݿ���Դ�ļ�·�����ڵ�����
	 * @param origPath
	 */
	public void delete(String origPath)
	{
		init();
		String[] args = {origPath};
		String clause = ORIG_PATH + "=?";
		imageDatabase.delete(IMAGE_TABLE, clause, args);
	}
	
	/**
	 * ��ѯ���ݿ��е�ͼƬ��Ϣ
	 * @param columns :��ѯ����
	 * @param selection :����
	 * @param selctionArgs
	 * @param orderBy :��������
	 * @return
	 */
	public Cursor query(String[] columns, String selection, 
			String[] selectionArgs, String orderBy)
	{
		init();
		Cursor c = null;
		try
		{
			c = imageDatabase.query(IMAGE_TABLE, columns, selection, selectionArgs, null, null, orderBy);
		}
		catch(IllegalStateException e)
		{
			Log.e(TAG, "database not open");
		}
		return c;
	}
	
	public void closeDatabase()
	{
		imageDatabase.close();
	}
}