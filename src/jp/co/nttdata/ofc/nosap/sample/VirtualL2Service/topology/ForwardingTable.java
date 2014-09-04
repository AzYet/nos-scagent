package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.Utility;

public class ForwardingTable {
	private LinkedList<ForwardingTableEntry> table;
	private short mode;
	private TopologyManager topologyManager;

	public ForwardingTable(short mode){
		this.table = new LinkedList<ForwardingTableEntry>();
		this.mode = mode;
		this.topologyManager = TopologyManager.getInstance();
	}

	public LinkedList<ForwardingTableEntry> getTable(){
		return this.table;
	}

	public short getMode(){
		return this.mode;
	}

	public LinkedList<long[]> getKeySet(){
		LinkedList<long[]> result = new LinkedList<long[]>();
		for(ForwardingTableEntry entry : this.table){
			result.add(entry.getKey());
		}
		return result;
	}

	public LinkedList<Integer> getPortList(long srcDpid, long dstDpid){
		LinkedList<Integer> ret = new LinkedList<Integer>();
		if(srcDpid == dstDpid){
			return ret;
		}
		for(ForwardingTableEntry entry : this.table){
			if((entry.getSrcDpid() == srcDpid) && (entry.getDstDpid() == dstDpid)){
				for(Integer i : entry.getPortList()){
					ret.add(new Integer(i.intValue()));
				}
				return ret;
			}
		}
		return ret;
	}

	public long[] getCostArray(long dpid, LinkedList<Integer> portList){
		long[] ret = new long[portList.size()];

		for(Trunk t : topologyManager.getTrunkList()){
			long[] dpids = t.getDpidPair();
			if((dpids[0] == dpid) || (dpids[1] == dpid)){
				LinkedList<Edge> edgeList = t.getEdgeList();
				for(int i = 0; i < edgeList.size(); i++){
					ret[i] = edgeList.get(i).getCost();
				}
			}
		}

		return ret;
	}

	public int getPort(long[] key){
		LinkedList<Integer> portList = this.getPortList(key[0], key[1]);
		int ret = portList.getFirst().intValue();

		if(portList.size() == 1){
			return ret;
		}

		switch(this.mode){
		case TopologyManager.ROUND_ROBIN:
			portList.removeFirst();
			portList.addLast(new Integer(ret));
			this.setPortList(key, portList);

			return ret;

		case TopologyManager.LEAST_FREQUENTLY_USED:
			long cost, minCost;
			minCost = Long.MAX_VALUE;
			for(Trunk trunk : topologyManager.getTrunkList()){
				long[] dpids = trunk.getDpidPair();

				if(trunk.matches(key[0], key[1])){
					for(Edge edge : trunk.getEdgeList()){
						if((cost = edge.getCost()) < minCost){
							minCost = cost;
							if(dpids[0] == key[0] && dpids[1] == key[1]){
								ret = edge.getPorts()[0];
							}
							else if(dpids[1] == key[0] && dpids[0] == key[1]){
								ret = edge.getPorts()[1];
							}
							else{
								ret = -1;
							}
						}
					}
					break;
				}
			}

			return ret;

		default:
			return -1;
		}
	}

	public long getCost(long[] key){
		if(key[0] == key[1]){
			return 0;
		}
		if(key.length != 2){
			return -1;
		}

		for(ForwardingTableEntry entry : this.table){
			if((entry.getSrcDpid() == key[0]) && (entry.getDstDpid() == key[1])){
				return entry.getCost();
			}
		}

		return -1;
	}

	public boolean setPortList(long[] key, LinkedList<Integer> newPortList){
		if((key.length != 2) || (key[0] == key[1])){
			return false;
		}
		for(ForwardingTableEntry entry : this.table){
			if((entry.getSrcDpid() == key[0]) && (entry.getDstDpid() == key[1])){
				entry.setPortList(newPortList);
				return true;
			}
		}
		return false;
	}

	public void setMode(short mode){
		this.mode = mode;
	}

