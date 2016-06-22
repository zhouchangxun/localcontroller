package com.netease.cns.agent.ovsdb.cache;

import com.netease.cns.agent.ovsdb.event.PortAddedEvent;
import com.netease.cns.agent.ovsdb.event.PortRemovedEvent;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by hzzhangdongya on 16-6-16.
 */
public class PortTableUpdateHandler extends TableUpdateHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PortTableUpdateHandler.class);

    public PortTableUpdateHandler(OVSDBCache cache) {
        super(cache);
    }

    @Override
    public void process(OVSDBCache cache, TableUpdates updates, DatabaseSchema dbSchema) {
        Map<UUID, Port> updatedPortRows = TyperUtils.extractRowsUpdated(Port.class, updates, dbSchema);
        Map<UUID, Port> oldPortRows = TyperUtils.extractRowsOld(Port.class, updates, dbSchema);
        Map<UUID, Port> removedRows = TyperUtils.extractRowsRemoved(Port.class, updates, dbSchema);

        // Process Update, use oldPortRows If necessary.
        for (Map.Entry<UUID, Port> entry : updatedPortRows.entrySet()) {
            LOG.info("Processing update for Port " + entry.getValue().getName());
            // TODO: update Port info in the local data store.
            Port oldPort = oldPortRows.get(entry.getKey());
            if (null == oldPort) {
                cache.notifyChange(new PortAddedEvent(entry.getKey(), entry.getValue().getName()));
            }
        }

        // Process Delete.
        for (Map.Entry<UUID, Port> entry : removedRows.entrySet()) {
            LOG.info("Processing remove for Port" + entry.getValue().getName());
            // TODO: delete Port info from the local data store.
            cache.notifyChange(new PortRemovedEvent(entry.getKey(), entry.getValue().getName()));
        }
    }
}
