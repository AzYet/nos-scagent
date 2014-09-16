package com.nsfocus.scagent.model;


import java.util.Arrays;

import jp.co.nttdata.ofc.common.except.NosException;
import jp.co.nttdata.ofc.common.util.MacAddress;
import jp.co.nttdata.ofc.nos.common.constant.OFPConstant.OFWildCard;
import jp.co.nttdata.ofc.nos.ofp.common.Flow;

import com.google.gson.JsonObject;
import com.nsfocus.scagent.utility.HexString;
import com.nsfocus.scagent.utility.MACAddress;


public class MatchArguments {

    final public static int OFPFW_ALL = ((1 << 22) - 1);
    public static final short VLAN_UNTAGGED = (short) 0xffff;

    protected int wildcards;
    protected short inputPort;
    protected byte[] dataLayerSource;
    protected byte[] dataLayerDestination;
    protected short dataLayerVirtualLan;
    protected byte dataLayerVirtualLanPriorityCodePoint;
    protected short dataLayerType;
    protected byte networkTypeOfService;
    protected byte networkProtocol;
    protected int networkSource;
    protected int networkDestination;
    protected short transportSource;
    protected short transportDestination;

    /**
     * @param args
     */
    public MatchArguments() {
        this.wildcards = OFPFW_ALL;
        this.dataLayerDestination = new byte[] { 0x0, 0x0, 0x0, 0x0, 0x0, 0x0 };
        this.dataLayerSource = new byte[] { 0x0, 0x0, 0x0, 0x0, 0x0, 0x0 };
        this.dataLayerVirtualLan = VLAN_UNTAGGED;
        this.dataLayerVirtualLanPriorityCodePoint = 0;
        this.dataLayerType = 0;
        this.inputPort = 0;
        this.networkProtocol = 0;
        this.networkTypeOfService = 0;
        this.networkSource = 0;
        this.networkDestination = 0;
        this.transportDestination = 0;
        this.transportSource = 0;
    }



