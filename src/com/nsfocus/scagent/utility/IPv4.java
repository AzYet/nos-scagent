package com.nsfocus.scagent.utility;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author David Erickson (daviderickson@cs.stanford.edu)
 *
 */
public class IPv4 {
    public static final byte PROTOCOL_ICMP = 0x1;
    public static final byte PROTOCOL_TCP = 0x6;
    public static final byte PROTOCOL_UDP = 0x11;


    public static final byte IPV4_FLAGS_MOREFRAG = 0x1;
    public static final byte IPV4_FLAGS_DONTFRAG = 0x2;
    public static final byte IPV4_FLAGS_MASK = 0x7;
    public static final byte IPV4_FLAGS_SHIFT = 13;
    public static final short IPV4_OFFSET_MASK = (1 << IPV4_FLAGS_SHIFT) - 1;

    protected byte version;
    protected byte headerLength;
    protected byte diffServ;
    protected short totalLength;
    protected short identification;
    protected byte flags;
    protected short fragmentOffset;
    protected byte ttl;
    protected byte protocol;
    protected short checksum;
    protected int sourceAddress;
    protected int destinationAddress;
    protected byte[] options;

    protected boolean isTruncated;
    protected boolean isFragment;

    /**
     * Default constructor that sets the version to 4.
     */
    public IPv4() {
        super();
        this.version = 4;
        isTruncated = false;
        isFragment = false;
    }

    /**
     * @return the version
     */
    public byte getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public IPv4 setVersion(byte version) {
        this.version = version;
        return this;
    }

    /**
     * @return the headerLength
     */
    public byte getHeaderLength() {
        return headerLength;
    }

    /**
     * @return the diffServ
     */
    public byte getDiffServ() {
        return diffServ;
    }

    /**
     * @param diffServ the diffServ to set
     */
    public IPv4 setDiffServ(byte diffServ) {
        this.diffServ = diffServ;
        return this;
    }

    /**
     * @return the totalLength
     */
    public short getTotalLength() {
        return totalLength;
    }

    /**
     * @return the identification
     */
    public short getIdentification() {
        return identification;
    }

    public boolean isTruncated() {
        return isTruncated;
    }

    public void setTruncated(boolean isTruncated) {
        this.isTruncated = isTruncated;
    }

    public boolean isFragment() {
        return isFragment;
    }

    public void setFragment(boolean isFrag) {
        this.isFragment = isFrag;
    }

    /**
     * @param identification the identification to set
     */
    public IPv4 setIdentification(short identification) {
        this.identification = identification;
        return this;
    }

    /**
     * @return the flags
     */
    public byte getFlags() {
        return flags;
    }

    /**
     * @param flags the flags to set
     */
    public IPv4 setFlags(byte flags) {
        this.flags = flags;
        return this;
    }

    /**
     * @return the fragmentOffset
     */
    public short getFragmentOffset() {
        return fragmentOffset;
    }

    /**
     * @param fragmentOffset the fragmentOffset to set
     */
    public IPv4 setFragmentOffset(short fragmentOffset) {
        this.fragmentOffset = fragmentOffset;
        return this;
    }

    /**
     * @return the ttl
     */
    public byte getTtl() {
        return ttl;
    }

    /**
     * @param ttl the ttl to set
     */
    public IPv4 setTtl(byte ttl) {
        this.ttl = ttl;
        return this;
    }

    /**
     * @return the protocol
     */
    public byte getProtocol() {
        return protocol;
    }

    /**
     * @param protocol the protocol to set
     */
    public IPv4 setProtocol(byte protocol) {
        this.protocol = protocol;
        return this;
    }

    /**
     * @return the checksum
     */
    public short getChecksum() {
        return checksum;
    }

    /**
     * @param checksum the checksum to set
     */
    public IPv4 setChecksum(short checksum) {
        this.checksum = checksum;
        return this;
    }

    /**
     * @return the sourceAddress
     */
    public int getSourceAddress() {
        return sourceAddress;
    }

