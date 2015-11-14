package com.allwinnertech.dragonsn.entity;

public class BindedColume extends PrivateColume{
	
	private String primValue = "";
    private String localData = "";
    private String remoteData = "";
    
	public String getLocalData() {
		return localData;
	}
	
	public void setLocalData(String localData) {
		if (localData != null) {
			this.localData = localData;			
		}
	}
	public String getRemoteData() {
		return remoteData;
	}
	public void setRemoteData(String remoteData) {
		if (remoteData != null) {
			this.remoteData = remoteData;			
		}
	}
    
    public String getPrimValue() {
		return primValue;
	}

	public boolean setPrimValue(String primValue) {
		if (isPrimaryKey() && primValue != null && getLength() == primValue.length()) {
			this.primValue = primValue;
			return true;
		}
		return false;
	}

	public void clearPrimValue(){
		this.primValue = "";
	}
	
	public boolean isRemoteValid(){
    	if (remoteData == null || getLength() != remoteData.length() || "".equals(remoteData)) {
    		return false;
    	}
    	return true;
    }
    
    public boolean isLocalValid(){
    	if (localData == null || getLength() != localData.length() || "".equals(localData)) {
    		return false;
    	}
    	return true;
    }
    
    public boolean isAllValid(){
    	return isRemoteValid() && isLocalValid() && getRemoteData().equals(getLocalData());
    }
    
}