    /**
     * 比较两个Match的关系
     * 
     * @param otherMatch
     * @return this is a xx of otherMatch
     */
    public MatchRelation compareWith(MatchArguments otherMatch) {
        boolean superSet = true;
        boolean equal = true;
        boolean subSet = true;

        if (Arrays.equals(this.dataLayerSource, new byte[] { 0x0, 0x0, 0x0,
                0x0, 0x0, 0x0 })) {// this 未指定src mac
            if (!Arrays.equals(otherMatch.dataLayerSource, // other指定了 src mac
                    new byte[] { 0x0, 0x0, 0x0, 0x0, 0x0, 0x0 })) {
                subSet = false; // 不可能是子集
                equal = false;
            }
        } else { // this指定了 src mac
            if (Arrays.equals(otherMatch.dataLayerSource, // other未指定了 src mac
                    new byte[] { 0x0, 0x0, 0x0, 0x0, 0x0, 0x0 })) {
                superSet = false; // 不可能是父集
                equal = false;
            } else if (Arrays.equals(otherMatch.dataLayerSource, // other指定了 src
                                                                 // mac
                    this.dataLayerSource)) {
                // unrelated = false; //即使指定相同的域，也可能不相关
            } else { // 二者有一域不同，则肯定不相关
                return MatchRelation.UNRELATED;
            }
        }

        if (Arrays.equals(this.dataLayerDestination, new byte[] { 0x0, 0x0,
                0x0, 0x0, 0x0, 0x0 })) {// this 未指定dst mac
            if (!Arrays.equals(otherMatch.dataLayerDestination, // other指定了 dst
                                                                // mac
                    new byte[] { 0x0, 0x0, 0x0, 0x0, 0x0, 0x0 })) {
                subSet = false; // 不可能是子父集
                equal = false;
            }
        } else { // this指定了 dst mac
            if (Arrays.equals(otherMatch.dataLayerDestination, // other未指定了 dst
                                                               // mac
                    new byte[] { 0x0, 0x0, 0x0, 0x0, 0x0, 0x0 })) {
                superSet = false; // 不可能是父集
                equal = false;
            } else if (Arrays.equals(otherMatch.dataLayerDestination, // other指定了
                                                                      // dst mac
                    this.dataLayerDestination)) {
                // unrelated = false;
            } else { // 二者有一域不同，则肯定不相关
                return MatchRelation.UNRELATED;
            }
        }

        if (this.dataLayerType == 0) {
            if (otherMatch.dataLayerType != 0) {
                subSet = false;
                equal = false;
            }
        } else if (otherMatch.dataLayerType != 0) {
            if (this.dataLayerType != otherMatch.dataLayerType) {// this
                                                                 // 未指定dataLayerType
                return MatchRelation.UNRELATED;
            }
        } else {
            superSet = false;
            equal = false;
        }

        /*
         * if(this.dataLayerVirtualLan != otherMatch.dataLayerVirtualLan){//this
         * 未指定dataLayerType return MatchRelation.UNRELATED; }
         */

        // TODO: dataLayerVirtualLanPriorityCodePoint

        // TODO: 未考虑Mask的情况
        if (this.networkSource == 0) {// this 未指定networkSource
            if (otherMatch.networkSource != 0) {
                subSet = false; // 不可能是子集
                equal = false;
            }
        } else { // this指定了 networkSource
            if (otherMatch.networkSource == 0) {
                superSet = false; // 不可能是父集
                equal = false;
            } else if (this.networkSource == otherMatch.networkSource) {
                // unrelated = false;
            } else { // 二者有一域不同，则肯定不相关
                return MatchRelation.UNRELATED;
            }
        }

        if (this.networkDestination == 0) {// this 未指定networkDestination
            if (otherMatch.networkDestination != 0) {
                subSet = false; // 不可能是子集
                equal = false;
            }
        } else { // this指定了 networkDestination
            if (otherMatch.networkDestination == 0) {
                superSet = false; // 不可能是父集
                equal = false;
            } else if (this.networkDestination == otherMatch.networkDestination) {
                // intersect = false;
            } else { // 二者有一域不同，则肯定不相关
                return MatchRelation.UNRELATED;
            }
        }

        if (this.networkProtocol == 0) {
            if (otherMatch.networkProtocol != 0) {
                subSet = false;
                equal = false;
            }
        } else if (otherMatch.networkProtocol != 0) {
            if (this.networkProtocol != otherMatch.networkProtocol) {// this
                                                                     // 未指定networkProtocol
                return MatchRelation.UNRELATED;
            }
        } else {
            superSet = false;
            equal = false;
        }

        // TODO: TOS

        if (this.transportSource == 0) {// this 未指定transportSource
            if (otherMatch.transportSource != 0) {
                subSet = false; // 不可能是子集
                equal = false;
            }
        } else { // this指定了 transportSource
            if (otherMatch.transportSource == 0) {
                superSet = false; // 不可能是父集
                equal = false;
            } else if (this.transportSource == otherMatch.transportSource) {// ip.src相同
                // intersect = false;
            } else { // 二者有一域不同，则肯定不相关
                return MatchRelation.UNRELATED;
            }
        }

        if (this.transportDestination == 0) {// this 未指定transportDestination
            if (otherMatch.transportDestination != 0) {
                subSet = false; // 不可能是子集
                equal = false;
            }
        } else { // this指定了 transportDestination
            if (otherMatch.transportDestination == 0) {
                superSet = false; // 不可能是父集
                equal = false;
            } else if (this.transportDestination == otherMatch.transportDestination) {
                // intersect = false;
            } else { // 二者有一域不同，则肯定不相关
                return MatchRelation.UNRELATED;
            }
        }
        if (equal)
            return MatchRelation.EQUAL;
        // if(unrelated)return Relation.UNRELATED;
        if (subSet)
            return MatchRelation.SUBSET;
        if (superSet)
            return MatchRelation.SUPERSET;
        return MatchRelation.INTERSECT;
    }

