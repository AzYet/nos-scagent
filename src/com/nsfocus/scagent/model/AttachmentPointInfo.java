package com.nsfocus.scagent.model;

public class AttachmentPointInfo {
    byte[] mac = null;
	String attchmentPoint = null;  //some string like "3.3.3.3:tap1"

    public AttachmentPointInfo() {
		super();
	}
	public AttachmentPointInfo(byte[] mac, String attchmentPoint) {
		super();
		this.mac = mac;
		this.attchmentPoint = attchmentPoint;
	}
	public byte[] getMac() {
		return mac;
	}
	public void setMac(byte[] mac) {
		this.mac = mac;
	}
	public String getAttchmentPoint() {
		return attchmentPoint;
	}
	public void setAttchmentPoint(String attchmentPoint) {
		this.attchmentPoint = attchmentPoint;
	}

}
