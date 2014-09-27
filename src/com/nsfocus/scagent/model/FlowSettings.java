package com.nsfocus.scagent.model;

import com.nsfocus.scagent.manager.SCAgentDriver;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chen on 14-9-16.
 */
public class FlowSettings {

    long cookie;
    FlowMod.FMCommand command;
    List<FlowMod.FMFlag> flags = new ArrayList<FlowMod.FMFlag>();
    short idleTimeout;
    short hardTimeout;
    short priority;

    int bufferId;

    int outPort;

    public FlowSettings() {
        super();
    }

    public static FlowSettings loadFromPoliceCommand(PolicyCommand policyCommand) {
        FlowSettings settings = new FlowSettings();
        ArrayList<FlowMod.FMFlag> fmFlags = new ArrayList<FlowMod.FMFlag>();
        fmFlags.add(FlowMod.FMFlag.SEND_FLOW_REM);
        settings.setIdleTimeout((short) policyCommand.getIdleTimeout())
                .setHardTimeout((short) policyCommand.getHardTimeout())
                .setCommand(FlowMod.FMCommand.ADD)
                .setBufferId(FlowMod.BUFFER_ID_NONE)
                .setFlags(fmFlags)
                .setCookie(SCAgentDriver.cookie)
                .setPriority((short) policyCommand.getCommandPriority());
        return settings;
    }

    public List<FlowMod.FMFlag> getFlags() {
        return flags;
    }

    public FlowSettings setFlags(List<FlowMod.FMFlag> flags) {
        this.flags = flags;
        return this;
    }

    public int getOutPort() {
        return outPort;
    }

    public FlowSettings setOutPort(int outPort) {
        this.outPort = outPort;
        return this;
    }

    public int getBufferId() {
        return bufferId;
    }

    public FlowSettings setBufferId(int bufferId) {
        this.bufferId = bufferId;
        return this;
    }

    public short getPriority() {
        return priority;
    }

    public FlowSettings setPriority(short priority) {
        this.priority = priority;
        return this;
    }

    public short getHardTimeout() {
        return hardTimeout;
    }

    public FlowSettings setHardTimeout(short hardTimeout) {
        this.hardTimeout = hardTimeout;
        return this;
    }

    public short getIdleTimeout() {
        return idleTimeout;
    }

    public FlowSettings setIdleTimeout(short idleTimeout) {
        this.idleTimeout = idleTimeout;
        return this;
    }

    public FlowMod.FMCommand getCommand() {
        return command;
    }

    public FlowSettings setCommand(FlowMod.FMCommand command) {
        this.command = command;
        return this;
    }

    public long getCookie() {
        return cookie;
    }

    public FlowSettings setCookie(long cookie) {
        this.cookie = cookie;
        return this;
    }

    @Override
    public String toString() {
        return "FlowSettings{" +
                "cookie=" + cookie +
                ", command=" + command +
                ", idleTimeout=" + idleTimeout +
                ", hardTimeout=" + hardTimeout +
                ", priority=" + priority +
                ", bufferId=" + bufferId +
                ", outPort=" + outPort +
                ", flags=" + flags +
                '}';
    }
}
