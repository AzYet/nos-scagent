package com.nsfocus.scagent.model;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nsfocus.scagent.restlet.RestApiServer;


public class PolicyCommand implements Comparable<PolicyCommand> {
    protected String id;
    protected String policyId;
    protected String policyName;
    protected int commandPriority;
    protected PolicyActionType type;
    protected MatchArguments match;
    protected List<SecurityDevice> devices;
    protected int idleTimeout;
    protected int hardTimeout;
    protected long dpid;
    protected short inPort;

    public PolicyCommand(String policyCommandId, String policyName,
                         int commandPriority, PolicyActionType type, MatchArguments match,
                         List<SecurityDevice> devices) {
        super();
        this.id = policyCommandId;
        this.policyName = policyName;
        this.commandPriority = commandPriority;
        this.type = type;
        this.match = match;
        this.devices = devices;
    }

    public PolicyCommand(String id, String policyName, int commandPriority,
                         PolicyActionType type, MatchArguments match,
                         List<SecurityDevice> devices, int idleTimeout, int hardTimeout) {
        super();
        this.id = id;
        this.policyName = policyName;
        this.commandPriority = commandPriority;
        this.type = type;
        this.match = match;
        this.devices = devices;
        this.idleTimeout = idleTimeout;
        this.hardTimeout = hardTimeout;
    }

    public PolicyCommand(String id, String policyName, int commandPriority,
                         PolicyActionType type, MatchArguments match,
                         List<SecurityDevice> devices, int idleTimeout, int hardTimeout,
                         long dpid, short inPort) {
        super();
        this.id = id;
        this.policyName = policyName;
        this.commandPriority = commandPriority;
        this.type = type;
        this.match = match;
        this.devices = devices;
        this.idleTimeout = idleTimeout;
        this.hardTimeout = hardTimeout;
        this.dpid = dpid;
        this.inPort = inPort;
    }

    public PolicyCommand(String id, String policyName, int commandPriority,
                         PolicyActionType type, MatchArguments match,
                         List<SecurityDevice> devices, int idleTimeout, int hardTimeout,
                         long dpid, short inPort, String policyId) {
        this(id, policyName, commandPriority, type, match, devices, idleTimeout, hardTimeout, dpid, inPort);
        this.policyId = policyId;
    }

