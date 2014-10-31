package com.nsfocus.scagent.model;

import com.nsfocus.scagent.utility.MACAddress;

import java.util.Arrays;

public class AttachmentPointInfo {
    byte[] mac = null;
	String attchmentPoint = null;  //some string like "3.3.3.3:tap1"

    public AttachmentPointInfo() {
		super();
	}
	public AttachmentPointInfo(byte[] mac, String attachmentPoint) {
		super();
		this.mac = mac;
		this.attchmentPoint = attachmentPoint;
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

    @Override
    public String toString() {
        return "AttachmentPointInfo{" +
                "mac=" + (mac == null ? "null":MACAddress.valueOf(mac)) +
                ", attchmentPoint='" + (attchmentPoint ==null ? "null":attchmentPoint)+ '\'' +
                '}';
    }
}
