package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology;

import java.util.LinkedList;

import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.Utility;

public class TableUpdater implements Runnable {

	private TopologyManager topologyManager;

	public TableUpdater(){
		topologyManager = TopologyManager.getInstance();
	}

	public void run(){
		this.costUpdate();
		System.out.println("Cost is updated.");

		if(topologyManager.getForwardingTable().generate()){
			System.out.println("ForwardingTable is updated.");
		}else{
			System.out.println("ForwardingTable is not updated.");
		}
	}

	private void costUpdate(){
		LinkedList<Edge> edgeList;
		int[] ports;
		long[] dpids;
		long pc1, pc2;
		for(Trunk t : topologyManager.getTrunkList()){
			dpids = t.getDpidPair();
			edgeList = t.getEdgeList();
			for(Edge edge : edgeList){
				ports = edge.getPorts();
				pc1 = Utility.getCounters(dpids[0], ports[0])[0];
				pc2 = Utility.getCounters(dpids[1], ports[1])[0];
				if((pc1 >= 0L) && (pc2 >= 0L)){
					edge.setPacket((long)((pc1 + pc2) / 2));
				}
				else{
					System.err.println("Edge " + Utility.toDpidHexString(dpids[0]) + ":" + ports[0] + " <-> "
							+ Utility.toDpidHexString(dpids[1]) + ":" + ports[1] + " is not updated.");
				}
			}
		}
	}
}
