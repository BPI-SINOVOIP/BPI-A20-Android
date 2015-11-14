package com.softwinner.tmp.nfs;

/**
 * NFS Server shared folder wrapper
 * @author A
 *
 */
public class NFSFolder {
	private String folderPath = "";		// NFS server shared one folder path
	private String mountedPoint = "";	// Mounted point path on local disk
	private boolean isMounted = false;	// If this shared folder already mounted on local disk

	public String getFolderPath() {
		return folderPath;
	}

	public void setFolderPath(String folderPath) {
		if (folderPath != null)
			this.folderPath = folderPath;
	}

	public String getMountedPoint() {
		return mountedPoint;
	}

	public void setMountedPoint(String mountedPoint) {
		this.mountedPoint = mountedPoint;
	}

	public boolean isMounted() {
		return isMounted;
	}

	public void setMounted(boolean isMounted) {
		this.isMounted = isMounted;
	}
	
	
}
