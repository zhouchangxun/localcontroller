package com.netease.cns.southbound.ovsdb.cache;

import com.netease.cns.southbound.ovsdb.event.BaseEvent;
import com.netease.cns.southbound.ovsdb.event.OVSDBChangeListener;
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
    // and also should use a wrapper class to keep agent not knowing about details of underlying ovsdb library.
    private OVSDBChangeListener listener;

    public OVSDBCache() {
        // TODO: other tables handler.
        UPDATE_HANDLERS.add(new OpenvSwitchTableUpdateHandler(this));
        UPDATE_HANDLERS.add(new BridgeTableUpdateHandler(this));
        UPDATE_HANDLERS.add(new InterfaceTableUpdateHandler(this));
        UPDATE_HANDLERS.add(new PortTableUpdateHandler(this));
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

    public void registerChangeListener(OVSDBChangeListener listener) {
        // TODO: support multiple listener???
        this.listener = listener;
    }

    public void notifyChange(BaseEvent event) {
        if (null != listener) {
            listener.notify(event);
        }
    }
}
