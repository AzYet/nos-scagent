package jp.co.nttdata.ofc.controller;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import com.nsfocus.scagent.manager.SCAgentDriver;
import jp.co.nttdata.ofc.common.except.NosSocketIOException;
import jp.co.nttdata.ofc.common.util.MacAddress;
import jp.co.nttdata.ofc.common.util.NetworkInputByteBuffer;
import jp.co.nttdata.ofc.common.util.Translator;
import jp.co.nttdata.ofc.nos.api.IFlowModifier;
import jp.co.nttdata.ofc.nos.api.INOSApi;
import jp.co.nttdata.ofc.nos.api.INOSApplication;
import jp.co.nttdata.ofc.nos.api.except.OFSwitchNotFoundException;
import jp.co.nttdata.ofc.nos.api.vo.PhysicalPortVO;
import jp.co.nttdata.ofc.nos.api.vo.event.BarrierReplyEventVO;
import jp.co.nttdata.ofc.nos.api.vo.event.DatapathJoinEventVO;
import jp.co.nttdata.ofc.nos.api.vo.event.DatapathLeaveEventVO;
import jp.co.nttdata.ofc.nos.api.vo.event.EchoReplyEventVO;
import jp.co.nttdata.ofc.nos.api.vo.event.ErrorRecvEventVO;
import jp.co.nttdata.ofc.nos.api.vo.event.FlowRemovedEventVO;
import jp.co.nttdata.ofc.nos.api.vo.event.GetConfigReplyEventVO;
import jp.co.nttdata.ofc.nos.api.vo.event.PacketInEventVO;
import jp.co.nttdata.ofc.nos.api.vo.event.PortStatusEventVO;
import jp.co.nttdata.ofc.nos.api.vo.event.QueueGetConfigReplyEventVO;
import jp.co.nttdata.ofc.nos.common.constant.OFPConstant;
import jp.co.nttdata.ofc.nos.ofp.common.Flow;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.DpidPortPair;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.NosFactory;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.Utility;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.exception.NosapFormatErrorException;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.logical.LogicalSwitch;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.Edge;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.LldpPacketGenerator;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.TopologyManager;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.Trunk;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.webservice.Operator;
import jp.co.nttdata.ofc.protocol.packet.EthernetPDU;
import jp.co.nttdata.ofc.protocol.packet.LldpPDU;
import jp.co.nttdata.ofc.protocol.packet.TlvPDU;
import jp.co.nttdata.ofc.protocol.packet.LldpPDU.TlvType;

import com.nsfocus.scagent.device.DeviceManager;

public class SCAgentApplication implements INOSApplication{
	private TopologyManager topologyManager;
	private MacAddress lldpMacAddress;
	private Operator operator;
	Map<String, DpidPortPair> macDpidPortMap = DeviceManager.getInstance().getMacDpidPortMap();
    private SCAgentDriver scAgentDriver = SCAgentDriver.getInstance();

    public SCAgentApplication()
	{
		this.topologyManager = TopologyManager.getInstance();
		this.lldpMacAddress = NosFactory.createMacAddress(LldpPacketGenerator.LLDP_MULTICAST_ADDR);
		this.operator = new Operator();
	}

