package com.nsfocus.scagent.device;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.DpidPortPair;

public class DeviceManager {

	private static DeviceManager deviceManager = new DeviceManager();
	private Map<String,DpidPortPair> macDpidPortMap;
	
	private DeviceManager(){
		macDpidPortMap = new ConcurrentHashMap<String, DpidPortPair>();
	}
	
	public static DeviceManager getInstance(){
		return deviceManager;
	}

	public DpidPortPair findHostByMac(String mac){
		return macDpidPortMap.get(mac);
	}
	public Map<String, DpidPortPair> getMacDpidPortMap() {
		return macDpidPortMap;
	}
}
