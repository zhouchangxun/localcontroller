package com.netease.cns.southbound.ovsdb.cache;

import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by hzzhangdongya on 16-6-15.
 */
public class BridgeTableUpdateHandler extends TableUpdateHandler {
    private static final Logger LOG = LoggerFactory.getLogger(BridgeTableUpdateHandler.class);

    @Override
    public void process(OVSDBCache cache, TableUpdates updates, DatabaseSchema dbSchema) {
        Map<UUID,Bridge> updatedBridgeRows = TyperUtils.extractRowsUpdated(Bridge.class, updates, dbSchema);
        Map<UUID, Bridge> oldBridgeRows = TyperUtils.extractRowsOld(Bridge.class, updates, dbSchema);
        Map<UUID, Bridge> removedRows = TyperUtils.extractRowsRemoved(Bridge.class, updates, dbSchema);

        // Process Update, use oldBridgeRows If necessary.
        for (Map.Entry<UUID, Bridge> entry : updatedBridgeRows.entrySet()) {
            LOG.info("Processing update for Bridge " + entry.getValue().getName());
            // TODO: update bridge info in the local data store.
        }

        // Process Delete.
        for (Map.Entry<UUID, Bridge> entry : removedRows.entrySet()) {
            LOG.info("Processing remove for Bridge" + entry.getValue().getName());
            // TODO: delete bridge info from the local data store.
        }
    }
}
