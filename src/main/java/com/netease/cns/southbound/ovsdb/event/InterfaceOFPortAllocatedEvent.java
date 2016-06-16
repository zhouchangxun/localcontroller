package com.netease.cns.southbound.ovsdb.event;

import org.opendaylight.ovsdb.lib.notation.UUID;

/**
 * Created by hzzhangdongya on 16-6-16.
 */
public class InterfaceOFPortAllocatedEvent extends BaseEvent {
    private final UUID uuid;
    private final String portName;
    private final long ofPort;

    public InterfaceOFPortAllocatedEvent(UUID uuid, String portName, long ofPort) {
        this.uuid = uuid;
        this.portName = portName;
        this.ofPort = ofPort;
    }

    public String getPortName() {
        return portName;
    }

    public long getOfPort() {
        return ofPort;
    }

    public UUID getUuid() {
        return uuid;
    }
}
