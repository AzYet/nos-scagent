package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology;

import java.util.Set;

import jp.co.nttdata.ofc.common.except.NosSocketIOException;
import jp.co.nttdata.ofc.nos.api.INOSApi;
import jp.co.nttdata.ofc.nos.api.IPacketOut;
import jp.co.nttdata.ofc.nos.api.NOSApi;
import jp.co.nttdata.ofc.nos.api.except.ActionNotSupportedException;
import jp.co.nttdata.ofc.nos.api.except.ArgumentInvalidException;
import jp.co.nttdata.ofc.nos.api.except.OFSwitchNotFoundException;
import jp.co.nttdata.ofc.nos.api.except.SwitchPortNotFoundException;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.logical.LogicalSwitch;

public class LldpSender implements Runnable{
	private final LldpPacketGenerator lpg = new LldpPacketGenerator();
	private final INOSApi nosApi = new NOSApi();

	private TopologyManager topologyManager;

	public LldpSender(){
		this.topologyManager = TopologyManager.getInstance();
	}

	@Override
	public void run(){
		System.out.println("lldp sending....");
			Set<Long> dpidSet = this.topologyManager.getDpidSet();

			for(Long dpid : dpidSet){
				this.sendLldpPacket(dpid.longValue());
			}

	}

	private boolean sendLldpPacket(long dpid){
		byte[] data = lpg.getByte(dpid);

		try {
			IPacketOut iOut = nosApi.createPacketOutInstance(dpid, data);
			for(int port = 1; port <= TopologyManager.PORT_NUM; port++){
				byte[] packetData = lpg.getByte(dpid, port);
				try {
					iOut.setInPort(LogicalSwitch.NONE_PORT);
					iOut.addOutputAction(port);
				} catch (SwitchPortNotFoundException e) {
//					System.err.println(Utility.toDpidHexString(dpid) + "-" + port + " does not exist.");
				}
				iOut.setData(packetData);
				iOut.addSetSrcMacaddrAction(port);
				iOut.send();
				iOut.clearAction();
			}
		} catch (OFSwitchNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ActionNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ArgumentInvalidException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NosSocketIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}
}
