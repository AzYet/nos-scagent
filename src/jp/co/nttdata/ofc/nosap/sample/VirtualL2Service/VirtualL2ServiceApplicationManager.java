package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import jp.co.nttdata.ofc.nos.api.INOSApplication;
import jp.co.nttdata.ofc.nos.api.INOSApplicationManager;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.ThreadGenerator;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.TopologyManager;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.webservice.IOperator;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.webservice.Operator;

public class VirtualL2ServiceApplicationManager implements INOSApplicationManager{
	private TopologyManager topologyManager;

	public VirtualL2ServiceApplicationManager(){
		topologyManager = TopologyManager.getInstance();
	}

	@Override
	public boolean start() {
		// TODO 自動生成されたメソッド・スタブ
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			//topologyManager.getDbm().close();
		}

		Thread t = new Thread(new ThreadGenerator());
		t.start();

		return true;
	}

	@Override
	public void stop() {
		// TODO 自動生成されたメソッド・スタブ
		return;
	}

	@Override
	public boolean readConfigfile(String filename) {
		// TODO 自動生成されたメソッド・スタブ
		return true;
	}

	//@Override
	public boolean reloadConfigfile(String filename) {
		// TODO 自動生成されたメソッド・スタブ
		return true;
	}

	@Override
	public INOSApplication newInstance() {
		// TODO 自動生成されたメソッド・スタブ
		return new VirtualL2ServiceApplication();
	}

}
