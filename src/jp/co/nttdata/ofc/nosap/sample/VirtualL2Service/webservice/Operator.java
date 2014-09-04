package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.webservice;

import java.rmi.RemoteException;
import java.util.LinkedList;

import jp.co.nttdata.ofc.common.except.NosSocketIOException;
import jp.co.nttdata.ofc.common.util.MacAddress;
import jp.co.nttdata.ofc.nos.api.IFlowModifier;
import jp.co.nttdata.ofc.nos.api.NOSApi;
import jp.co.nttdata.ofc.nos.api.except.OFSwitchNotFoundException;
import jp.co.nttdata.ofc.nos.ofp.common.Flow;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.DpidPortPair;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.Utility;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.logical.LogicalSwitch;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.path.Path;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.path.PathManager;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.TopologyManager;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.Trunk;


public class Operator implements IOperator {
	private NOSApi nosApi;
	private TopologyManager topologyManager;
	private PathManager pathManager;

	public Operator(){
		this.nosApi = new NOSApi();
		this.topologyManager = TopologyManager.getInstance();
		this.pathManager = PathManager.getInstance();
	}

	@Override
	public String executeCommand(LinkedList<String> cmd) throws RemoteException {
		Utility.showCommand(cmd);

		String ret = "";
		if(cmd.size() > 0){
			for(String str : cmd){
				if(str.length() < str.getBytes().length){
					return "Multibyte character is not permitted.";
				}
			}

			if(cmd.get(0).equals("create")){
				if(cmd.size() < 2){
					ret += "create command require more arguments.\n";
					return ret;
				}
				if(cmd.get(1).equals("host")){
					if(cmd.size() < 4){
						ret += "create host command require more arguments.\n";
						return ret;
					}

					long dpid = this.parseDpid(cmd.get(2));
					int port = this.parsePort(cmd.get(3));

					if(dpid < 0L){
						ret += "DatapathID:" + cmd.get(2) + " is illegal.\n";
						return ret;
					}
					if(port < 0){
						ret += "Port:" + cmd.get(3) + " is illegal.\n";
						return ret;
					}


					if(topologyManager.checkDpidPortPair(dpid, port)){
						ret += "Host \"" + cmd.get(2) + ":" + cmd.get(3) + "\" is already created.\n";
						return ret;
					}

					if(topologyManager.addHost(new DpidPortPair(dpid, port))){
						//topologyManager.getDbm().insertHostInfo(dpid, port);
						ret += "Host \"" + cmd.get(2) + ":" + cmd.get(3) + "\" is created.\n";
					}
				}
				else if(cmd.get(1).equals("switch")){
					if(cmd.size() < 3){
						ret += "create switch command require more arguments.\n";
						return ret;
					}

					String swName = cmd.get(2);
					if(topologyManager.checkSwitchName(swName)){
						ret += "LogicalSwitch \"" + cmd.get(2) + "\" is already created.\n";
						return ret;
					}

					if(swName.length() > 15){
						ret += "SwitchName is restricted to 15 characters.\n";
						return ret;
					}

					if(topologyManager.getSwitchList().add(new LogicalSwitch(swName))){
						//topologyManager.getDbm().insertSwitchInfo(swName);
					}
					ret += "LogicalSwitch \"" + cmd.get(2) + "\" is created.\n";
				}
				else if(cmd.get(1).equals("connection")){
					if(cmd.size() < 6){
						ret += "create connection command require more arguments.\n";
						return ret;
					}

					long dpid1 = this.parseDpid(cmd.get(2));
					int port1 = this.parsePort(cmd.get(3));
					long dpid2 = this.parseDpid(cmd.get(4));
					int port2 = this.parsePort(cmd.get(5));

					if(dpid1 < 0L){
						ret += "DatapathID:" + cmd.get(2) + " is illegal.\n";
						return ret;
					}
					if(port1 < 0){
						ret += "Port:" + cmd.get(3) + " is illegal.\n";
						return ret;
					}
					if(dpid2 < 0L){
						ret += "DatapathID:" + cmd.get(4) + " is illegal.\n";
						return ret;
					}
					if(port2 < 0){
						ret += "Port:" + cmd.get(5) + " is illegal.\n";
						return ret;
					}

					DpidPortPair p1 = new DpidPortPair(dpid1, port1);
					DpidPortPair p2 = new DpidPortPair(dpid2, port2);

					for(DpidPortPair p : topologyManager.getHostList()){
						if(p.equals(p1)){
							ret += Utility.toDpidHexString(p1.getDpid()) + ":" + p1.getPort() + " is already used.\n";
							return ret;
						}
						if(p.equals(p2)){
							ret += Utility.toDpidHexString(p2.getDpid()) + ":" + p2.getPort() + " is already used.\n";
							return ret;
						}
					}

					for(Trunk t : topologyManager.getTrunkList()){
						if(t.contains(p1)){
							ret += Utility.toDpidHexString(p1.getDpid()) + ":" + p1.getPort() + " is already used.\n";
							return ret;
						}
						if(t.contains(p2)){
							ret += Utility.toDpidHexString(p2.getDpid()) + ":" + p2.getPort() + " is already used.\n";
							return ret;
						}
					}

					if(topologyManager.addTrunk(p1, p2)){
						//topologyManager.getDbm().insertConnectionInfo(p1.getDpid(), p2.getDpid(), p1.getPort(), p2.getPort());
						topologyManager.updateForwardingTable();
						this.deleteDetourRouteFlowEntries(p1.getDpid(), p2.getDpid());
						ret += "Connection " + Utility.toDpidHexString(p1.getDpid()) + ":" + p1.getPort()
						+ " <-> " + Utility.toDpidHexString(p2.getDpid()) + ":" + p2.getPort() + " is created.\n";
					}
					else{
						ret += "The same DatapathID:" + Utility.toDpidHexString(p1.getDpid()) + " is illegal.\n";
					}
				}
				else{
					ret += cmd.get(0) + " command does not support " + cmd.get(1) + " option.\n";
				}
			}
			else if(cmd.get(0).equals("delete")){
				if(cmd.size() < 2){
					ret += "delete command require more arguments.\n";
					return ret;
				}
				if(cmd.get(1).equals("host")){
					if(cmd.size() < 3){
						ret += "delete host command require more arguments.\n";
						return ret;
					}

					try{
						int index = Integer.parseInt(cmd.get(2));

						if((index < 0) || (topologyManager.getHostList().size() <= index)){
							ret += "Index:" + index + " is out of host list range.\n";
							return ret;
						}

						DpidPortPair p = topologyManager.getHostList().get(index);

						for(LogicalSwitch sw : topologyManager.getSwitchList()){
							if(sw.contains(p)){
								LinkedList<MacAddress> macList = sw.getKeys(p);
								for(MacAddress mac : macList){
									this.deleteHostRelatedFlowEntries(mac);
									sw.removeMac(mac);
								}
								sw.remove(p);
							}
						}

						//topologyManager.getDbm().deleteHostInfo(p.getDpid(), p.getPort());
						topologyManager.getHostList().remove(index);
						ret += "Host \"" + Utility.toDpidHexString(p.getDpid()) + ":" + p.getPort() + "\" is deleted.\n";

					}catch(NumberFormatException e){
						ret += "Index:" + cmd.get(2) + " is illegal.\n";
						return ret;
					}
				}
				else if(cmd.get(1).equals("switch")){
					if(cmd.size() < 3){
						ret += "delete switch command require more arguments.\n";
						return ret;
					}

					String swName = cmd.get(2);
					if(!topologyManager.checkSwitchName(swName)){
						ret += "LogicalSwitch \"" + cmd.get(2) + "\" does not exist.\n";
						return ret;
					}

					for(LogicalSwitch sw : topologyManager.getSwitchList()){
						if(sw.getName().equals(swName)){
							for(DpidPortPair p : sw.getDpidPortPairList()){
								LinkedList<MacAddress> macList = sw.getKeys(p);
								for(MacAddress mac : macList){
									this.deleteHostRelatedFlowEntries(mac);
									sw.removeMac(mac);
								}
							}
							//topologyManager.getDbm().deleteSwitchInfo(swName);
							topologyManager.getSwitchList().remove(sw);
							ret += "LogicalSwitch \"" + swName + "\" is deleted.\n";
							return ret;
						}
					}
				}
				else if(cmd.get(1).equals("connection")){
					if(cmd.size() < 3){
						ret += "delete connection command require more arguments.\n";
						return ret;
					}

					try{
						int index = Integer.parseInt(cmd.get(2));
						if(index < 0){
							ret += "Index:" + index + " is out of connection list range.\n";
							return ret;
						}

						int size;
						for(Trunk t : topologyManager.getTrunkList()){
							size = t.getEdgeList().size();
							if(index < size){
								long[] dpidPair = t.getDpidPair();
								int[] portPair = t.getEdgeList().get(index).getPorts();

								this.deleteConnectionRelatedFlowEntries(dpidPair[0], dpidPair[1], portPair[0], portPair[1]);
								//topologyManager.getDbm().deleteConnectionInfo(dpidPair[0], dpidPair[1], portPair[0], portPair[1]);
								if(t.remove(index) != null){
									topologyManager.updateForwardingTable();
								}
								ret += "Connection \"" + Utility.toDpidHexString(dpidPair[0]) + ":" + portPair[0] + " <-> "
										+ Utility.toDpidHexString(dpidPair[1]) + ":" + portPair[1] + "\" is deleted.\n";
								return ret;
							}
							else{
								index -= size;
							}
						}
					}catch(NumberFormatException e){
						ret += "Index:" + cmd.get(2) + " is illegal.\n";
						return ret;
					}
				}
				else{
					ret += cmd.get(0) + " command does not support " + cmd.get(1) + " option.\n";
				}
			}
			else if(cmd.get(0).equals("show")){
				if(cmd.size() < 2){
					ret += "show command require more arguments.\n";
					return ret;
				}

				if(cmd.get(1).equals("dpid")){
					ret += "\n";
					ret += "+-----+--------------+\n";
					ret += " [No.]   DatapathID   \n";
					ret += "+-----+--------------+\n";
					int i = 0;
					for(Long dpid : topologyManager.getDpidSet()){
						ret += Utility.formatA(i++) + Utility.toDpidHexString(dpid) + "\n";
					}
					ret += "+-----+--------------+\n";
					ret += "\n";
				}
				else if(cmd.get(1).equals("host")){
					ret += "\n";
					ret += "+-----+--------------+----+\n";
					ret += " [No.]   DatapathID   Port \n";
					ret += "+-----+--------------+----+\n";
					int i = 0;
					for(DpidPortPair p : topologyManager.getHostList()){
						ret += p.show(i++);
					}
					ret += "+-----+--------------+----+\n";
					ret += "\n";
				}
				else if(cmd.get(1).equals("switch")){
					if(topologyManager.getSwitchList().size() == 0){
						ret += "There are no switches.\n";
						return ret;
					}

					for(LogicalSwitch sw : topologyManager.getSwitchList()){
						ret += "\n";
						ret += "+-------------------------+\n";
						ret += " [SW-Name] " + sw.getName() + "\n";
						ret += "+-------------------------+\n";
						ret += " [No.]   DatapathID   Port \n";
						ret += "+-----+--------------+----+\n";
						ret += sw.show();
						ret += "+-----+--------------+----+\n";
					}
					ret += "\n";
				}
				else if(cmd.get(1).equals("connection")){
					ret += "\n";
					ret += "+-----+--------------+----+---+--------------+----+------+\n";
					ret += " [No.]   DatapathID   Port <->   DatapathID   Port  Cost\n";
					ret += "+-----+--------------+----+---+--------------+----+------+\n";
					int i = 0;
					LinkedList<String> l = null;
					for(Trunk t : topologyManager.getTrunkList()){
						l = t.show(i);
						for(String s : l){
							ret += s;
						}
						i += l.size();
					}
					ret += "+-----+--------------+----+---+--------------+----+------+\n";
					ret += "\n";
				}
				else if(cmd.get(1).equals("table")){
					ret += "\n";
					ret += "+--------------+--+--------------+------+----+\n";
					ret += " SRC-DatapathID -> DST-DatapathID  Cost  Port \n";
					ret += "+--------------+--+--------------+------+----+\n";
					ret += topologyManager.getForwardingTable().show();
					ret += "+--------------+--+--------------+------+----+\n";
					ret += "\n";
				}
				else if(cmd.get(1).equals("path")){
					ret += "\n";
					for(Path path : pathManager.getPathList()){
						ret += path.show();
					}
					ret += "\n";
				}
				else{
					ret += cmd.get(0) + " command does not support " + cmd.get(1) + " option.\n";
				}
			}
			else if(cmd.get(0).equals("link")){
				if(cmd.size() < 3){
					ret += "link command require more arguments.\n";
					return ret;
				}

				LogicalSwitch sw = topologyManager.getSwitchByName(cmd.get(1));
				if(sw == null){
					ret += "LogicalSwitch \"" + cmd.get(1) + "\" does not exist.\n";
					return ret;
				}

				int id;
				boolean free;
				for(int i = 2; i < cmd.size(); i++){
					try{
						id = Integer.parseInt(cmd.get(i));
						if((0 <= id) && (id < topologyManager.getHostList().size())){
							DpidPortPair p = topologyManager.getHostList().get(id);
							free = true;
							for(LogicalSwitch s : topologyManager.getSwitchList()){
								if(s.contains(p)){
									ret += "Host \"" + Utility.toDpidHexString(p.getDpid()) + ":" + p.getPort() + "\" already belongs to LogicalSwitch \"" + s.getName() + "\".\n";
									free = false;
									break;
								}
							}
							if(free){
								//topologyManager.getDbm().insertSwitchInfo(sw.getName(), p.getDpid(), p.getPort());
								sw.add(p);
								ret += "Host \"" + Utility.toDpidHexString(p.getDpid()) + ":" + p.getPort() + "\" is linked to LogicalSwitch \"" + sw.getName() + "\".\n";
							}

						}
						else{
							ret += "ID:" + id + " is illegal.\n";
						}
					}catch(NumberFormatException e){
						e.printStackTrace();
					}
				}
			}
			else if(cmd.get(0).equals("unlink")){
				if(cmd.size() < 3){
					ret += "unlink command require more arguments.\n";
					return ret;
				}

				LogicalSwitch sw = topologyManager.getSwitchByName(cmd.get(1));
				if(sw == null){
					ret += "LogicalSwitch \"" + cmd.get(1) + "\" does not exist.\n";
					return ret;
				}

				int index;
				for(int i = 2; i < cmd.size(); i++){
					try{
						index = Integer.parseInt(cmd.get(i));
						DpidPortPair p = sw.remove(index);

						LinkedList<MacAddress> macList = sw.getKeys(p);
						for(MacAddress mac : macList){
							this.deleteHostRelatedFlowEntries(mac);
							sw.removeMac(mac);
						}

						//topologyManager.getDbm().deleteSwitchInfo(sw.getName(), p.getDpid(), p.getPort());
						ret += "Host \"" + Utility.toDpidHexString(p.getDpid()) + ":" + p.getPort() + "\" is unlinked to LogicalSwitch \"" + sw.getName() + "\".\n";
					}catch(NumberFormatException e){
						e.printStackTrace();
					}
				}
			}
			else{
				ret += "\"" + cmd.get(0) + "\" is undefined command.\n";
			}
		}
		return ret;
	}

