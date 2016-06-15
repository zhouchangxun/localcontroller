package com.netease.cns.southbound.ovsdb.cache;

import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by hzzhangdongya on 16-6-15.
 * Maintain a OVSDB cache which mirror content in ovsdb server.
 */
public class OVSDBCache implements MonitorCallBack {
    private static final Logger LOG = LoggerFactory.getLogger(OVSDBCache.class);
    private static final ArrayList<TableUpdateHandler> UPDATE_HANDLERS = new ArrayList<TableUpdateHandler>();
    // TODO: decide what in-memory structure to store the cached data.

    public OVSDBCache() {
        // TODO: other tables handler.
        //UPDATE_HANDLERS.add(new OpenvSwitchTableUpdateHandler());
        UPDATE_HANDLERS.add(new BridgeTableUpdateHandler());
    }

    @Override
    public void update(TableUpdates result, DatabaseSchema dbSchema) {
        LOG.info("OVSDBCache receive update, result: " + result);
        for (TableUpdateHandler handler: UPDATE_HANDLERS) {
            handler.process(this, result, dbSchema);
        }
    }

    @Override
    public void exception(Throwable throwable) {

    }

    public void invalidate() {
        // TODO: purge cache if loss connection of ovsdb-server.
    }
}
