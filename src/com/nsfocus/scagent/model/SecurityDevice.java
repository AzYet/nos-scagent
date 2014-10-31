package com.nsfocus.scagent.model;


import com.google.gson.JsonObject;
import com.nsfocus.scagent.utility.HexString;


public class SecurityDevice {
	protected String deviceKey;
	protected String deviceName;
	protected int deviceTag;
	protected AttachmentPointInfo ingressAttachmentPointInfo;
	protected AttachmentPointInfo egressAttachmentPointInfo;
	
	public SecurityDevice() {
		super();
	}

	public SecurityDevice(String deviceKey, String deviceName, int deviceTag,
			AttachmentPointInfo ingressAttachmentPointInfo, AttachmentPointInfo egressAttachmentPointInfo) {
		super();
		this.deviceKey = deviceKey;
		this.deviceName = deviceName;
		this.deviceTag = deviceTag;
		this.ingressAttachmentPointInfo = ingressAttachmentPointInfo;
		this.egressAttachmentPointInfo = egressAttachmentPointInfo;
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
		if(deviceNode.has("egress")){
			JsonObject outAPNode = deviceNode.get("egress").getAsJsonObject();
			AttachmentPointInfo attachmentPointInfo = new AttachmentPointInfo();
			if(outAPNode.has("mac"))
				attachmentPointInfo.setMac(HexString.fromHexString(outAPNode.get("mac").getAsString()));
			if(outAPNode.has("ap"))
				attachmentPointInfo.setAttchmentPoint(outAPNode.get("ap").getAsString());
			setEgressAttachmentPointInfo(attachmentPointInfo);
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

	public AttachmentPointInfo getEgressAttachmentPointInfo() {
		return egressAttachmentPointInfo;
	}

	public void setEgressAttachmentPointInfo(AttachmentPointInfo egressAttachmentPointInfo) {
		this.egressAttachmentPointInfo = egressAttachmentPointInfo;
	}

	@Override
	public String toString() {
		return "SecurityDevice [deviceKey=" + deviceKey + ", deviceName="
				+ deviceName + ", deviceTag=" + deviceTag + ", inAP="
				+ ingressAttachmentPointInfo + ", outAP=" + egressAttachmentPointInfo + "]";
	}
	
}

