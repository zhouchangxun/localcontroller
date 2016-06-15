package com.netease.cns.southbound.ovsdb.cache;

import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;

/**
 * Created by hzzhangdongya on 16-6-15.
 */
public class OpenvSwitchTableUpdateHandler extends TableUpdateHandler {
    public OpenvSwitchTableUpdateHandler() {
        super();
    }

    @Override
    public void process(OVSDBCache cache, TableUpdates updates, DatabaseSchema dbSchema) {

    }
}
