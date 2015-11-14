package com.softwinner.TvdFileManager;

import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.util.Log;
public class ThumbnailCreator 
{
	private int width;
	private int height;
	private static final String TAG = "ThumbnailCreator";
	public ThumbnailCreator(int width, int height)
	{
		this.width = width;
		this.height = height;
	}
	
	/**
	 * ��������ͼ
	 * @param imageSrc
	 * @return ����null,�����ʱ�޷��������ͼ
	 */
	public Bitmap createThumbnail(String imageSrc) 
	{
		boolean isJPG = false;
		Bitmap thumbnail = null;
		try
		{
			String ext = imageSrc.substring(imageSrc.lastIndexOf(".") + 1);
			if(ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg"))
			{
				isJPG = true;
			}
		}catch(IndexOutOfBoundsException e)
		{
			e.printStackTrace();
			return null;
		}
		
		if(isJPG)
		{
			try {
				ExifInterface mExif = null;
				mExif = new ExifInterface(imageSrc);
				if(mExif != null)
				{
					byte[] thumbData = mExif.getThumbnail();
					if(thumbData == null)
					{
						thumbnail = createThumbnailByOptions(imageSrc);
					}
					else
					{
						thumbnail = BitmapFactory.decodeByteArray(thumbData, 0, thumbData.length);
					}
				}
				else
				{
					thumbnail = createThumbnailByOptions(imageSrc);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
		else
		{
			thumbnail = createThumbnailByOptions(imageSrc);
		}
		return thumbnail;
	}
	
	private Bitmap createThumbnailByOptions(String imageSrc)
	{
		Bitmap thumb = null;
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		thumb = BitmapFactory.decodeFile(imageSrc, options);
		int be = (int) (Math.min(options.outWidth / width, options.outHeight / height));
		if(be <= 0)
			be = 1;
		options.inSampleSize = be;
		options.inJustDecodeBounds = false;
		thumb = BitmapFactory.decodeFile(imageSrc, options);
		if(thumb == null)
		{
			/* ������decodeʱ����null */
			return null;
		}
		thumb = Bitmap.createScaledBitmap(thumb, width, height, false);
		Log.d("MediaProvider","image:" + String.valueOf(width) + "*" + String.valueOf(height));
		return thumb;
	}
}