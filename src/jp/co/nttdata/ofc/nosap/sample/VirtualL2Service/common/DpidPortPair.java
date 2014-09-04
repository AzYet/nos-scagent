package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common;

import jp.co.nttdata.ofc.common.util.MacAddress;

public class DpidPortPair {
	private long dpid;
	private int port;

	public DpidPortPair(long dpid, int port, MacAddress mac){
		this.dpid = dpid;
		this.port = port;
	}

	public DpidPortPair(long dpid, int port){
		this(dpid, port, null);
	}

	public long getDpid(){
		return this.dpid;
	}

	public int getPort(){
		return this.port;
	}

	public boolean equals(DpidPortPair p){
		if(this.dpid != p.dpid){
			return false;
		}
		if(this.port != p.port){
			return false;
		}

		return true;
	}

	public String show(){
		return Utility.toDpidHexString(dpid) + ":" + Integer.toString(this.port);
	}

	public String show(int id){
		return Utility.formatA(id) + Utility.toDpidHexString(dpid)
			+ "   " + Utility.formatB(this.port) + "\n";
	}

}
