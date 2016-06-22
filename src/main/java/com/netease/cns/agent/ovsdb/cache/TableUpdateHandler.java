package com.netease.cns.agent.ovsdb.cache;

import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;

/**
 * Created by hzzhangdongya on 16-6-15.
 */
public abstract class TableUpdateHandler {
    public OVSDBCache cache;

    public TableUpdateHandler(OVSDBCache cache) {
        this.cache = cache;
    }

    public abstract void process(OVSDBCache cache, TableUpdates updates, DatabaseSchema dbSchema);
}
