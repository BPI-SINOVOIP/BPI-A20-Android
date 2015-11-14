package com.allwinnertech.dragonsn.entity;

public class PrivateColume {
	
	public static final String PRIMARY_KEY_STR = "prim";
	public static final String RESULT_KEY_STR = "result";
	
	private String showName;
	private String colName;
	private String type;
    private int length;
    
    private String burnName;
    
	public String getShowName() {
		return showName;
	}
	public void setShowName(String showName) {
		this.showName = showName;
	}
	public String getColName() {
		return colName;
	}
	public void setColName(String colName) {
		this.colName = colName;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
    
	public String getBurnName() {
		return burnName;
	}
	public void setBurnName(String burnName) {
		this.burnName = burnName;
	}
	public boolean isPrimaryKey(){
		return PRIMARY_KEY_STR.equalsIgnoreCase(getType());
	}
    
	public boolean isResultKey(){
		return RESULT_KEY_STR.equalsIgnoreCase(getType());
	}
	
}
