package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DBManager {
	private static final String CLASS_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
	//private static final String DB_PATH = "jdbc:derby:/root/MyDB";
	private static final String DB_PATH = "jdbc:derby:/usr/src/vnc-trial-nsfocus_2.4-0/MyDB";
	private static final String DB_USER_NAME = "nttdata";
	private static final String DB_PASSWORD = "nttdata";

	private Connection conn = null;
	private Statement state = null;
	private ResultSet rs = null;
	private Properties prop = null;

	public DBManager(){
		try {
			Class.forName(CLASS_NAME);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		prop = new Properties();
		prop.put("user", DBManager.DB_USER_NAME);
		prop.put("password", DBManager.DB_PASSWORD);
	}

	public void connect() throws SQLException{
		conn = DriverManager.getConnection(DB_PATH, prop);
		conn.setAutoCommit(true);
		state = conn.createStatement();
	}

	public void close(){
		try {
			if(rs != null){
				rs.close();
			}
			if(state != null){
				state.close();
			}
			if(conn != null){
				conn.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void insertHostInfo(long dpid, int port){
		String query = null;
		try {
			connect();

			String dpidStr;
			dpidStr = "\'"+ Utility.toDpidHexString(dpid) + "\'";
			query = "insert into hostinfo (dpid, port) values(" + dpidStr + ", " + port + ")";
			state.executeUpdate(query);
			System.out.println("[DB] " + query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.err.println(query);
			e.printStackTrace();
		} finally {
			close();
		}
	}

	public void insertSwitchInfo(String name){
		this.insertSwitchInfo(name, 0L, 0);
	}

	public void insertSwitchInfo(String name, long dpid, int port){
		String query = null;
		try {
			connect();

			String nameStr, dpidStr;
			nameStr = "\'" + name + "\'";
			if(dpid > 0){
				dpidStr = "\'" + Utility.toDpidHexString(dpid) + "\'";
			}
			else{
				dpidStr = "null";
			}

			query = "insert into switchinfo (name, dpid, port) values(" + nameStr
			+ ", " + dpidStr + ", " + port + ")";
			state.executeUpdate(query);
			System.out.println("[DB] " + query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.err.println(query);
			e.printStackTrace();
		} finally {
			close();
		}
	}

	public void insertConnectionInfo(long dpid1, long dpid2, int port1, int port2){
		String query = null;
		try {
			connect();

			String dpid1Str, dpid2Str;
			dpid1Str = "\'"+ Utility.toDpidHexString(dpid1) + "\'";
			dpid2Str = "\'"+ Utility.toDpidHexString(dpid2) + "\'";

			query = "insert into connectioninfo (dpid1, dpid2, port1, port2) values(" + dpid1Str
			+ ", " + dpid2Str + ", " + port1 + ", " + port2 + ")";
			state.executeUpdate(query);
			System.out.println("[DB] " + query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.err.println(query);
			e.printStackTrace();
		} finally {
			close();
		}
	}

	public void deleteHostInfo(long dpid, int port){
		String query1 = null;
		String query2 = null;
		try {
			connect();

			query1 = "delete from hostinfo where dpid = \'" +  Utility.toDpidHexString(dpid) + "\' and port = " + port;
			query2 = "delete from switchinfo where dpid = \'" +  Utility.toDpidHexString(dpid) + "\' and port = " + port;
			state.executeUpdate(query1);
			state.executeUpdate(query2);
			System.out.println("[DB] " + query1);
			System.out.println("[DB] " + query2);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.err.println(query1);
			System.err.println(query2);
			e.printStackTrace();
		} finally {
			close();
		}
	}

	public void deleteSwitchInfo(String name){
		String query = null;
		try{
			connect();

			query = "delete from switchinfo where name = \'" + name + "\'";
			state.executeUpdate(query);
			System.out.println("[DB] " + query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.err.println(query);
			e.printStackTrace();
		} finally {
			close();
		}
	}

	public void deleteSwitchInfo(String name, long dpid, int port){
		String query = null;
		try{
			connect();

			String dpidStr;
			dpidStr = Utility.toDpidHexString(dpid);

			query = "delete from switchinfo where name = \'" + name + "\' and dpid = \'" + dpidStr + "\' and port = " + port;
			state.executeUpdate(query);
			System.out.println("[DB] " + query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.err.println(query);
			e.printStackTrace();
		} finally {
			close();
		}
	}

	public void deleteConnectionInfo(long dpid1, long dpid2, int port1, int port2){
		String query = null;
		try{
			connect();

			String dpid1Str, dpid2Str;
			dpid1Str = "\'" + Utility.toDpidHexString(dpid1) + "\'";
			dpid2Str = "\'" + Utility.toDpidHexString(dpid2) + "\'";
			query = "delete from connectioninfo where dpid1 = " + dpid1Str + " and dpid2 = " + dpid2Str
			+ " and port1 = " + port1 + " and port2 = " + port2;
			state.executeUpdate(query);
			System.out.println("[DB] " + query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.err.println(query);
			e.printStackTrace();
		} finally {
			close();
		}
	}

	public void deletePathInfo(long cookie){
		String query = null;
		try{
			connect();

			query = "delete from pathinfo where cookie = " + cookie;
			state.executeUpdate(query);
			System.out.println("[DB] " + query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.err.println(query);
			e.printStackTrace();
		} finally {
			close();
		}
	}

	public ResultSet getHostInfo(){
		if(state == null){
			System.out.println("connect process is not invocated.");
			return null;
		}

		String query = "select * from hostinfo";
		try {
			rs = state.executeQuery(query);
			System.out.println("[DB] " + query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("getHostInfo() is done.");

		return rs;
	}

	public ResultSet getSwitchInfo(){
		if(state == null){
			System.out.println("connect process is not invocated.");
			return null;
		}

		String query = "select * from switchinfo";
		try {
			rs = state.executeQuery(query);
			System.out.println("[DB] " + query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("getSwitchInfo() is done.");

		return rs;
	}

	public ResultSet getConnectionInfo(){
		if(state == null){
			System.out.println("connect process is not invocated.");
			return null;
		}

		String query = "select * from connectioninfo";
		try {
			rs = state.executeQuery(query);
			System.out.println("[DB] " + query);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("getConnectionInfo() is done.");

		return rs;
	}
}
