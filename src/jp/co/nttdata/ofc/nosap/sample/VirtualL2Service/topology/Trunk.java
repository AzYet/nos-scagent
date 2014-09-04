package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology;

import java.util.LinkedList;

import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.DpidPortPair;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.Utility;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.path.PathManager;

public class Trunk {
	private long[] dpidPair;
	private LinkedList<Edge> edgeList;
	private PathManager pathManager;

	public Trunk(){
		this.dpidPair = new long[2];
		this.edgeList = new LinkedList<Edge>();
		this.pathManager = PathManager.getInstance();
	}

	public Trunk(DpidPortPair p1, DpidPortPair p2){
		this();
		if(p1.getDpid() < p2.getDpid()){
			this.dpidPair[0] = p1.getDpid();
			this.dpidPair[1] = p2.getDpid();
			this.add(p1, p2);
		}
		else if(p1.getDpid() > p2.getDpid()){
			this.dpidPair[0] = p2.getDpid();
			this.dpidPair[1] = p1.getDpid();
			this.add(p1, p2);
		}
		else{
			System.out.println("p1.dpid and p2.dpid are the same.");
		}
	}

	public long[] getDpidPair(){
		return this.dpidPair;
	}

	public LinkedList<Edge> getEdgeList(){
		return this.edgeList;
	}

	public LinkedList<Integer> getFirstPortList(){
		LinkedList<Integer> ret = new LinkedList<Integer>();
		for(Edge edge : this.edgeList){
			ret.add(new Integer(edge.getPorts()[0]));
		}
		return ret;
	}

	public LinkedList<Integer> getSecondPortList(){
		LinkedList<Integer> ret = new LinkedList<Integer>();
		for(Edge edge : this.edgeList){
			ret.add(new Integer(edge.getPorts()[1]));
		}
		return ret;
	}

	public long getCost(int index){
		if((index < 0) || (this.edgeList.size() < index)){
			return -1L;
		}
		else{
			return this.edgeList.get(index).getCost();
		}
	}

	public long getCost(){
		long ret = Long.MAX_VALUE;
		long cost;
		for(Edge edge : this.edgeList){
			cost = edge.getCost();
			if(cost < ret){
				ret = cost;
			}
		}
		return ret;
	}

	public long[] getCounters(){
		long[] totalCounts = {0L, 0L, 0L};
		long[] counts;

		for(Edge edge : this.edgeList){
			counts = Utility.getCounters(this.dpidPair[0], edge.getPorts()[0]);
			for(int i = 0; i < counts.length; i++){
				totalCounts[i] += counts[i];
			}
		}

		return totalCounts;
	}

	public boolean add(DpidPortPair p1, DpidPortPair p2){
		int port1, port2;
		if((this.dpidPair[0] == p1.getDpid()) && (this.dpidPair[1] == p2.getDpid())){
			port1 = p1.getPort();
			port2 = p2.getPort();
		}
		else if((this.dpidPair[0] == p2.getDpid()) && (this.dpidPair[1] == p1.getDpid())){
			port1 = p2.getPort();
			port2 = p1.getPort();
		}
		else{
			return false;
		}

		int index = 0;
		for(Edge edge : this.edgeList){
			if(port1 > edge.getPorts()[0]){
				index++;
			}
		}
		this.edgeList.add(index, new Edge(port1, port2));

		return true;
	}

	public Edge remove(int index){
		int[] ports = this.edgeList.get(index).getPorts();

		this.pathManager.removeElements(this.dpidPair[0], ports[0], this.dpidPair[1], ports[1]);

		return this.edgeList.remove(index);
	}

	public Edge remove(Edge edge){
		for(int i = 0; i < this.edgeList.size(); i++){
			if(this.edgeList.get(i).equals(edge)){
				return this.remove(i);
			}
		}
		return null;
	}

	public boolean contains(DpidPortPair p){
		long dpid = p.getDpid();
		int port = p.getPort();

		if(dpid == this.dpidPair[0]){
			for(Edge edge : this.edgeList){
				if(port == edge.getPorts()[0]){
					return true;
				}
			}
		}
		else if(dpid == this.dpidPair[1]){
			for(Edge edge : this.edgeList){
				if(port == edge.getPorts()[1]){
					return true;
				}
			}
		}

		return false;
	}


	public boolean matches(long dpid1, long dpid2){
		if(((this.dpidPair[0] == dpid1) && (this.dpidPair[1] == dpid2))
				|| ((this.dpidPair[0] == dpid2) && (this.dpidPair[1] == dpid1))){
			return true;
		}
		return false;
	}

	public LinkedList<String> show(int id){
		LinkedList<String> ret = new LinkedList<String>();
		for(Edge edge : this.edgeList){
			ret.add(Utility.formatA(id++)
					+ Utility.toDpidHexString(this.dpidPair[0]) + "   " + Utility.formatB(edge.getPorts()[0]) + "  <->  "
					+ Utility.toDpidHexString(this.dpidPair[1]) + "   " + Utility.formatB(edge.getPorts()[1]) + " : " + edge.getCost() + "\n");
		}
		return ret;
	}
}
