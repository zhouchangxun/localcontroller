package com.netease.cns.agent.ovsdb;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.netease.cns.agent.ovsdb.cache.OVSDBCache;
import com.netease.cns.agent.ovsdb.common.Constants;
import com.netease.cns.agent.ovsdb.event.OVSDBChangeListener;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.opendaylight.ovsdb.lib.message.MonitorRequest;
import org.opendaylight.ovsdb.lib.message.MonitorRequestBuilder;
import org.opendaylight.ovsdb.lib.message.MonitorSelect;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * Created by hzzhangdongya on 16-6-7.
 * Provider a single entity for interact with local ovsdb-server.
 */
public class OVSDBManager implements OvsdbConnectionListener {
    private static final Logger LOG = LoggerFactory.getLogger(OVSDBManager.class);
    // TODO: we should define a associative MAP, in order to monitor only neccesary column of a specified table.
    private static final ArrayList<String> MONITOR_TABLES = new ArrayList<>();
    private static final HashMap<String, ArrayList<String>> MONITOR_TABLE_ROWS_MAP = new HashMap<>();
    private static OvsdbConnectionService ovsdbConnectionService = new OvsdbConnectionService();
    private static ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
    private final Map<String, OVSDBBridge> bridgeMap = new HashMap<>();

    private OVSDBCache cache;
    private OvsdbClient client;
    private DatabaseSchema schema;
    private boolean working = false;

    // TODO:
    // 0. Better to implement this class as a FSM since it's have some phase which is not fully fuctionaly.
    // 1. ovsdb connection maintainance
    // 2. api like add port/delete port and etc.
    // 3. registration and notification of ovsdb notify like ofport allocated by vswitchd.


    public OVSDBManager(InetAddress ovsdbServerAddr, int ovsdbServerPort) {
        setupMonitoredTables();
        cache = new OVSDBCache();

        LOG.info("Connection to ovsdb server: "+ovsdbServerAddr+"." + ovsdbServerPort);
        OvsdbClient client = ovsdbConnectionService.connect(ovsdbServerAddr, ovsdbServerPort);
        if (client != null) {
            LOG.info("Connection to ovsdb server actively successfully...");
            // Actively connect(synchronous call) will not be notified due to implementation of ovsdb library.
            // Refer to: https://wiki.opendaylight.org/view/OVSDB:OVSDB_Library_Developer_Guide
            //ovsdbConnectionManager.getOvsdbConnectionServer().registerConnectionListener(ovsdbConnectionManager);
            connected(client);
        } else {
            LOG.error("Connection to ovsdb server actively failed...");
            // TODO: restart connect strategy?
        }
        // Do initial connect.
    }

    private void setupMonitoredTables() {
        MONITOR_TABLES.add(Constants.TBL_OPEN_VSWITCH);
        MONITOR_TABLES.add(Constants.TBL_BRIDGE);
        MONITOR_TABLES.add(Constants.TBL_PORT);
        MONITOR_TABLES.add(Constants.TBL_INTERFACE);

        ArrayList<String> tblOpenvSwitchMonitoredColumns = new ArrayList<>();
        tblOpenvSwitchMonitoredColumns.add(Constants.TBL_OPEN_VSWITCH_COL_NEXTCFG);
        MONITOR_TABLE_ROWS_MAP.put(Constants.TBL_OPEN_VSWITCH, tblOpenvSwitchMonitoredColumns);

        ArrayList<String> tblBridgeMonitoredColumns = new ArrayList<>();
        tblBridgeMonitoredColumns.add(Constants.TBL_BRIDGE_COL_NAME);
        MONITOR_TABLE_ROWS_MAP.put(Constants.TBL_BRIDGE, tblBridgeMonitoredColumns);

        ArrayList<String> tblInterfaceMonitoredColumns = new ArrayList<>();
        tblInterfaceMonitoredColumns.add(Constants.TBL_INTERFACE_COL_OFPORT);
        tblInterfaceMonitoredColumns.add(Constants.TBL_INTERFACE_COL_NAME);
        MONITOR_TABLE_ROWS_MAP.put(Constants.TBL_INTERFACE, tblInterfaceMonitoredColumns);

        ArrayList<String> tblPortMonitoredColumns = new ArrayList<>();
        tblPortMonitoredColumns.add(Constants.TBL_PORT_COL_UUID);
        tblPortMonitoredColumns.add(Constants.TBL_PORT_COL_NAME);
        MONITOR_TABLE_ROWS_MAP.put(Constants.TBL_PORT, tblPortMonitoredColumns);
    }