	@Override
	public void datapathJoinEvent(INOSApi nosApi,
			DatapathJoinEventVO datapathJoin) {
		/*
		 * OFSがControllerに接続してきたときの処理を記述する。
		 * ここでは、OFSの初期化処理として、OFSのFlowTableのFlowEntryを全て削除する処理を記述する。
		 */
        scAgentDriver.setNosApi(nosApi);

		if(this.topologyManager.getDpidSet().contains(datapathJoin.dpid)){
			System.out.println(Utility.toDpidHexString(datapathJoin.dpid) + " has already joined.");
			return;
		}

		for(PhysicalPortVO port : datapathJoin.physiPorts){
			System.out.println("portName: "+port.portName);
			System.out.println("portNo: "+port.portNo);
			System.out.println("portMAC: "+port.macaddr);

			String dpid = String.valueOf(datapathJoin.dpid);
			LogicalSwitch srcSw = topologyManager.getSwitchByName(dpid);
			if(srcSw == null){
				srcSw = new LogicalSwitch(dpid); 
				topologyManager.getSwitchList().add(srcSw);
			}
			srcSw.getPhysicalPorts().put(new Integer(port.portNo), port);
		}
		
		Flow flow = new Flow();		// マッチングルールを作成する。
		flow.setAllWildCards();		// 全てANYで作成し、全てのFlowEntryがマッチするようにする。

		IFlowModifier imodifier;	// OFSに送信するFlowModメッセージのひな形を用意する。
		try {
			imodifier = nosApi.createFlowModifierInstance(datapathJoin.dpid, flow);		// dpid向けのFlowModを作成する。
			imodifier.setDeleteCommand();		// FlowEntryを削除するため、DELETEコマンドを設定する。
			imodifier.send();		// FlowModメッセージを送信する。
		} catch (OFSwitchNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (NosSocketIOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		System.out.println(Utility.toDpidHexString(datapathJoin.dpid) + ": FlowTable is cleared.");
		topologyManager.addDpid(new Long(datapathJoin.dpid));
		System.out.println(Utility.toDpidHexString(datapathJoin.dpid) + " joined.");
		

/*		IFlowModifier flowModifier = null;
		try {
			//flow.wildCards = OFWildCard.ALL & ~ OFWildCard.SRC_PORT & ~ OFWildCard.DST_MACADDR;
			//flow.dstIpaddr = new IpAddress("101.0.0.0");
			//flow.srcPort=233;
			//flow.setIdentificationWithDstIpNetmask(24);

			flow = new Flow();
			flow.wildCards = OFWildCard.ALL & ~ OFWildCard.DST_MACADDR;
			flow.dstMacaddr = new MacAddress(MacAddress.BROADCAST_MAC_ADDR);
			//flow.wildCards = OFWildCard.ALL ;
			
			flowModifier = nosApi.createFlowModifierInstance(datapathJoin.dpid, flow);
			flowModifier.setAddCommand();
			flowModifier.setBufferId(-1);
			flowModifier.addOutputAction(OFPort.ALL, 0);
			flowModifier.send();
			
			System.out.println("Sent broadcast flow");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}*/
	}

	@Override
	public void datapathLeaveEvent(INOSApi nosApi,
			DatapathLeaveEventVO datapathLeave) {
		// TODO 自動生成されたメソッド・スタブ
		topologyManager.removeDpid(new Long(datapathLeave.dpid));

		String dpid = String.valueOf(datapathLeave.dpid);
		LogicalSwitch srcSw = topologyManager.getSwitchByName(dpid);
		if(srcSw != null){
			topologyManager.getSwitchList().remove(srcSw);	
		}	
		
		System.out.println(Utility.toDpidHexString(datapathLeave.dpid) + " left.");
	}

	@Override
	public void packetInEvent(INOSApi nosApi, PacketInEventVO packetIn) {

        scAgentDriver.setNosApi(nosApi);
        TopologyManager topologyManager = TopologyManager.getInstance();

		try{
		
		

		// TODO 自動生成されたメソッド・スタブ
		if(packetIn.flow.dstMacaddr.equals(this.lldpMacAddress)){
			//System.out.println("LLDP packet in..................");
			if(TopologyManager.USE_LLDP){
                long dstDpid = packetIn.dpid;
                int dstPort = packetIn.inPort;

				byte[] data = packetIn.data;

				EthernetPDU pdu = new EthernetPDU();

				if(!pdu.parse(new NetworkInputByteBuffer(data))){
					System.err.println("Packet from " + Utility.toDpidHexString(dstDpid) + " - " + dstPort + " is not LLDP.");
				} else {
					LldpPDU lldp = (LldpPDU)pdu.next;
					this.checkLldpPacketFormat(lldp);

					long srcDpid = Translator.byteToLong(lldp.optional.get(0).value, 4);
					int srcPort = (int)pdu.srcMacaddr.toLong();

					this.checkTopology(srcDpid, srcPort, dstDpid, dstPort);


					DpidPortPair host = topologyManager.findDpidPortPair(srcDpid, srcPort);
					if(host == null){
						host = new DpidPortPair(srcDpid, srcPort);
						topologyManager.addHost(host);
					}
					String dpid = String.valueOf(srcDpid);
					LogicalSwitch srcSw = topologyManager.getSwitchByName(dpid);
					if(srcSw == null){
						srcSw = new LogicalSwitch(dpid); 
						topologyManager.getSwitchList().add(srcSw);
					}				
					if(!srcSw.contains(host))
						srcSw.add(host);	
					
					System.out.println(" "+srcDpid + "/"+srcPort +"-->"+dstDpid+"/"+dstPort);
					
					for(LogicalSwitch sw: topologyManager.getSwitchList()){
						System.out.print("switch [" + sw.getName()+"]:");
						for(DpidPortPair pair: sw.getDpidPortPairList()){
							if(pair != null)
								System.out.print("("+pair.getDpid() + "/"+pair.getPort()+")\t");
						}
						System.out.println("");
					}
					System.out.println("================================================");
				}
			}
			else{
				System.out.println("LLDP packet is detected, but OFC does not work LLDP mode.");
			}
		} else {
            if (packetIn.inPort == OFPConstant.OFPort.NONE) {
                System.out.println("ignoring packet from port any on dpid:"+packetIn.dpid);
                return;
            }
            System.out.println("\npacketInEvent has invoked from " + Utility.toDpidHexString(packetIn.dpid)
                    + ":" + packetIn.inPort+":"+packetIn.flow.srcMacaddr+"--"+packetIn.flow.dstMacaddr + "etherType: "+packetIn.flow.etherType+ ".");
            DpidPortPair host = topologyManager.findDpidPortPair(packetIn.dpid, packetIn.inPort);
            if (host == null) {
                host = new DpidPortPair(packetIn.dpid, packetIn.inPort);
                topologyManager.addHost(host);
            }
            String dpid = String.valueOf(packetIn.dpid);
            LogicalSwitch srcSw = topologyManager.getSwitchByName(dpid);
            if (srcSw == null) {
                srcSw = new LogicalSwitch(dpid);
                topologyManager.getSwitchList().add(srcSw);
            }

            if (!srcSw.contains(host))
                srcSw.add(host);

            MacAddress srcMac = packetIn.flow.srcMacaddr;
            if (!srcSw.contains(srcMac))
                srcSw.addMac(srcMac, host);


            DpidPortPair p = new DpidPortPair(packetIn.dpid, packetIn.inPort);
            //PC_Chen
            if (!macDpidPortMap.containsKey(srcMac.toString())) {
                macDpidPortMap.put(srcMac.toString(), p);
            }

            scAgentDriver.handleIncomingPackets(nosApi, packetIn);

            for (LogicalSwitch sw : topologyManager.getSwitchList()) {
                if (sw.contains(p)) {
                    sw.packetIn(nosApi, packetIn);
                    return;
                }
            }
            System.out.println("No switch is matched, " + packetIn.dpid + "/" + packetIn.inPort);
        }
        }catch(Exception e){
			System.err.println("Error during packet in....");
			e.printStackTrace();
		}
		
		//System.out.println("packet in end----------------------------------------");
		
	}

	@Override
	public void echoReplyEvent(INOSApi nosApi, EchoReplyEventVO echoReply) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void portStatusChangeEvent(INOSApi nosApi,
			PortStatusEventVO portStatus) {
		// TODO 自動生成されたメソッド・スタブ
		System.out.println("port status changing");
		if(portStatus.reason ==2 && portStatus.physiPort.portStatus == 1){
			long dpid =portStatus.dpid;
			int portNo = portStatus.physiPort.portNo;
			Map<String, DpidPortPair> map = DeviceManager.getInstance().getMacDpidPortMap();
			ArrayList<String> toRem = new ArrayList<String>();
			for(Entry<String, DpidPortPair> entry : map.entrySet()){
				if(entry.getValue().getDpid() == dpid && entry.getValue().getPort() == portNo){
					toRem.add(entry.getKey());
				}
			}
			for(String toRemItem : toRem){
				map.remove(toRemItem);
			}
		}

	}

	@Override
	public void flowRemovedEvent(INOSApi nosApi, FlowRemovedEventVO flowRemoved) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void errorRecvEvent(INOSApi nosApi, ErrorRecvEventVO errorRecv) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void getConfigReplyEvent(INOSApi nosApi,
			GetConfigReplyEventVO getConfigReply) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void barrierReplyEvent(INOSApi nosApi,
			BarrierReplyEventVO barrierReply) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void queueGetConfigReplyEvent(INOSApi nosApi,
			QueueGetConfigReplyEventVO queueGetConfigReply) {
		// TODO 自動生成されたメソッド・スタブ

	}

	private void checkLldpPacketFormat(LldpPDU lldp){

		if(lldp == null)
		{
			throw new NosapFormatErrorException("lldp is null");
		}

		/*
		 * L1ポート番号の取得
		 */
		TlvPDU portId = lldp.portId;

		if(portId == null)
		{
			throw new NosapFormatErrorException("portId is null.");
		}

		final String ERROR_MESSAGE = "unknown lldp packet is packet_in";

		if(portId.value == null)
		{
			throw new NosapFormatErrorException(ERROR_MESSAGE);
		}

		if(portId.value.length != 3)
		{
			throw new NosapFormatErrorException(ERROR_MESSAGE);
		}

		/*
		 * DatapathIdの取得
		 */
		if(lldp.optional.size() != 2)
		{
			throw new NosapFormatErrorException(ERROR_MESSAGE);
		}

		TlvPDU tlv = lldp.optional.get(0);
		if(tlv.type != TlvType.ORG_SPECIFIC)
		{
			throw new NosapFormatErrorException(ERROR_MESSAGE);
		}

		if(tlv.value == null)
		{
			throw new NosapFormatErrorException(ERROR_MESSAGE);
		}

		if(tlv.value.length != 12) // 12 is original specific size
		{
			throw new NosapFormatErrorException(ERROR_MESSAGE);
		}

		tlv = lldp.optional.get(1);
		if(tlv.type != TlvType.ORG_SPECIFIC)
		{
			throw new NosapFormatErrorException(ERROR_MESSAGE);
		}

		if(tlv.value == null)
		{
			throw new NosapFormatErrorException(ERROR_MESSAGE);
		}

		if(tlv.value.length != 12) // 12 is original specific size
		{
			throw new NosapFormatErrorException(ERROR_MESSAGE);
		}

		if(!Arrays.equals(tlv.value,LldpPacketGenerator.LLDP_NTTDATA_PACKET_ID))
		{
			throw new NosapFormatErrorException("NTTDATA_PACKET_ID is invalid");
		}

	}

	private boolean checkTopology(long dpid1, int port1, long dpid2, int port2){
		LinkedList<Edge> removeEdges = new LinkedList<Edge>();
		boolean flag = true;
		long[] dpids;
		int[] ports;

		for(Trunk trunk : this.topologyManager.getTrunkList()){
			//System.out.println("LLDP packet in");
			dpids = trunk.getDpidPair();
			for(Edge edge : trunk.getEdgeList()){
				ports = edge.getPorts();
				if(dpids[0] == dpid1 && ports[0] == port1){
					if(dpids[1] == dpid2 && ports[1] == port2){
						System.out.println("Update timestamp of edge " + Utility.toDpidHexString(dpid1) + ":" + port1 + " - " + Utility.toDpidHexString(dpid2) + ":" + port2);
						edge.setTimestamp();
						return true;
					}
					else{
						removeEdges.add(edge);
					}
				}
				else if(dpids[0] == dpid2 && ports[0] == port2){
					if(dpids[1] == dpid1 && ports[1] == port1){
						System.out.println("Update timestamp of edge " + Utility.toDpidHexString(dpid1) + ":" + port1 + " - " + Utility.toDpidHexString(dpid2) + ":" + port2);
						edge.setTimestamp();
						return true;
					}
					else{
						removeEdges.add(edge);
					}
				}
			}

			if(removeEdges.size() > 0){
				flag = false;
				for(Edge e : removeEdges){
					this.operator.deleteConnectionRelatedFlowEntries(dpid1, dpid2, port1, port2);
					//this.topologyManager.getDbm().deleteConnectionInfo(dpid1, dpid2, port1, port2);
					if(trunk.remove(e) == null){
						System.err.println("Edge remove failed at checkTopology. " + Utility.toDpidHexString(dpids[0]) + ":" + e.getPorts()[0] + " - " + Utility.toDpidHexString(dpids[1]) + ":" + e.getPorts()[1]);
					}
					else{
						System.err.println("Remove edge " + Utility.toDpidHexString(dpids[0]) + ":" + e.getPorts()[0] + " - " + Utility.toDpidHexString(dpids[1]) + ":" + e.getPorts()[1]);
					}
				}
			}
		}

		if(flag){
			System.out.println("Detect new edge " + Utility.toDpidHexString(dpid1) + ":" + port1 + " - " + Utility.toDpidHexString(dpid2) + ":" + port2);
			if(this.topologyManager.addTrunk(new DpidPortPair(dpid1, port1), new DpidPortPair(dpid2, port2))){
				//this.topologyManager.getDbm().insertConnectionInfo(dpid1, dpid2, port1, port2);
				this.topologyManager.updateForwardingTable();
				this.operator.deleteDetourRouteFlowEntries(dpid1, dpid2);
				return true;
			}
			else{
				return false;
			}
		}
		else{
			return false;
		}
	}
}
