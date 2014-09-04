package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.path;

import java.util.LinkedList;

import jp.co.nttdata.ofc.common.util.MacAddress;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.DpidPortPair;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.logical.LogicalSwitch;

/**
 * OFSに設定済みのホスト対ホストの通信パスを格納するクラス。通信パスはSrcMac, DstMac, 及び, 通過するOFSとポートの組み合わせで表す。
 * また、この通信パスが所属する論理スイッチ({@link LogicalSwitch})の名前を保持することで、通信パスと論理スイッチを結びつけることができる。
 * @author kikutak
 *
 */
public class Path {
	private long cookie;
	private MacAddress[] macs;
	private String swName;
	private LinkedList<DpidPortPair[]> connections;

	public Path(long cookie, MacAddress mac1, MacAddress mac2, String swName){
		this.cookie = cookie;
		this.macs = new MacAddress[2];
		this.macs[0] = mac1;
		this.macs[1] = mac2;
		this.swName = swName;
		this.connections = new LinkedList<DpidPortPair[]>();
	}

	public long getCookie(){
		return this.cookie;
	}

	public MacAddress[] getMacs(){
		return this.macs;
	}

	public String getSwName(){
		return this.swName;
	}

	public LinkedList<DpidPortPair[]> getConnections(){
		return this.connections;
	}

	public boolean add(DpidPortPair[] connection){
		return this.connections.add(connection);
	}

	public boolean add(long dpid1, int port1, long dpid2, int port2){
		DpidPortPair[] connection = new DpidPortPair[2];
		connection[0] = new DpidPortPair(dpid1, port1);
		connection[1] = new DpidPortPair(dpid2, port2);

		return this.add(connection);
	}

	public boolean contains(MacAddress mac){
		if((mac.equals(macs[0])) || (mac.equals(macs[1]))){
			return true;
		}
		else{
			return false;
		}
	}

	public boolean contains(DpidPortPair p1, DpidPortPair p2){
		for(DpidPortPair[] connection : this.connections){
			if((connection[0].equals(p1)) && (connection[1].equals(p2))){
				return true;
			}
			if((connection[0].equals(p2)) && (connection[1].equals(p1))){
				return true;
			}
		}

		return false;
	}

	public boolean contains(long dpid1, int port1, long dpid2, int port2){
		return this.contains(new DpidPortPair(dpid1, port1), new DpidPortPair(dpid2, port2));
	}

	public boolean contains(long dpid1, long dpid2){
		boolean flag1, flag2;
		flag1 = flag2 = false;
		for(DpidPortPair[] connection : this.connections){
			if((connection[0].getDpid() == dpid1) || (connection[1].getDpid() == dpid1)){
				flag1 = true;
			}
			if((connection[0].getDpid() == dpid2) || (connection[1].getDpid() == dpid2)){
				flag2 = true;
			}
		}

		return flag1 && flag2;
	}

	public String show(){
		String ret = Long.toString(this.cookie);
		if(this.macs[0] != null){
			ret += "," + this.macs[0].toString();
		}
		else{
			ret += ",null";
		}
		if(this.macs[1] != null){
			ret += "," + this.macs[1].toString();
		}
		else{
			ret += ",null";
		}
		ret += "," + this.swName;
		for(DpidPortPair[] p : this.connections){
			ret += "," + p[0].show() + "-" + p[1].show();
		}
		ret += "\n";

		return ret;
	}
}
