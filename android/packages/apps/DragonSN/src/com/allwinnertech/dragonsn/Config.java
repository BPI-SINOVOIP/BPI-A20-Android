
package com.allwinnertech.dragonsn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.allwinnertech.dragonsn.entity.BindedColume;

public class Config {

    public static final String CONFIG_FILE = "/DragonBox/DragonInt.txt";
	
    private static final String CONFIG_FILE_MOUNT_POINT[] = {"/mnt/sdcard", "/mnt/extsd", "/mnt/usbhost0", "/mnt/usbhost1"};
    private static final String DATABASE_URL = "jdbc:jtds:sqlserver://%s/%s";
    private static final String DATABASE_SERVER = "dbserver";
    private static final String DATABASE_NAME = "dbname";
    private static final String DATABASE_TABLE = "dbtable";
    private static final String DATABASE_ACCOUNT = "dbaccount";
    private static final String DATABASE_PASSWORD = "dbpassword";
    private static final String DATABASE_COL = "colume";

    private static final String COL_SHOW_NAME = "show_name";
    private static final String COL_NAME = "colname";
    private static final String COL_TYPE = "type";
    private static final String COL_BURN_NAME = "burnname";
    private static final String COL_LENGTH = "len";

    public String serverUrl;
    public String account;
    public String password;
    public String tableName;

    private static Config mConfig;
    
    public String primKey;
    
    private Config(){
    	
    }
    
    /**
     * Singleton
     * @return
     */
    public static Config getConfig(){
    	if (mConfig == null) {
    		mConfig = new Config();
    	}
    	return mConfig;
    }
    
    public static String getConfigFileName() {
        String configFileName = null;
        for (int i = 0; i < CONFIG_FILE_MOUNT_POINT.length; i++) {
            configFileName = CONFIG_FILE_MOUNT_POINT[i] + CONFIG_FILE;
            File configFile = new File(configFileName);
            if (configFile.exists() && configFile.isFile()) {
                return configFileName;
            }

        }
        return configFileName;
    }
    
    public boolean parseConfig(List<BindedColume> columes){
        primKey = null;
        String configFileName = getConfigFileName();
        if (configFileName == null){
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bf = null;
        try {
            bf = new BufferedReader(new FileReader(configFileName));
            String line;
            while ((line = bf.readLine()) != null) {
                stringBuilder.append(line);
            }

            JSONObject jsonObject;
            jsonObject = new JSONObject(stringBuilder.toString());
            String path      = jsonObject.getString(DATABASE_SERVER);
            String dbname    = jsonObject.getString(DATABASE_NAME);
            serverUrl = String.format(DATABASE_URL, path, dbname);

            account  = jsonObject.getString(DATABASE_ACCOUNT);
            password = jsonObject.getString(DATABASE_PASSWORD);
            tableName = jsonObject.getString(DATABASE_TABLE);
            
            columes.clear();
            JSONArray colArray = jsonObject.getJSONArray(DATABASE_COL);
            int len = colArray.length();
            for(int i=0;i<len;i++){
                JSONObject jsonItem = colArray.getJSONObject(i);
                BindedColume colume = new BindedColume();
                colume.setShowName(jsonItem.getString(COL_SHOW_NAME)) ;
                colume.setColName(jsonItem.getString(COL_NAME));
                colume.setType(jsonItem.getString(COL_TYPE));
                colume.setLength(jsonItem.getInt(COL_LENGTH));
                if (!colume.isResultKey()) {
                	colume.setBurnName(jsonItem.getString(COL_BURN_NAME));
                }
                if (colume.isPrimaryKey()) {
                	if (primKey == null) {
                		primKey = colume.getShowName();
                	} else {
                		Log.i(DragonSNActivity.TAG, "already set primkey,it just need set one times");
                		return false;                		
                	}
                }
                columes.add(colume);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e){
        	e.printStackTrace();
        	return false;
        } finally{
        	if (bf != null) {
        		try {
					bf.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
        	}
        }
        if (primKey == null || "".equals(primKey)) {
        	return false;
        }
        return true;
    }
}
