package com.nsfocus.scagent.manager;

import com.nsfocus.scagent.device.DeviceManager;
import com.nsfocus.scagent.model.*;
import com.nsfocus.scagent.restlet.RestApiServer;
import com.nsfocus.scagent.utility.*;
import jp.co.nttdata.ofc.common.except.NosException;
import jp.co.nttdata.ofc.common.except.NosSocketIOException;
import jp.co.nttdata.ofc.common.util.IpAddress;
import jp.co.nttdata.ofc.common.util.MacAddress;
import jp.co.nttdata.ofc.common.util.NetworkInputByteBuffer;
import jp.co.nttdata.ofc.nos.api.IFlowModifier;
import jp.co.nttdata.ofc.nos.api.INOSApi;
import jp.co.nttdata.ofc.nos.api.except.ActionNotSupportedException;
import jp.co.nttdata.ofc.nos.api.except.ArgumentInvalidException;
import jp.co.nttdata.ofc.nos.api.except.OFSwitchNotFoundException;
import jp.co.nttdata.ofc.nos.api.except.SwitchPortNotFoundException;
import jp.co.nttdata.ofc.nos.api.vo.event.PacketInEventVO;
import jp.co.nttdata.ofc.nos.common.constant.OFPConstant;
import jp.co.nttdata.ofc.nos.ofp.common.Flow;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.DpidPortPair;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.logical.LogicalSwitch;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.Edge;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.ForwardingTable;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.TopologyManager;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.Trunk;
import jp.co.nttdata.ofc.protocol.packet.EthernetPDU;
import jp.co.nttdata.ofc.protocol.packet.IPv4PDU;
import jp.co.nttdata.ofc.protocol.packet.TcpPDU;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by chen on 14-9-15.
 */
public class SCAgentDriver implements ISCAgentDriver {


    public static final long cookie = 0xabcdefL;
    static Logger logger = LoggerFactory.getLogger(SCAgentDriver.class);
    private static SCAgentDriver scAgentDriver = new SCAgentDriver();


    public static SCAgentDriver getInstance() {
        return scAgentDriver;
    }

    private INOSApi nosApi;

    public SCAgentDriver() {
        super();

    }

    public static class Route {
        Route(Long target, int dist, List<Trunk> path) {
            this.target = target;
            this.dist = dist;
            if (path != null)
                this.path = path;
        }
        Long target;
        int dist;
        List<Trunk> path = new LinkedList<Trunk>();
    }


