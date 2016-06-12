package com.netease.cns;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * Created by hzzhangdongya on 16-6-7.
 */
public class OVSDBConnectionManager implements OvsdbConnectionListener {
    private static final Logger LOG = LoggerFactory.getLogger(OVSDBConnectionManager.class);
    private static OvsdbConnectionService ovsdbConnectionService = new OvsdbConnectionService();
    private ExecutorService executor = null;
    private OvsdbClient activeClient = null;
    public DatabaseSchema schema = null;

    public OVSDBConnectionManager(ListeningExecutorService executor) {
        this.executor = executor;
    }

    public void connected(OvsdbClient client) {
        LOG.info("an ovsdb instanced connected...");
        activeClient = client;

        // Fetch schema once connected.
        // Just hardcoded here to eliminate extra async call.
        final ListenableFuture<DatabaseSchema> future = activeClient.getSchema("Open_vSwitch");
        future.addListener(new Runnable() {
            public void run() {
                try {
                    schema = future.get();
                    LOG.info("The ovsdb instance hold schema for Open_vSwitch database: " + future.get());
                } catch (InterruptedException e) {
                    LOG.error("The get database rpc is interrupted...");
                } catch (ExecutionException e) {
                    LOG.error("Exception in task");
                }
            }
        }, executor);
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

    public DatabaseSchema getSchema() {
        return schema;
    }
}
