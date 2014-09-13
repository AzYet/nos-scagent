package com.nsfocus.scagent.command;


public class PolicyCommandRelated implements Comparable<PolicyCommandRelated>{
	protected PolicyCommand policyCommand;
    protected short flowPriority;
    protected MatchRelation relation;
    
	public PolicyCommandRelated(PolicyCommand policyCommnd, short flowPriority,
			MatchRelation relation) {
		super();
		this.policyCommand = policyCommnd;
		this.flowPriority = flowPriority;
		this.relation = relation;
	}
	
	public PolicyCommand getPolicyCommnd() {
		return policyCommand;
	}
	public void setPolicyCommnd(PolicyCommand policyCommnd) {
		this.policyCommand = policyCommnd;
	}
	public short getFlowPriority() {
		return flowPriority;
	}
	public void setFlowPriority(short flowPriority) {
		this.flowPriority = flowPriority;
	}
	public MatchRelation getRelation() {
		return relation;
	}
	public void setRelation(MatchRelation relation) {
		this.relation = relation;
	}

	@Override
	public String toString() {
		return "PolicyCommandRelated [policyCommand=" + policyCommand
				+ ", flowPriority=" + flowPriority + ", relation=" + relation
				+ "]";
	}

	@Override
	public int compareTo(PolicyCommandRelated o) {
		return policyCommand.compareTo(o.getPolicyCommnd());
	}

}