    public static List<? extends PolicyCommand> fromJson(JsonObject rootNode, PolicyActionType type) {
        ArrayList<PolicyCommand> pmList = new ArrayList<PolicyCommand>();
        String policyId = rootNode.get("id").getAsString();
        if (type == PolicyActionType.REDIRECT_FLOW) {
            JsonArray commandlistNode = rootNode.get("commandlist").getAsJsonArray();
            Iterator<JsonElement> commandlist = commandlistNode.iterator();
            while (commandlist.hasNext()) { //对应一个PolicyCommand
                JsonObject flowCommandNode = commandlist.next().getAsJsonObject();
                PolicyCommand policyCommand = new PolicyCommand();
                policyCommand.setType(type);
                policyCommand.setPolicyId(policyId);
                policyCommand.setId(flowCommandNode.get("id").getAsString());
                int policyPriority = flowCommandNode.get("commandPriority").getAsInt();
                policyCommand.setCommandPriority(policyPriority);
                policyCommand.setHardTimeout(flowCommandNode.get("hardTimeout").getAsInt());
                policyCommand.setIdleTimeout(flowCommandNode.get("idleTimeout").getAsInt());

                String commandName = flowCommandNode.get("commandName").getAsString();
                policyCommand.setPolicyName(commandName);
                MatchArguments ma = new MatchArguments();
                ma.fromJson(flowCommandNode.get("matchArguments").getAsJsonObject());
                policyCommand.setMatch(ma);
                JsonArray devicesNode = flowCommandNode.get("devices").getAsJsonArray();
                Iterator<JsonElement> deviceList = devicesNode.iterator();
                List<SecurityDevice> secDeviceList = new ArrayList<SecurityDevice>();
                while (deviceList.hasNext()) { //一个device
                    JsonObject deviceNode = deviceList.next().getAsJsonObject();
                    SecurityDevice device = new SecurityDevice();
                    device.fromJson(deviceNode);
                    secDeviceList.add(device);
                }
                policyCommand.setDevices(secDeviceList);
                pmList.add(policyCommand);
            }
        } else if (type == PolicyActionType.DROP_FLOW || type == PolicyActionType.ALLOW_FLOW
                || type == PolicyActionType.BYOD_ALLOW) {
            JsonArray commandlistNode = rootNode.get("commandlist").getAsJsonArray();
            Iterator<JsonElement> commandlist = commandlistNode.iterator();
            while (commandlist.hasNext()) { //对应一个PolicyCommand
                JsonObject flowCommandNode = commandlist.next().getAsJsonObject();
                PolicyCommand policyCommand = new PolicyCommand();
                policyCommand.setType(type);
                policyCommand.setPolicyId(policyId);
                policyCommand.setId(flowCommandNode.get("id").getAsString());
                int policyPriority = flowCommandNode.get("commandPriority").getAsInt();
                policyCommand.setCommandPriority(policyPriority);
                policyCommand.setHardTimeout(flowCommandNode.get("hardTimeout").getAsInt());
                policyCommand.setIdleTimeout(flowCommandNode.get("idleTimeout").getAsInt());
                policyCommand.setInPort((short) flowCommandNode.get("inPort").getAsInt());
                policyCommand.setDpid(flowCommandNode.get("dpid").getAsLong());
                String commandName = flowCommandNode.get("commandName").getAsString();
                policyCommand.setPolicyName(commandName);
                MatchArguments ma = new MatchArguments();
                ma.fromJson(flowCommandNode.get("matchArguments").getAsJsonObject());
                policyCommand.setMatch(ma);
                pmList.add(policyCommand);
            }
        } else if (type == PolicyActionType.RESTORE_REDIRECT_FLOW
                || type == PolicyActionType.RESTORE_ALLOW_FLOW
                || type == PolicyActionType.RESTORE_BYOD_ALLOW
                || type == PolicyActionType.RESTORE_DROP_FLOW) {
            JsonArray commandlistNode = rootNode.get("commandlist").getAsJsonArray();
            Iterator<JsonElement> commandlist = commandlistNode.iterator();
            while (commandlist.hasNext()) { //对应一个PolicyCommand
                JsonObject flowCommandNode = commandlist.next().getAsJsonObject();
                PolicyCommandDeployed policyCommandDeployed = RestApiServer.policyCommandsDeployed.get(flowCommandNode.get("id").getAsString());
                if (policyCommandDeployed != null)
                    pmList.add(policyCommandDeployed.getPolicyCommand());
            }
        } else if (type == PolicyActionType.BYOD_INIT) {
            JsonArray commandlistNode = rootNode.get("commandlist").getAsJsonArray();
            Iterator<JsonElement> commandlist = commandlistNode.iterator();
            while (commandlist.hasNext()) { //对应一个PolicyCommand
                JsonObject flowCommandNode = commandlist.next().getAsJsonObject();
                BYODRedirectCommand policyCommand = new BYODRedirectCommand();
                policyCommand.setType(type);
                policyCommand.setId(flowCommandNode.get("id").getAsString());
                policyCommand.setPolicyId(policyId);
                int policyPriority = flowCommandNode.get("commandPriority").getAsInt();
                policyCommand.setCommandPriority(policyPriority);
                policyCommand.setHardTimeout(flowCommandNode.get("hardTimeout").getAsInt());
                policyCommand.setHardTimeout(flowCommandNode.get("hardTimeout").getAsInt());
                policyCommand.setDpid(flowCommandNode.get("dpid").getAsLong());
                policyCommand.setInPort((short) flowCommandNode.get("inPort").getAsInt());
                policyCommand.setNetwork(flowCommandNode.get("network").getAsString());
                policyCommand.setMask((byte) flowCommandNode.get("mask").getAsInt());
                policyCommand.setServerIp(flowCommandNode.get("serverIp").getAsString());
                policyCommand.setServerMac(flowCommandNode.get("serverMac").getAsString());
                String commandName = flowCommandNode.get("commandName").getAsString();
                policyCommand.setPolicyName(commandName);

                pmList.add(policyCommand);
            }
        } else if (type == PolicyActionType.RESTORE_BYOD_INIT) {
            JsonArray commandlistNode = rootNode.get("commandlist").getAsJsonArray();
            Iterator<JsonElement> commandlist = commandlistNode.iterator();
            while (commandlist.hasNext()) { //对应一个PolicyCommand
                JsonObject flowCommandNode = commandlist.next().getAsJsonObject();
                for (int i = 0; i < 6; i++) {
                    String policyCommandId = String.format("ByodInit_%d_%s", i, flowCommandNode.get("id").getAsString());
                    PolicyCommandDeployed policyCommandDeployed = RestApiServer.policyCommandsDeployed.get(policyCommandId);
                    if (policyCommandDeployed != null)
                        pmList.add(policyCommandDeployed.getPolicyCommand());
                }
            }
        } /*else if (type == PolicyActionType.DNAT) {
            JsonArray commandlistNode = rootNode.get("commandlist").getAsJsonArray();
            Iterator<JsonElement> commandlist = commandlistNode.iterator();
            while (commandlist.hasNext()) { //对应一个PolicyCommand
                JsonObject flowCommandNode = commandlist.next().getAsJsonObject();
                DNatPolicyCommand policyCommand = new DNatPolicyCommand();
                policyCommand.setType(type);
                policyCommand.setId(flowCommandNode.get("id").getAsString());
                policyCommand.setPolicyId(policyId);
                int policyPriority = flowCommandNode.get("commandPriority").getAsInt();
                policyCommand.setCommandPriority(policyPriority);
                policyCommand.setHardTimeout(flowCommandNode.get("hardTimeout").getAsInt());
                policyCommand.setHardTimeout(flowCommandNode.get("hardTimeout").getAsInt());
                policyCommand.setDpid(flowCommandNode.get("dpid").getAsLong());
                policyCommand.setInPort((short) flowCommandNode.get("inPort").getAsInt());
                String commandName = flowCommandNode.get("commandName").getAsString();
                policyCommand.setPolicyName(commandName);
                policyCommand.setDstHostIp(flowCommandNode.get("dstHostIp").getAsString());
                policyCommand.setDstHostMac(flowCommandNode.get("dstHostMac").getAsString());
                policyCommand.setPublicGwMac(flowCommandNode.get("publicGwMac").getAsString());
                pmList.add(policyCommand);
            }
        }*/
        return pmList;
    }

