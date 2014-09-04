package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.webservice;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.LinkedList;
import java.util.StringTokenizer;


public class WebClient{
	private static final String SERVER_IP = "127.0.0.1";
	private static final String URI = "IOperator";
	private static final String[] checkList = {"<", ">", "&", "/", ";", "*", "\"", "\'", "\\", "\t", "\b", "\r"};

	private Registry registry;
	private IOperator stub;

	public WebClient(){

		try {
			this.registry = LocateRegistry.getRegistry(SERVER_IP);
			this.stub = (IOperator)registry.lookup(URI);
		} catch (AccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public LinkedList<String> show(String arg){
		String str = null;
		StringTokenizer st = null;
		LinkedList<String> ret = new LinkedList<String>();
		LinkedList<String> cmd = new LinkedList<String>();

		cmd.add("show");
		cmd.add(arg);

		try {
			str = this.stub.executeCommand(cmd);
			st = new StringTokenizer(str, "\n");
			while(st.hasMoreTokens()){

				ret.add(st.nextToken());
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;
	}

	public String createHost(String dpid, String port){
		String ret = "";
		StringTokenizer st1, st2;
		LinkedList<String> cmd = new LinkedList<String>();
		cmd.add("create");
		cmd.add("host");
		cmd.add(dpid);

		if(!this.characterCheck(port)){
			return "Illegal character is contained.\n";
		}

		st1 = new StringTokenizer(port, " ");
		while(st1.hasMoreTokens()){
			st2 = new StringTokenizer(st1.nextToken(), ",");
			while(st2.hasMoreTokens()){
				cmd.add(st2.nextToken());
				try {
					ret += this.stub.executeCommand(cmd);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				cmd.removeLast();
			}
		}

		return ret;
	}

	public String createSwitch(String name){
		String ret = "";
		StringTokenizer st1, st2;
		LinkedList<String> cmd = new LinkedList<String>();
		cmd.add("create");
		cmd.add("switch");

		if(!this.characterCheck(name)){
			return "Illegal character is contained.\n";
		}

		st1 = new StringTokenizer(name, " ");
		while(st1.hasMoreTokens()){
			st2 = new StringTokenizer(st1.nextToken(), ",");
			while(st2.hasMoreTokens()){
				cmd.add(st2.nextToken());
				try {
					ret += this.stub.executeCommand(cmd);
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				cmd.removeLast();
			}
		}

		return ret;
	}

	public String createConnection(String dpid1, String port1, String dpid2, String port2){
		String ret = "";
		LinkedList<String> cmd = new LinkedList<String>();
		cmd.add("create");
		cmd.add("connection");
		cmd.add(dpid1);
		cmd.add(port1);
		cmd.add(dpid2);
		cmd.add(port2);

		if((!this.characterCheck(port1)) || (!this.characterCheck(port2))){
			return "Illegal character is contained.\n";
		}

		try {
			ret = this.stub.executeCommand(cmd);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;
	}

	public String deleteHost(String host){
		String ret = null;
		String id;
		LinkedList<String> cmd = new LinkedList<String>();
		cmd.add("delete");
		cmd.add("host");

		id = host.substring(3, 5);
		if(id.contains(" ")){
			id = id.substring(1, 2);
		}
		cmd.add(id);

		try {
			ret = this.stub.executeCommand(cmd);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;
	}

	public String deleteSwitch(String name){
		String ret = null;
		LinkedList<String> cmd = new LinkedList<String>();
		cmd.add("delete");
		cmd.add("switch");
		cmd.add(name);

		try {
			ret = this.stub.executeCommand(cmd);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;
	}

	public String deleteConnection(String id){
		String ret = null;
		LinkedList<String> cmd = new LinkedList<String>();
		cmd.add("delete");
		cmd.add("connection");
		cmd.add(id);

		try {
			ret = this.stub.executeCommand(cmd);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	public String linkHost(String sw, String[] ids){
		String ret = null;
		LinkedList<String> cmd = new LinkedList<String>();
		cmd.add("link");
		cmd.add(sw);
		for(String id : ids){
			System.err.println(id);
			cmd.add(id);
		}
		try {
			ret = this.stub.executeCommand(cmd);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;
	}

	public String unlinkHost(String sw, String id){
		String ret = null;
		LinkedList<String> cmd = new LinkedList<String>();
		cmd.add("unlink");
		cmd.add(sw);
		cmd.add(id);
		try {
			ret = this.stub.executeCommand(cmd);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;
	}

	private boolean characterCheck(String str){
		for(String s : WebClient.checkList){
			if(str.contains(s)){
				return false;
			}
		}

		return true;
	}
}