package com.netease.cns.southbound.ovsdb;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by hzzhangdongya on 16-6-7.
 * Provider a single entity for interact with local ovsdb-server.
 */
public class OVSDBManager implements OvsdbConnectionListener {
    private static final Logger LOG = LoggerFactory.getLogger(OVSDBManager.class);
    private static OvsdbConnectionService ovsdbConnectionService = new OvsdbConnectionService();
    private static ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
    private OvsdbClient client;
    public DatabaseSchema schema;

    // TODO:
    // 1. ovsdb connection maintainance
    // 2. api like add port/delete port and etc.
    // 3. registration and notification of ovsdb notify like ofport allocated by vswitchd.

    public OVSDBManager(InetAddress ovsdbServerAddr, int ovsdbServerPort) {
        // Actively connect(synchronous call) will not be notified due to implementation of ovsdb library.
        // Refer to: https://wiki.opendaylight.org/view/OVSDB:OVSDB_Library_Developer_Guide
        //ovsdbConnectionManager.getOvsdbConnectionServer().registerConnectionListener(ovsdbConnectionManager);
        OvsdbClient client = ovsdbConnectionService.connect(ovsdbServerAddr, ovsdbServerPort);
        if (client != null) {
            LOG.info("Connection to ovsdb server actively successfully...");
            connected(client); // Notify manually.
        } else {
            LOG.error("Connection to ovsdb server actively failed...");
        }
    }

    @Override
    public void connected(OvsdbClient client) {
        LOG.info("an ovsdb instanced connected...");
        client = client;

        // Fetch schema once connected.
        // Just hardcoded here to eliminate extra async call.
        final ListenableFuture<DatabaseSchema> future = client.getSchema("Open_vSwitch");
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

    @Override
    public void disconnected(OvsdbClient client) {
        LOG.info("an ovsdb instanced say byebye...");
        client = null;
    }

    public boolean isOVSDBConnectionWorking() {
        if (null != client) {
            return true;
        } else {
            return false;
        }
    }
}
