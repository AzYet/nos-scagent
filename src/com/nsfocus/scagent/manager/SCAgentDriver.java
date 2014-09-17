package com.nsfocus.scagent.manager;

import com.nsfocus.scagent.model.*;
import com.nsfocus.scagent.restlet.RestApiServer;
import com.nsfocus.scagent.utility.Cypher;
import com.nsfocus.scagent.utility.Ethernet;
import com.nsfocus.scagent.utility.IPv4;
import com.nsfocus.scagent.utility.MACAddress;
import jp.co.nttdata.ofc.common.except.NosException;
import jp.co.nttdata.ofc.common.except.NosSocketIOException;
import jp.co.nttdata.ofc.common.util.MacAddress;
import jp.co.nttdata.ofc.nos.api.IFlowModifier;
import jp.co.nttdata.ofc.nos.api.INOSApi;
import jp.co.nttdata.ofc.nos.api.except.ActionNotSupportedException;
import jp.co.nttdata.ofc.nos.api.except.ArgumentInvalidException;
import jp.co.nttdata.ofc.nos.api.except.OFSwitchNotFoundException;
import jp.co.nttdata.ofc.nos.api.except.SwitchPortNotFoundException;
import jp.co.nttdata.ofc.nos.common.constant.OFPConstant;
import jp.co.nttdata.ofc.nos.ofp.common.Flow;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.DpidPortPair;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.Edge;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.ForwardingTable;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.TopologyManager;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.Trunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by chen on 14-9-15.
 */
public class SCAgentDriver implements ISCAgentDriver {


    public static final long cookie = 0xabcdefL;
    static Logger logger = LoggerFactory.getLogger(SCAgentDriver.class);
    private static SCAgentDriver scAgentDriver = new SCAgentDriver();
    public Map<String, BYODRedirectCommand> redirectCommands = new HashMap<String, BYODRedirectCommand>();


    public static SCAgentDriver getInstance(){
        return scAgentDriver;
    }

    private INOSApi nosApi;

    public SCAgentDriver() {
        super();

    }

    public Map<String, BYODRedirectCommand> getRedirectCommands() {
        return redirectCommands;
    }

