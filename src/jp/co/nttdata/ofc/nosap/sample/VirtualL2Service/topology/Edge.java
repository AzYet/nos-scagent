package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology;

import java.sql.Timestamp;

public class Edge {
	private int[] ports;
	private long[] packets;
	private Timestamp timestamp;

	public Edge(int p1, int p2){
		this.ports = new int[2];
		this.ports[0] = p1;
		this.ports[1] = p2;
		this.packets = new long[2];
		this.packets[0] = 0L;
		this.packets[1] = 0L;
		this.timestamp = new Timestamp(System.currentTimeMillis());
	}

	public boolean equals(Edge edge){
		if(this.ports[0] == edge.getPorts()[0] && this.ports[1] == edge.getPorts()[1]){
			return true;
		}
		else{
			return false;
		}
	}

	public void setPacket(long packet){
		this.packets[0] = this.packets[1];
		this.packets[1] = packet;
	}

	public long getCost(){
		return this.packets[1] - this.packets[0] + TopologyManager.DEFAULT_COST;
	}

	/**
	 * @return ports
	 */
	public int[] getPorts() {
		return ports;
	}

	/**
	 * @param ports セットする ports
	 */
	public void setPorts(int[] ports) {
		this.ports = ports;
	}

	/**
	 * @return packets
	 */
	public long[] getPackets() {
		return packets;
	}

	/**
	 * @param packets セットする packets
	 */
	public void setPackets(long[] packets) {
		this.packets = packets;
	}

	public long getTimestamp(){
		return this.timestamp.getTime();
	}

	public void setTimestamp(){
		this.timestamp.setTime(System.currentTimeMillis());
	}
}
