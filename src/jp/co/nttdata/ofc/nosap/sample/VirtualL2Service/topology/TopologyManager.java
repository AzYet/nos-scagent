package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.DBManager;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.DpidPortPair;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.Utility;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.logical.LogicalSwitch;

public class TopologyManager {
	public static boolean USE_LLDP = false;
	public static final int DPID_LENGTH = 16;
	public static final int PORT_NUM = 10; //TODO
	public static final int INIT_SLEEP = 10 * 1000;				// msec
	public static final int COST_UPDATE_INTERVAL = 10 * 1000;		// msec
	public static final int TABLE_UPDATE_INTERVAL = 10 * 1000;	// msec
	public static final int LLDP_INTERVAL = 10 * 1000;			// msec
	public static final long DEFAULT_COST = 100;
	public static final short LEAST_FREQUENTLY_USED = 1;
	public static final short ROUND_ROBIN = 0;

	private static TopologyManager topologyManager = new TopologyManager();
	private TreeSet<Long> dpidSet = new TreeSet<Long>();
	private ForwardingTable forwardingTable  = new ForwardingTable(LEAST_FREQUENTLY_USED);
	private CopyOnWriteArrayList<Trunk> trunkList = new CopyOnWriteArrayList<Trunk>();
	private CopyOnWriteArrayList<DpidPortPair> hostList = new CopyOnWriteArrayList<DpidPortPair>();
	private CopyOnWriteArrayList<LogicalSwitch> switchList = new CopyOnWriteArrayList<LogicalSwitch>();
	private DBManager dbm = new DBManager();

	private TopologyManager(){
		dpidSet = new TreeSet<Long>();
		forwardingTable = new ForwardingTable(LEAST_FREQUENTLY_USED);
		trunkList = new CopyOnWriteArrayList<Trunk>();
		hostList = new CopyOnWriteArrayList<DpidPortPair>();
		switchList = new CopyOnWriteArrayList<LogicalSwitch>();
		dbm = new DBManager();
	}

	public static TopologyManager getInstance(){
		return topologyManager;
	}

	public boolean checkDpidPortPair(long dpid, int port){
		for(DpidPortPair p : hostList){
			if((p.getDpid() == dpid) && (p.getPort() == port)){
				return true;
			}
		}
		return false;
	}
	
	public DpidPortPair findDpidPortPair(long dpid, int port){
		for(DpidPortPair p : hostList){
			if((p.getDpid() == dpid) && (p.getPort() == port)){
				return p;
			}
		}
		return null;
	}

	public boolean checkSwitchName(String name){
		for(LogicalSwitch sw : switchList){
			if(sw.getName().equals(name)){
				return true;
			}
		}
		return false;
	}

	public boolean addDpid(Long dpid){
		return dpidSet.add(dpid);
	}

	public boolean removeDpid(Long dpid){
		return dpidSet.remove(dpid);
	}

	public boolean addHost(DpidPortPair host){
		int index = 0;

		for(DpidPortPair p : hostList){
			if((host.getDpid() < p.getDpid())){
				break;
			}
			else if((host.getDpid() == p.getDpid()) && (host.getPort() < p.getPort())){
				break;
			}
			index++;
		}

		if(host == null){
			System.err.println("nullllllllllllllllllllllll");
		}
		hostList.add(index, host);

		return true;
	}

	public boolean addTrunk(DpidPortPair p1, DpidPortPair p2){
		long dpid1, dpid2;
		if(p1.getDpid() < p2.getDpid()){
			dpid1 = p1.getDpid();
			dpid2 = p2.getDpid();
		}
		else if(p1.getDpid() > p2.getDpid()){
			dpid1 = p2.getDpid();
			dpid2 = p1.getDpid();
		}
		else{
			return false;
		}

		int index = 0;
		for(Trunk t : trunkList){
			if(t.matches(p1.getDpid(), p2.getDpid())){
				return t.add(p1, p2);
			}
			else if(dpid1 > t.getDpidPair()[0]){
				index++;
			}
			else if((dpid1 == t.getDpidPair()[0]) && (dpid2 > t.getDpidPair()[1])){
				index++;
			}
		}

		trunkList.add(index, new Trunk(p1, p2));

		return true;
	}

	public LinkedList<Long> getHoppedDpidList(long srcDpid, long dstDpid){
		LinkedList<Long> ret = new LinkedList<Long>();

		long[] key = {srcDpid, dstDpid};
		long cost = forwardingTable.getCost(key);

		if(cost < 0){
			return null;
		}
		else if(cost == 0){
			ret.add(new Long(srcDpid));
			return ret;
		}
		else{
			long dpid = srcDpid;
			int port;
			while(dpid != dstDpid){
				ret.add(new Long(dpid));

				key[0] = dpid;
				port = forwardingTable.getPortList(key[0], key[1]).get(0);

				for(Trunk t : trunkList){
					long[] p = t.getDpidPair();
					LinkedList<Integer> l1 = t.getFirstPortList();
					LinkedList<Integer> l2 = t.getSecondPortList();
					if((p[0] == dpid) && l1.contains(port)){
						dpid = p[1];
						break;
					}
					if((p[1] == dpid) && l2.contains(port)){
						dpid = p[0];
						break;
					}
				}
			}
			ret.add(new Long(dpid));

			return ret;
		}
	}