    @Override
    public void connected(OvsdbClient client) {
        LOG.info("an ovsdb instanced connected...");
        this.client = client;

        // Try fetch schema once connected.
        executor.submit(new Runnable() {
            @Override
            public void run() {

                final ListenableFuture<DatabaseSchema> future = client.getSchema(Constants.DB_OPEN_VSWITCH);
                future.addListener(new Runnable() {
                    public void run() {
                        try {
                            onSchemaFetched(future.get());
                            LOG.info("The ovsdb instance hold schema for Open_vSwitch database: " + future.get());
                        } catch (InterruptedException e) {
                            LOG.error("The get schema rpc is interrupted...");
                        } catch (ExecutionException e) {
                            LOG.error("Exception in get schema task");
                        }
                    }
                }, executor);
            }
        });
    }

    private void monitorTables() {
        // Try fetch schema once connected.
        executor.submit(new Runnable() {
            @Override
            public void run() {
                List<MonitorRequest> monitorRequests = Lists.newArrayList();
                for (String tableName : MONITOR_TABLES) {
                    LOG.info("OVSDBManager monitoring table {} in {}", tableName, schema.getName());
                    GenericTableSchema tableSchema = schema.table(tableName, GenericTableSchema.class);
                    ArrayList<String> monitoredColumns = MONITOR_TABLE_ROWS_MAP.get(tableName);
                    if ((null == monitoredColumns) || (0 == monitoredColumns.size())) {
                        continue;
                    }

                    MonitorRequestBuilder<GenericTableSchema> monitorBuilder = new MonitorRequestBuilder(tableSchema);
                    for (String columnName: monitoredColumns) {
                        monitorBuilder.addColumn(columnName);
                    }
                    monitorRequests.add(monitorBuilder.with(new MonitorSelect(true, true, true, true)).build());
                }

                // Update cache because monitor is a sync interface and we set select initial as true.
                cache.update(client.monitor(schema, monitorRequests, cache), schema);
                // After initial data fetched, mark as working.
                working = true;
            }
        });
    }

    private void onSchemaFetched(DatabaseSchema schema) {
        this.schema = schema;
        monitorTables();
    }

    @Override
    public void disconnected(OvsdbClient client) {
        LOG.info("an ovsdb instanced say byebye...");
        this.client = null;
        schema = null;
        cache.invalidate();
        working = false;
    }

    public boolean isOVSDBConnectionWorking() {
        return working;
    }

    public OVSDBBridge getOVSDBBridge(String bridgeName) {
        OVSDBBridge ovsdbBridge = bridgeMap.get(bridgeName);
        if (ovsdbBridge == null) {
            ovsdbBridge = new OVSDBBridge(this, bridgeName);
            bridgeMap.put(bridgeName, ovsdbBridge);
        }

        return ovsdbBridge;
    }

    public ListeningExecutorService getExecutor() {
        return executor;
    }

    public OvsdbClient getClient() {
        return client;
    }

    public DatabaseSchema getDBSchema() {
        return schema;
    }

    public void registerOVSDBChangeListener(OVSDBChangeListener listener) {
        this.cache.registerChangeListener(listener);
    }

    public OVSDBCache getCache() {
        return cache;
    }
}
