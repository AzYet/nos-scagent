package com.nsfocus.scagent.command;

import java.util.List;


public class BYODRedirectCommand extends PolicyCommand {
    private String network;
    private byte mask;
    private String serverIp;
    private String serverMac;
    public BYODRedirectCommand(String id, String policyName,
            int commandPriority, PolicyActionType type, MatchArguments match,
            List<SecurityDevice> devices, int idleTimeout, int hardTimeout,
            long dpid, short ofPort, String network, byte mask, String serverIp,
            String serverMac) {
        super(id, policyName, commandPriority, type, match, devices,
                idleTimeout, hardTimeout, dpid, ofPort);
        this.network = network;
        this.mask = mask;
        this.serverIp = serverIp;
        this.serverMac = serverMac;
    }
    public BYODRedirectCommand() {
        super();
    }
    public String getNetwork() {
        return network;
    }
    public void setNetwork(String string) {
        this.network = string;
    }
    public byte getMask() {
        return mask;
    }
    public void setMask(byte mask) {
        this.mask = mask;
    }
    public String getServerIp() {
        return serverIp;
    }
    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }
    public String getServerMac() {
        return serverMac;
    }
    public void setServerMac(String serverMac) {
        this.serverMac = serverMac;
    }

    @Override
    public String toString() {
        return "BYODRedirectCommand{" +
                "network='" + network + '\'' +
                ", mask=" + mask +
                ", serverIp='" + serverIp + '\'' +
                ", serverMac='" + serverMac + '\'' +
                "} " + super.toString();
    }
}

