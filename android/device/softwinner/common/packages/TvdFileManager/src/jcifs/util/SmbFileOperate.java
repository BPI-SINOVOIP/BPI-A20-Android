package jcifs.util;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import jcifs.smb.SmbException;
import com.softwinner.TvdFileManager.net.SambaManager;
import java.io.File;
import android.util.Log;
import jcifs.smb.SmbFile;
import jcifs.smb.NtlmPasswordAuthentication;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import com.softwinner.SystemMix;
import com.softwinner.TvdFileManager.FileOperate;
import com.softwinner.TvdFileManager.RefreshMedia;
import android.content.Context;

public class SmbFileOperate extends FileOperate
{
    private static final String TAG = "SmbFileOperate";
    private SambaManager mSamba;
    private long deleteNum = 0;
    private boolean isCancel = false;
    private Context mContext;
    
    public SmbFileOperate(Context context, SambaManager sambaManager) {
        mContext = context;
        mSamba = sambaManager;
    }

    @Override
    public void setCancel() {
        isCancel = true;
    }
    
    @Override
    public int deleteTarget(String path) {
        if(isCancel)
        {
            return 0;
        }
        File target = new File(path);
        if(target.exists() && target.isFile() && canWrite(target.getAbsolutePath())) {
            deleteNum ++;
            delete(target.getAbsolutePath());
            RefreshMedia mRefresh = new RefreshMedia(mContext);
            mRefresh.notifyMediaDelete(path);
            return 0;
        } else if(target.exists() && target.isDirectory() && canRead(target.getAbsolutePath())) {
            String[] file_list = target.list();
            if(file_list != null && file_list.length == 0) {
                deleteNum ++;
                delete(target.getAbsolutePath());
                return 0;
            } else if(file_list != null && file_list.length > 0) {
                for(int i = 0; i < file_list.length; i++) {
                    String filePath = target.getAbsolutePath() + "/" + file_list[i];
                    File temp_f = new File(filePath);
                    if(temp_f.isDirectory()) {
                        deleteTarget(temp_f.getAbsolutePath());
                    } else if(temp_f.isFile()) {
                        deleteNum ++;
                        delete(temp_f.getAbsolutePath());

                        RefreshMedia mRefresh = new RefreshMedia(mContext);
                        mRefresh.notifyMediaDelete(filePath);
                    }
                }
            }
            if(target.exists()) {
                if(0 == delete(target.getAbsolutePath())) {
                    return 0;
                }
            }
        }
        return -1;
    }
	
    @Override
    public boolean mkdirTarget(String parent, String newDir) {
        File parentFile = new File(parent);
        File newFile = new File(parentFile,newDir);
        return mkdir(parent + "/", newDir);
    }
    
    @Override
    public int renameTarget(String filePath, String newName) {
        File src = new File(filePath);
        String ext = "";
        File dest;
    
        if(src.isFile()) {
            try {
                ext = filePath.substring(filePath.lastIndexOf("."), filePath.length());
            } catch(IndexOutOfBoundsException e) {
            }
        }
        if(newName.length() < 1)	{
            return -2;
        }

        String temp = filePath.substring(0, filePath.lastIndexOf("/"));
        String destPath = temp + "/" + newName + ext;
        dest = new File(destPath);
        if(dest.exists()) {
            return -1;
        }
        if (rename(filePath, destPath)) {
            RefreshMedia mRefresh = new RefreshMedia(mContext);
            mRefresh.notifyMediaAdd(destPath);
            mRefresh.notifyMediaDelete(filePath);
            return 0;
        } else {
            return -2;
        }
    }
	
    private SmbFile newSmbFile(String path) {
        if(!path.startsWith(mSamba.mountRoot.getAbsolutePath()))
            return null;
        String mountPoint = mSamba.getSambaMountedPointFromPath(path);
        String smbPath = mSamba.getSmbPathFromMountedPoint(mountPoint);
        String newPath = path.replaceFirst(mountPoint + "/", smbPath);
        try {
            SmbFile smbFile = new SmbFile(smbPath);
            NtlmPasswordAuthentication ntlm = mSamba.getLoginDataFromDB(smbFile);
            if(ntlm != null) {
                return (new SmbFile(newPath, ntlm));
            } else {
                return smbFile;
            }
        } catch (MalformedURLException e) {
            return null;
        }
    }
     	
     public boolean canWrite(String path) {
        return SystemMix.CanWrite(path);
     }
     public boolean canRead(String path) {
        return SystemMix.CanRead(path);
     }
     
     public boolean rename(String src, String dst) {
        return SystemMix.rename(src, dst);
     }
     
     public int delete(String path) {
        return SystemMix.Delete(path);
     }
	
     public boolean mkdir(String parent, String newDir) {
        return SystemMix.mkdir(parent + newDir);
     }
     
     
     public InputStream getInputStream(String path) {
        SmbFile smbFile = newSmbFile(path);
        if(smbFile == null)
            return null;
        try {
            return smbFile.getInputStream();
        } catch (IOException e) {
            return null;
        }
     }
     
     public OutputStream getOutputStream(String path) {
        SmbFile smbFile = newSmbFile(path);
        if(smbFile == null)
            return null;
        try {
            return smbFile.getOutputStream();
        } catch (IOException e) {
            return null;
        }
     }
}