	public void add(long[] key, LinkedList<Integer> portList, long cost, boolean overWrite){
		ForwardingTableEntry entry = new ForwardingTableEntry(key, portList, cost);
		if(!this.contains(key)){
			this.table.add(entry);
		}
		else if(overWrite){
			this.remove(key);
			this.table.add(entry);
		}
	}

	public boolean remove(long[] key){
		for(ForwardingTableEntry entry : this.table){
			if((entry.getSrcDpid() == key[0]) && (entry.getDstDpid() == key[1])){
				this.table.remove(entry);
				return true;
			}
		}

		return false;
	}

	public boolean contains(long[] key){
		for(ForwardingTableEntry entry : this.table){
			if((entry.getSrcDpid() == key[0]) && (entry.getDstDpid() == key[1])){
				return true;
			}
		}

		return false;
	}

	@SuppressWarnings("unchecked")
	public ForwardingTable clone(){
		ForwardingTable clone = new ForwardingTable(this.mode);
		clone.table = (LinkedList<ForwardingTableEntry>)this.table.clone();

		return clone;
	}

	public void clear(){
		this.table.clear();
	}

	public synchronized boolean generate() {
		this.clear();

		if(topologyManager == null){
			topologyManager = TopologyManager.getInstance();
		}

		List<Trunk> trunks = Collections.synchronizedList(topologyManager.getTrunkList());
		//List<Trunk> trunks = topologyManager.getTrunkList();
		synchronized(trunks){
			for(Trunk t : trunks){
				long[] dpidPair = t.getDpidPair();
				LinkedList<Edge> edgeList = t.getEdgeList();
				long cost = t.getCost();
	
				if(edgeList.size() > 0){
					long[] key1 = {dpidPair[0], dpidPair[1]};
					long[] key2 = {dpidPair[1], dpidPair[0]};
	
					LinkedList<Integer> portList1 = new LinkedList<Integer>();
					LinkedList<Integer> portList2 = new LinkedList<Integer>();
	
					for(Edge edge : edgeList){
						portList1.add(edge.getPorts()[0]);
						portList2.add(edge.getPorts()[1]);
					}
	
					this.add(key1, portList1, cost, true);
					this.add(key2, portList2, cost, true);
				}
			}
		}

		boolean loop;
		do{
			loop = false;
			LinkedList<long[]> keySet = this.getKeySet();
			for(long[] k1 : keySet){
				for(long[] k2 : keySet){
					if(!k1.equals(k2)){
						if((k1[1] == k2[0]) && (k1[0] != k2[1])){
							LinkedList<Integer> list = this.getPortList(k1[0], k1[1]);
							long c1 = this.getCost(k1);
							long c2 = this.getCost(k2);

							long[] key = {k1[0], k2[1]};
							long cost = c1 + c2;

							if(!this.contains(key)){
								this.add(key, list, cost, true);
								loop = true;
							}
							else if(cost < this.getCost(key)){
								this.remove(key);
								this.add(key, list, cost, true);
								loop = true;
							}
						}
					}
				}
			}
		}while(loop);

		System.out.println("forward table: \n" + this.show());

		return true;
	}

	public String show(){
		String ret = "";
		for(ForwardingTableEntry entry : this.table){
			ret += entry.show();
		}
		return ret;
	}

	class ForwardingTableEntry {
		private long[] key;
		private LinkedList<Integer> portList;
		private long cost;

		public ForwardingTableEntry(){
			this.key = new long[2];
			this.portList = new LinkedList<Integer>();
			this.cost = 0;
		}

		public ForwardingTableEntry(long[] key, LinkedList<Integer> portList, long cost){
			this.key = key;
			this.portList = portList;
			this.cost = cost;
		}

		public long[] getKey(){
			return this.key;
		}

		public LinkedList<Integer> getPortList(){
			return this.portList;
		}

		public long getCost(){
			return this.cost;
		}

		public long getSrcDpid(){
			return this.getKey()[0];
		}

		public long getDstDpid(){
			return this.getKey()[1];
		}

		public void setPortList(LinkedList<Integer> newList){
			this.portList = newList;
		}