    public PolicyCommand() {
        super();
    }

    public PolicyCommand(String id, PolicyActionType type, int hardTimeout) {
        super();
        this.id = id;
        this.type = type;
        this.hardTimeout = hardTimeout;
    }

    public String getId() {
        return id;
    }

    public void setId(String policyCommandId) {
        this.id = policyCommandId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public int getCommandPriority() {
        return commandPriority;
    }

    public void setCommandPriority(int commandPriority) {
        this.commandPriority = commandPriority;
    }

    public PolicyActionType getType() {
        return type;
    }

    public void setType(PolicyActionType type) {
        this.type = type;
    }

    public MatchArguments getMatch() {
        return match;
    }

    public void setMatch(MatchArguments match) {
        this.match = match;
    }

    public List<SecurityDevice> getDevices() {
        return devices;
    }

    public void setDevices(List<SecurityDevice> devices) {
        this.devices = devices;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public int getHardTimeout() {
        return hardTimeout;
    }

    public void setHardTimeout(int hardTimeout) {
        this.hardTimeout = hardTimeout;
    }


    @Override
    public String toString() {
        return "PolicyCommand [id=" + id + ", policyName=" + policyName
                + ", commandPriority=" + commandPriority + ", type=" + type
                + ", match=" + match + ", devices=" + devices
                + ", idleTimeout=" + idleTimeout + ", hardTimeout="
                + hardTimeout + "]";
    }

    @Override
    public int compareTo(PolicyCommand o) {
        return commandPriority == o.commandPriority ? 0 : (commandPriority > o.commandPriority ? 1 : -1);
    }

    public short getInPort() {
        return inPort;
    }

    public void setInPort(short inPort) {
        this.inPort = inPort;
    }

    public long getDpid() {
        return dpid;
    }

    public void setDpid(long swIPAddress) {
        this.dpid = swIPAddress;
    }

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }
}
