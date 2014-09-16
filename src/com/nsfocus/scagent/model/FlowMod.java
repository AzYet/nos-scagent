package com.nsfocus.scagent.model;

/**
 * Created by chen on 14-9-15.
 */
public class FlowMod {
    public static enum FMCommand {ADD,DELETE,MODIFY,MODIFY_STRICT,DELETE_STRICT};
    public static enum FMFlag {SEND_FLOW_REM,CHECK_OVERLAP,EMERG};

    public static final long	NONE_BUFFER_ID = 0x00000000FFFFFFFFL;
    public static int BUFFER_ID_NONE = 0xffffffff;

    MatchArguments match ;
    FlowAction actions;
    FlowSettings settings;

    public FlowMod(MatchArguments match, FlowAction actions, FlowSettings settings) {
        this.match = match;
        this.actions = actions;
        this.settings = settings;
    }

    public MatchArguments getMatch() {
        return match;
    }

    public FlowMod setMatch(MatchArguments match) {
        this.match = match;
        return this;
    }

    public FlowAction getActions() {
        return actions;
    }

    public FlowMod setActions(FlowAction actions) {
        this.actions = actions;
        return this;
    }

    public FlowSettings getSettings() {
        return settings;
    }

    public void setSettings(FlowSettings settings) {
        this.settings = settings;
    }

    @Override
    public String toString() {
        return "FlowMod{" +
                "match=" + match +
                ", actions=" + actions +
                ", settings=" + settings +
                '}';
    }
}
