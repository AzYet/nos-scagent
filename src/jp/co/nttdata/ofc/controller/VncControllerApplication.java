package jp.co.nttdata.ofc.controller;

import jp.co.nttdata.ofc.common.util.MacAddress;
import jp.co.nttdata.ofc.nos.api.IFlowModifier;
import jp.co.nttdata.ofc.nos.api.INOSApi;
import jp.co.nttdata.ofc.nos.api.INOSApplication;
import jp.co.nttdata.ofc.nos.api.IPacketOut;
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
import jp.co.nttdata.ofc.nos.common.constant.OFPConstant.OFPort;
import jp.co.nttdata.ofc.nos.common.constant.OFPConstant.OFWildCard;
import jp.co.nttdata.ofc.nos.ofp.common.Flow;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.NosFactory;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.LldpPacketGenerator;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.TopologyManager;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.webservice.Operator;


public class VncControllerApplication implements INOSApplication
{


	private TopologyManager topologyManager;
	private MacAddress lldpMacAddress;
	private Operator operator;
	

	public VncControllerApplication()
	{
		this.topologyManager = TopologyManager.getInstance();
		this.topologyManager.USE_LLDP = true;
		this.lldpMacAddress = NosFactory.createMacAddress(LldpPacketGenerator.LLDP_MULTICAST_ADDR);
		this.operator = new Operator();
	}
	
	
	
	
	@Override
	public void datapathLeaveEvent(INOSApi nosApi, DatapathLeaveEventVO event)
	{
		System.out.println("Switch["+Long.toHexString(event.dpid)+"] is down");
	}

	@Override
	public void datapathJoinEvent(INOSApi nosApi, DatapathJoinEventVO event)
	{
		System.out.println("Switch["+Long.toHexString(event.dpid)+"] is up");
		for(PhysicalPortVO port : event.physiPorts){
			System.out.println("portName: "+port.portName);
			System.out.println("portNo: "+port.portNo);
			System.out.println("portMAC: "+port.macaddr);
		}
				
		IFlowModifier flowModifier = null;
		try {
			//flow.wildCards = OFWildCard.ALL & ~ OFWildCard.SRC_PORT & ~ OFWildCard.DST_MACADDR;
			//flow.dstIpaddr = new IpAddress("101.0.0.0");
			//flow.srcPort=233;
			//flow.setIdentificationWithDstIpNetmask(24);

			Flow flow = new Flow();
			flow.wildCards = OFWildCard.ALL & ~ OFWildCard.DST_MACADDR;
			flow.dstMacaddr = new MacAddress(MacAddress.BROADCAST_MAC_ADDR);
			//flow.wildCards = OFWildCard.ALL ;
			
			flowModifier = nosApi.createFlowModifierInstance(event.dpid, flow);
			flowModifier.setAddCommand();
			flowModifier.setBufferId(-1);
			flowModifier.addOutputAction(OFPort.ALL, 0);
			flowModifier.send();
			
			System.out.println("Sent "+ flow);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
		
	}

	@Override
	public void echoReplyEvent(INOSApi nosApi, EchoReplyEventVO event)
	{
		System.out.println("echo reply");
	}

	@Override
	public void portStatusChangeEvent(INOSApi nosApi, PortStatusEventVO event)
	{
	
	}

	@Override
	public void flowRemovedEvent(INOSApi nosApi, FlowRemovedEventVO event)
	{

	}

	@Override
	public void errorRecvEvent(INOSApi nosApi, ErrorRecvEventVO event)
	{

	}

	@Override
	public void getConfigReplyEvent(INOSApi nosApi, GetConfigReplyEventVO event)
	{

	}

	@Override
	public void barrierReplyEvent(INOSApi nosApi, BarrierReplyEventVO event)
	{

	}

	@Override
	public void queueGetConfigReplyEvent(INOSApi nosApi, QueueGetConfigReplyEventVO event)
	{

	}

	@Override
	public void packetInEvent(INOSApi nosApi, PacketInEventVO event)
	{
		//System.out.println("packet in...");
		MacTable macTable = MacTable.getInstance();
		macTable.learningMacAddr(event.flow.srcMacaddr.toLong(), event.flow.inPort); 
		Integer dstPort = macTable.searchEntry(event.flow.dstMacaddr.toLong());

		
		if(dstPort == null){
			try{
				IPacketOut pout = nosApi.createPacketOutInstance(event.dpid, event.data);
				pout.setInPort(event.flow.inPort);
				pout.addOutputAction(OFPort.ALL);
				pout.setBufferId(-1);
				pout.send();
				//System.out.print("src mac:" + event.flow.srcMacaddr.toString() +", dst port:" + dstPort);
				//System.out.println("\t pkg_out sent all: "+OFPort.ALL);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		else{
			try{
				Flow flow = new Flow();
				flow.wildCards = OFWildCard.ALL & ~OFWildCard.DST_MACADDR;
				flow.dstMacaddr = event.flow.dstMacaddr;
				IFlowModifier mod = nosApi.createFlowModifierInstance(event.dpid, flow);
				mod.setBufferId(-1);
				mod.setAddCommand();
				mod.addOutputAction(dstPort, 0);
				mod.send();
				

				IPacketOut pout = nosApi.createPacketOutInstance(event.dpid, event.data);
				pout.setInPort(event.flow.inPort);
				pout.addOutputAction(dstPort);
				pout.setBufferId(-1);
				pout.send();
				System.out.print("src mac:" + event.flow.srcMacaddr.toString() +", dst port:" + dstPort);
				System.out.println("\t pkg_out sent one: "+dstPort);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
}
