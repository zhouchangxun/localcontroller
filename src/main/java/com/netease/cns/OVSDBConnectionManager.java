package com.netease.cns;

import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by hzzhangdongya on 16-6-7.
 */
public class OVSDBConnectionManager implements OvsdbConnectionListener {
    private static final Logger LOG = LoggerFactory.getLogger(OVSDBConnectionManager.class);
    private static OvsdbConnectionService ovsdbConnectionService = new OvsdbConnectionService();

    OvsdbClient activeClient = null;

    public void connected(OvsdbClient client) {
        LOG.info("an ovsdb instanced connected...");
        activeClient = client;
/*
        final ListenableFuture databases_future = activeClient.getDatabases();
        databases_future.addListener(new Runnable() {
            public void run() {
                try {
                    LOG.info("The ovsdb instance hold: " + databases_future.get());
                } catch (InterruptedException e) {
                    LOG.error("The get database rpc is interrupted...");
                } catch (ExecutionException e) {
                    LOG.error("Exception in task");
                }
            }
        }, pool);
*/
    }

    public void disconnected(OvsdbClient client) {
        LOG.info("an ovsdb instanced say byebye...");
        activeClient = null;
    }

    public OvsdbConnectionService getOvsdbConnectionServer() {
        return ovsdbConnectionService;
    }

    public OvsdbClient getActiveOvsdbClient() {
        return activeClient;
    }

    public void setActiveOvsdbClient(OvsdbClient client) {
        activeClient = client;
    }
}