		public String show(){
			String str = "";
			String s = "  ";

			s += Utility.toDpidHexString(this.getSrcDpid()) + "  ->  " + Utility.toDpidHexString(this.getDstDpid()) + "   " + Utility.formatB(this.getCost()) + "   ";
			for(int i = 0; i < this.portList.size(); i++){
				s += Utility.formatB(this.portList.get(i));
				str += s + "\n";
				s = "                                          ";
			}
			return str;
		}
	}

//	public boolean startCostUpdater(){
//		if((ct == null) || !ct.isAlive()){
//			ct = new Thread(new CostUpdater());
//			ct.start();
//			return true;
//		}
//		else{
//			return false;
//		}
//	}

//	class CostUpdater implements Runnable {
//		private NOSApi nosApi;
//
//		public void run(){
//			this.nosApi = new NOSApi();
//
//			while(true){
//				this.update();
//				System.out.println("Cost is updated.");
//				try {
//					Thread.sleep(TopologyManager.COST_UPDATE_INTERVAL);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		}
//
//		private long getPacketCount(long dpid, int port){
//
//			List<TableStatsVO> tableStats;
//			AggregateFlowStatsVO flowStats;
//			Flow flow;
//
//			try {
//				tableStats = nosApi.getTableStatsRequest(dpid);
//				flow = new Flow();
//				flow.setAllWildCards();
//
//				if((flowStats = nosApi.getAggregateFlowStatsRequest(dpid, flow, tableStats.get(0).tableId, port)) == null){
//					System.err.println("flowStats is null.");
//					return -1L;
//				}
//
//				return flowStats.packetCount;
//			} catch (NosSocketIOException e) {
//				// TODO 自動生成された catch ブロック
//				e.printStackTrace();
//			} catch (OFSwitchNotFoundException e) {
//				// TODO 自動生成された catch ブロック
//				e.printStackTrace();
//			} catch (SwitchPortNotFoundException e) {
//				// TODO 自動生成された catch ブロック
//				e.printStackTrace();
//			} catch (ArgumentInvalidException e) {
//				// TODO 自動生成された catch ブロック
//				e.printStackTrace();
//			}
//			System.err.println("[" + Utility.toDpidHexString(dpid) + ":" + port +"]");
//			return -1L;
//		}
//
//		private void update(){
//			LinkedList<Edge> edgeList;
//			int[] ports;
//			long[] dpids;
//			long pc1, pc2;
//			for(Trunk t : topologyManager.getTrunkList()){
//				dpids = t.getDpidPair();
//				edgeList = t.getEdgeList();
//				for(Edge edge : edgeList){
//					ports = edge.getPorts();
//					pc1 = this.getPacketCount(dpids[0], ports[0]);
//					pc2 = this.getPacketCount(dpids[1], ports[1]);
//					if((pc1 >= 0L) && (pc2 >= 0L)){
//						edge.setPacket((long)((pc1 + pc2) / 2));
//					}
//					else{
//						System.err.println("Edge " + Utility.toDpidHexString(dpids[0]) + ":" + ports[0] + " <-> "
//								+ Utility.toDpidHexString(dpids[1]) + ":" + ports[1] + " is not updated.");
//					}
//				}
//			}
//		}
//	}

//	public boolean startTableUpdater(){
//		if((tt == null) || !tt.isAlive()){
//			tt = new Thread(new TableUpdater());
//			tt.start();
//			return true;
//		}
//		else{
//			return false;
//		}
//	}

//	class TableUpdater implements Runnable {
//		public void run(){
////			while(topologyManager.getOfsCount() < TopologyManager.OFS_NUM){
////				try {
////					Thread.sleep(1000);
////				} catch (InterruptedException e) {
////					// TODO Auto-generated catch block
////					e.printStackTrace();
////				}
////			}
//
//			while(true){
//				this.update();
//				try {
//					Thread.sleep(TopologyManager.TABLE_UPDATE_INTERVAL);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		}
//
//		private void update(){
//
//			if(topologyManager.getForwardingTable().generate()){
//				System.out.println("ForwardingTable is updated.");
//			}else{
//				System.out.println("ForwardingTable is not updated.");
//			}
//		}
//	}
}
