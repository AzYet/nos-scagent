package jp.co.nttdata.ofc.controller;

import java.util.HashMap;

public class MacTable {
	
	private static MacTable  instance = new MacTable();
	
	private MacTable(){
		
	}
	
	public static MacTable getInstance(){
		return instance;
	}
	

	public HashMap<Long, Integer> table = new HashMap<Long, Integer>();
	
	synchronized public void learningMacAddr(long macAddr, int portNo){
		if(table.containsKey(new Long(macAddr)))
			return;
		System.out.println("table items "+table.size());
		System.out.println("learning item "+Long.toHexString(macAddr));
		table.put(new Long(macAddr), new Integer(portNo));
	}

	synchronized public Integer searchEntry(long macAddr){
		return table.get(new Long(macAddr));
	}
	

}
