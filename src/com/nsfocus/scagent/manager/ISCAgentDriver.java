package com.nsfocus.scagent.manager;

import com.nsfocus.scagent.model.FlowAction;
import com.nsfocus.scagent.model.FlowMod;
import com.nsfocus.scagent.model.FlowSettings;
import com.nsfocus.scagent.model.MatchArguments;
import jp.co.nttdata.ofc.nos.ofp.common.Flow;
import jp.co.nttdata.ofc.nosap.sample.VirtualL2Service.common.DpidPortPair;

import java.util.List;

/**
 * Created by chen on 14-9-15.
 */
public interface ISCAgentDriver {
    // members of an interface are automatically made public
    String sendFlowMod(Long dpid, FlowMod flowMod);
    String sendFlowMod(Long dpid, MatchArguments match, FlowAction actions, FlowSettings settings);
    List<DpidPortPair>  computeRoute(DpidPortPair start , DpidPortPair end);
}
