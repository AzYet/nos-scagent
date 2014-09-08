package jp.co.nttdata.ofc.controller;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import com.nsfocus.securitycontrolleragent.restlet.RestApiServer;

import jp.co.nttdata.ofc.nos.api.INOSApplication;
import jp.co.nttdata.ofc.nos.api.INOSApplicationManager;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.VirtualL2ServiceApplication;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.ThreadGenerator;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.TopologyManager;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.webservice.IOperator;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.webservice.Operator;


public class VncControllerApplicationManager implements INOSApplicationManager
{


	private TopologyManager topologyManager;

	public VncControllerApplicationManager(){
		topologyManager = TopologyManager.getInstance();
		TopologyManager.USE_LLDP = true;
	}
	
	
	@Override
	public boolean start()
	{

		try{

			Operator operator = new Operator();
			IOperator stub = (IOperator)UnicastRemoteObject.exportObject(operator, 0);
			Registry registry = LocateRegistry.getRegistry();

			registry.bind("IOperator", stub);

			System.out.println("Server Ready!");
		}catch(Exception e){
			System.err.println("Server exception:" + e.toString());
			e.printStackTrace();
		}

		try {
			/*
			topologyManager.getDbm().connect();
			topologyManager.loadHostInfo();
			topologyManager.loadSwitchInfo();
			topologyManager.loadConnectionInfo();
			*/
			

			if(topologyManager.getForwardingTable().generate()){
				System.out.println("ForwardingTable is generated.");
			}
			else{
				System.out.println("ForwardingTable is not generated.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			//topologyManager.getDbm().close();
		}

		Thread t = new Thread(new ThreadGenerator());
		t.start();

		System.out.println("method start() is called");
		try {
			RestApiServer.runServer();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Rest Server is running , non-block process!");
		
		System.out.println("Start");
		return true;
	}

	@Override
	public void stop()
	{
		System.out.println("Stop");
	}

	@Override
	public boolean readConfigfile(String filename)
	{
		System.out.println("Read file: "+filename);
		return true;
	}

	@Override
	public INOSApplication newInstance()
	{
		System.out.println("New instance.");
		return new VirtualL2ServiceApplication();
	}
}