	public LogicalSwitch getSwitchByName(String name){
		for(LogicalSwitch sw : switchList){
			if(sw.getName().equals(name)){
				return sw;
			}
		}
		return null;
	}

	public int getCorrespondPort(long dpid1, long dpid2, int port){
		for(Trunk t : trunkList){
			if(t.matches(dpid1, dpid2)){
				long[] p = t.getDpidPair();
				LinkedList<Integer> list1 = t.getFirstPortList();
				LinkedList<Integer> list2 = t.getSecondPortList();
				int index = 0;

				if((p[0] == dpid1) && (p[1] == dpid2)){
					for(int i = 0; i < list1.size(); i++){
						if(list1.get(i) == port){
							index = i;
							break;
						}
					}
					return list2.get(index);
				}
				else if((p[0] == dpid2) && (p[1] == dpid1)){
					for(int i = 0; i < list2.size(); i++){
						if(list2.get(i) == port){
							index = i;
							break;
						}
					}
					return list1.get(index);
				}
			}
		}
		return -1;
	}

	public void updateForwardingTable(){
		try{
			if(forwardingTable.generate()){
				System.out.println("ForwardingTable is updated.");
			}else{
				System.out.println("ForwardingTable is not updated.");
			}
		}catch(NullPointerException e){
			System.out.println("ForwardingTable is not updated.");
			e.printStackTrace();
		}
	}


	public void loadHostInfo(){
		ResultSet rs = dbm.getHostInfo();

		long dpid;
		int port;

		try {
			while(rs.next()){
				port = rs.getInt("port");
				dpid = Long.parseLong(rs.getString("dpid"), 16);
				addHost(new DpidPortPair(dpid, port));

				System.out.println("[LOAD] dpid:" + Utility.toDpidHexString(dpid) + ", port:" + port);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("loadHostInfo() is done.");
	}

	public void loadSwitchInfo(){
		ResultSet rs = dbm.getSwitchInfo();

		String name, dpidStr;
		long dpid;
		int port;
		try {
			while(rs.next()){
				name = rs.getString("name");
				if(!checkSwitchName(name)){
					switchList.add(new LogicalSwitch(name));
					System.out.println("[LOAD] name:" + name);
				}

				if((dpidStr = rs.getString("dpid")) != null){
					dpid = Long.parseLong(dpidStr, 16);
				}
				else{
					dpid = 0L;
				}
				port = rs.getInt("port");

				for(DpidPortPair p : hostList){
					if((p.getDpid() == dpid) && (p.getPort() == port)){
						getSwitchByName(name).add(p);
						System.out.println("[LOAD] name:" + name + ", dpid:" + Utility.toDpidHexString(dpid) + ", port:" + port);
						break;
					}
				}

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("loadSwitchInfo() is done.");
	}

	public void loadConnectionInfo(){
		ResultSet rs = dbm.getConnectionInfo();

		long dpid1, dpid2;
		int port1, port2;
		try {
			while(rs.next()){
				dpid1 = Long.parseLong(rs.getString("dpid1"), 16);
				dpid2 = Long.parseLong(rs.getString("dpid2"), 16);
				port1 = rs.getInt("port1");
				port2 = rs.getInt("port2");
				addTrunk(new DpidPortPair(dpid1, port1), new DpidPortPair(dpid2, port2));
				System.out.println("[LOAD] dpid1:" + Utility.toDpidHexString(dpid1) + ", port1:" + port1 + " <-> dpid2:" + Utility.toDpidHexString(dpid2) + ", port2:" + port2);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("loadConnectionInfo() is done.");
	}

	/**
	 * データパスリストのシャローコピーを返却する。
	 * @return データパスリストのシャローコピー
	 */
	@SuppressWarnings("unchecked")
	public TreeSet<Long> getDpidSet()
	{
		return (TreeSet<Long>) dpidSet.clone();
	}

	/**
	 * @return forwardingTable
	 */
	public synchronized ForwardingTable getForwardingTable() {
		return forwardingTable;
	}

	/**
	 * @return trunkList
	 */
	public CopyOnWriteArrayList<Trunk> getTrunkList() {
		return trunkList;
	}

	public CopyOnWriteArrayList<DpidPortPair> getHostList() {
		return hostList;
	}

	public void setSwitchList(CopyOnWriteArrayList<LogicalSwitch> switchList) {
		this.switchList = switchList;
	}

	public CopyOnWriteArrayList<LogicalSwitch> getSwitchList() {
		return switchList;
	}

	/*
	public DBManager getDbm() {
		return dbm;
	}*/
}