    public void fromJson(JsonObject matchArgumentsNode) {
        if (matchArgumentsNode != null) {
            if (matchArgumentsNode.has("wildcards")) {
                int asInt = matchArgumentsNode.get("wildcards").getAsInt();
                if (asInt != 0)
                    this.setWildcards(asInt);
            }
            if (matchArgumentsNode.has("inputport")) {
                int asInt = matchArgumentsNode.get("inputport").getAsInt();
                if (asInt != 0)
                    this.setInputPort((short) asInt);
            }
            if (matchArgumentsNode.has("dataLayerSource")) {
                String asText = matchArgumentsNode.get("dataLayerSource")
                        .getAsString();
                if (!asText.equals("00:00:00:00:00:00"))
                	
                    this.setDataLayerSource(HexString.fromHexString(asText));
            }
            if (matchArgumentsNode.has("dataLayerDestination")) {
                String asText = matchArgumentsNode.get("dataLayerDestination")
                        .getAsString();
                if (!asText.equals("00:00:00:00:00:00"))
                    this.setDataLayerDestination(HexString
                            .fromHexString(asText));
            }
            if (matchArgumentsNode.has("dataLayerVirtualLan")) {
                int asInt = matchArgumentsNode.get("dataLayerVirtualLan")
                        .getAsInt();
                if (asInt != 0)
                    this.setDataLayerVirtualLan((short) asInt);
            }
            if (matchArgumentsNode
                    .has("dataLayerVirtualLanPriorityCodePoint")) {
                int asInt = matchArgumentsNode.get(
                        "dataLayerVirtualLanPriorityCodePoint").getAsInt();
                if (asInt != 0)
                    this.setDataLayerVirtualLanPriorityCodePoint((byte) asInt);
            }
            if (matchArgumentsNode.has("dataLayerType")) {
                int asInt = matchArgumentsNode.get("dataLayerType").getAsInt();
                if (asInt != 0)
                    this.setDataLayerType((short) asInt);
            }
            if (matchArgumentsNode.has("networkTypeOfService")) {
                int asInt = matchArgumentsNode.get("networkTypeOfService")
                        .getAsInt();
                if (asInt != 0) {
                    this.setNetworkTypeOfService((byte) asInt);
                }
            }
            if (matchArgumentsNode.has("networkProtocol")) {
                int asInt = matchArgumentsNode.get("networkProtocol").getAsInt();
                if (asInt != 0)
                    this.setNetworkProtocol((byte) asInt);
            }
            if (matchArgumentsNode.has("networkSource")) {
                String asText = matchArgumentsNode.get("networkSource")
                        .getAsString();
                if (!asText.equals("0"))
                    this.setNetworkSource(toIPv4Address(asText));
            }
            if (matchArgumentsNode.has("networkDestination")) {
                String asText = matchArgumentsNode.get("networkDestination")
                        .getAsString();
                if (!asText.equals("0"))
                    this.setNetworkDestination(toIPv4Address(asText));
            }

            if (matchArgumentsNode.has("transportSource")) {
                int asInt = matchArgumentsNode.get("transportSource").getAsInt();
                if (asInt != 0) {
                    this.setTransportSource((byte) asInt);
                }
            }
            if (matchArgumentsNode.has("transportDestination")) {
                int asInt = matchArgumentsNode.get("transportDestination")
                        .getAsInt();
                if (asInt != 0) {
                    this.setTransportDestination((byte) asInt);
                }
            }
        }
    }

    public static void main(String[] argvs) {

        MatchArguments m1 = new MatchArguments();
        MatchArguments m2 = new MatchArguments();
        System.out.println(m1);
        m2.setDataLayerDestination(new byte[] { 1, 1, 1, 1, 1, 1 });
        System.out.println(m1.compareWith(m2));
    }

    public int getWildcards() {
        return wildcards;
    }

    public void setWildcards(int wildcards) {
        this.wildcards = wildcards;
    }

    public short getInputPort() {
        return inputPort;
    }

    public void setInputPort(short inputPort) {
        this.inputPort = inputPort;
    }

    public byte[] getDataLayerSource() {
        return dataLayerSource;
    }

    public void setDataLayerSource(byte[] dataLayerSource) {
        this.dataLayerSource = dataLayerSource;
    }

    public byte[] getDataLayerDestination() {
        return dataLayerDestination;
    }

    public void setDataLayerDestination(byte[] dataLayerDestination) {
        this.dataLayerDestination = dataLayerDestination;
    }

    public short getDataLayerVirtualLan() {
        return dataLayerVirtualLan;
    }

    public void setDataLayerVirtualLan(short dataLayerVirtualLan) {
        this.dataLayerVirtualLan = dataLayerVirtualLan;
    }

    public byte getDataLayerVirtualLanPriorityCodePoint() {
        return dataLayerVirtualLanPriorityCodePoint;
    }

    public void setDataLayerVirtualLanPriorityCodePoint(
            byte dataLayerVirtualLanPriorityCodePoint) {
        this.dataLayerVirtualLanPriorityCodePoint = dataLayerVirtualLanPriorityCodePoint;
    }

