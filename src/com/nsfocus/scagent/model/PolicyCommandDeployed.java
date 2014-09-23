package com.nsfocus.scagent.model;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyCommandDeployed implements Comparable<PolicyCommandDeployed>{
	public static final short DEFAULT_FLOW_PRIORITY = 1000;
	public static final short STEP = 8;

	protected String id;
	protected PolicyCommand policyCommand;
	protected Map<String,PolicyCommandRelated> relatedPoliciesMap = new HashMap<String,PolicyCommandRelated>();
	protected List<PolicyCommandRelated> relatedPoliciesList = new ArrayList<PolicyCommandRelated>();
	protected static Logger logger = LoggerFactory.getLogger(PolicyCommandDeployed.class);
	protected List<String> flowModIdList = new ArrayList<String>();


	public PolicyCommandDeployed(String id, PolicyCommand policyCommand,
			Map<String, PolicyCommandRelated> relatedPoliciesMap) {
		super();
		this.id = id;
		this.policyCommand = policyCommand;
		if(relatedPoliciesMap != null){
			this.relatedPoliciesMap = relatedPoliciesMap;
			for(Entry<String, PolicyCommandRelated> pre : relatedPoliciesMap.entrySet()){
				relatedPoliciesList.add(pre.getValue());
			}
			Collections.sort(relatedPoliciesList);
		}

		this.relatedPoliciesMap = relatedPoliciesMap;
	}
	
	public PolicyCommandDeployed(String id, PolicyCommand policyCommand,
			Map<String, PolicyCommandRelated> relatedPoliciesMap,List<String> flowModMessageIds) {
		super();
		this.id = id;
		this.policyCommand = policyCommand;
		if(relatedPoliciesMap != null){
			this.relatedPoliciesMap = relatedPoliciesMap;
			for(Entry<String, PolicyCommandRelated> pre : relatedPoliciesMap.entrySet()){
				relatedPoliciesList.add(pre.getValue());
			}
			Collections.sort(relatedPoliciesList);
		}
		flowModIdList = flowModMessageIds;
		this.relatedPoliciesMap = relatedPoliciesMap;
	}

	public static void findRelatedPolicies2(PolicyCommand pc , Map<String , PolicyCommandDeployed> policyCommandsDeployed,
			List<PolicyCommandRelated> pcrsPrior,List<PolicyCommandRelated>pcrsPosterior){
		//改进点：并不需要遍历所有的策略，无论较高或较低组，一旦遇到Match为pc父集的策略，即可终止，前提是要先排序
		for(Entry<String, PolicyCommandDeployed> pdEntry : policyCommandsDeployed.entrySet()){
			PolicyCommandDeployed pd = pdEntry.getValue();
			switch(pc.getMatch().compareWith(pd.getPolicyCommand().getMatch())){
			case SUBSET:
				if(pc.getCommandPriority() > pd.getPolicyCommand().getCommandPriority()){
					pcrsPosterior.add(	new PolicyCommandRelated(
							pd.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.SUBSET));
				}else if(pc.getCommandPriority() < pd.getPolicyCommand().getCommandPriority()){
					pcrsPrior.add(	new PolicyCommandRelated(
							pd.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.SUBSET));
				}else{//如果相关策略优先级相同，则报错
					logger.error("error,policies {} and {} conflicts.",pd.getPolicyCommand(),pc);
					return;
				}
				break;
			case SUPERSET:
				if(pc.getCommandPriority() > pd.getPolicyCommand().getCommandPriority()){
					pcrsPosterior.add(	new PolicyCommandRelated(
							pd.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.SUPERSET));
				}else if(pc.getCommandPriority() < pd.getPolicyCommand().getCommandPriority()){
					pcrsPrior.add(	new PolicyCommandRelated(
							pd.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.SUPERSET));
				}else{
					logger.error("error,policies {} and {} conflicts.",pd.getPolicyCommand(),pc);
					return;
				}
				break;
			case EQUAL:
				if(pc.getCommandPriority() > pd.getPolicyCommand().getCommandPriority()){
					pcrsPosterior.add(	new PolicyCommandRelated(
							pd.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.EQUAL));
				}else if(pc.getCommandPriority() < pd.getPolicyCommand().getCommandPriority()){
					pcrsPrior.add(	new PolicyCommandRelated(
							pd.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.EQUAL));
				}else{
					logger.error("error,policies {} and {} conflicts.",pd.getPolicyCommand(),pc);
					return;
				}
				break;
			case INTERSECT:
				if(pc.getCommandPriority() > pd.getPolicyCommand().getCommandPriority()){
					pcrsPosterior.add(	new PolicyCommandRelated(
							pd.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.INTERSECT));
				}else if(pc.getCommandPriority() < pd.getPolicyCommand().getCommandPriority()){
					pcrsPrior.add(	new PolicyCommandRelated(
							pd.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.INTERSECT));
				}else{
					logger.error("error,policies {} and {} conflicts.",pd.getPolicyCommand(),pc);
					return;
				}
				break;
			default:
				break;
			}
		}
		//对相关策略进行排序和修剪
		if(pcrsPrior.size() > 1){
			//较高策略由低到高
			Collections.sort(pcrsPrior);
			//由低到高遍历较高策略，如果遇到父集或相等，则去掉之后的策略
			int count = 0;
			for(PolicyCommandRelated pr : pcrsPrior){
				if(pr.getRelation() == MatchRelation.SUBSET || pr.getRelation() == MatchRelation.EQUAL){
					for(int i = count +1; i<pcrsPosterior.size();i++){
						pcrsPosterior.remove(i);
					}
					break;
				}
				count ++;
			}
		}
		if(pcrsPosterior.size() > 1){
			Collections.sort(pcrsPosterior);
			//较低策略由高到低
			Collections.reverse(pcrsPosterior);
			//由高到低遍历较低策略，如果遇到父集或相等，则去掉之后的策略
			int count = 0;
			Set<Integer> indexToRem = new TreeSet<Integer>();
			for(PolicyCommandRelated pr : pcrsPosterior){
				if(pr.getRelation() == MatchRelation.SUBSET || pr.getRelation() == MatchRelation.EQUAL){
					for(int i = count +1; i<pcrsPosterior.size();i++){
						indexToRem.add(i);
					}
					break;
					//如果当前策略覆盖了其它较低策略，也应删除
				}else if(count < pcrsPosterior.size()-1){
					for (int i = count+1 ; i< pcrsPosterior.size();i++){
						MatchRelation relation  = pr.getPolicyCommnd().getMatch().compareWith(pcrsPosterior.get(i).getPolicyCommnd().getMatch());
						if(relation == MatchRelation.SUPERSET || relation == MatchRelation.EQUAL){
							indexToRem.add(i);
						}

					}
				}
				count ++;
			}
			for(int i : indexToRem){
				pcrsPosterior.remove(i);
			}
		}
	}

	public static void findRelatedPolicies(PolicyCommand pc , Map<String , PolicyCommandDeployed> policyCommandsDeployed,
			List<PolicyCommandRelated> pcrsPrior,List<PolicyCommandRelated>pcrsPosterior){
		//改进点：并不需要遍历所有的策略，无论较高或较低组，一旦遇到Match为pc父集的策略，即可终止，前提是要先排序
		List<PolicyCommandDeployed> pdPrior = new ArrayList<PolicyCommandDeployed>();
		List<PolicyCommandDeployed> pdPosterior = new ArrayList<PolicyCommandDeployed>();
		for(PolicyCommandDeployed pDe:policyCommandsDeployed.values()){
		    //only consider REDIRECT_FLOW Command
		    if(pDe.getPolicyCommand().getType()==PolicyActionType.REDIRECT_FLOW){
		        if(pDe.getPolicyCommand().getCommandPriority() > pc.getCommandPriority()){
		            pdPrior.add(pDe);
		        }else if(pDe.getPolicyCommand().getCommandPriority() < pc.getCommandPriority()){
		            pdPosterior.add(pDe);
		        }else if(pDe.getPolicyCommand().getMatch().compareWith(pc.getMatch()) != MatchRelation.UNRELATED && !pc.getId().equals(pDe.getPolicyCommand().getId())){
		            logger.error("error,policies {} and {} conflicts.",pDe.getPolicyCommand(),pc);
		        }
		    }
		}
		Collections.sort(pdPrior);
		Collections.sort(pdPosterior);
		Collections.reverse(pdPosterior);
		for(PolicyCommandDeployed pDePrior:pdPrior){
			boolean breakNow =false;
			switch(pc.getMatch().compareWith(pDePrior.getPolicyCommand().getMatch())){
			case SUBSET:
				pcrsPrior.add(	new PolicyCommandRelated(
						pDePrior.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.SUBSET));
				breakNow = true;
				break;
			case SUPERSET:
				pcrsPrior.add(	new PolicyCommandRelated(
						pDePrior.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.SUPERSET));
				break;
			case EQUAL:
				pcrsPrior.add(	new PolicyCommandRelated(
						pDePrior.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.EQUAL));
				breakNow = true;
				break;
			case INTERSECT:
				pcrsPrior.add(	new PolicyCommandRelated(
						pDePrior.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.INTERSECT));
				break;
			default:
				break;
			}
			if(breakNow)break;
		}
		for(PolicyCommandDeployed pDePosterior:pdPosterior){
			boolean breakNow =false;
			switch(pc.getMatch().compareWith(pDePosterior.getPolicyCommand().getMatch())){
			case SUBSET:
				pcrsPosterior.add(	new PolicyCommandRelated(
						pDePosterior.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.SUBSET));
				breakNow = true;
				break;
			case SUPERSET:
				pcrsPosterior.add(	new PolicyCommandRelated(
						pDePosterior.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.SUPERSET));
				break;
			case EQUAL:
				pcrsPosterior.add(	new PolicyCommandRelated(
						pDePosterior.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.EQUAL));
				breakNow = true;
				break;
			case INTERSECT:
				pcrsPosterior.add(	new PolicyCommandRelated(
						pDePosterior.getPolicyCommand(),PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.INTERSECT));
				break;
			default:
				break;
			}
			if(breakNow)break;
		}
		//如果较低策略组中，较高的策略屏蔽了较低策略，需要删除
		if(pcrsPosterior.size() > 1){
			int count = 0;
			Set<Integer> indexToRem = new HashSet<Integer>();
			for(PolicyCommandRelated pr : pcrsPosterior){
				if(count < pcrsPosterior.size()-1){
					for (int i = count+1 ; i< pcrsPosterior.size();i++){
						MatchRelation relation  = pr.getPolicyCommnd().getMatch().compareWith(pcrsPosterior.get(i).getPolicyCommnd().getMatch());
						if(relation == MatchRelation.SUPERSET || relation == MatchRelation.EQUAL){
							indexToRem.add(i);
						}
					}
				}
				count ++;
			}
			for(int i : indexToRem){
				pcrsPosterior.remove(i);
			}
		}
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public PolicyCommand getPolicyCommand() {
		return policyCommand;
	}
	public void setPolicy(PolicyCommand policy) {
		this.policyCommand = policy;
	}
	public Map<String, PolicyCommandRelated> getRelatedPoliciesMap() {
		return relatedPoliciesMap;
	}
	public void setRelatedPoliciesMap(
			Map<String, PolicyCommandRelated> relatedPoliciesMap) {
		this.relatedPoliciesMap = relatedPoliciesMap;
	}
	public List<PolicyCommandRelated> getRelatedPoliciesList() {
		return relatedPoliciesList;
	}
	public void setRelatedPoliciesList(
			List<PolicyCommandRelated> relatedPoliciesList) {
		this.relatedPoliciesList = relatedPoliciesList;
	}

	public void putPolicyToMap(PolicyCommandRelated policyCommandRelated) {
		relatedPoliciesMap.put(policyCommandRelated.getPolicyCommnd().getId(), policyCommandRelated);
		relatedPoliciesList.add(policyCommandRelated);
		Collections.sort(relatedPoliciesList);		
	}

	/**
	 * remove policies been covered by <i>policy</i>
	 * @return list of policies deleted: List PolicyRelated
	 */
	public static List<PolicyCommandRelated> removeCoveredPolicies(PolicyCommand policy ,PolicyCommandDeployed policyCommandDeployed,Direction direction){
		//		public static List<PolicyRelated> removeCoveredPolicies(Policy policy ,PolicyRelated hostPolicy, Map<String,PolicyRelated>relatedMap,Direction direction){
		ArrayList<PolicyCommandRelated> res = new ArrayList <PolicyCommandRelated> ();
		HashSet<String> idsToRemove = new HashSet<String> ();
		for(PolicyCommandRelated outRelatedPolicy : policyCommandDeployed.getRelatedPoliciesList()){
			if(outRelatedPolicy.getPolicyCommnd().getCommandPriority() > policy.getCommandPriority()){ 
				if(direction == Direction.ASCEND){
					MatchRelation relation;
					if((relation = outRelatedPolicy.getPolicyCommnd().getMatch().compareWith(policyCommandDeployed.getPolicyCommand().getMatch())) == MatchRelation.EQUAL
							||relation == MatchRelation.SUPERSET
							||(relation = outRelatedPolicy.getPolicyCommnd().getMatch().compareWith(policy.getMatch())) == MatchRelation.EQUAL
							|| relation == MatchRelation.SUPERSET){
						res.add(new PolicyCommandRelated(policy,PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY,MatchRelation.UNKNOWN));
						return res;
					}else{
						continue;
					}
				}
				else{
					MatchRelation relation;
					if((relation = outRelatedPolicy.getPolicyCommnd().getMatch().compareWith(policy.getMatch())) == MatchRelation.SUBSET ||
							relation == MatchRelation.EQUAL  || (relation = policyCommandDeployed.getPolicyCommand().getMatch().compareWith(policy.getMatch())) == MatchRelation.SUBSET || relation == MatchRelation.EQUAL){
						//如果p覆盖了Xmap中低优先级的流，应将被覆盖流删除 
						idsToRemove.add(outRelatedPolicy.getPolicyCommnd().getId());
						//相应的流也应该删除
						//						System.out.println("Delete: "+removedPolicy.getPolicy().getMatch()+removedPolicy.getPolicy().getDevice()+"-"+removedPolicy.getFlowPriority()+"->"+removedPolicy.getPolicy().getDevice());
					}	
				}
			}
			else if(outRelatedPolicy.getPolicyCommnd().getCommandPriority() == policy.getCommandPriority()){
				if(outRelatedPolicy.getPolicyCommnd().getMatch().compareWith(policy.getMatch()) == MatchRelation.UNRELATED){
					continue;
				}else{
					System.err.println("Policy "+outRelatedPolicy+" & "+policy+ "conflict");
				}
			}else{
				if(direction == Direction.ASCEND){
					MatchRelation relation;
					if((relation = outRelatedPolicy.getPolicyCommnd().getMatch().compareWith(policy.getMatch())) == MatchRelation.SUBSET ||
							relation == MatchRelation.EQUAL ||
							(relation = policyCommandDeployed.getPolicyCommand().getMatch().compareWith(policy.getMatch())) == MatchRelation.SUBSET
							|| relation == MatchRelation.EQUAL){
						//如果p覆盖了Xmap中低优先级的流，应将被覆盖流删除 
						idsToRemove.add(outRelatedPolicy.getPolicyCommnd().getId());
						//相应的流也应该删除
						//					System.out.println("Delete: "+removedPolicy.getPolicy().getMatch()+removedPolicy.getPolicy().getDevice()+"-"+removedPolicy.getFlowPriority()+"->"+removedPolicy.getPolicy().getDevice());
					}
				}else{
					continue;
				}
			}
		}
		for(String id : idsToRemove){
			res.add(policyCommandDeployed.removePolicyFromMap(id));
		}
		return res;
	}

	public PolicyCommandRelated removePolicyFromMap(String prid){
		PolicyCommandRelated removed = relatedPoliciesMap.remove(prid);
		relatedPoliciesList.remove(removed);
		return removed;
	}

	public static short computePriority(PolicyCommand policyCommand , Map<String,PolicyCommandRelated> flowRelatedPolicyCommands ,
			List<PolicyCommandRelated> pcrsPrior ,List<PolicyCommandRelated> pcrsPosterior , Direction direction){
		short flowPriority = -1;
		short referencePriorityTop = -1;
		short referencePriorityBottom = -1;
		if(direction == Direction.ASCEND){ //比较较高相关策略，计算优先级
			for(PolicyCommandRelated policyPrior : pcrsPrior){ //找到较高策略中，在flowRelated中的策略流侁先级
				if(flowRelatedPolicyCommands.containsKey(policyPrior.getPolicyCommnd().getId())){
					if(referencePriorityTop == -1 || referencePriorityTop > flowRelatedPolicyCommands.get(policyPrior.getPolicyCommnd().getId()).getFlowPriority())
						referencePriorityTop = flowRelatedPolicyCommands.get(policyPrior.getPolicyCommnd().getId()).getFlowPriority();
				}

			}
			PolicyCommandRelated pr = null;
			for(PolicyCommandRelated policyPosterior : pcrsPosterior){
				if((pr = flowRelatedPolicyCommands.get(policyPosterior.getPolicyCommnd().getId())) != null){
					if(referencePriorityBottom == -1 || referencePriorityBottom < pr.getFlowPriority())
						referencePriorityBottom = pr.getFlowPriority();
				}
			}
			if(referencePriorityTop == -1){
				if(referencePriorityBottom == -1)
					flowPriority = PolicyCommandDeployed.DEFAULT_FLOW_PRIORITY;
				else
					flowPriority = (short) (referencePriorityBottom + PolicyCommandDeployed.STEP);
			}else{
				if(referencePriorityBottom == -1){
					flowPriority = (short) (referencePriorityTop - PolicyCommandDeployed.STEP);
				}else{
					if(referencePriorityTop - referencePriorityBottom >1){
						flowPriority = (short) ((referencePriorityBottom + referencePriorityTop)/2);
					}else{
						logger.error("cannot allocate new priority between {} and {}", referencePriorityTop,referencePriorityBottom);
					}
				}
			}
		}
		return flowPriority;
	}

	@Override
	public int compareTo(PolicyCommandDeployed o) {
		// TODO Auto-generated method stub
		return policyCommand.compareTo(o.policyCommand);
	}

	public List<String> getFlowModIdList() {
		return flowModIdList;
	}

	public void setFlowModIdList(List<String> flowModIdList) {
		this.flowModIdList = flowModIdList;
	}

}
