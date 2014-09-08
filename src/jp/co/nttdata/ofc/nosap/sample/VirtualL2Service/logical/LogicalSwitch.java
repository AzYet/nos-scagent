package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.logical;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import jp.co.nttdata.ofc.common.except.NosSocketIOException;
import jp.co.nttdata.ofc.common.util.MacAddress;
import jp.co.nttdata.ofc.nos.api.IFlowModifier;
import jp.co.nttdata.ofc.nos.api.INOSApi;
import jp.co.nttdata.ofc.nos.api.IPacketOut;
import jp.co.nttdata.ofc.nos.api.except.ActionNotSupportedException;
import jp.co.nttdata.ofc.nos.api.except.ArgumentInvalidException;
import jp.co.nttdata.ofc.nos.api.except.OFSwitchNotFoundException;
import jp.co.nttdata.ofc.nos.api.except.SwitchPortNotFoundException;
import jp.co.nttdata.ofc.nos.api.vo.PhysicalPortVO;
import jp.co.nttdata.ofc.nos.api.vo.event.PacketInEventVO;
import jp.co.nttdata.ofc.nos.common.constant.OFPConstant.OFPort;
import jp.co.nttdata.ofc.nos.ofp.common.Flow;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.DpidPortPair;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.Utility;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.path.Path;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.path.PathManager;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.TopologyManager;

public class LogicalSwitch {
	public static final long	NONE_BUFFER_ID = 0x00000000FFFFFFFFL;
	public static final int	NONE_PORT = 0xFFFF;
	public static final int HIGH   = 10000;
	public static final int MIDDLE = 1000;
	public static final int LOW    = 100;
	public static final int DEFAULT_IDLE_TIMEOUT = 60 * 60;	// sec

	private String name;
	private int dpid;//added by liuwenmao
	private CopyOnWriteArrayList<DpidPortPair> dppl;
	private Hashtable<MacAddress, DpidPortPair> macTable;

	private PathManager pathManager;
	private TopologyManager topologyManager;
	
	private Map<Integer, PhysicalPortVO> physicalPorts;

	public Map<Integer, PhysicalPortVO> getPhysicalPorts() {
		return physicalPorts;
	}

	public LogicalSwitch(String name){
		//TODO
		this.dpid = Integer.parseInt(name);
		this.name = name;
		this.dppl = new CopyOnWriteArrayList<DpidPortPair>();
		this.macTable = new Hashtable<MacAddress, DpidPortPair>();
		this.physicalPorts = new HashMap<Integer, PhysicalPortVO>();
		this.pathManager = PathManager.getInstance();
		this.topologyManager = TopologyManager.getInstance();
	}

	public String getName(){
		return this.name;
	}

	public CopyOnWriteArrayList<DpidPortPair> getDpidPortPairList(){
		return this.dppl;
	}

	public DpidPortPair getHost(MacAddress mac){
		if(this.contains(mac)){
			return this.macTable.get(mac);
		}

		return null;
	}

	public void updateMacTable(INOSApi nosApi, MacAddress mac, long dpid, int port){
		this.macTable.put(mac, new DpidPortPair(dpid, port));

		//古い通信パスを削除
		pathManager.removeElements(mac);

		Flow flow = Utility.createFlow(port, null, mac, -1);
		long cookie = Utility.getNextCookie();
		Path path = new Path(cookie, flow.srcMacaddr, flow.dstMacaddr, this.name);

		//新しい通信パスを登録
		pathManager.addPath(path);

		this.addFlowEntry(nosApi, dpid, flow, -1, HIGH, DEFAULT_IDLE_TIMEOUT, 0, cookie/*, false*/);
	}

	public LinkedList<MacAddress> getKeys(DpidPortPair p){
		LinkedList<MacAddress> ret = new LinkedList<MacAddress>();
		for(Map.Entry<MacAddress, DpidPortPair> e : this.macTable.entrySet()){
			if(e.getValue().equals(p)){
				ret.add(e.getKey());
			}
		}
		return ret;
	}

	public boolean contains(DpidPortPair p){
		for(DpidPortPair pair : this.dppl){
			if((pair.getDpid() == p.getDpid()) && (pair.getPort() == p.getPort())){
				return true;
			}
		}
		return false;
	}

	public boolean contains(MacAddress mac){
		if(this.macTable.containsKey(mac)){
			return true;
		}
		return false;
	}

	public boolean add(DpidPortPair host){
		int index = 0;

		for(DpidPortPair p : this.dppl){
			if((host.getDpid() < p.getDpid())){
				System.out.println("lt");
				break;
			}
			else if((host.getDpid() == p.getDpid()) && (host.getPort() < p.getPort())){
				System.out.println("lt1");
				break;
			}
			index++;
		}

		this.dppl.add(index, host);

		return true;
	}

	public DpidPortPair remove(int index){
		return this.dppl.remove(index);
	}

	public boolean remove(DpidPortPair p){
		return this.dppl.remove(p);
	}