    @Override
    public List<DpidPortPair> computeRoute(DpidPortPair start, DpidPortPair end) {
        ArrayList<DpidPortPair> path = new ArrayList<DpidPortPair>();
        if(start.getDpid() == end.getDpid()){
            path.add(start);
            path.add(end);
            return path;
        }
        ForwardingTable forwardingTable = TopologyManager.getInstance().getForwardingTable();
        CopyOnWriteArrayList<Trunk> trunkList = TopologyManager.getInstance().getTrunkList();
        boolean proceed =true;
        DpidPortPair currentPair = start;
        while(proceed) {
            boolean noPath = true;
            path.add(currentPair);
            for ( ForwardingTable.ForwardingTableEntry entry : forwardingTable.getTable()){
                if(entry.getKey()[0]==currentPair.getDpid() && entry.getKey()[1] == end.getDpid()){
                    noPath = false;
                    Integer portForward = entry.getPortList().get(0);
                    Long dpid = currentPair.getDpid();
                    path.add(new DpidPortPair(dpid,portForward));
                    //find which dpid portForward links to'
                    for(Trunk trunk : trunkList){
                        if(trunk.getDpidPair()[0] == dpid ){
                            for(Edge edge : trunk.getEdgeList()){
                                if(edge.getPorts()[0] == portForward ){
                                    Long nextDpid = trunk.getDpidPair()[1];
                                    int nextPort = edge.getPorts()[1];
                                    currentPair = new DpidPortPair(nextDpid,nextPort);
                                    if(nextDpid == end.getDpid()){
                                        proceed =false;
                                    }
                                }
                            }
                        }else if(trunk.getDpidPair()[1] == dpid ){
                            for(Edge edge : trunk.getEdgeList()){
                                if(edge.getPorts()[1] == portForward ){
                                    Long nextDpid = trunk.getDpidPair()[0];
                                    int nextPort = edge.getPorts()[0];
                                    currentPair = new DpidPortPair(nextDpid,nextPort);
                                    if(nextDpid == end.getDpid()){
                                        proceed =false;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
            if(noPath){
                return null;
            }
        }
        path.add(currentPair);
        path.add(end);
        return path;
    }

    public static void loadFMAction(IFlowModifier flowModifier, FlowAction action)
            throws ActionNotSupportedException, OFSwitchNotFoundException, ArgumentInvalidException, SwitchPortNotFoundException {
        if(action == null)
            return;
        if(action.getOutput()!=null && action.getOutput().size()>0) {
            for (int o : action.getOutput()) {
                flowModifier.addOutputAction(o, 0);
            }
        }
        if (!Arrays.equals(new byte[6], action.getDlSrc())) {
            long srcMac = MACAddress.valueOf(action.getDlSrc()).toLong();
            flowModifier.addSetSrcMacaddrAction(srcMac);
        }
        if (!Arrays.equals(new byte[6], action.getDlDst())) {
            long dstMac = MACAddress.valueOf(action.getDlDst()).toLong();
            flowModifier.addSetSrcMacaddrAction(dstMac);
        }
        if (action.getNwSrc() != 0) {
            flowModifier.addSetSrcIpaddrAction(action.getNwSrc());
        }
        if (action.getNwDst() != 0) {
            flowModifier.addSetSrcIpaddrAction(action.getNwDst());
        }
        // TODO: complete with vlan support etc.
    }

    public static void loadFMSettings(IFlowModifier flowModifier, FlowSettings settings) {
        flowModifier.setBufferId(settings.getBufferId());
        flowModifier.setCookie(settings.getCookie());
        flowModifier.setFlags(toU16Flag(settings.getFlags()));
        flowModifier.setOutPort(settings.getOutPort());
        if (settings.getCommand() == FlowMod.FMCommand.ADD) {
            flowModifier.setAddCommand();
        }else if (settings.getCommand() == FlowMod.FMCommand.DELETE) {
            flowModifier.setDeleteCommand();
        }else if (settings.getCommand() == FlowMod.FMCommand.DELETE_STRICT) {
            flowModifier.setDeleteStrictCommand();
        }else if (settings.getCommand() == FlowMod.FMCommand.MODIFY) {
            flowModifier.setModifyCommand();
        }else if (settings.getCommand() == FlowMod.FMCommand.MODIFY_STRICT) {
            flowModifier.setModifyStrictCommand();
        }else{      //default to add command
            flowModifier.setAddCommand();
        }
        flowModifier.setHardTimeoutSec(settings.getHardTimeout());
        flowModifier.setIdleTimeoutSec(settings.getIdleTimeout());
        flowModifier.setPriority(settings.getPriority());
    }

    public static short toU16Flag(List<FlowMod.FMFlag> flags) {
        short res = 0;
        for (FlowMod.FMFlag flag : flags) {
            if (flag == FlowMod.FMFlag.SEND_FLOW_REM) {
                res |=1;
            }else if (flag == FlowMod.FMFlag.CHECK_OVERLAP) {
                res |= 1<<1;
            }else if (flag == FlowMod.FMFlag.EMERG) {
                res |= 1<<2;
            }
        }
        return res;
    }

    /**
     *
     * @return an OFMatch object without in_port
     */
    public Flow createFlowFromMatch(MatchArguments match) {
        Integer wildcard_hints = OFPConstant.OFWildCard.ALL;
        Flow flow = new Flow();
        if (match.getInputPort() != 0) {
            wildcard_hints &= ~OFPConstant.OFWildCard.IN_PORT;
            flow.inPort = match.getInputPort();
        }
        if (!Arrays.equals(match.getDataLayerSource(), new byte[] { 0, 0, 0, 0, 0, 0 })) {
            wildcard_hints &= ~OFPConstant.OFWildCard.SRC_MACADDR;
            try {
                flow.srcMacaddr = new MacAddress(match.getDataLayerSource());
            } catch (NosException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (!Arrays.equals(match.getDataLayerDestination(),
                new byte[] { 0, 0, 0, 0, 0, 0 })) {
            wildcard_hints &= ~OFPConstant.OFWildCard.DST_MACADDR;
            try {
                flow.dstMacaddr = new MacAddress(match.getDataLayerDestination());
            } catch (NosException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (match.getDataLayerVirtualLan() != MatchArguments.VLAN_UNTAGGED) {
//            wildcard_hints &= ~OFMatch.OFPFW_DL_VLAN;
            wildcard_hints &= ~OFPConstant.OFWildCard.VLAN_ID;
            flow.vlanId = (match.getDataLayerVirtualLan());
            if (match.getDataLayerVirtualLanPriorityCodePoint() != 0) {
                flow.vlanPriority = (match.getDataLayerVirtualLanPriorityCodePoint());
            }
        }
        if (match.getDataLayerType() != 0) {
//            wildcard_hints &= ~OFMatch.OFPFW_DL_TYPE;
            wildcard_hints &= ~OFPConstant.OFWildCard.ETHER_TYPE;//Match.OFPFW_DL_TYPE;
            flow.etherType = (match.getDataLayerType());
        }
//        if (networkSource != 0) {
//            wildcard_hints &= ~OFWildCard.OFMatch.OFPFW_NW_DST_MASK;
//            flow.setNetworkSource(networkSource);
//        }
//        if (networkDestination != 0) {
//            wildcard_hints &= ~OFMatch.OFPFW_NW_SRC_MASK;
//            flow.setNetworkDestination(networkDestination);
//        }
        if (match.getNetworkProtocol() != 0) {
            wildcard_hints &= ~OFPConstant.OFWildCard.NW_PROTO;//Match.OFPFW_NW_PROTO;
            flow.proto=(match.getNetworkProtocol());
        }
        if (match.getNetworkTypeOfService() != 0) {
            wildcard_hints &= ~OFPConstant.OFWildCard.TOS;//Match.OFPFW_NW_TOS;
            flow.tos = (match.getNetworkTypeOfService());
        }
        if (match.getTransportSource() != 0) {
            wildcard_hints &= ~OFPConstant.OFWildCard.SRC_PORT;//Match.OFPFW_TP_SRC;
            flow.srcPort = (match.getTransportSource());
        }
        if (match.getTransportDestination() != 0) {
            wildcard_hints &= ~OFPConstant.OFWildCard.DST_PORT;//Match.OFPFW_TP_DST;
            flow.dstPort = (match.getTransportDestination());
        }
        flow.wildCards = (wildcard_hints);
        return flow;

    }

    @Override
    public String sendFlowMod(Long dpid, FlowMod flowMod) {
        String flowModId = null;

        if (logger.isTraceEnabled()) {
            logger.trace("Pushing Route flowmod routeIndx={} "
                            + "sw={} inPort={} outPort={}",
                    new Object[]{dpid, flowMod.getMatch().getInputPort(),
                            flowMod.getActions().getOutput()});
        }
        logger.info("adding a flow: {} to {}", flowMod, dpid);
        //TODO: implement send flowMod Message
        Flow flow = createFlowFromMatch(flowMod.getMatch());
        if (nosApi == null) {
            logger.warn("no nosApi access, quit operation");
            return null;
        }
        try {
            IFlowModifier flowModifier = nosApi.createFlowModifierInstance(dpid, flow);
            loadFMAction(flowModifier, flowMod.getActions());
            loadFMSettings(flowModifier, flowMod.getSettings());
            long res = flowModifier.send();


            flowModId = Cypher.getMD5(new String[]{dpid+"",
                    flowMod.toString()});
            // add to corresponding map
            Map<String, SwitchFlowModCount> sMap = RestApiServer.switchFlowModCountMap.get(dpid);
            if (sMap == null) {
                sMap = new HashMap<String, SwitchFlowModCount>();
                RestApiServer.switchFlowModCountMap.put(dpid, sMap);
            }
            SwitchFlowModCount sfmc = sMap.get(flowModId);
            if (sfmc == null) {
                sfmc = new SwitchFlowModCount(dpid, flowMod, 1);
                sMap.put(flowModId, sfmc);
                RestApiServer.globalFlowModCountMap.put(flowModId, sfmc);
            } else {
                sfmc.increaseCount();
            }
            return flowModId;
        } catch (OFSwitchNotFoundException e) {
            e.printStackTrace();
        } catch (ActionNotSupportedException e) {
            e.printStackTrace();
        } catch (ArgumentInvalidException e) {
            e.printStackTrace();
        } catch (NosSocketIOException e) {
            e.printStackTrace();
        } catch (SwitchPortNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String sendFlowMod(Long dpid, MatchArguments match, FlowAction actions, FlowSettings settings) {
        FlowMod flowMod = new FlowMod(match, actions, settings);
        return sendFlowMod(dpid,flowMod);
    }

    public String sendFlowMod(PolicyCommand policyCommand,
                              MatchArguments matchSpecified, int flowPriority,
                              DpidPortPair previousAttachPoint, DpidPortPair nextAttachPoint,
                              boolean setInPort) {
        logger.info("Generating a FLOW_MOD Message from " + previousAttachPoint
                + " to " + nextAttachPoint + " with Match = "
                + policyCommand.getMatch() + " and setInPort = " + setInPort);
        boolean dropPacket = false;
        Long sw = previousAttachPoint.getDpid();
        if (nextAttachPoint == null) {
            dropPacket = true;
        } else if (sw != nextAttachPoint
                .getDpid()) {
            logger.error("error : src port and dst port not on the same bridge!");
            return null;
        }
//        IFlowModifier imodifier = nosApi.createFlowModifierInstance(sw, flow);

        Flow flow;
        if (matchSpecified == null)
            flow = createFlowFromMatch(policyCommand.getMatch());
        else
            flow = createFlowFromMatch(matchSpecified);
        // check if need match in_port
        if (setInPort) {
            flow.wildCards = (flow.wildCards & ~OFPConstant.OFWildCard.IN_PORT);
            flow.inPort = (previousAttachPoint.getPort());
        }
        FlowAction actions = new FlowAction();
        if (!dropPacket)
            actions.getOutput().add(nextAttachPoint.getPort());
        FlowSettings settings = new FlowSettings();
        settings.setIdleTimeout((short) policyCommand.getIdleTimeout())
                .setHardTimeout((short) policyCommand.getHardTimeout())
                .setBufferId(FlowMod.BUFFER_ID_NONE)
                .setCommand(FlowMod.FMCommand.ADD);
        settings.getFlags().add(FlowMod.FMFlag.SEND_FLOW_REM);
        settings.setCookie(cookie);
        settings.setPriority((short) flowPriority);

        FlowMod flowMod = new FlowMod(policyCommand.getMatch(),actions,settings);

        return sendFlowMod(sw, flowMod);
    }

    public void setNosApi(INOSApi nosApi) {
        if(this.nosApi == null) {
            this.nosApi = nosApi;
        }
    }


    public BYODRedirectCommand addPacketInRedirect(BYODRedirectCommand policyCommand) {
        if (redirectCommands.containsKey(policyCommand.getId())) {
            return redirectCommands.get(policyCommand.getId());
        }
        return this.redirectCommands.put(policyCommand.getId(),
                policyCommand);
    }

    public List<PolicyCommand> generateInitCommands(PolicyCommand policyCommand) {
        ArrayList<PolicyCommand> initCommands = new ArrayList<PolicyCommand>();
        // priority=0 , inport , controller
        if (getRedirectCommands().size() == 1) {
            PolicyCommand controllerAllCommand = new PolicyCommand("ByodInit_0_"
                    + policyCommand.getId(), "controllerAllCommand", 0,
                    PolicyActionType.ALLOW_FLOW, new MatchArguments(), null, 0, 0,
                    policyCommand.getDpid(), policyCommand.getInPort());
            initCommands.add(controllerAllCommand);
        }
        // priority=1 , inport , drop
        MatchArguments inPortMatch = new MatchArguments();
        inPortMatch.setInputPort(policyCommand.getInPort());
        PolicyCommand dropAllCommand = new PolicyCommand("ByodInit_1_"
                + policyCommand.getId(), "dropAllCommand", 1,
                PolicyActionType.DROP_FLOW, inPortMatch, null, 0, 0,
                policyCommand.getDpid(), policyCommand.getInPort());
        initCommands.add(dropAllCommand);
        // allow arp, priorty = 2
        MatchArguments allowArpMatch = new MatchArguments();
        allowArpMatch.setDataLayerType(Ethernet.TYPE_ARP);
        allowArpMatch.setInputPort(policyCommand.getInPort());
        PolicyCommand allowArpCommand = new PolicyCommand("ByodInit_2_"
                + policyCommand.getId(), "AllowArp", 2,
                PolicyActionType.ALLOW_FLOW, allowArpMatch, null, 0, 0,
                policyCommand.getDpid(), policyCommand.getInPort());
        initCommands.add(allowArpCommand);
        // allow dhcp, priorty = 2
        MatchArguments allowDhcpMatch = new MatchArguments();
        allowDhcpMatch.setDataLayerType(Ethernet.TYPE_IPv4);
        allowDhcpMatch.setNetworkProtocol(IPv4.PROTOCOL_UDP);
        allowDhcpMatch.setTransportDestination((short) 67);
        allowArpMatch.setInputPort(policyCommand.getInPort());
        PolicyCommand allowDhcpCommand = new PolicyCommand("ByodInit_3_"
                + policyCommand.getId(), "AllowDHCP", 2,
                PolicyActionType.ALLOW_FLOW, allowDhcpMatch, null, 0, 0,
                policyCommand.getDpid(), policyCommand.getInPort());
        initCommands.add(allowDhcpCommand);

        // allow dns, priorty = 2
        MatchArguments allowDnsMatch = new MatchArguments();
        allowDnsMatch.setDataLayerType(Ethernet.TYPE_IPv4);
        allowDnsMatch.setNetworkProtocol(IPv4.PROTOCOL_UDP);
        allowDnsMatch.setTransportDestination((short) 53);
        allowArpMatch.setInputPort(policyCommand.getInPort());
        PolicyCommand allowDnsCommand = new PolicyCommand("ByodInit_4_"
                + policyCommand.getId(), "AllowDns", 2,
                PolicyActionType.ALLOW_FLOW, allowDnsMatch, null, 0, 0,
                policyCommand.getDpid(), policyCommand.getInPort());
        initCommands.add(allowDnsCommand);

        // redirect tcp 80
        MatchArguments httpMatch = new MatchArguments();
        httpMatch.setDataLayerType(Ethernet.TYPE_IPv4);
        httpMatch.setNetworkProtocol(IPv4.PROTOCOL_TCP);
        httpMatch.setTransportDestination((short) 80);
        allowArpMatch.setInputPort(policyCommand.getInPort());
        PolicyCommand redirectHpptCommand = new PolicyCommand("ByodInit_5_"
                + policyCommand.getId(), "redirectHttp", 2,
                PolicyActionType.ALLOW_FLOW, httpMatch, null, 0, 0,
                policyCommand.getDpid(), policyCommand.getInPort());
        initCommands.add(redirectHpptCommand);
        return initCommands;
    }

    public String processSingleFlowCommand(PolicyCommand policyCommand) {
        if (RestApiServer.policyCommandsDeployed.containsKey(policyCommand.getId())) {
            logger.error("policyCommand with the id={} exists!",
                    policyCommand.getId());
            return "policyCommand with the id exists!";
        }
        logger.info("Generating a " + policyCommand.getType() + " Message to "
                + policyCommand.getDpid() + " inPort= "
                + policyCommand.getInPort() + " with Match = "
                + policyCommand.getMatch());


        DpidPortPair tgtAp = new DpidPortPair(policyCommand.getDpid(),
                policyCommand.getInPort());
        if (tgtAp.getPort() == 0 && tgtAp.getDpid() == 0) {
            logger.warn("cannot find swith port of {}:{}",
                    policyCommand.getDpid(), policyCommand.getInPort());
            return "cannot find switch port of " + policyCommand.getDpid()
                    + ":" + policyCommand.getInPort();
        }
        Long tgtSw = tgtAp.getDpid();
        if (!policyCommand.getMatch().isEmpty()) {
            policyCommand.getMatch().setInputPort((short) tgtAp.getPort());
        }
        FlowSettings settings = new FlowSettings();
        settings.setIdleTimeout((short) policyCommand.getIdleTimeout())
                .setHardTimeout((short) policyCommand.getHardTimeout())
                .setBufferId(FlowMod.BUFFER_ID_NONE)
                .setPriority((short) policyCommand.getCommandPriority())
                .setCookie(cookie);

        ArrayList<FlowMod.FMFlag> flags = new ArrayList<FlowMod.FMFlag>();
        flags.add(FlowMod.FMFlag.SEND_FLOW_REM);
        settings.setFlags(flags);
        FlowAction action = new FlowAction();
        if (policyCommand.getType() == PolicyActionType.ALLOW_FLOW
                || policyCommand.getType() == PolicyActionType.BYOD_ALLOW) {
            ArrayList<Integer> ports = new ArrayList<Integer>();
            ports.add(OFPConstant.OFPort.CONTROLLER);
            action.setOutput(ports);
        }
        String ret = sendFlowMod(tgtSw, policyCommand.getMatch(), action, settings);
        return ret;

    }
}
