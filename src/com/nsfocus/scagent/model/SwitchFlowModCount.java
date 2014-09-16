package com.nsfocus.scagent.model;

import jp.co.nttdata.ofc.nos.ofp.common.Flow;



public class SwitchFlowModCount {
	private long dpid;
	private FlowMod flowModMessage;
	private int count;
	public SwitchFlowModCount(long dpid, FlowMod flowModMessage, int count) {
		super();
		this.dpid = dpid;
		this.flowModMessage = flowModMessage;
		this.count = count;
	}
	
	public int increaseCount(){
		return ++count;
	}
	public int decreaseCount(){
		return --count;
	}
	public long getDpid() {
		return dpid;
	}
	public void setDpid(long dpid) {
		this.dpid = dpid;
	}
	public FlowMod getFlowModMessage() {
		return flowModMessage;
	}
	public void setFlowModMessage(FlowMod flowModMessage) {
		this.flowModMessage = flowModMessage;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	
}
