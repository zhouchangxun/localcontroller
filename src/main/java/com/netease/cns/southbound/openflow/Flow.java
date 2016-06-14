package com.netease.cns.southbound.openflow;

import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.action.rev150203.actions.grouping.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.instruction.rev130731.instructions.grouping.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.grouping.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.v10.grouping.MatchV10;

import java.math.BigInteger;
import java.util.List;

/**
 * Created by hzzhangdongya on 16-6-14.
 */
public class Flow {
    private FlowKey flowKey;
    private FlowModCommand command;
    private BigInteger cookie;
    private BigInteger cookieMask;
    private FlowModFlags flags;
    private java.lang.Integer hardTimeout;
    private java.lang.Integer idleTimeout;
    private List<Action> actions;

    public Flow() {
        flowKey = new FlowKey();
        command = FlowModCommand.OFPFCADD;
        cookie = BigInteger.valueOf(0L);
        cookieMask = BigInteger.valueOf(0L);
        flags = Constants.EMPTY_FLOWMOD_FLAGS;
        hardTimeout = java.lang.Integer.valueOf(0);
        idleTimeout = java.lang.Integer.valueOf(0);
    }

    public List<Action> getActions() {
        return actions;
    }

    public void setActions(List<Action> actions) {
        this.actions = actions;
    }

    public FlowModCommand getCommand() {
        return command;
    }

    public void setCommand(FlowModCommand command) {
        this.command = command;
    }

    public BigInteger getCookie() {
        return cookie;
    }

    public void setCookie(BigInteger cookie) {
        this.cookie = cookie;
    }

    public BigInteger getCookieMask() {
        return cookieMask;
    }

    public void setCookieMask(BigInteger cookieMask) {
        this.cookieMask = cookieMask;
    }

    public FlowModFlags getFlags() {
        return flags;
    }

    public void setFlags(FlowModFlags flags) {
        this.flags = flags;
    }

    public Integer getHardTimeout() {
        return hardTimeout;
    }

    public void setHardTimeout(Integer hardTimeout) {
        this.hardTimeout = hardTimeout;
    }

    public Integer getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Integer idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public FlowKey getFlowKey() {
        return flowKey;
    }
}