    public short getDataLayerType() {
        return dataLayerType;
    }

    public void setDataLayerType(short dataLayerType) {
        this.dataLayerType = dataLayerType;
    }

    public byte getNetworkTypeOfService() {
        return networkTypeOfService;
    }

    public void setNetworkTypeOfService(byte networkTypeOfService) {
        this.networkTypeOfService = networkTypeOfService;
    }

    public byte getNetworkProtocol() {
        return networkProtocol;
    }

    public void setNetworkProtocol(byte networkProtocol) {
        this.networkProtocol = networkProtocol;
    }

    public int getNetworkSource() {
        return networkSource;
    }

    public void setNetworkSource(int networkSource) {
        this.networkSource = networkSource;
    }

    public int getNetworkDestination() {
        return networkDestination;
    }

    public void setNetworkDestination(int networkDestination) {
        this.networkDestination = networkDestination;
    }

    public short getTransportSource() {
        return transportSource;
    }

    public void setTransportSource(short transportSource) {
        this.transportSource = transportSource;
    }

    public short getTransportDestination() {
        return transportDestination;
    }

    public void setTransportDestination(short transportDestination) {
        wildcards &= ~OFWildCard.DST_PORT;//Match.OFPFW_TP_DST;
        this.transportDestination = transportDestination;
    }

    @Override
    public String toString() {
        String res = "";
        if (wildcards != OFPFW_ALL)
            res += "wildcards = " + wildcards + ", ";
        if (inputPort != 0)
            res += "inputPort = " + inputPort + ", ";
        if (!Arrays.equals(dataLayerSource, new byte[] { 0, 0, 0, 0, 0, 0 }))
            res += "dataLayerSource = "
                    + MACAddress.valueOf(dataLayerSource).toString() + ", ";
        if (!Arrays.equals(dataLayerDestination,
                new byte[] { 0, 0, 0, 0, 0, 0 }))
            res += "dataLayerDestination = "
                    + MACAddress.valueOf(dataLayerDestination).toString()
                    + ", ";
        if (dataLayerVirtualLan != VLAN_UNTAGGED)
            res += "dataLayerVirtualLan = " + dataLayerVirtualLan + ", ";
        if (dataLayerVirtualLanPriorityCodePoint != 0)
            res += "dataLayerVirtualLanPriorityCodePoint = "
                    + dataLayerVirtualLanPriorityCodePoint + ", ";
        if (dataLayerType != 0)
            res += "dataLayerType = " + dataLayerType + ", ";
        if (networkTypeOfService != 0)
            res += "networkTypeOfService = " + networkTypeOfService + ", ";
        if (networkProtocol != 0)
            res += "networkProtocol = " + networkProtocol + ", ";
        if (networkSource != 0)
            res += "networkSource = " + networkSource + ", ";
        if (networkDestination != 0)
            res += "networkDestination = " + networkDestination + ", ";
        if (transportSource != 0)
            res += "transportSource = " + transportSource + ", ";
        if (transportDestination != 0)
            res += "transportDestination = " + transportDestination + ", ";
        return res == "" ? "no field specified(wildcards all)" : res;
    }
    
    /**
     * if matches nothing, namely wildcards all 
     * @return true when wildcards all ,false when any field specified
     */
    public boolean isEmpty() {
        if (toString().equals("no field specified(wildcards all)")) {
            return true;
        }
        return false;
    }
    /**
     * Accepts an IPv4 address of the form xxx.xxx.xxx.xxx, ie 192.168.0.1 and
     * returns the corresponding 32 bit integer.
     * @param ipAddress
     * @return
     */
    public static int toIPv4Address(String ipAddress) {
        if (ipAddress == null)
            throw new IllegalArgumentException("Specified IPv4 address must" +
                "contain 4 sets of numerical digits separated by periods");
        String[] octets = ipAddress.split("\\.");
        if (octets.length != 4)
            throw new IllegalArgumentException("Specified IPv4 address must" +
                "contain 4 sets of numerical digits separated by periods");

        int result = 0;
        for (int i = 0; i < 4; ++i) {
            int oct = Integer.valueOf(octets[i]);
            if (oct > 255 || oct < 0)
                throw new IllegalArgumentException("Octet values in specified" +
                        " IPv4 address must be 0 <= value <= 255");
            result |=  oct << ((3-i)*8);
        }
        return result;
    }
}

