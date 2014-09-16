package com.nsfocus.scagent.restlet;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.nsfocus.scagent.manager.SCAgentDriver;
import com.nsfocus.scagent.model.*;
import com.nsfocus.scagent.utility.HexString;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.DpidPortPair;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.topology.TopologyManager;

import org.restlet.Component;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nsfocus.scagent.device.DeviceManager;

public class RestApiServer extends ServerResource {

    private static final int ORIGIN_DESTINATION_PRIORITY = -1;
    private static final int ORIGIN_SOURCE_PRIORITY = -1;
    protected static Logger logger = LoggerFactory
    .getLogger(RestApiServer.class);
    private static final int DEFAULT_IDLE_TIMEOUT = 5000;
    private static final int DEFAULT_HARD_TIMEOUT = 0;

    private SCAgentDriver scAgentDriver = SCAgentDriver.getInstance();
    // 将所有策略保存在内存中
    public static Map<String, PolicyCommandDeployed> policyCommandsDeployed = new HashMap<String, PolicyCommandDeployed>();
    static Map<String, PolicyCommand> sourcePolicyCommandsWithoutInPort = new HashMap<String, PolicyCommand>();
    // <DPID,SwitchFlowModCount>
    public static Map<Long, Map<String, SwitchFlowModCount>> switchFlowModCountMap = new HashMap<Long, Map<String, SwitchFlowModCount>>();
    // <FlowModId,SwitchFlowModCount>
    public static Map<String, SwitchFlowModCount> globalFlowModCountMap = new HashMap<String, SwitchFlowModCount>();
	public static void runServer() throws Exception {

		// new Server(Context.getCurrent(), Protocol.HTTP,
		// 8182,RestApiServer.class).start();
		Component component = new Component();
		component.getServers().add(Protocol.HTTP, 8182);
		component.getDefaultHost().attach(new RestApiApplication());
		component.start();
		logger.debug("test_______________________________");
	}

	@Get("json")
	public String handleGetRequest() {
        ServerResource restApi = (ServerResource) getContext().getAttributes().get(this.getClass().getCanonicalName());
        String type = (String) getRequestAttributes().get("op");
		if (type != null) {
			if(type.equals("device")){
				Form form = getQuery();
				String mac = form.getFirstValue("mac");
				Gson gson = new Gson();
				if (mac != null) {
					DpidPortPair dpidPort = DeviceManager.getInstance()
					.findHostByMac(mac);
					return gson.toJson(dpidPort);
				} else {
					return gson.toJson(DeviceManager.getInstance()
							.getMacDpidPortMap());
				}
			}else if (type.equalsIgnoreCase("route")){
				Form form = getQuery();
				String ends = form.getFirstValue("ends").toUpperCase();
				if(ends !=null){
					String[] split = ends.split("-");
					if(split.length !=2){
						return "{\"result\":\"param error\"}";
					}
					DpidPortPair start = DeviceManager.getInstance()
					.findHostByMac(split[0]);
					DpidPortPair end = DeviceManager.getInstance()
					.findHostByMac(split[1]);
					String res = "";
					if(start == null){
						res += " "+split[0]+" ";
					}
					if(end == null){
						res = " "+split[1]+" ";
					}
					if(!res.equals("")){
						return "{\"result\":\"can't find "+res+" \"}";
					}
                    List<DpidPortPair> path = SCAgentDriver.getInstance().computeRoute(start, end);
                    return new Gson().toJson(path);
				}else{
					return "{\"result\":\"ends needed\"}";
				}

			}else if(type.equalsIgnoreCase("test")) {
				return new Gson().toJson(TopologyManager.getInstance().getForwardingTable().getTable());
//				return new Gson().toJson(TopologyManager.getInstance().getTrunkList());
			}
		}
		return "{\"result\":\"error\"}";
		// return "testValue ="+ ""
		// +"\nResource URI  : " + getReference() + '\n' + "Root URI      : "
		// + getRootRef() + '\n' + "Routed part   : "
		// + getReference().getBaseRef() + '\n' + "Remaining part: "
		// + getReference().getRemainingPart();
	}

