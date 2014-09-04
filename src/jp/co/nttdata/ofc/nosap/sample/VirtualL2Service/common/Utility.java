package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common;

import java.util.LinkedList;
import java.util.List;

import jp.co.nttdata.ofc.common.except.NosSocketIOException;
import jp.co.nttdata.ofc.common.util.IpAddress;
import jp.co.nttdata.ofc.common.util.MacAddress;
import jp.co.nttdata.ofc.nos.api.IFlowModifier;
import jp.co.nttdata.ofc.nos.api.INOSApi;
import jp.co.nttdata.ofc.nos.api.NOSApi;
import jp.co.nttdata.ofc.nos.api.except.ArgumentInvalidException;
import jp.co.nttdata.ofc.nos.api.except.OFSwitchNotFoundException;
import jp.co.nttdata.ofc.nos.api.except.SwitchPortNotFoundException;
import jp.co.nttdata.ofc.nos.api.vo.AggregateFlowStatsVO;
import jp.co.nttdata.ofc.nos.api.vo.TableStatsVO;
import jp.co.nttdata.ofc.nos.common.constant.OFPConstant.OFWildCard;
import jp.co.nttdata.ofc.nos.ofp.common.Flow;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.TopologyManager;

public class Utility {
	private static long cookie = 0L;
	private static NOSApi nosApi = new NOSApi();

	public static synchronized long getNextCookie(){
		return ++cookie;
	}

	// 受信ポート、送信元MACアドレス、宛先MACアドレス、イーサタイプを指定してフローを生成する
	// 無効な値(負の値またはNULL)を指定したパラメータはANYとして扱われる
	// VLAN-ID以降の8項目は全てANY
	public static Flow createFlow(int inPort, MacAddress srcMac, MacAddress dstMac, int etherType){
		return Utility.createFlow(inPort, srcMac, dstMac, etherType, -1, (short)-1, null, null, (short)-1, (short)-1, -1, -1);
	}

	// 12タプルの値を指定してフローを生成する
	// 無効な値(負の値またはNULL)を指定した項目はANYとして扱われる
	public static Flow createFlow(int inPort, MacAddress srcMac, MacAddress dstMac,
			int etherType, int vlanId, short vlanPriority, IpAddress srcIp, IpAddress dstIp,
			short proto, short tos, int srcPort, int dstPort){
		Flow flow = new Flow();

		if(inPort < 0){
			flow.wildCards |= OFWildCard.IN_PORT;
		}else{
			flow.inPort = inPort;
		}

		if(srcMac == null){
			flow.wildCards |= OFWildCard.SRC_MACADDR;
		}else{
			flow.srcMacaddr = srcMac;
		}

		if(dstMac == null){
			flow.wildCards |= OFWildCard.DST_MACADDR;
		}else{
			flow.dstMacaddr = dstMac;
		}

		if(etherType < 0){
			flow.wildCards |= OFWildCard.ETHER_TYPE;
		}else{
			flow.etherType = etherType;
		}

		if(vlanId < 0){
			flow.wildCards |= OFWildCard.VLAN_ID;
		}else{
			flow.vlanId = vlanId;
		}

		if(vlanPriority < 0){
			flow.wildCards |= OFWildCard.VLAN_PRIORITY;
		}else{
			flow.vlanPriority = vlanPriority;
		}

		if(srcIp == null){
			flow.wildCards |= OFWildCard.SRC_IPADDR_ALL;
		}else{
			flow.srcIpaddr = srcIp;
		}

		if(dstIp == null){
			flow.wildCards |= OFWildCard.DST_IPADDR_ALL;
		}else{
			flow.dstIpaddr = dstIp;
		}

		if(proto < 0){
			flow.wildCards |= OFWildCard.NW_PROTO;
		}else{
			flow.proto = proto;
		}

		if(tos < 0){
			flow.wildCards |= OFWildCard.TOS;
		}else{
			flow.tos = tos;
		}

		if(srcPort < 0){
			flow.wildCards |= OFWildCard.SRC_PORT;
		}else{
			flow.srcPort = srcPort;
		}

		if(dstPort < 0){
			flow.wildCards |= OFWildCard.DST_PORT;
		}else{
			flow.dstPort = dstPort;
		}

		return flow;
	}

	public static void deleteAllFlowEntries(INOSApi nosApi, long dpid){
		// OFSが持つFlowTableを初期化する処理
		// (Test Precondition-1)
		Flow flow = new Flow();  // FlowEntryのひな形作成
		flow.setAllWildCards();  // FlowEntryのHeader Fieldが全てANY

		IFlowModifier imodifier;
		try {
			imodifier = nosApi.createFlowModifierInstance(dpid, flow);
			imodifier.setDeleteCommand();

			// FlowModifyを実行する処理
			imodifier.send();

			// Fail safe
			try
			{
				Thread.sleep(1000);
			}
			catch(InterruptedException e2)
			{
				e2.printStackTrace();
			}
		} catch (OFSwitchNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (NosSocketIOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
	}

	public static long[] getCounters(long dpid, int port){
		long[] ret = {0L, 0L, 0L};

		List<TableStatsVO> tableStats;
		AggregateFlowStatsVO flowStats;
		Flow flow;

		try {
			tableStats = nosApi.getTableStatsRequest(dpid);
			flow = new Flow();
			flow.setAllWildCards();

			if((flowStats = nosApi.getAggregateFlowStatsRequest(dpid, flow, tableStats.get(0).tableId, port)) == null){
				System.err.println("flowStats is null.");
				return null;
			}

			ret[0] = flowStats.packetCount;
			ret[1] = flowStats.byteCount;
			ret[2] = flowStats.flowCount;

			return ret;

		} catch (NosSocketIOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (OFSwitchNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (SwitchPortNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (ArgumentInvalidException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		System.err.println("[" + Utility.toDpidHexString(dpid) + ":" + port +"]");

		return ret;
	}

	public static void showCommand(LinkedList<String> cmd){
		String str = "[COMMAND]";
		for(String s : cmd){
			str += " " + s;
		}

		System.out.println(str);

		return;
	}

	public static String toDpidHexString(long dpid){
		String ret = Long.toHexString(dpid);

		while(ret.length() < TopologyManager.DPID_LENGTH){
			ret = "0" + ret;
		}

		return ret;
	}

	public static String formatA(int i){
		return String.format(" [%1$3d]  ", i);
	}

	public static String formatB(long l){
		return String.format("%1$2d", l);
	}
}