	private long parseDpid(String str){
		long dpid;

		try{
			dpid = Long.parseLong(str, 16);
			if(!topologyManager.getDpidSet().contains(new Long(dpid))){
				return -1L;
			}
		}catch(NumberFormatException e){
			return -1L;
		}

		return dpid;
	}

	private int parsePort(String str){
		int port;

		try{
			port = Integer.parseInt(str);
			if((port <= 0) || (TopologyManager.PORT_NUM < port)){
				return -1;
			}
		}catch(NumberFormatException e){
			return -1;
		}

		return port;
	}

	public void deleteHostRelatedFlowEntries(MacAddress mac){

		Flow flow1 = Utility.createFlow(-1, mac, null, -1);
		Flow flow2 = Utility.createFlow(-1, null, mac, -1);

		IFlowModifier imodifier1, imodifier2;
		for(Long dpid : topologyManager.getDpidSet()){
			try {
				imodifier1 = this.nosApi.createFlowModifierInstance(dpid, flow1);
				imodifier2 = this.nosApi.createFlowModifierInstance(dpid, flow2);
				imodifier1.setDeleteCommand();
				imodifier2.setDeleteCommand();
				imodifier1.send();
				imodifier2.send();
			} catch (OFSwitchNotFoundException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			} catch (NosSocketIOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
		}
	}

	public void deleteConnectionRelatedFlowEntries(long dpid1, long dpid2, int port1, int port2){
		LinkedList<Path> pathList = pathManager.getPathList(dpid1, dpid2, port1, port2);

		MacAddress[] macs;
		Flow flow;
		IFlowModifier imodifier;
		for(Path path : pathList){
			macs = path.getMacs();
			flow = Utility.createFlow(-1, macs[0], macs[1], -1);
			for(long dpid : topologyManager.getDpidSet()){
				try {
					imodifier = this.nosApi.createFlowModifierInstance(dpid, flow);
					imodifier.setDeleteCommand();
					imodifier.send();
				} catch (OFSwitchNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NosSocketIOException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			}
		}

		pathManager.getPathList().removeAll(pathList);
	}

	public void deleteDetourRouteFlowEntries(long dpid1, long dpid2){
		LinkedList<Path> pathList = pathManager.getPathList(dpid1, dpid2);

		MacAddress[] macs;
		Flow flow;
		IFlowModifier imodifier;
		for(Path path : pathList){
			macs = path.getMacs();
			flow = Utility.createFlow(-1, macs[0], macs[1], -1);
			for(long dpid : topologyManager.getDpidSet()){
				try {
					imodifier = this.nosApi.createFlowModifierInstance(dpid, flow);
					imodifier.setDeleteCommand();
					imodifier.send();
				} catch (OFSwitchNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NosSocketIOException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			}
		}

		pathManager.getPathList().removeAll(pathList);
	}
}