	@Post
	public Representation handlePostRequest(Representation entity) {
		String op = (String) getRequestAttributes().get("op");

		Representation result = null;
		// Parse the given representation and retrieve data
		if(op.equalsIgnoreCase("test")){
			try {
				String text = entity.getText();
				Gson gson = new Gson();
				JsonParser parser = new JsonParser();
				JsonObject rootNode = parser.parse(text).getAsJsonObject();
				String id = rootNode.get("id").getAsString();
				System.out.println("id: " + id);
				JsonArray ifs = rootNode.get("ifs").getAsJsonArray();
				for (int i = 0; i < ifs.size(); i++) {
					JsonObject ifObj = gson.fromJson(ifs.get(i), JsonObject.class);
					JsonElement jsonElement = ifObj.get("abc");
					String connectTo = ifObj.get("connect_to").getAsString();
					String mac = ifObj.get("mac").getAsString();
					System.out
					.println("connect_to:" + connectTo + "\t mac: " + mac);
				}
				result = new StringRepresentation(text, MediaType.APPLICATION_JSON);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				result = null;
				e.printStackTrace();
			}
		}else if (op.equalsIgnoreCase("policyaction")){
			String text;
			try {
				text = entity.getText(); // throw exception
				Gson gson = new Gson();
				JsonParser parser = new JsonParser();
				JsonObject rootNode = parser.parse(text).getAsJsonObject();
				String type = rootNode.get("type").getAsString();
		        List<? extends PolicyCommand> policyCommands = PolicyCommand.fromJson(
		                rootNode, PolicyActionType.valueOf(type));
                String res = "";
				if(type.equalsIgnoreCase("REDIRECT_FLOW")){
		            logger.info("Start to handle " + type + " policyCommands");
                    for (PolicyCommand policyCommand : policyCommands) {
                        res += processRedirectFlowCommand(policyCommand);
                    }
				}else if (type.equals(PolicyActionType.BYOD_INIT.toString())) {
                    ArrayList<PolicyCommand> initCommands = new ArrayList<PolicyCommand>();
                    HashMap<String, String> resMap = new HashMap<String, String>();
                    String status = "ok";
                    for (PolicyCommand policyCommand : policyCommands) {
                        // add redirect rules to scAgent
                        BYODRedirectCommand addInitRes = scAgentService
                                .addByodRedirectCommand((BYODRedirectCommand) policyCommand);
                        if (addInitRes != null) {
                            res += "add policy with the same id "
                                    + policyCommand.getId() + " exists, abort.";
                            resMap.put(policyCommand.getId(), "exists");
                            status = "error";
                            continue;
                        }
                        // generate init commands to allow dhcp , arp , dns and redirect
                        initCommands.addAll(generateInitCommands(policyCommand));
                    }
                    // Send initiating flows generated above
                    for (PolicyCommand policyCommand : initCommands) {
                        String s = processSingleFlowCommand(policyCommand);
                        resMap.put(policyCommand.getPolicyName(), s);
                        if (!s.equalsIgnoreCase("ok")) {
                            status = "error";
                        }
                    }
                    JsonFactory jasonFactory = new JsonFactory();
                    StringWriter writer = new StringWriter();
                    //start a thread to maitain the byod flows
                    ScheduledExecutorService scheduledExecutor = threadPoolService.getScheduledExecutor();
                    scheduledExecutor.schedule(new Runnable() {
                        @Override
                        public void run() {
                            logger.info("start a thread to monitor the byod flows");
                            while (true) {
                                maintainPolicyFlows();
                                try {
                                    // Set the poll interval
                                    Thread.sleep(30 * 1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if(scAgentService.getByodRedirectCommands().size() == 0 && scAgentService.getAuthorizedMacs().size() ==0){
                                    logger.info("no BYOD policy, stop monitor thread");
                                    break;
                                }
                            }
                        }
                    }, 5, TimeUnit.SECONDS);
                    try {
                        JsonGenerator generator = jasonFactory
                                .createGenerator(writer);
                        generator.writeStartObject();
                        generator.writeArrayFieldStart("result");
                        for (String id : resMap.keySet()) {
                            generator.writeStartObject();
                            generator.writeStringField(id, resMap.get(id));
                            generator.writeEndObject();
                        }
                        generator.writeEndArray();
                        generator.writeStringField("status", status);
                        generator.writeEndObject();
                        generator.close();
                        return writer.toString();
                    } catch (JsonGenerationException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return "{\"status\" : \"error\", \"result\" : \"json conversion failed. \"}";
                }
                result = new StringRepresentation(gson.toJson(policyCommands),MediaType.APPLICATION_JSON);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return result;
	}

    List<DpidPortPair> computeRoute(DpidPortPair start, DpidPortPair end){
        List<DpidPortPair> path = new ArrayList<DpidPortPair>();
        //TODO
        return path;
    }

    private String processRedirectFlowCommand(PolicyCommand policyCommand) {
        logger.info("Processing REDIRECT_FLOW policyCommand: {}", policyCommand);
        if (policyCommandsDeployed.containsKey(policyCommand.getId())) {
            logger.error("a policyCommand with the same id={} exists!",
                    policyCommand.getId());
            return "a policyCommand with the same id exists!";
        }
        List<String> flowModMessageIds = new ArrayList<String>();
        // 先将同一策略内部的Devices连接起来
        List<SecurityDevice> devices = policyCommand.getDevices();
        int deviceNum = 0;
        if (devices != null) {
            deviceNum = policyCommand.getDevices().size();
        }
        if (deviceNum > 1) {
            for (int i = 0; i < deviceNum - 1; i++) {
                SecurityDevice deviceStart = policyCommand.getDevices().get(i);
                SecurityDevice deviceEnd = policyCommand.getDevices()
                        .get(i + 1);
                List<DpidPortPair> path = computeRoute(
                        getAttachmentPoint(deviceStart
                                .getOutgressAttachmentPointInfo()),
                        getAttachmentPoint(deviceEnd
                                .getIngressAttachmentPointInfo()));
                if (path == null || path.size() < 1) {
                    logger.error(
                            "routeEngine cannot find a path from {} to {}",
                            deviceStart.getOutgressAttachmentPointInfo()
                                    .toString(), deviceEnd
                                    .getIngressAttachmentPointInfo().toString());
                    return "cannot find  a path ";
                }
                for (int j = 0; j < path.size() - 1; j += 2) {
//                    String flowModId = pushFlowEntry(policyCommand, null,
//                            PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,
//                            path.get(j), path.get(j + 1), true);
                    policyCommand.getMatch().setInputPort((short) path.get(j).getPort());
                    FlowAction action = new FlowAction((short) path.get(j + 1).getPort());
                    FlowSettings settings = new FlowSettings();
                    settings.setIdleTimeout((short) policyCommand.getIdleTimeout())
                            .setHardTimeout((short) policyCommand.getHardTimeout())
                            .setCommand(FlowMod.FMCommand.ADD)
                            .setBufferId(FlowMod.BUFFER_ID_NONE)
                            .setCookie(SCAgentDriver.cookie)
                            .setPriority((short) policyCommand.getCommandPriority())
                            .getFlags().add(FlowMod.FMFlag.SEND_FLOW_REM);
                    FlowMod flowMod = new FlowMod(policyCommand.getMatch(), action, settings);
                    String flowModId = scAgentDriver.sendFlowMod(path.get(j).getDpid(), flowMod);
                    if (flowModId != null) {
                        flowModMessageIds.add(flowModId);
                    }
                }
            }
        }
        List<PolicyCommandRelated> policyCommandsRelatedPrior = new ArrayList<PolicyCommandRelated>();
        List<PolicyCommandRelated> policyCommandsRelatedPosterior = new ArrayList<PolicyCommandRelated>();
        // 如果从Match中可以确定重定向起点那么新建一个对应的源策略

        // TODO:还可以根据IP+Vlan来确定源主机AP
        DpidPortPair sourceAP = null;
        AttachmentPointInfo sourceAPInfo = null;
        if (!Arrays.equals(policyCommand.getMatch().getDataLayerSource(),
                new byte[]{0, 0, 0, 0, 0, 0})) {
            sourceAPInfo = new AttachmentPointInfo(policyCommand.getMatch()
                    .getDataLayerSource(), null);
            sourceAP = getAttachmentPoint(sourceAPInfo);
        }

        // TODO:还可以根据IP+Vlan来确定源主机AP
        DpidPortPair destinationAP = null;
        AttachmentPointInfo destinationAPInfo = null;
        if (!Arrays.equals(policyCommand.getMatch().getDataLayerDestination(),
                new byte[]{0, 0, 0, 0, 0, 0})) {
            destinationAPInfo = new AttachmentPointInfo(policyCommand
                    .getMatch().getDataLayerDestination(), null);
            destinationAP = getAttachmentPoint(destinationAPInfo);
        }

        if (sourceAP == null && destinationAP == null) {
            return "no source and destination host specified ";
        }
        if (sourceAP != null) {
            addImplicitPolicyCommandDeployed(sourceAP, sourceAPInfo,
                    ORIGIN_SOURCE_PRIORITY, true);
        }
        if (destinationAP != null) {
            addImplicitPolicyCommandDeployed(destinationAP, destinationAPInfo,
                    ORIGIN_DESTINATION_PRIORITY, false);
            if (sourceAP == null) {
                addImplicitPolicyCommandDeployed(destinationAP,
                        destinationAPInfo, ORIGIN_SOURCE_PRIORITY, false);
            }
        }

        PolicyCommandDeployed.findRelatedPolicies(policyCommand,
                policyCommandsDeployed, policyCommandsRelatedPrior,
                policyCommandsRelatedPosterior);

        // 先处理较高策略,

        // 依次处理policyCommandsRelatedPrior中的策略，将每条策略后加入一条pr.device -priority->
        // policyCommand.device的策略
        Collections.reverse(policyCommandsRelatedPrior);
        for (PolicyCommandRelated policyPrior : policyCommandsRelatedPrior) {
            // 如果覆盖掉了pr相关策略的也应该删除
            boolean noAction = false;
            List<PolicyCommandRelated> removedPolicies = PolicyCommandDeployed
                    .removeCoveredPolicies(policyCommand,
                            policyCommandsDeployed.get(policyPrior
                                    .getPolicyCommnd().getId()),
                            Direction.ASCEND);
            if (!removedPolicies.isEmpty()) {
                // 相应的流也应该删除
                for (PolicyCommandRelated removed : removedPolicies) {
                    if (removed.getPolicyCommnd().getId()
                            .equals(policyCommand.getId())) {
                        noAction = true;
                        break;
                    }
                    MatchArguments match = removed.getPolicyCommnd().getMatch();
                    if (removed.getPolicyCommnd().getCommandPriority() == ORIGIN_DESTINATION_PRIORITY)
                        match = policyPrior.getPolicyCommnd().getMatch();

                    List<SecurityDevice> devicesOfPrePolicy = policyPrior
                            .getPolicyCommnd().getDevices();
                    DpidPortPair startPoint = getAttachmentPoint(devicesOfPrePolicy
                            .get(devicesOfPrePolicy.size() - 1)
                            .getOutgressAttachmentPointInfo());
                    DpidPortPair endPoint = getAttachmentPoint(removed
                            .getPolicyCommnd().getDevices().get(0)
                            .getIngressAttachmentPointInfo());
                    logger.info(
                            "Prepare to delete a path, Computing a route form {} to {}",
                            startPoint, endPoint);
                    List<DpidPortPair> path = computeRoute(startPoint, endPoint);
                    if (path == null || path.size() < 1) {
                        logger.error(
                                "routeEngine cannot find a path from {} to {}",
                                devicesOfPrePolicy
                                        .get(devicesOfPrePolicy.size() - 1)
                                        .getOutgressAttachmentPointInfo()
                                        .toString(), policyCommand.getDevices()
                                        .get(0).getIngressAttachmentPointInfo()
                                        .toString());
                        return "cannot find  a path ";
                    }
                    short flowPriority = removed.getFlowPriority();
                    if (sourcePolicyCommandsWithoutInPort
                            .containsKey(policyPrior.getPolicyCommnd().getId())) { // 如果是源头且无IN端口
                        logger.info("Start AP={} is an origin SOURCE",
                                path.get(0));
                        removeFlowEntry(policyCommand, null, flowPriority,
                                path.get(0), path.get(1), false);
                    } else {
                        removeFlowEntry(policyCommand, null, flowPriority,
                                path.get(0), path.get(1), true);
                    }
                    if (path.size() > 2) {
                        for (int i = 2; i < path.size() - 1; i += 2) {
                            removeFlowEntry(policyCommand, null, flowPriority,
                                    path.get(i), path.get(i + 1), true);
                        }
                    }

                    logger.info(match + ":__Delete: "
                            + policyPrior.getPolicyCommnd().getDevices() + "--"
                            + removed.getFlowPriority() + "-->"
                            + removed.getPolicyCommnd().getDevices());
                }
            }
            // if(policyPrior.getPolicy().getMatch().compareWith())
            if (!noAction) {
                short flowPriority = PolicyCommandDeployed.computePriority(
                        policyCommand,
                        policyCommandsDeployed.get(
                                policyPrior.getPolicyCommnd().getId())
                                .getRelatedPoliciesMap(),
                        policyCommandsRelatedPrior,
                        policyCommandsRelatedPosterior, Direction.ASCEND);

                List<SecurityDevice> devicesOfPrePolicy = policyPrior
                        .getPolicyCommnd().getDevices();
                DpidPortPair startPoint = getAttachmentPoint(devicesOfPrePolicy
                        .get(devicesOfPrePolicy.size() - 1)
                        .getOutgressAttachmentPointInfo());
                if (policyCommand.getType().equals(PolicyActionType.DROP_FLOW)) {
                    // TODO: push a actions=drop flow entry
                    DpidPortPair npt = new DpidPortPair(
                            startPoint.getDpid(), startPoint.getPort());
                    MatchArguments match = policyCommand.getMatch();
                    match.setInputPort((short) npt.getPort());
                    FlowAction action = new FlowAction();
                    FlowSettings settings = FlowSettings.loadFromPoliceCommand(policyCommand)
                            .setPriority(flowPriority);

                    String flowModId = scAgentDriver.sendFlowMod(npt.getDpid(), match, action, settings);
//                    String flowModId = pushFlowEntry(policyCommand, null,
//                            flowPriority, npt, null, true);
                    if (flowModId != null) {
                        flowModMessageIds.add(flowModId);
                    }
                    return "drop flow success";
                }
                DpidPortPair endPoint = getAttachmentPoint(policyCommand
                        .getDevices().get(0).getIngressAttachmentPointInfo());
                logger.info("Computing a route form {} to {}", startPoint,
                        endPoint);
                List<DpidPortPair> path = computeRoute(startPoint, endPoint);
                if (path == null || path.size() < 1) {
                    logger.error(
                            "routeEngine cannot find a path from {} to {}",
                            devicesOfPrePolicy
                                    .get(devicesOfPrePolicy.size() - 1)
                                    .getOutgressAttachmentPointInfo()
                                    .toString(), policyCommand.getDevices()
                                    .get(0).getIngressAttachmentPointInfo()
                                    .toString());
                    return "cannot find  a path ";
                }

                if (sourcePolicyCommandsWithoutInPort.containsKey(policyPrior
                        .getPolicyCommnd().getId())) { // 如果是源头且无IN端口
                    logger.info("Start AP={} is an origin SOURCE", path.get(0));
//                    String flowModId = pushFlowEntry(policyCommand, null,
//                            flowPriority, path.get(0), path.get(1), false);
                    MatchArguments match = policyCommand.getMatch();
                    match.setInputPort((short) 0);
                    FlowAction action = new FlowAction((short) path.get(1).getPort());
                    FlowSettings settings = FlowSettings.loadFromPoliceCommand(policyCommand).setPriority(flowPriority);
                    String flowModId = scAgentDriver.sendFlowMod(path.get(0).getDpid(), match, action, settings);
                    if (flowModId != null) {
                        flowModMessageIds.add(flowModId);
                    }
                } else {
//                    String flowModId = pushFlowEntry(policyCommand, null,
//                            flowPriority, path.get(0), path.get(1), true);
                    MatchArguments match = policyCommand.getMatch();
                    match.setInputPort((short) path.get(1).getPort());
                    FlowAction action = new FlowAction((short) path.get(1).getPort());
                    FlowSettings settings = FlowSettings.loadFromPoliceCommand(policyCommand).setPriority(flowPriority);
                    String flowModId = scAgentDriver.sendFlowMod(path.get(0).getDpid(), match, action, settings);
                    if (flowModId != null) {
                        flowModMessageIds.add(flowModId);
                    }
                }
                if (path.size() > 2) {
                    for (int i = 2; i < path.size() - 1; i += 2) {
//                        String flowModId = pushFlowEntry(policyCommand, null,
//                                flowPriority, path.get(i), path.get(i + 1),
//                                true);
                        MatchArguments match = policyCommand.getMatch();
                        match.setInputPort((short) path.get(i+1).getPort());
                        FlowAction action = new FlowAction((short) path.get(i+1).getPort());
                        FlowSettings settings = FlowSettings.loadFromPoliceCommand(policyCommand).setPriority(flowPriority);
                        String flowModId = scAgentDriver.sendFlowMod(path.get(i).getDpid(), match, action, settings);

                        if (flowModId != null) {
                            flowModMessageIds.add(flowModId);
                        }
                    }
                }

                logger.info("" + policyCommand.getMatch() + ": "
                        + policyPrior.getPolicyCommnd().getDevices() + " --"
                        + flowPriority + "--> " + policyCommand.getDevices());
                // 将P加入relatedPolicies
                policyCommandsDeployed.get(
                        policyPrior.getPolicyCommnd().getId())
                        .putPolicyToMap(
                                new PolicyCommandRelated(policyCommand,
                                        flowPriority, policyPrior
                                        .getPolicyCommnd()
                                        .getMatch()
                                        .compareWith(
                                                policyCommand
                                                        .getMatch())));
            }
        }

        // 再来处理较低策略
        HashMap<String, PolicyCommandRelated> policiesRelated = new HashMap<String, PolicyCommandRelated>();
        short flowPriority = PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY;
        for (PolicyCommandRelated policyPosterior : policyCommandsRelatedPosterior) {
            MatchArguments match = policyPosterior.getPolicyCommnd().getMatch();
            if (policyPosterior.getPolicyCommnd().getCommandPriority() == ORIGIN_DESTINATION_PRIORITY)
                match = policyCommand.getMatch();
            List<SecurityDevice> devicesOfPrePolicy = policyCommand
                    .getDevices();
            DpidPortPair startPoint = getAttachmentPoint(devicesOfPrePolicy.get(
                    devicesOfPrePolicy.size() - 1)
                    .getOutgressAttachmentPointInfo());
            DpidPortPair endPoint = getAttachmentPoint(policyPosterior
                    .getPolicyCommnd().getDevices().get(0)
                    .getIngressAttachmentPointInfo());

            logger.info("Computing a route form {} to {}", startPoint, endPoint);
            List<DpidPortPair> path = computeRoute(startPoint, endPoint);
            if (path == null || path.size() < 1) {
                logger.error("routeEngine cannot find a path from {} to {}",
                        devicesOfPrePolicy.get(devicesOfPrePolicy.size() - 1)
                                .getOutgressAttachmentPointInfo().toString(),
                        policyCommand.getDevices().get(0)
                                .getIngressAttachmentPointInfo().toString());
                return "cannot find  a path ";
            }
            for (int i = 0; i < path.size() - 1; i += 2) {
//                String flowModId = pushFlowEntry(
//                        policyPosterior.getPolicyCommnd(), match, flowPriority,
//                        path.get(i), path.get(i + 1), true);
                match.setInputPort((short) path.get(1).getPort());
                FlowAction action = new FlowAction((short) path.get(1).getPort());
                FlowSettings settings = FlowSettings.loadFromPoliceCommand(policyCommand).setPriority(flowPriority);
                String flowModId = scAgentDriver.sendFlowMod(path.get(0).getDpid(), match, action, settings);

                if (flowModId != null) {
                    flowModMessageIds.add(flowModId);
                }
            }
            logger.info("" + match + ": " + policyCommand.getDevices() + " --"
                    + flowPriority + "--> "
                    + policyPosterior.getPolicyCommnd().getDevices());
            policiesRelated.put(policyPosterior.getPolicyCommnd().getId(),
                    policyPosterior);
            flowPriority -= PolicyCommandDeployed.STEP;
        }
        policyCommandsDeployed.put(policyCommand.getId(),
                new PolicyCommandDeployed(policyCommand.getId(), policyCommand,
                        policiesRelated, flowModMessageIds));

        return "operation success";
    }


    private void removeFlowEntry(PolicyCommand policyCommand, Object o, short flowPriority, DpidPortPair dpidPortPair, DpidPortPair dpidPortPair1, boolean b) {
        //TODO
    }

    private DpidPortPair getAttachmentPoint(AttachmentPointInfo attachmentPointInfo) {
        //TODO:
        return null;
    }

    public void addImplicitPolicyCommandDeployed(DpidPortPair sp,
                                                 AttachmentPointInfo apInfo, int priority, boolean setInPort) {
        String pcid;
        if (setInPort)
            pcid = HexString.toHexString(sp.getDpid()) + "::"
                    + sp.getPort();
        else
            pcid = HexString.toHexString(sp.getDpid());
        String point;
        if (priority > 0) {
            point = "SOURCE";
        } else {
            point = "DESTINATION";
        }
        pcid += "-" + point;
        String with;
        if (setInPort) {
            with = "WITH";
        } else {
            with = "WITHOUT";
        }
        if (!policyCommandsDeployed.containsKey(pcid)) {
            logger.info("Adding a origin " + point + " policy " + with
                    + " in_Port and priority= {}", priority);
            PolicyCommand policyCommandImplicit = new PolicyCommand();
            policyCommandImplicit.setId(pcid);
            policyCommandImplicit.setCommandPriority(priority);
            policyCommandImplicit.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
            policyCommandImplicit.setHardTimeout(DEFAULT_HARD_TIMEOUT);
            policyCommandImplicit.setMatch(new MatchArguments());
            SecurityDevice device = new SecurityDevice();
            if (priority > 0)
                device.setOutgressAttachmentPointInfo(apInfo);
            else
                device.setIngressAttachmentPointInfo(apInfo);
            ArrayList<SecurityDevice> devices = new ArrayList<SecurityDevice>();
            devices.add(device);
            policyCommandImplicit.setDevices(devices);
            policyCommandsDeployed.put(pcid, new PolicyCommandDeployed(pcid,
                    policyCommandImplicit,
                    new HashMap<String, PolicyCommandRelated>()));
            if (priority > 0 && setInPort == false) {
                logger.info("Adding an origin source policyCommand without inPort: "
                        + pcid);
                sourcePolicyCommandsWithoutInPort.put(pcid,
                        policyCommandImplicit);
            }
        }
    }

}
