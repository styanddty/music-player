package com.example.music.utils;

import java.io.File;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

/**
 * �ļ�������
 *
 */
public class FileUtil {
	/**
	 * ����SDcard״̬
	 * @return boolean
	 */
	public static boolean checkSDCard()
	{
		if(android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
		{
			return true;
		}else{
			return false;
		}
	}
	/**
	 * �����ļ��ļ���Ŀ¼
	 * @param context
	 * @return  �ļ������Ŀ¼
	 */
	public static String setMkdir(Context context)
	{
		String filePath;
		if(checkSDCard())
		{// ������getExternalStorageDirectory()  ���ͣ����� File ����ȡ�ⲿ�洢Ŀ¼�� SDCard
			filePath = Environment.getExternalStorageDirectory()+File.separator+"myfile";
		}else{
			filePath = context.getCacheDir().getAbsolutePath()+File.separator+"myfile";
		}
		File file = new File(filePath);
		if(!file.exists())
		{
			boolean b = file.mkdirs();
			Log.e("file", "�ļ�������  �����ļ�    "+b);
		}else{
			Log.e("file", "�ļ�����");
		}
		return filePath;
	}
}
