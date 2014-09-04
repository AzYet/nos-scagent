package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology;

import java.util.LinkedList;
import java.util.List;

import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.Utility;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.webservice.Operator;

public class LldpChecker implements Runnable{
	private TopologyManager topologyManager;
	private List<Edge> removeEdges;
	private Operator operator;
	private long timestampDiff;

	public LldpChecker() {
		this.topologyManager = TopologyManager.getInstance();
		this.removeEdges = new LinkedList<Edge>();
		this.operator = new Operator();
	}

	@Override
	public void run() {
		for(Trunk trunk : this.topologyManager.getTrunkList()){
			this.removeEdges.clear();
			for(Edge edge : trunk.getEdgeList()){
				timestampDiff = System.currentTimeMillis() - edge.getTimestamp();
				if(timestampDiff > TopologyManager.LLDP_INTERVAL * 2){
					System.err.println("LLDP timeout.");
					removeEdges.add(edge);
				}
			}
			for(Edge e : removeEdges){
				long[] dpids = trunk.getDpidPair();
				int[] ports = e.getPorts();
				this.operator.deleteConnectionRelatedFlowEntries(dpids[0], dpids[1], ports[0], ports[1]);
				//this.topologyManager.getDbm().deleteConnectionInfo(dpids[0], dpids[1], ports[0], ports[1]);
				if(trunk.remove(e) == null){
					System.err.println("Edge remove failed at LLDP Checker. " + Utility.toDpidHexString(dpids[0]) + ":" + ports[0] + " - " + Utility.toDpidHexString(dpids[1]) + ":" + ports[1]);
				}
				else{
					System.err.println("Remove edge " + Utility.toDpidHexString(dpids[0]) + ":" + ports[0] + " - " + Utility.toDpidHexString(dpids[1]) + ":" + ports[1]);
				}
			}
		}
	}
}