	public boolean addMac(MacAddress mac, DpidPortPair p){
		this.macTable.put(mac, p);
		System.out.println(this.name + " learn MAC Address " + mac.toString());

		return true;
	}

	public boolean removeMac(MacAddress mac){
		for(MacAddress m : this.macTable.keySet()){
			if(m.equals(mac)){
				this.macTable.remove(m);
				System.out.println(this.name + " forget MAC Address " + m.toString());

				pathManager.removeElements(mac);

				return true;
			}
		}

		return false;
	}

	public synchronized void packetIn(INOSApi nosApi, PacketInEventVO packetIn){
		int inPort = packetIn.flow.inPort;
		MacAddress srcMac = packetIn.flow.srcMacaddr;
		MacAddress dstMac = packetIn.flow.dstMacaddr;

		System.out.println("srcMac:" + srcMac.toString() + ", dstMac:" + dstMac.toString());

		DpidPortPair p1 = this.getHost(srcMac);
		DpidPortPair p2 = this.getHost(dstMac);

		if((p1 != null) && ((p1.getDpid() != packetIn.dpid) || (p1.getPort() != inPort))){
			this.removeMac(srcMac);

			Flow flow1 = Utility.createFlow(-1, srcMac, null, -1);
			Flow flow2 = Utility.createFlow(-1, null, srcMac, -1);

			for(long dpid : topologyManager.getDpidSet()){
				this.deleteFlowEntry(nosApi, dpid, flow1);
				this.deleteFlowEntry(nosApi, dpid, flow2);
			}

			pathManager.removeElements(srcMac);

			System.out.println("Delete FlowEntries related MACAddress " + srcMac.toString());
		}
		else if((p1 != null) && (p2 != null)){
			long cookie;
			Path path;
			Flow flow;
			if(p1.getDpid() == p2.getDpid()){
				flow = Utility.createFlow(inPort, srcMac, dstMac, -1);
				cookie = Utility.getNextCookie();
				path = new Path(cookie, flow.srcMacaddr, flow.dstMacaddr, this.name);

				pathManager.addPath(path);

				this.addFlowEntry(nosApi, p2.getDpid(), flow, p2.getPort(), LOW, DEFAULT_IDLE_TIMEOUT, 0, cookie);
				System.out.println(Utility.toDpidHexString(p1.getDpid()) + ":" + p1.getPort() + " -> " + Utility.toDpidHexString(p2.getDpid()) + ":" + p2.getPort());
			}
			else{
				LinkedList<Long> dpidList = topologyManager.getHoppedDpidList(p1.getDpid(), p2.getDpid());

				if(dpidList == null){
					System.out.println("dpidArray Error.");
					return;
				}

				long[] key = new long[2];
				int in, out;
				in = inPort;

				cookie = Utility.getNextCookie();
				path = new Path(cookie, srcMac, dstMac, this.name);
				for(int i = 0; i < dpidList.size(); i++){
					flow = Utility.createFlow(in, srcMac, dstMac, -1);
					if(i == dpidList.size() - 1){
						key[0] = key[1] = dpidList.get(i);
						out = p2.getPort();
					}
					else{
						key[0] = dpidList.get(i);
						key[1] = dpidList.get(i + 1);
						out = topologyManager.getForwardingTable().getPort(key);
						in = topologyManager.getCorrespondPort(key[0], key[1], out);
						path.add(key[0], out, key[1], in);
					}
					this.addFlowEntry(nosApi, key[0], flow, out, LOW, DEFAULT_IDLE_TIMEOUT, 0, cookie);
					System.out.println(Utility.toDpidHexString(key[0]) + ":" + out + " -> " + Utility.toDpidHexString(key[1]) + ":" + in);
				}
				pathManager.addPath(path);
			}
			this.packetOutToTable(nosApi, packetIn, inPort);
		}
		else{
			if((p1 != null) && (p2 == null)){
				System.out.println("[p1] " + Utility.toDpidHexString(p1.getDpid()) + ":" + p1.getPort());
			}
			if((p1 == null) && (p2 != null)){
				System.out.println("[p2] " + Utility.toDpidHexString(p2.getDpid()) + ":" + p2.getPort());
			}
			this.packetOutToAll(nosApi, packetIn, inPort);
		}
		synchronized(this.dppl){
			for(DpidPortPair p : this.dppl){
				if((p.getDpid() == packetIn.dpid) && (p.getPort() == packetIn.inPort) && !this.macTable.containsKey(srcMac)){
					this.addMac(srcMac, p);
					Flow flow = Utility.createFlow(p.getPort(), null, srcMac, -1);
					long cookie = Utility.getNextCookie();
					Path path = new Path(cookie, flow.srcMacaddr, flow.dstMacaddr, this.name);
					pathManager.addPath(path);
					this.addFlowEntry(nosApi, packetIn.dpid, flow, -1, HIGH, 0, 0, cookie/*, false*/);
				}
			}
		}
	}

	public String show(){
		String ret = "";
		int i = 0;
		for(DpidPortPair p : this.dppl){
			ret += p.show(i++);
		}
		return ret;
	}

