package com.nsfocus.scagent.restlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.DpidPortPair;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.Edge;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.ForwardingTable;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.TopologyManager;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.Trunk;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.ForwardingTable.ForwardingTableEntry;

import org.restlet.Component;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nsfocus.scagent.device.DeviceManager;

public class RestApiServer extends ServerResource {
	public static void runServer() throws Exception {

		// new Server(Context.getCurrent(), Protocol.HTTP,
		// 8182,RestApiServer.class).start();
		Component component = new Component();
		component.getServers().add(Protocol.HTTP, 8182);
		component.getDefaultHost().attach(new RestApiApplication());
		component.start();
	}

	@Get("json")
	public String toString() {
		String type = (String) getRequestAttributes().get("type");
		if (type != null) {
			if(type.equals("device")){
				Form form = getQuery();
				String mac = form.getFirstValue("mac");
				Gson gson = new Gson();
				if (mac != null) {
					DpidPortPair dpidPort = DeviceManager.getInstance()
					.findHostByMac(mac);
					return gson.toJson(dpidPort);
				} else {
					return gson.toJson(DeviceManager.getInstance()
							.getMacDpidPortMap());
				}
			}else if (type.equalsIgnoreCase("route")){
				Form form = getQuery();
				String ends = form.getFirstValue("ends").toUpperCase();
				if(ends !=null){
					String[] split = ends.split("-");
					if(split.length !=2){
						return "{\"result\":\"param error\"}";
					}
					DpidPortPair start = DeviceManager.getInstance()
					.findHostByMac(split[0]);
					DpidPortPair end = DeviceManager.getInstance()
					.findHostByMac(split[1]);
					String res = "";
					if(start == null){
						res += " "+split[0]+" ";
					}
					if(end == null){
						res = " "+split[1]+" ";
					}
					if(!res.equals("")){
						return "{\"result\":\"can't find "+res+" \"}";
					}
					ArrayList<DpidPortPair> path = new ArrayList<DpidPortPair>();
					if(start.getDpid() == end.getDpid()){
						path.add(start);
						path.add(end);
						return new Gson().toJson(path);
					}
					ForwardingTable forwardingTable = TopologyManager.getInstance().getForwardingTable();
					CopyOnWriteArrayList<Trunk> trunkList = TopologyManager.getInstance().getTrunkList();
					boolean proceed =true;
					DpidPortPair currentPair = start;
					while(proceed) {
						boolean noPath = true;
						path.add(currentPair);
						for ( ForwardingTableEntry entry : forwardingTable.getTable()){
							if(entry.getKey()[0]==currentPair.getDpid() && entry.getKey()[1] == end.getDpid()){
								noPath = false;
								Integer portForward = entry.getPortList().get(0);
								Long dpid = currentPair.getDpid();
								path.add(new DpidPortPair(dpid,portForward));
								//find which dpid portForward links to'
								for(Trunk trunk : trunkList){
									if(trunk.getDpidPair()[0] == dpid ){
										for(Edge edge : trunk.getEdgeList()){
											if(edge.getPorts()[0] == portForward ){
												Long nextDpid = trunk.getDpidPair()[1];
												int nextPort = edge.getPorts()[1];
												currentPair = new DpidPortPair(nextDpid,nextPort);
												if(nextDpid == end.getDpid()){
													proceed =false;
												}
											}
										}
									}else if(trunk.getDpidPair()[1] == dpid ){
										for(Edge edge : trunk.getEdgeList()){
											if(edge.getPorts()[1] == portForward ){
												Long nextDpid = trunk.getDpidPair()[0];
												int nextPort = edge.getPorts()[0];
												currentPair = new DpidPortPair(nextDpid,nextPort);
												if(nextDpid == end.getDpid()){
													proceed =false;
												}
											}
										}
									}
								}
								break;
							}
						}
						if(noPath){
							return "[]";
						}
					}
					path.add(currentPair);
					path.add(end);
					return new Gson().toJson(path);
				}else{
					return "{\"result\":\"ends needed\"}";
				}

			}else if(type.equalsIgnoreCase("test")) {
				return new Gson().toJson(TopologyManager.getInstance().getTrunkList());
			}
		}
		return "{\"result\":\"error\"}";
		// return "testValue ="+ ""
		// +"\nResource URI  : " + getReference() + '\n' + "Root URI      : "
		// + getRootRef() + '\n' + "Routed part   : "
		// + getReference().getBaseRef() + '\n' + "Remaining part: "
		// + getReference().getRemainingPart();
	}

	@Post
	public Representation acceptItem(Representation entity) {
		Representation result = null;
		// Parse the given representation and retrieve data
		try {
			String text = entity.getText();
			Gson gson = new Gson();
			JsonParser parser = new JsonParser();
			JsonObject jsonObject = parser.parse(text).getAsJsonObject();
			String id = jsonObject.get("id").getAsString();
			System.out.println("id: " + id);
			JsonArray ifs = jsonObject.get("ifs").getAsJsonArray();
			for (int i = 0; i < ifs.size(); i++) {
				JsonObject ifObj = gson.fromJson(ifs.get(i), JsonObject.class);
				JsonElement jsonElement = ifObj.get("abc");
				String connectTo = ifObj.get("connect_to").getAsString();
				String mac = ifObj.get("mac").getAsString();
				System.out
						.println("connect_to:" + connectTo + "\t mac: " + mac);
			}
			result = new StringRepresentation(text, MediaType.APPLICATION_JSON);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			result = null;
			e.printStackTrace();
		}

		return result;
	}

}
