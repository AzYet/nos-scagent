package com.nsfocus.scagent.command;


import com.google.gson.JsonObject;
import com.nsfocus.scagent.utility.HexString;


public class SecurityDevice {
	protected String deviceKey;
	protected String deviceName;
	protected int deviceTag;
	protected AttachmentPointInfo ingressAttachmentPointInfo;
	protected AttachmentPointInfo outgressAttachmentPointInfo;
	
	public SecurityDevice() {
		super();
	}

	public SecurityDevice(String deviceKey, String deviceName, int deviceTag,
			AttachmentPointInfo ingressAttachmentPointInfo, AttachmentPointInfo outgressAttachmentPointInfo) {
		super();
		this.deviceKey = deviceKey;
		this.deviceName = deviceName;
		this.deviceTag = deviceTag;
		this.ingressAttachmentPointInfo = ingressAttachmentPointInfo;
		this.outgressAttachmentPointInfo = outgressAttachmentPointInfo;
	}
	
	public void fromJson(JsonObject deviceNode){
		setDeviceKey(deviceNode.get("deviceid").getAsString());
		setDeviceTag(deviceNode.get("tag").getAsInt());
		if(deviceNode.has("ingress")){
			JsonObject inAPNode = deviceNode.get("ingress").getAsJsonObject();
			AttachmentPointInfo attachmentPointInfo = new AttachmentPointInfo();
			if(inAPNode.has("mac"))
				attachmentPointInfo.setMac(HexString.fromHexString(inAPNode.get("mac").getAsString()));
			if(inAPNode.has("ap"))
				attachmentPointInfo.setAttchmentPoint(inAPNode.get("ap").getAsString());
			setIngressAttachmentPointInfo(attachmentPointInfo);
		}
		if(deviceNode.has("outgress")){
			JsonObject outAPNode = deviceNode.get("outgress").getAsJsonObject();
			AttachmentPointInfo attachmentPointInfo = new AttachmentPointInfo();
			if(outAPNode.has("mac"))
				attachmentPointInfo.setMac(HexString.fromHexString(outAPNode.get("mac").getAsString()));
			if(outAPNode.has("ap"))
				attachmentPointInfo.setAttchmentPoint(outAPNode.get("ap").getAsString());
			setOutgressAttachmentPointInfo(attachmentPointInfo);
		}
	}

	public String getDeviceKey() {
		return deviceKey;
	}

	public void setDeviceKey(String deviceKey) {
		this.deviceKey = deviceKey;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public int getDeviceTag() {
		return deviceTag;
	}

	public void setDeviceTag(int deviceTag) {
		this.deviceTag = deviceTag;
	}

	public AttachmentPointInfo getIngressAttachmentPointInfo() {
		return ingressAttachmentPointInfo;
	}

	public void setIngressAttachmentPointInfo(AttachmentPointInfo ingressAttachmentPointInfo) {
		this.ingressAttachmentPointInfo = ingressAttachmentPointInfo;
	}

	public AttachmentPointInfo getOutgressAttachmentPointInfo() {
		return outgressAttachmentPointInfo;
	}

	public void setOutgressAttachmentPointInfo(AttachmentPointInfo outgressAttachmentPointInfo) {
		this.outgressAttachmentPointInfo = outgressAttachmentPointInfo;
	}

	@Override
	public String toString() {
		return "SecurityDevice [deviceKey=" + deviceKey + ", deviceName="
				+ deviceName + ", deviceTag=" + deviceTag + ", inAP="
				+ ingressAttachmentPointInfo + ", outAP=" + outgressAttachmentPointInfo + "]";
	}
	
}

