package jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology;

import jp.co.nttdata.ofc.common.util.MacAddress;
import jp.co.nttdata.ofc.common.util.Translator;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.NosFactory;
import jp.co.nttdata.ofc.protocol.packet.EthernetPDU;
import jp.co.nttdata.ofc.protocol.packet.EthernetPDU.EtherType;
import jp.co.nttdata.ofc.protocol.packet.LldpPDU;
import jp.co.nttdata.ofc.protocol.packet.LldpPDU.TlvType;
import jp.co.nttdata.ofc.protocol.packet.TlvPDU;

public class LldpPacketGenerator {
	public static final byte[] LLDP_NTTDATA_PACKET_ID = new byte[] { 0x6E, 0x74, 0x74, 0x64, 0x61, 0x74, 0x61, 0x2D, 0x63, 0x6F, 0x72, 0x70 };
	public static final String LLDP_MULTICAST_ADDR = "01:80:C2:00:00:0E";

	private final EthernetPDU base = new EthernetPDU();

	public LldpPacketGenerator(){
		this.createBaseLldpPacket();
	}

	private TlvPDU createTlvPacket(int type, int length, byte[] value){
		TlvPDU tlv = new TlvPDU();

		tlv.type = type;
		tlv.length = length;
		tlv.value = value;

		return tlv;
	}

	private void createBaseLldpPacket(){
		this.base.srcMacaddr = null;
		this.base.dstMacaddr = NosFactory.createMacAddress(LLDP_MULTICAST_ADDR);
		this.base.etherType = EtherType.LLDP;

		LldpPDU lldp = new LldpPDU();
		this.base.next = lldp;

		TlvPDU chassisIdTlv = createTlvPacket(TlvType.CHASSIS_ID, 7, new byte[] { 0x04, 0, 0, 0, 0, 0, 0 });

		TlvPDU portIdTlv = createTlvPacket(TlvType.PORT_ID, 3, new byte[] { 0x02, 0, 0 });
		TlvPDU ttlTlv = createTlvPacket(TlvType.TTL, 2, new byte[] { 0, 0x78 }); // 0x78->120
		TlvPDU dpidTlv = createTlvPacket(TlvType.ORG_SPECIFIC, 12, new byte[12]);
		TlvPDU idTlv = createTlvPacket(TlvType.ORG_SPECIFIC, 12, LLDP_NTTDATA_PACKET_ID);
		TlvPDU endTlv = createTlvPacket(TlvType.END, 0, null);

		lldp.chassisId = chassisIdTlv;
		lldp.portId = portIdTlv;
		lldp.ttl = ttlTlv;
		lldp.optional.add(dpidTlv);
		lldp.optional.add(idTlv);
		lldp.optional.add(endTlv);
	}

	public byte[] getByte(long dpid){
		LldpPDU lldp = (LldpPDU)base.next;
		TlvPDU dpidTlv = lldp.optional.get(0);

		byte[] binDpid = Translator.longToByte(dpid);
		byte[] binMac = new byte[EthernetPDU.MAC_ADDR_BYTE_LEN];

		System.arraycopy(binDpid, 2, binMac, 0, EthernetPDU.MAC_ADDR_BYTE_LEN);
		System.arraycopy(binDpid, 2, lldp.chassisId.value, 1, EthernetPDU.MAC_ADDR_BYTE_LEN);
		System.arraycopy(binDpid, 0, dpidTlv.value, 4, 8); // 8 is dpid size

		this.base.srcMacaddr = NosFactory.createMacAddress(MacAddress.ZERO_MAC_ADDR);

		return this.base.getBytes();
	}

	public byte[] getByte(long dpid, long macaddr){
		this.base.srcMacaddr = NosFactory.createMacAddress(macaddr);
		return this.base.getBytes();
	}

}
