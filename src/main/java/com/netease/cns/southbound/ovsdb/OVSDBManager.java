package com.netease.cns.southbound.ovsdb;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.netease.cns.southbound.ovsdb.cache.OVSDBCache;
import com.netease.cns.southbound.ovsdb.common.Constants;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * Created by hzzhangdongya on 16-6-7.
 * Provider a single entity for interact with local ovsdb-server.
 */
public class OVSDBManager implements OvsdbConnectionListener, OVSDBApi {
    private static final Logger LOG = LoggerFactory.getLogger(OVSDBManager.class);
    private static OvsdbConnectionService ovsdbConnectionService = new OvsdbConnectionService();
    private static ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
    // TODO: we should define a associative MAP, in order to monitor only neccesary column of a specified table.
    private static final ArrayList<String> MONITOR_TABLES = new ArrayList<>();
    private static final HashMap<String, ArrayList<String>> MONITOR_TABLE_ROWS_MAP = new HashMap<>();

    private OVSDBCache cache;
    private OvsdbClient client;
    private DatabaseSchema schema;
    private boolean working = true;

    // TODO:
    // 1. ovsdb connection maintainance
    // 2. api like add port/delete port and etc.
    // 3. registration and notification of ovsdb notify like ofport allocated by vswitchd.

    public OVSDBManager(InetAddress ovsdbServerAddr, int ovsdbServerPort) {
        setupMonitoredTables();
        cache = new OVSDBCache();

        // Do initial connect.
        executor.submit(new Runnable() {
            @Override
            public void run() {
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
            }
        });
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

                    MonitorRequestBuilder<GenericTableSchema> monitorBuilder = MonitorRequestBuilder.builder(tableSchema);
                    for (String columnName: monitoredColumns) {
                        monitorBuilder.addColumn(columnName);
                    }
                    monitorRequests.add(monitorBuilder.with(new MonitorSelect(true, true, true, true)).build());
                }

                // Update cache because monitor is a sync interface and we set select initial as true.
                cache.update(client.monitor(schema, monitorRequests, cache), schema);
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
}