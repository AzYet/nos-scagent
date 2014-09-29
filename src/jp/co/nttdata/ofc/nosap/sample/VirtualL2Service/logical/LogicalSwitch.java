package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.logical;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.nsfocus.scagent.device.DeviceManager;
import com.nsfocus.scagent.utility.Cypher;
import com.nsfocus.scagent.utility.HexString;
import jp.co.nttdata.ofc.common.except.NosSocketIOException;
import jp.co.nttdata.ofc.common.util.MacAddress;
import jp.co.nttdata.ofc.common.util.NetworkInputByteBuffer;
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

import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.Trunk;
import jp.co.nttdata.ofc.protocol.packet.EthernetPDU;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicalSwitch {
	public static final long	NONE_BUFFER_ID = 0x00000000FFFFFFFFL;
	public static final int	NONE_PORT = 0xFFFF;
	public static final int HIGH   = 10000;
	public static final int MIDDLE = 1000;
	public static final int LOW    = 100;
	public static final int DEFAULT_IDLE_TIMEOUT = 60 * 60;	// sec

	private String name;

    public long getDpid() {
        return dpid;
    }

    private long dpid;//added by liuwenmao
	private CopyOnWriteArrayList<DpidPortPair> dppl;
	private Hashtable<MacAddress, DpidPortPair> macTable;

	private PathManager pathManager;
	private TopologyManager topologyManager;
	
	private Map<Integer, PhysicalPortVO> physicalPorts;
	
	private static Logger logger = LoggerFactory.getLogger(LogicalSwitch.class);
	
	public Map<Integer, PhysicalPortVO> getPhysicalPorts() {
		return physicalPorts;
	}

	public LogicalSwitch(String name){
		//TODO
		this.dpid = Long.parseLong(name);
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

		System.out.println(packetIn.dpid +":"+packetIn.inPort+"srcMac:" + srcMac.toString() + ", dstMac:" + dstMac.toString()+"\n etherType: "+ Utility.toDpidHexString(packetIn.flow.etherType));

		DpidPortPair p1 = this.getHost(srcMac);
		DpidPortPair p2 = this.getHost(dstMac);
		
		//add by PC_Chen
		//to prevent broadcast storm 
		
		/*if(isMultiCastAddr(dstMac) && antiStorm(packetIn)){
			logger.info("droped a multicast pkt, dpid: {}, inPort: {} srcMac="+ packetIn.flow.srcMacaddr +
					" dstMac=" + packetIn.flow.dstMacaddr,packetIn.dpid,packetIn.inPort);
			return;
		}*/
		
		if((p1 != null) && ((p1.getDpid() != packetIn.dpid) || (p1.getPort() != inPort))) {
            //PC_Chen: TODO: if this packet was broadcast, just ignore it
            /*if (unicastPacketMulticast.containsKey(Cypher.getMD5(packetIn.data))) {
                logger.info("ignore a multicast unicast pkt, {}-{} srcMac="+ packetIn.flow.srcMacaddr +
                        " dstMac=" + packetIn.flow.dstMacaddr+" "+packetIn.flow.etherType ,packetIn.dpid,packetIn.inPort);
                return;
            }*/
            this.removeMac(srcMac);

            Flow flow1 = Utility.createFlow(-1, srcMac, null, -1);
            Flow flow2 = Utility.createFlow(-1, null, srcMac, -1);

            for (long dpid : topologyManager.getDpidSet()) {
                this.deleteFlowEntry(nosApi, dpid, flow1);
                this.deleteFlowEntry(nosApi, dpid, flow2);
            }

            pathManager.removeElements(srcMac);

            System.out.println("Delete FlowEntries related MACAddress " + srcMac.toString());
        } else if((p1 != null) && (p2 != null)){
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

            //PC_Chen
            //if it's a unicast packet, record it
            /*if (!isMultiCastAddr(packetIn.flow.dstMacaddr)) {
                String hash = getMD5(packetIn.data);
                if (!unicastPacketMulticast.containsKey(hash)) {
                    unicastPacketMulticast.put(hash, System.currentTimeMillis());
                }
            }*/
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
	
	private boolean isMultiCastAddr(MacAddress dstMac) {
		if((dstMac.toLong()&0x100000000L) == 0){
			return false;
		}
		return true;
	}

	private static final int BCAST_INTERVAL = 1000;
	private static final int RECYCLE_INTERVAL = 300*1000;
	//dpidPktPortTimeMap, <dpid, <pktHash, port&time>>
	static Map<Long, HashMap<String, Long[]>> dpidPktPortTimeMap = new ConcurrentHashMap<Long, HashMap<String, Long[]>>();
    static Map<String,Long> unicastPacketMulticast = new ConcurrentHashMap<String, Long>();
    static {
        Timer timer = new Timer("recycle task");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                for (Entry<Long, HashMap<String, Long[]>> stringHashMap : dpidPktPortTimeMap.entrySet()) {
                    for (Entry<String, Long[]> portTime : stringHashMap.getValue().entrySet()) {
                        long time = System.currentTimeMillis() - portTime.getValue()[1];
                        if (time > RECYCLE_INTERVAL) {
                            stringHashMap.getValue().remove(portTime.getKey());
                        }
                    }
                }
                for (Entry<String, Long> stringLongEntry : unicastPacketMulticast.entrySet()) {
                    long interval = System.currentTimeMillis() - stringLongEntry.getValue();
                    if (interval > RECYCLE_INTERVAL) {
                        unicastPacketMulticast.remove(stringLongEntry.getKey());
                    }
                }
            }
        },10000,RECYCLE_INTERVAL);
    }

	private boolean antiStorm(PacketInEventVO packetIn) {
		String hash = getMD5(packetIn.data);
		long currentTimeMillis = System.currentTimeMillis();
		if(dpidPktPortTimeMap.containsKey(packetIn.dpid)){
			HashMap<String, Long[]> hashPortTimeMap = dpidPktPortTimeMap.get(packetIn.dpid);
			if(hashPortTimeMap.containsKey(hash)){ // second time the packet enters this dpid
				Long[] longs = hashPortTimeMap.get(hash);
				if(currentTimeMillis - longs[1] > BCAST_INTERVAL){ // exceed the threshold
					longs[0] = (long) packetIn.inPort;
					longs[1]= currentTimeMillis;
					return false;
				}else if(packetIn.inPort == longs[0]){ // lighter than threshold, via the same port 
					longs[1]= currentTimeMillis;
					return false;
				}else{ // lighter than threshold,  via different port 
					//ignore this packet, no updating the time and Port
					return true;
				}
			}else{ // packet reaches dpid the first time
				hashPortTimeMap.put(hash, new Long[] {(long)packetIn.inPort,currentTimeMillis});
				return false;
			}
		}else { //dpid receives first bcast packet 
			HashMap<String, Long[]> tempMap = new HashMap<String , Long[]>();
			tempMap.put(hash, new Long[] {(long)packetIn.inPort,currentTimeMillis});
			dpidPktPortTimeMap.put(packetIn.dpid, tempMap);
			return false;
		}
	}

	public static String getMD5(byte[] source) {  
        String s = null;  
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',  
                'a', 'b', 'c', 'd', 'e', 'f' };//
        try {  
            java.security.MessageDigest md = java.security.MessageDigest  
                    .getInstance("MD5");  
            md.update(source);  
            byte tmp[] = md.digest();//
            //
            char str[] = new char[16 * 2];//
            //
            int k = 0;//
            for (int i = 0; i < 16; i++) {//
                byte byte0 = tmp[i];//
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];//
                //
                str[k++] = hexDigits[byte0 & 0xf];//
  
            }  
            s = new String(str);//
  
        } catch (NoSuchAlgorithmException e) {  
            // TODO Auto-generated catch block  
            e.printStackTrace();  
        }  
        return s;  
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
    static class PacketStats{
        String hash;
        Long dpid;
        int port;
        long timestamp;

        PacketStats(String hash, Long dpid, int port, long timestamp) {
            this.hash = hash;
            this.dpid = dpid;
            this.port = port;
            this.timestamp = timestamp;
        }
    }
    static Map<String,PacketStats> multiCastPkts = new ConcurrentHashMap<String, PacketStats>();
	// 同一SWに属する受信ポート以外の全てのポートに転送する

    public Map<Long, Set<Integer>> getTrunkPortsMap() {
        // collect trunk ports
        Map<Long,Set<Integer>> trunkMap = new HashMap<Long, Set<Integer>>();
        for (Trunk trunk : TopologyManager.getInstance().getTrunkList()) {
            long dpid0 = trunk.getDpidPair()[0];
            long dpid1 = trunk.getDpidPair()[1];
            if (!trunkMap.containsKey(dpid0)) {
                trunkMap.put(dpid0, new HashSet<Integer>());
            }
            if (!trunkMap.containsKey(dpid1)) {
                trunkMap.put(dpid1, new HashSet<Integer>());
            }
            trunkMap.get(dpid0).addAll(trunk.getFirstPortList());
            trunkMap.get(dpid1).addAll(trunk.getSecondPortList());
        }
        logger.info("trunks: {}.",trunkMap);
        return trunkMap;
    }

	private long packetOutToAll(INOSApi nosApi, PacketInEventVO packetIn, int inPort){

        Map<Long, Set<Integer>> trunkMap = getTrunkPortsMap();
        String macStr = packetIn.flow.srcMacaddr.toString().toUpperCase();
        Map<String, DpidPortPair> macDpidPortMap = DeviceManager.getInstance().getMacDpidPortMap();
        DpidPortPair p = new DpidPortPair(packetIn.dpid, packetIn.inPort);
        //PC_Chen: manage host
        if (!macDpidPortMap.containsKey(macStr.toString().toUpperCase())) {
            if(!trunkMap.get(p.getDpid()).contains(p.getPort()))
                macDpidPortMap.put(macStr.toString().toUpperCase(), p);
        }
        DpidPortPair dpp = DeviceManager.getInstance().findHostByMac(macStr);
        long ret = 0L;
        if (dpp.getDpid() != packetIn.dpid || dpp.getPort() != packetIn.inPort) {   // packet not from the origin ap
            logger.info("packet not from origin ap:"+packetIn.dpid+":"+packetIn.inPort+","+packetIn.flow.srcMacaddr+"->"+packetIn.flow.dstMacaddr+" etherType: "+packetIn.flow.etherType);
            return ret;
        }
        long currentTimeMillis = System.currentTimeMillis();
        String hash = getMD5(packetIn.data);
        if(multiCastPkts.containsKey(hash)) {       //recevied the same pkt before, maybe new packet or looped

            PacketStats packetStats = multiCastPkts.get(hash);
            if (packetIn.dpid == packetStats.dpid && packetIn.inPort == packetStats.port){    //from the same point
                if( currentTimeMillis - packetStats.timestamp < BCAST_INTERVAL) {   //2 packets too close
                    logger.info("same packet too close");
                    return ret;
                } else {    //  prepare to broadcast it
                    logger.info("cast same packet");
                    packetStats.timestamp = currentTimeMillis;
                }
            }else{  //from a different port, host migrated, multicast it
                logger.info("host migrated");
                multiCastPkts.get(hash).timestamp = currentTimeMillis;
                multiCastPkts.get(hash).dpid = packetIn.dpid;
                multiCastPkts.get(hash).port = packetIn.inPort;
            }
        }else { //new packet met
            logger.info("new packet");
            multiCastPkts.put(hash, new PacketStats(hash, packetIn.dpid, packetIn.inPort, currentTimeMillis));
        }
        // packet_out to all dpid ports, not including inPort , trunk port
        for (LogicalSwitch logicalSwitch : TopologyManager.getInstance().getSwitchList()) {
            IPacketOut iOut;
            String portsStr = "";
            for (PhysicalPortVO physicalPort : logicalSwitch.physicalPorts.values()) {
                portsStr += " "+physicalPort.portNo;
            }
            logger.info("dpid: {}, ports: {}",logicalSwitch.dpid,portsStr);
            for (Entry<Integer, PhysicalPortVO> portVOEntry : logicalSwitch.physicalPorts.entrySet()) {
                int port = portVOEntry.getKey();
                if((logicalSwitch.dpid == packetIn.dpid && port == packetIn.inPort) || port == OFPort.LOCAL
                        || trunkMap.containsKey(logicalSwitch.getDpid()) && trunkMap.get(logicalSwitch.getDpid()).contains(port)){
                    continue;
                }
                try {
                    iOut = nosApi.createPacketOutInstance(logicalSwitch.dpid, LogicalSwitch.checkPacketOutData(packetIn.data));
                    iOut.setBufferId(LogicalSwitch.NONE_BUFFER_ID);
                    iOut.setInPort(LogicalSwitch.NONE_PORT);
                    iOut.addOutputAction(port);
                    ret += iOut.send();
                    logger.info("sent to {}:{}",logicalSwitch.getDpid(),port);
                } catch (OFSwitchNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ArgumentInvalidException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (ActionNotSupportedException e) {
                    // TODO ??????????????????????catch ????????????
                    e.printStackTrace();
                } catch (SwitchPortNotFoundException e) {
                    // TODO ??????????????????????catch ????????????
                    e.printStackTrace();
                } catch (NosSocketIOException e) {
                    // TODO ??????????????????????catch ????????????
                    e.printStackTrace();
                }

            }
        }
		return ret;
	}// 同一SWに属する受信ポート以外の全てのポートに転送する
	private long packetOutToAll1(INOSApi nosApi, PacketInEventVO packetIn, int inPort){
        EthernetPDU eth = new EthernetPDU();
        eth.parse(new NetworkInputByteBuffer(packetIn.data));
        System.out.println("packetOut to all on dpid:"+packetIn.dpid+":"+packetIn.inPort+","+eth.srcMacaddr+"->"+eth.dstMacaddr+" etherType: "+eth.etherType);
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
//				System.out.println("Output to " + Utility.toDpidHexString(this.dpid) + ":" + port + ".");

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
//			System.out.println("PacketOut to TABLE.(" + Utility.toDpidHexString(packetIn.dpid) + ")");
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