    public List<DpidPortPair> dijkstra(DpidPortPair start, DpidPortPair end) {
        if (start.getDpid() == end.getDpid()) {
            return Arrays.asList(new DpidPortPair[]{start, end});
        }
        TopologyManager topologyManager = TopologyManager.getInstance();
        CopyOnWriteArrayList<Trunk> trunkList = topologyManager.getTrunkList();
        // <target:{dist,path}>
        Map<Long, Route> routeMap = new HashMap<Long, Route>();
        List<Long> resList = new LinkedList<Long>();
        routeMap.put(start.getDpid(), new Route(start.getDpid(), 0, null));
        List<Long> uDpids = new CopyOnWriteArrayList<Long>();
        List<Long> sDpids = new CopyOnWriteArrayList<Long>();
        sDpids.add(start.getDpid());
        for (LogicalSwitch logicalSwitch : topologyManager.getSwitchList()) {
            if (start.getDpid() != logicalSwitch.getDpid()) {
                uDpids.add(logicalSwitch.getDpid());
                routeMap.put(logicalSwitch.getDpid(),
                        new Route(logicalSwitch.getDpid(), Integer.MAX_VALUE, null));
            }
        }
        List<Long> vertexPending = new ArrayList<Long>(Arrays.asList(new Long[]{start.getDpid()}));
        gotIt:
        while (!uDpids.isEmpty()) {
            for (Long cPoint : new ArrayList<Long>(vertexPending)) {
                Route orig = new Route(0L, Integer.MAX_VALUE, null);
                List<Route> minDist = new LinkedList<Route>();
                minDist.add(orig);
                for (Long uDpid : uDpids) {
                    long ltr = uDpid < cPoint ? uDpid : cPoint;
                    long gtr = uDpid > cPoint ? uDpid : cPoint;
                    for (Trunk trunk : trunkList) {//for every target
                        if (trunk.getDpidPair()[0] == ltr && trunk.getDpidPair()[1] == gtr) { // is neighbour
                            int dist = routeMap.get(cPoint).dist + 1;    //calculate new dist
                            if (dist < minDist.get(0).dist) {
                                List<Trunk> trunks = new LinkedList<Trunk>(routeMap.get(cPoint).path);
                                trunks.add(trunk);
                                minDist.clear();
                                minDist.add(new Route(uDpid, dist, trunks));
                            } else if (dist == minDist.get(0).dist) {
                                List<Trunk> trunks = new LinkedList<Trunk>(routeMap.get(cPoint).path);
                                trunks.add(trunk);
                                minDist.add(new Route(uDpid, dist, trunks));
                            }
                            if (dist < routeMap.get(uDpid).dist) {
                                List<Trunk> trunks = new LinkedList<Trunk>(routeMap.get(cPoint).path);
                                trunks.add(trunk);
                                routeMap.get(uDpid).dist = dist;
                                routeMap.get(uDpid).path = trunks;
                            }
                        }
                    }
                }
                if (minDist.contains(end.getDpid())) {
                    break gotIt;
                }
                for (Route newMin : minDist) {
                    resList.add(newMin.target);
                    uDpids.remove(newMin.target);
                    vertexPending.add(newMin.target);
                }
                vertexPending.remove(cPoint);
            }
        }
        List<Trunk> route = routeMap.get(end.getDpid()).path;
        LinkedList<DpidPortPair> path = new LinkedList<DpidPortPair>();
        path.add(start);
        for (Trunk trunk : route) {
            if (path.getLast().getDpid() == trunk.getDpidPair()[0]) {
                path.add(new DpidPortPair(trunk.getDpidPair()[0], trunk.getEdgeList().get(0).getPorts()[0]));
                path.add(new DpidPortPair(trunk.getDpidPair()[1], trunk.getEdgeList().get(0).getPorts()[1]));
            } else {
                path.add(new DpidPortPair(trunk.getDpidPair()[1], trunk.getEdgeList().get(0).getPorts()[1]));
                path.add(new DpidPortPair(trunk.getDpidPair()[0], trunk.getEdgeList().get(0).getPorts()[0]));
            }
        }
        path.add(end);
        return path;
    }

    private Long extraceMinDist(List<Trunk> trunkList, List<Long> uDpids, Long src) {
        return null;
    }

