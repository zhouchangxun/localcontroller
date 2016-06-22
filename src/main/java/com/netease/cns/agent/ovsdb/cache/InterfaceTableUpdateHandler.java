package com.netease.cns.agent.ovsdb.cache;

import com.netease.cns.agent.ovsdb.event.InterfaceOFPortAllocatedEvent;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by hzzhangdongya on 16-6-16.
 */
public class InterfaceTableUpdateHandler extends TableUpdateHandler {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceTableUpdateHandler.class);

    public InterfaceTableUpdateHandler(OVSDBCache cache) {
        super(cache);
    }

    @Override
    public void process(OVSDBCache cache, TableUpdates updates, DatabaseSchema dbSchema) {
        Map<UUID, Interface> updatedInterfaceRows = TyperUtils.extractRowsUpdated(Interface.class, updates, dbSchema);
        Map<UUID, Interface> oldInterfaceRows = TyperUtils.extractRowsOld(Interface.class, updates, dbSchema);
        Map<UUID, Interface> removedRows = TyperUtils.extractRowsRemoved(Interface.class, updates, dbSchema);

        // Process Update, use oldInterfaceRows If necessary.
        for (Map.Entry<UUID, Interface> entry : updatedInterfaceRows.entrySet()) {
            LOG.info("Processing update for interface " + entry.getValue().getName());
            // TODO: update bridge info in the local data store.
            // Note: should not get uuid from the data because the update may not have that column included.
            processUpdate(entry.getKey(), oldInterfaceRows.get(entry.getKey()), entry.getValue());

        }

        // Process Delete.
        for (Map.Entry<UUID, Interface> entry : removedRows.entrySet()) {
            LOG.info("Processing remove for interface " + entry.getValue().getName());
            // TODO: delete bridge info from the local data store.
        }
    }

    private long getOfPort(Interface _interface) {
        long ofPort = -1;

        if (null == _interface) {
            return ofPort;
        }

        Set<Long> ofPorts = _interface.getOpenFlowPortColumn().getData();
        if (ofPorts != null && !ofPorts.isEmpty()) {
            Iterator<Long> ofPortsIter = ofPorts.iterator();
            ofPort = ofPortsIter.next();
        }

        return ofPort;
    }

    private void processUpdate(UUID uuid, Interface oldInterface, Interface updatedInterface) {
        long oldOfPort = -1;
        long newOfPort = -1;

        oldOfPort = getOfPort(oldInterface);
        newOfPort = getOfPort(updatedInterface);

        if ((oldOfPort != newOfPort) && (-1 != newOfPort)) {
            cache.notifyChange(new InterfaceOFPortAllocatedEvent(uuid, updatedInterface.getName(), newOfPort));
        }
    }
}
