package com.netease.cns.agent.ovsdb.event;

import org.opendaylight.ovsdb.lib.notation.UUID;

/**
 * Created by hzzhangdongya on 16-6-16.
 * Port removed. (possible by other application like libvirt)
 */
public class PortRemovedEvent extends BaseEvent {
    private final UUID uuid;
    private final String portName;

    // TODO: may be we later remove uuid, and only expose name to
    // agent, in that case, we should use cache to find uuid of a port.
    public PortRemovedEvent(UUID uuid, String portName) {
        this.uuid = uuid;
        this.portName = portName;
    }

    public String getPortName() {
        return portName;
    }

    public UUID getUuid() {
        return uuid;
    }
}