	private long deleteFlowEntry(INOSApi nosApi, long dpid, Flow flow){
		long ret = 0L;
		try {
			IFlowModifier imodifier = nosApi.createFlowModifierInstance(dpid, flow);
			imodifier.setBufferId(LogicalSwitch.NONE_BUFFER_ID);
			imodifier.setDeleteCommand();
			ret = imodifier.send();
		} catch (OFSwitchNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NosSocketIOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		return ret;
	}

	private long addFlowEntry(INOSApi nosApi, long dpid, Flow flow,
			int outPort, int priority, int idleTimeout, int hardTimeout, long cookie/*, boolean mirrored*/){
		long ret = 0L;
		try {
			IFlowModifier imodifier = nosApi.createFlowModifierInstance(dpid, flow);
			imodifier.setBufferId(LogicalSwitch.NONE_BUFFER_ID);
			imodifier.setInPort(LogicalSwitch.NONE_PORT);
			imodifier.setAddCommand();
			try {
				if(outPort >= 0){
					imodifier.addOutputAction(outPort, 0);
				}
			} catch (ActionNotSupportedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SwitchPortNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ArgumentInvalidException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			imodifier.setPriority(priority);

			if(idleTimeout > 0){
				imodifier.setIdleTimeoutSec(idleTimeout);
			}

			if(hardTimeout > 0){
				imodifier.setHardTimeoutSec(hardTimeout);
			}

			imodifier.setCookie(cookie);

			ret = imodifier.send();

		} catch (OFSwitchNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NosSocketIOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		return ret;
	}

	// 同一SWに属する受信ポート以外の全てのポートに転送する
	private long packetOutToAll(INOSApi nosApi, PacketInEventVO packetIn, int inPort){

		long ret = 0L;

		IPacketOut iOut;
		//for(DpidPortPair p : this.dppl){
		for(Entry<Integer, PhysicalPortVO> entry: this.physicalPorts.entrySet()){
			int port = entry.getKey();
			if((this.dpid == packetIn.dpid && port == packetIn.inPort) || port == OFPort.LOCAL){
			//if((p.getDpid() == packetIn.dpid) && (p.getPort() == packetIn.inPort)){
				continue;
			}

			try {
				//iOut = nosApi.createPacketOutInstance(p.getDpid(), LogicalSwitch.checkPacketOutData(packetIn.data));
				//iOut.addOutputAction(p.getPort());
				iOut = nosApi.createPacketOutInstance(this.dpid, LogicalSwitch.checkPacketOutData(packetIn.data));
				iOut.setBufferId(LogicalSwitch.NONE_BUFFER_ID);
				iOut.setInPort(LogicalSwitch.NONE_PORT);
				iOut.addOutputAction(port);

				ret += iOut.send();
				//System.out.println("Output to " + Utility.toDpidHexString(p.getDpid()) + ":" + p.getPort() + ".");
				System.out.println("Output to " + Utility.toDpidHexString(this.dpid) + ":" + port + ".");

			} catch (OFSwitchNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ArgumentInvalidException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ActionNotSupportedException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (SwitchPortNotFoundException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (NosSocketIOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}

		return ret;
	}

	// OFSのフローテーブルに従って転送する
	private long packetOutToTable(INOSApi nosApi, PacketInEventVO packetIn, int inPort){
		long ret = 0L;
		IPacketOut iPacketOut;
		try {
			iPacketOut = nosApi.createPacketOutInstance(packetIn.dpid, LogicalSwitch.checkPacketOutData(packetIn.data));
			iPacketOut.setBufferId(LogicalSwitch.NONE_BUFFER_ID);
			iPacketOut.setInPort(LogicalSwitch.NONE_PORT);
			iPacketOut.addOutputAction(OFPort.DEPEND_ON_TABLE);
			System.out.println("PacketOut to TABLE.(" + Utility.toDpidHexString(packetIn.dpid) + ")");
			ret = iPacketOut.send();
		} catch (OFSwitchNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (ArgumentInvalidException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (SwitchPortNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (ActionNotSupportedException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (NosSocketIOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		return ret;
	}

	private static byte[] checkPacketOutData(byte[] data) {
		int dataSize = data.length - 4;
		byte[] packetOutData = null;
		byte[] type = new byte[2];
		byte[] realType = new byte[2];
		packetOutData = new byte[dataSize];
		// Vlan Type 確認
		System.arraycopy(data, 12, type, 0, 2);
		ByteBuffer wb = ByteBuffer.wrap(type);
		short packetType = wb.getShort();

		if ((packetType & 0xFFFF) != 0x8100) {
			return data;
		}
		// 実際eth type
		System.arraycopy(data, 16, realType, 0, 2);
		//data
		System.arraycopy(data, 0, packetOutData, 0, 12);
		//type change
		System.arraycopy(data, 16, packetOutData, 12, 2);
		// data copy
		System.arraycopy(data, 18, packetOutData, 14, dataSize - 14);
		return packetOutData;
	}
}