    /**
     * @param sourceAddress the sourceAddress to set
     */
    public IPv4 setSourceAddress(int sourceAddress) {
        this.sourceAddress = sourceAddress;
        return this;
    }

    /**
     * @param sourceAddress the sourceAddress to set
     */
    public IPv4 setSourceAddress(String sourceAddress) {
        this.sourceAddress = IPv4.toIPv4Address(sourceAddress);
        return this;
    }

    /**
     * @return the destinationAddress
     */
    public int getDestinationAddress() {
        return destinationAddress;
    }

    /**
     * @param destinationAddress the destinationAddress to set
     */
    public IPv4 setDestinationAddress(int destinationAddress) {
        this.destinationAddress = destinationAddress;
        return this;
    }

    /**
     * @param destinationAddress the destinationAddress to set
     */
    public IPv4 setDestinationAddress(String destinationAddress) {
        this.destinationAddress = IPv4.toIPv4Address(destinationAddress);
        return this;
    }

    /**
     * @return the options
     */
    public byte[] getOptions() {
        return options;
    }

    /**
     * @param options the options to set
     */
    public IPv4 setOptions(byte[] options) {
        if (options != null && (options.length % 4) > 0)
            throw new IllegalArgumentException(
                    "Options length must be a multiple of 4");
        this.options = options;
        return this;
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

    /**
     * Accepts an IPv4 address in a byte array and returns the corresponding
     * 32-bit integer value.
     * @param ipAddress
     * @return
     */
    public static int toIPv4Address(byte[] ipAddress) {
        int ip = 0;
        for (int i = 0; i < 4; i++) {
            int t = (ipAddress[i] & 0xff) << ((3-i)*8);
            ip |= t;
        }
        return ip;
    }

    /**
     * Accepts an IPv4 address and returns of string of the form xxx.xxx.xxx.xxx
     * ie 192.168.0.1
     *
     * @param ipAddress
     * @return
     */
    public static String fromIPv4Address(int ipAddress) {
        StringBuffer sb = new StringBuffer();
        int result = 0;
        for (int i = 0; i < 4; ++i) {
            result = (ipAddress >> ((3-i)*8)) & 0xff;
            sb.append(Integer.valueOf(result).toString());
            if (i != 3)
                sb.append(".");
        }
        return sb.toString();
    }

    /**
     * Accepts a collection of IPv4 addresses as integers and returns a single
     * String useful in toString method's containing collections of IP
     * addresses.
     *
     * @param ipAddresses collection
     * @return
     */
    public static String fromIPv4AddressCollection(Collection<Integer> ipAddresses) {
        if (ipAddresses == null)
            return "null";
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (Integer ip : ipAddresses) {
            sb.append(fromIPv4Address(ip));
            sb.append(",");
        }
        sb.replace(sb.length()-1, sb.length(), "]");
        return sb.toString();
    }

    /**
     * Accepts an IPv4 address of the form xxx.xxx.xxx.xxx, ie 192.168.0.1 and
     * returns the corresponding byte array.
     * @param ipAddress The IP address in the form xx.xxx.xxx.xxx.
     * @return The IP address separated into bytes
     */
    public static byte[] toIPv4AddressBytes(String ipAddress) {
        String[] octets = ipAddress.split("\\.");
        if (octets.length != 4)
            throw new IllegalArgumentException("Specified IPv4 address must" +
                    "contain 4 sets of numerical digits separated by periods");

        byte[] result = new byte[4];
        for (int i = 0; i < 4; ++i) {
            result[i] = Integer.valueOf(octets[i]).byteValue();
        }
        return result;
    }

    /**
     * Accepts an IPv4 address in the form of an integer and
     * returns the corresponding byte array.
     * @param ipAddress The IP address as an integer.
     * @return The IP address separated into bytes.
     */
    public static byte[] toIPv4AddressBytes(int ipAddress) {
        return new byte[] {
                (byte)(ipAddress >>> 24),
                (byte)(ipAddress >>> 16),
                (byte)(ipAddress >>> 8),
                (byte)ipAddress};
    }


}
