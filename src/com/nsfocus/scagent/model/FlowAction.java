package com.nsfocus.scagent.model;

import com.nsfocus.scagent.utility.HexString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by chen on 14-9-15.
 */
public class FlowAction {

    List<Short> output = new ArrayList<Short>();        /* Output to switch port. */
    short vlanId;        /* Set the 802.1q VLAN id. */
    byte vlanPcp;        /* Set the 802.1q priority. */
    boolean stripVlan;      /* Strip the 802.1q header. */
    byte[] dlSrc = new byte[6];        /* Ethernet source address. */
    byte[] dlDst = new byte[6];        /* Ethernet destination address. */
    int nwSrc;           /* IP source address. */
    int nwDst;           /* IP destination address. */
    byte nwTos;          /* IP ToS (DSCP field, 6 bits). */
    short tpSrc;         /* TCP/UDP source port. */
    short tpDst;         /* TCP/UDP destination port. */
    int[] enqueue = new int[2];          /* 0 port 1 queueId. Output to queue. */

    public FlowAction(short port) {
        this.output.add(port);
    }

    public FlowAction() {
        super();
    }

    public String getDlSrcString() {
        return HexString.toHexString(dlSrc);
    }

    public String getDlDstString() {
        return HexString.toHexString(dlDst);
    }

    public List<Short> getOutput() {
        return output;
    }

    public void setOutput(List<Short> output) {
        this.output = output;
    }

    public short getVlanId() {
        return vlanId;
    }

    public void setVlanId(short vlanId) {
        this.vlanId = vlanId;
    }

    public byte getVlanPcp() {
        return vlanPcp;
    }

    public void setVlanPcp(byte vlanPcp) {
        this.vlanPcp = vlanPcp;
    }

    public boolean isStripVlan() {
        return stripVlan;
    }

    public void setStripVlan(boolean stripVlan) {
        this.stripVlan = stripVlan;
    }

    public byte[] getDlSrc() {
        return dlSrc;
    }

    public void setDlSrc(byte[] dlSrc) {
        this.dlSrc = dlSrc;
    }

    public byte[] getDlDst() {
        return dlDst;
    }

    public void setDlDst(byte[] dlDst) {
        this.dlDst = dlDst;
    }

    public int getNwSrc() {
        return nwSrc;
    }

    public void setNwSrc(int nwSrc) {
        this.nwSrc = nwSrc;
    }

    public int getNwDst() {
        return nwDst;
    }

    public void setNwDst(int nwDst) {
        this.nwDst = nwDst;
    }

    public byte getNwTos() {
        return nwTos;
    }

    public void setNwTos(byte nwTos) {
        this.nwTos = nwTos;
    }

    public short getTpSrc() {
        return tpSrc;
    }

    public void setTpSrc(short tpSrc) {
        this.tpSrc = tpSrc;
    }

    public short getTpDst() {
        return tpDst;
    }

    public void setTpDst(short tpDst) {
        this.tpDst = tpDst;
    }

    public int[] getEnqueue() {
        return enqueue;
    }

    public void setEnqueue(int[] enqueue) {
        this.enqueue = enqueue;
    }

    @Override
    public String toString() {
        return "FlowAction{" +
                "output=" + output +
                ", vlanId=" + vlanId +
                ", vlanPcp=" + vlanPcp +
                ", stripVlan=" + stripVlan +
                ", dlSrc=" + Arrays.toString(dlSrc) +
                ", dlDst=" + Arrays.toString(dlDst) +
                ", nwSrc=" + nwSrc +
                ", nwDst=" + nwDst +
                ", nwTos=" + nwTos +
                ", tpSrc=" + tpSrc +
                ", tpDst=" + tpDst +
                ", enqueue=" + Arrays.toString(enqueue) +
                '}';
    }
};