    @Override
    public List<DpidPortPair> computeRoute(DpidPortPair start, DpidPortPair end) {
        logger.info("computing a route from {}:{} to {}:{}", start.getDpid(), start.getPort(), end.getDpid(), end.getPort());
        ArrayList<DpidPortPair> path = new ArrayList<DpidPortPair>();
        if (start.getDpid() == end.getDpid()) {
            path.add(start);
            path.add(end);
            return path;
        }
        ForwardingTable forwardingTable = TopologyManager.getInstance().getForwardingTable();
        CopyOnWriteArrayList<Trunk> trunkList = TopologyManager.getInstance().getTrunkList();
        boolean proceed = true;
        DpidPortPair currentPair = start;
        while (proceed) {
            boolean noPath = true;
            path.add(currentPair);
            for (ForwardingTable.ForwardingTableEntry entry : forwardingTable.getTable()) {
                if (entry.getKey()[0] == currentPair.getDpid() && entry.getKey()[1] == end.getDpid()) {
                    noPath = false;
                    Integer portForward = entry.getPortList().get(0);
                    Long dpid = currentPair.getDpid();
                    path.add(new DpidPortPair(dpid, portForward));
                    //find which dpid portForward links to'
                    for (Trunk trunk : trunkList) {
                        if (trunk.getDpidPair()[0] == dpid) {
                            for (Edge edge : trunk.getEdgeList()) {
                                if (edge.getPorts()[0] == portForward) {
                                    Long nextDpid = trunk.getDpidPair()[1];
                                    int nextPort = edge.getPorts()[1];
                                    currentPair = new DpidPortPair(nextDpid, nextPort);
                                    if (nextDpid == end.getDpid()) {
                                        proceed = false;
                                    }
                                }
                            }
                        } else if (trunk.getDpidPair()[1] == dpid) {
                            for (Edge edge : trunk.getEdgeList()) {
                                if (edge.getPorts()[1] == portForward) {
                                    Long nextDpid = trunk.getDpidPair()[0];
                                    int nextPort = edge.getPorts()[0];
                                    currentPair = new DpidPortPair(nextDpid, nextPort);
                                    if (nextDpid == end.getDpid()) {
                                        proceed = false;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
            if (noPath) {
                return null;
            }
        }
        path.add(currentPair);
        path.add(end);
        return path;
    }

    public static void loadFMAction(IFlowModifier flowModifier, FlowAction action)
            throws ActionNotSupportedException, OFSwitchNotFoundException, ArgumentInvalidException, SwitchPortNotFoundException {
        if (action == null)
            return;
        if (action.getOutput() != null && action.getOutput().size() > 0) {
            for (int o : action.getOutput()) {
                flowModifier.addOutputAction(o, 65535);
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
        } else if (settings.getCommand() == FlowMod.FMCommand.DELETE) {
            flowModifier.setDeleteCommand();
        } else if (settings.getCommand() == FlowMod.FMCommand.DELETE_STRICT) {
            flowModifier.setDeleteStrictCommand();
        } else if (settings.getCommand() == FlowMod.FMCommand.MODIFY) {
            flowModifier.setModifyCommand();
        } else if (settings.getCommand() == FlowMod.FMCommand.MODIFY_STRICT) {
            flowModifier.setModifyStrictCommand();
        } else {      //default to add command
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
                res |= 1;
            } else if (flag == FlowMod.FMFlag.CHECK_OVERLAP) {
                res |= 1 << 1;
            } else if (flag == FlowMod.FMFlag.EMERG) {
                res |= 1 << 2;
            }
        }
        return res;
    }

    /**
     * @return an OFMatch object without in_port
     */
    public Flow createFlowFromMatch(MatchArguments match) {
        Integer wildcard_hints = OFPConstant.OFWildCard.ALL;
        Flow flow = new Flow();
        if (match.getInputPort() != 0) {
            wildcard_hints &= ~OFPConstant.OFWildCard.IN_PORT;
            flow.inPort = match.getInputPort();
        }
        if (!Arrays.equals(match.getDataLayerSourceBytes(), new byte[]{0, 0, 0, 0, 0, 0})) {
            wildcard_hints &= ~OFPConstant.OFWildCard.SRC_MACADDR;
            try {
                flow.srcMacaddr = new MacAddress(match.getDataLayerSourceBytes());
            } catch (NosException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (!Arrays.equals(match.getDataLayerDestinationBytes(),
                new byte[]{0, 0, 0, 0, 0, 0})) {
            wildcard_hints &= ~OFPConstant.OFWildCard.DST_MACADDR;
            try {
                flow.dstMacaddr = new MacAddress(match.getDataLayerDestinationBytes());
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
        if (match.getNetworkSource() != 0) {
            wildcard_hints &= ~OFPConstant.OFWildCard.SRC_MASK;
            try {
                flow.srcIpaddr = new IpAddress(match.getNetworkSource());
            } catch (NosException e) {
                e.printStackTrace();
            }
        }
        if (match.getNetworkDestination() != 0) {
            wildcard_hints &= ~OFPConstant.OFWildCard.DST_MASK;
            try {
                flow.dstIpaddr = new IpAddress(match.getNetworkDestination());
            } catch (NosException e) {
                e.printStackTrace();
            }
        }
        if (match.getNetworkProtocol() != 0) {
            wildcard_hints &= ~OFPConstant.OFWildCard.NW_PROTO;//Match.OFPFW_NW_PROTO;
            flow.proto = (match.getNetworkProtocol());
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
            // send flow now
            long res = flowModifier.send();


            flowModId = Cypher.getMD5(new String[]{dpid + "",
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
        return sendFlowMod(dpid, flowMod);
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

        FlowMod flowMod = new FlowMod(policyCommand.getMatch(), actions, settings);

        return sendFlowMod(sw, flowMod);
    }

    public void setNosApi(INOSApi nosApi) {
        if (this.nosApi == null) {
            this.nosApi = nosApi;
        }
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
        return sendFlowMod(tgtSw, policyCommand.getMatch(), action, settings);
    }

    public String deletePolicies(PolicyActionType type, List<PolicyCommand> policyCommands) {
        logger.info("Start to handle " + type + " policyCommands");
        for (PolicyCommand policyCommand : policyCommands) {
            List<String> flowModIdList = RestApiServer.policyCommandsDeployed.get(
                    policyCommand.getId()).getFlowModIdList();
            for (String fmid : flowModIdList) {
                SwitchFlowModCount switchFlowModCount = RestApiServer.globalFlowModCountMap
                        .get(fmid);
                if (switchFlowModCount != null) {
                    int remainCount = switchFlowModCount.decreaseCount();
                    if (remainCount == 0) {
                        scAgentDriver.removeFlowEntry(fmid);
                    }
                }
            }
            RestApiServer.policyCommandsDeployed.remove(policyCommand.getId());
            // still need to remove entry from RestApiServer.allowPolicies
            if (type == PolicyActionType.RESTORE_BYOD_ALLOW) {
                RestApiServer.removeFromAllowPolicies(policyCommand.getId());
            }
        }
        return type.toString();

    }

    private int removeFlowEntry(String flowModMessageId) {
        SwitchFlowModCount sfmc = RestApiServer.globalFlowModCountMap.get(flowModMessageId);
        if (sfmc == null) {
            logger.info("no such flow exists : {}", flowModMessageId);
            return -1;
        }
        int count;
        if ((count = sfmc.decreaseCount()) > 0) {
            return count;
        }
        long sw = sfmc.getDpid();
        FlowMod flowModMessage = sfmc.getFlowModMessage();
        flowModMessage.getSettings().setCommand(FlowMod.FMCommand.DELETE_STRICT);
        if (flowModMessage.getActions() != null && flowModMessage.getActions().getOutput() != null && flowModMessage.getActions().getOutput().size() > 0) {
            Integer out = flowModMessage.getActions().getOutput().get(0);
            flowModMessage.getSettings().setOutPort(out);
        }
        sendFlowMod(sw, flowModMessage);
        SwitchFlowModCount removed = RestApiServer.globalFlowModCountMap
                .remove(flowModMessageId);
        Map<String, SwitchFlowModCount> sMap = RestApiServer.switchFlowModCountMap
                .get(removed.getDpid());
        sMap.remove(flowModMessageId);
        return count;
    }

    /**
     * process packet in according to policies
     *
     * @param nosApi   api to create and send flowMod message
     * @param packetIn
     * @return true to continue , false to break;
     */
    public boolean handleIncomingPackets(INOSApi nosApi, PacketInEventVO packetIn) throws NosException {
        if (this.nosApi == null) {
            this.nosApi = nosApi;
        }
        if (RestApiServer.redirectCommands.isEmpty()) {
            return true;
        }
        EthernetPDU eth = new EthernetPDU();
        if (!eth.parse(new NetworkInputByteBuffer(packetIn.data))) {
            logger.warn("failed to parse data as an ethernet frame");
            return false;
        }
        Boolean res = true;
        for (Map.Entry<String, BYODRedirectCommand> rulesEntry : RestApiServer.redirectCommands
                .entrySet()) {
            BYODRedirectCommand byodCommand = rulesEntry.getValue();
            // check if the dpid matches
            if (byodCommand.getDpid() != 0
                    && byodCommand.getDpid() != packetIn.dpid) {
                res = true;
                continue;
            }
            // check if the inport matches
            if (byodCommand.getInPort() != 0
                    && byodCommand.getInPort() != packetIn.inPort) {
                res = true;
                continue;
            }
            MacAddress piDlSrc = eth.srcMacaddr;
            // pass all type of pi from authorized devices
            for (PolicyCommand p : RestApiServer.allowPolicies.values()) {
                if (piDlSrc != null
                        && MACAddress.valueOf(p.getMatch().getDataLayerSourceBytes())
                        .equals(MACAddress.valueOf(piDlSrc.toLong()))
                        ) {
                    return true;
                }
            }
            // packets of unauthorized devices from specified dpid and inPort
            if (eth.etherType == Ethernet.TYPE_IPv4) {
                IPv4PDU ip = (IPv4PDU) eth.next;
//                IPv4 ip = (IPv4) eth.getPayload();
                IpAddress piNwSrc = ip.srcIpaddr;
                // check if the source ip belongs to the specified subnet
                if ((piNwSrc.toLong() & (0xFFFFFFFF << (32 - byodCommand
                        .getMask()))) != (IPv4.toIPv4Address(byodCommand
                        .getNetwork()) & (0xFFFFFFFF << (32 - byodCommand
                        .getMask())))) {
                    logger.info("network not match {}", piNwSrc);
                    //won't match another init policy , suppose that a switch port connects to single ap
                    // so just return
                    return true;
                }
                if (ip.proto == IPv4.PROTOCOL_TCP) { // case tcp
                    TcpPDU tcpFrame = (TcpPDU) ip.next;
//                    TCP tcpFrame = (TCP) ip.getPayload();
                    int piTpDst = (int) (tcpFrame.dstPort >= 0 ? tcpFrame
                            .dstPort : tcpFrame
                            .dstPort + 65536);
                    if (piTpDst == 80) {
                        logger.info("captured a matching packet , launching redirect operation");

//                        OFMatch match = new OFMatch();
                        Flow match = new Flow();
                        long origNwDst = 0;
                        MacAddress origDlDst = null;
                        origNwDst = ip.dstIpaddr.toLong();
                        origDlDst = eth.dstMacaddr;
                        logger.info(
                                "orgin ip and mac = {} and {},now the dpid is {}",
                                new Object[]{IPv4.fromIPv4Address((int) origNwDst),
                                        origDlDst,
                                        HexString.toHexString(packetIn.dpid, 8)});
                        match.extractFlowInfo(eth, packetIn.inPort);
//                        match.loadFromPacket(pi.getPacketData(), pi.getInPort());
                        match.wildCards = OFPConstant.OFWildCard.ALL
                                & ~OFPConstant.OFWildCard.SRC_MACADDR
                                & ~OFPConstant.OFWildCard.DST_MACADDR
                                & ~OFPConstant.OFWildCard.SRC_MASK
                                & ~OFPConstant.OFWildCard.DST_MASK & ~OFPConstant.OFWildCard.ETHER_TYPE
                                & ~OFPConstant.OFWildCard.NW_PROTO & ~OFPConstant.OFWildCard.DST_PORT
                                & ~OFPConstant.OFWildCard.SRC_PORT;

                        DpidPortPair dpidPortPair = DeviceManager.getInstance().findHostByMac(byodCommand
                                .getServerMac().toUpperCase());
                        DpidPortPair svrAp = null;
                        if (dpidPortPair != null) {
                            svrAp = dpidPortPair;
                        } else {
                            logger.warn("cannot find the ap of {}, abort!",
                                    byodCommand.getServerMac());
                            return false;
                        }
                        DpidPortPair devAp = new DpidPortPair(
                                byodCommand.getDpid(), byodCommand.getInPort());

                        // compute a route form device to authenication server
//                        Route route = computeRoute(devAp, svrAp);
                        List<DpidPortPair> path = computeRoute(devAp, svrAp);
                        if (path == null || path.size() < 1) {
                            logger.warn(
                                    "routeEngine cannot find a path from {} to {}",
                                    devAp.toString(), svrAp.toString());
                            return false;
                        }
                        // push flows into every switch in the route
                        for (int j = 0; path != null && j < path.size() - 1; j += 2) {
                            if (j + 2 < path.size()) {
                                // do nothing more than output
                                IFlowModifier flowModifier = nosApi.createFlowModifierInstance(path.get(j).getDpid(), match);
                                flowModifier.addOutputAction(path.get(j + 1).getPort(), 0);
                                flowModifier.setFlags(1);
                                flowModifier.setHardTimeoutSec(0);
                                flowModifier.setIdleTimeoutSec(500);
                                flowModifier.setCookie(cookie);
                                flowModifier.setAddCommand();
                                flowModifier.setPriority(byodCommand.getCommandPriority());
                                flowModifier.setBufferId(FlowMod.NONE_BUFFER_ID);
                                flowModifier.send();
                            } else {
                                IFlowModifier flowModifier = nosApi.createFlowModifierInstance(path.get(j).getDpid(), match);
                                flowModifier.addSetDstIpaddrAction(IPv4.toIPv4Address(byodCommand.getServerIp()));
                                flowModifier.addSetDstMacaddrAction(MACAddress.valueOf(byodCommand.getServerMac()).toLong());
                                flowModifier.addOutputAction(path.get(j + 1).getPort(), 0);
                                flowModifier.setFlags(1);
                                flowModifier.setHardTimeoutSec(0);
                                flowModifier.setIdleTimeoutSec(500);
                                flowModifier.setCookie(cookie);
                                flowModifier.setAddCommand();
                                flowModifier.setPriority(byodCommand.getCommandPriority());
                                flowModifier.setBufferId(FlowMod.NONE_BUFFER_ID);
                                flowModifier.send();
                            }
                        }
                        // now need to add a route back from server to device ,
                        // just reverse the previous route will do
                        // and reverse the match too, replace origin destination
                        // server to authentications server info
                        logger.info("now creating a route for flow to back to device...");
                        Flow match1 = match.clone();
//                        match1.setWildcards(match.getWildcards()
//                                | OFMatch.OFPFW_DL_DST);
                        match1.wildCards |= OFPConstant.OFWildCard.DST_MACADDR;
                        // match1.setDataLayerDestination(match.getDataLayerSourceBytes());
                        match1.dstMacaddr = match
                                .srcMacaddr;
                        match1.srcMacaddr = new MacAddress(byodCommand.getServerMac());
                        match1.dstIpaddr = match.srcIpaddr;
                        match1.srcIpaddr = new IpAddress(byodCommand.getServerIp());
                        match1.dstPort = match
                                .srcPort;
                        match1.srcPort = match
                                .dstPort;
                        Flow match2 = match1.clone();
                        match2.srcMacaddr = new MacAddress(origDlDst);
                        match2.srcIpaddr = new IpAddress(origNwDst);

                        for (int j = path.size() - 1; j >= 0; j -= 2) {
                            if (j != path.size() - 1) {
                                // do nothing more than output
                                IFlowModifier modifier1 = nosApi.createFlowModifierInstance(path.get(j).getDpid(), match2);
                                modifier1.setAddCommand();
                                modifier1.setBufferId(FlowMod.NONE_BUFFER_ID);
                                modifier1.setPriority(byodCommand.getCommandPriority());
                                modifier1.setIdleTimeoutSec(500);
                                modifier1.setHardTimeoutSec(0);
                                modifier1.setFlags(1);
                                modifier1.setCookie(cookie);
                                // set output port
                                modifier1.addOutputAction(path.get(j - 1).getPort(), 0);
                                modifier1.send();

                            } else {
                                IFlowModifier modifier2 = nosApi.createFlowModifierInstance(path.get(j).getDpid(), match1);
                                modifier2.setAddCommand();
                                modifier2.setBufferId(FlowMod.NONE_BUFFER_ID);
                                modifier2.setPriority(byodCommand.getCommandPriority());
                                modifier2.setIdleTimeoutSec(500);
                                modifier2.setHardTimeoutSec(0);
                                modifier2.setFlags(1);
                                modifier2.setCookie(cookie);
                                // set output port
                                modifier2.addSetSrcMacaddrAction(origDlDst.toLong());
                                modifier2.addSetDstMacaddrAction(eth.srcMacaddr.toLong());
                                modifier2.addSetSrcIpaddrAction(origNwDst);
                                modifier2.addOutputAction(path.get(j - 1).getPort(), 0);
                                modifier2.send();

                            }
                        }
                        return false;
                    } else if (piTpDst == 53 || piTpDst == 67) {
                        return true;
                    } else {
                        return false;
                    }
                } else { // none tcp (udp) packet, may be dns and dhcp , should
                    // let it pass
                    return true;
                }
            } else { // none ipv4 packet , may be arp , let it pass
                return true;
            }
        }
        return res;
    }

    public void removeFlowEntry(PolicyCommand policyCommand, MatchArguments matchSpecified, short flowPriority, DpidPortPair previousAttachPoint, DpidPortPair nextAttachPoint, boolean setInPort) {
        logger.info("Generating a FLOW_MOD Message from " + previousAttachPoint
                + " to " + nextAttachPoint + " with Match = "
                + policyCommand.getMatch() + " and setInPort = " + setInPort);

        if (previousAttachPoint.getDpid() != nextAttachPoint.getDpid()) {
            logger.error("error : src port and dst port not on the same bridge!");
            return;
        }
        long sw = previousAttachPoint.getDpid();
        Flow match;
        if (matchSpecified == null)
            match = createFlowFromMatch(policyCommand.getMatch());
        else
            match = createFlowFromMatch(matchSpecified);
        // check if need match in_port
        if (setInPort) {
            match.wildCards &= ~OFPConstant.OFWildCard.IN_PORT;
            match.inPort = previousAttachPoint.getPort();
        }

        IFlowModifier fm = null;
        try {
            fm = nosApi.createFlowModifierInstance(previousAttachPoint.getDpid(), match);
            loadFMSettings(fm, policyCommand.createFlowSettings());
            fm.setOutPort(nextAttachPoint.getPort());
            fm.setDeleteStrictCommand();
            fm.send();
        } catch (OFSwitchNotFoundException e) {
            logger.warn("Create flowMod error. policy id: {}", policyCommand.getId());
            e.printStackTrace();
        } catch (NosSocketIOException e) {
            logger.warn("delete flow error. dpid: {}, match: {}", policyCommand.getDpid(), match);
            e.printStackTrace();
        }
    }

    public String handleRestoreCommands(PolicyActionType type,
                                        List<? extends PolicyCommand> policyCommands) {
        logger.info("Start to handle " + type + " policyCommands");
        for (PolicyCommand policyCommand : policyCommands) {
            List<String> flowModIdList = RestApiServer.policyCommandsDeployed.get(
                    policyCommand.getId()).getFlowModIdList();
            for (String fmid : flowModIdList) {
                SwitchFlowModCount switchFlowModCount = RestApiServer.globalFlowModCountMap
                        .get(fmid);
                if (switchFlowModCount != null) {
                    int remainCount = switchFlowModCount.decreaseCount();
                    if (remainCount == 0) {
                        removeFlowEntry(fmid);
                    }
                }
            }
            RestApiServer.policyCommandsDeployed.remove(policyCommand.getId());
            // still need to remove entry from authorizedMacs
            if (type == PolicyActionType.RESTORE_BYOD_ALLOW) {
                RestApiServer.removeFromAllowPolicies(policyCommand.getId());
            }
        }
        return type.toString();
    }

}
