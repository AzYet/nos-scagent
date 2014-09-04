package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.webservice;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

public interface IOperator extends Remote {
	String executeCommand(LinkedList<String> cmd) throws RemoteException;
}
