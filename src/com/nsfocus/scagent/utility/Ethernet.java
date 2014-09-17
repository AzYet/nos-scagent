package com.nsfocus.scagent.utility;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class Ethernet {
    private static String HEXES = "0123456789ABCDEF";
    public static final short TYPE_ARP = 0x0806;
    public static final short TYPE_RARP = (short) 0x8035;
    public static final short TYPE_IPv4 = 0x0800;
    public static final short TYPE_LLDP = (short) 0x88cc;
    public static final short TYPE_BSN = (short) 0x8942;
    public static final short VLAN_UNTAGGED = (short)0xffff;
    public static final short DATALAYER_ADDRESS_LENGTH = 6; // bytes


    protected MACAddress destinationMACAddress;
    protected MACAddress sourceMACAddress;
    protected byte priorityCode;
    protected short vlanID;
    protected short etherType;
    protected boolean pad = false;

    /**
     * By default, set Ethernet to untagged
     */
    public Ethernet() {
        super();
        this.vlanID = VLAN_UNTAGGED;
    }

    /**
     * @return the destination MAC as a byte array
     */
    public byte[] getDestinationMACAddress() {
        return destinationMACAddress.toBytes();
    }

    /**
     * @return the destination MAC
     */
    public MACAddress getDestinationMAC() {
        return destinationMACAddress;
    }

    /**
     * @param destinationMACAddress the destination MAC to set
     */
    public Ethernet setDestinationMACAddress(byte[] destinationMACAddress) {
        this.destinationMACAddress = MACAddress.valueOf(destinationMACAddress);
        return this;
    }

    /**
     * @param destinationMACAddress the destination MAC to set
     */
    public Ethernet setDestinationMACAddress(String destinationMACAddress) {
        this.destinationMACAddress = MACAddress.valueOf(destinationMACAddress);
        return this;
    }

    /**
     * @return the source MACAddress as a byte array
     */
    public byte[] getSourceMACAddress() {
        return sourceMACAddress.toBytes();
    }

    /**
     * @return the source MACAddress
     */
    public MACAddress getSourceMAC() {
        return sourceMACAddress;
    }

    /**
     * @param sourceMACAddress the source MAC to set
     */
    public Ethernet setSourceMACAddress(byte[] sourceMACAddress) {
        this.sourceMACAddress = MACAddress.valueOf(sourceMACAddress);
        return this;
    }

    /**
     * @param sourceMACAddress the source MAC to set
     */
    public Ethernet setSourceMACAddress(String sourceMACAddress) {
        this.sourceMACAddress = MACAddress.valueOf(sourceMACAddress);
        return this;
    }

    /**
     * @return the priorityCode
     */
    public byte getPriorityCode() {
        return priorityCode;
    }

    /**
     * @param priorityCode the priorityCode to set
     */
    public Ethernet setPriorityCode(byte priorityCode) {
        this.priorityCode = priorityCode;
        return this;
    }

    /**
     * @return the vlanID
     */
    public short getVlanID() {
        return vlanID;
    }

    /**
     * @param vlanID the vlanID to set
     */
    public Ethernet setVlanID(short vlanID) {
        this.vlanID = vlanID;
        return this;
    }

    /**
     * @return the etherType
     */
    public short getEtherType() {
        return etherType;
    }

    /**
     * @param etherType the etherType to set
     */
    public Ethernet setEtherType(short etherType) {
        this.etherType = etherType;
        return this;
    }

    /**
     * @return True if the Ethernet frame is broadcast, false otherwise
     */
    public boolean isBroadcast() {
        assert(destinationMACAddress.length() == 6);
        return destinationMACAddress.isBroadcast();
    }

    /**
     * @return True is the Ethernet frame is multicast, False otherwise
     */
    public boolean isMulticast() {
        return destinationMACAddress.isMulticast();
    }
    /**
     * Pad this packet to 60 bytes minimum, filling with zeros?
     * @return the pad
     */
    public boolean isPad() {
        return pad;
    }

    /**
     * Pad this packet to 60 bytes minimum, filling with zeros?
     * @param pad the pad to set
     */
    public Ethernet setPad(boolean pad) {
        this.pad = pad;
        return this;
    }


    /**
     * Checks to see if a string is a valid MAC address.
     * @param macAddress
     * @return True if macAddress is a valid MAC, False otherwise
     */
    public static boolean isMACAddress(String macAddress) {
        String[] macBytes = macAddress.split(":");
        if (macBytes.length != 6)
            return false;
        for (int i = 0; i < 6; ++i) {
            if (HEXES.indexOf(macBytes[i].toUpperCase().charAt(0)) == -1 ||
                    HEXES.indexOf(macBytes[i].toUpperCase().charAt(1)) == -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Accepts a MAC address of the form 00:aa:11:bb:22:cc, case does not
     * matter, and returns a corresponding byte[].
     * @param macAddress The MAC address to convert into a bye array
     * @return The macAddress as a byte array
     */
    public static byte[] toMACAddress(String macAddress) {
        return MACAddress.valueOf(macAddress).toBytes();
    }


    /**
     * Accepts a MAC address and returns the corresponding long, where the
     * MAC bytes are set on the lower order bytes of the long.
     * @param macAddress
     * @return a long containing the mac address bytes
     */
    public static long toLong(byte[] macAddress) {
        return MACAddress.valueOf(macAddress).toLong();
    }

    /**
     * Convert a long MAC address to a byte array
     * @param macAddress
     * @return the bytes of the mac address
     */
    public static byte[] toByteArray(long macAddress) {
        return MACAddress.valueOf(macAddress).toBytes();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 7867;
        int result = super.hashCode();
        result = prime * result + destinationMACAddress.hashCode();
        result = prime * result + etherType;
        result = prime * result + vlanID;
        result = prime * result + priorityCode;
        result = prime * result + (pad ? 1231 : 1237);
        result = prime * result + sourceMACAddress.hashCode();
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (!(obj instanceof Ethernet))
            return false;
        Ethernet other = (Ethernet) obj;
        if (!destinationMACAddress.equals(other.destinationMACAddress))
            return false;
        if (priorityCode != other.priorityCode)
            return false;
        if (vlanID != other.vlanID)
            return false;
        if (etherType != other.etherType)
            return false;
        if (pad != other.pad)
            return false;
        if (!sourceMACAddress.equals(other.sourceMACAddress))
            return false;
        return true;
    }


}