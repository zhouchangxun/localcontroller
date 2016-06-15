/**
 * Created by hzzhangdongya on 16-6-6.
 */

package com.netease.cns.southbound;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.netease.cns.southbound.openflow.*;
import com.netease.cns.southbound.openflow.flow.Flow;
import com.netease.cns.southbound.openflow.flow.FlowKey;
import com.netease.cns.southbound.openflow.flow.FlowMatchHelper;
import com.netease.cns.southbound.ovsdb.OVSDBManager;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionConfiguration;
import org.opendaylight.openflowjava.protocol.api.connection.ThreadConfiguration;
import org.opendaylight.openflowjava.protocol.api.connection.TlsConfiguration;
import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.message.MonitorRequest;
import org.opendaylight.ovsdb.lib.message.MonitorRequestBuilder;
import org.opendaylight.ovsdb.lib.message.MonitorSelect;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.config.rev140630.TransportProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

public class PerfTestLocalController {
    private static final Logger LOG = LoggerFactory.getLogger(PerfTestLocalController.class);
    private static ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

    public static void main(String[] args) throws Exception {
        final int ovsdbPort = 6634;
        final InetAddress ovsdbAddr = InetAddress.getByName("10.166.224.11");
        OFBridgeManager ofBridgeManager = new OFBridgeManager();
        final OFBridge ofBridge = ofBridgeManager.getOFBridge("br-int");
        OVSDBManager ovsdbManager = new OVSDBManager(ovsdbAddr, ovsdbPort);

        // Start a runnable which will push 1k flows once connection active,
        // we will then benchmark performance by using barrier.
        if (true) {
            pool.submit(new Runnable() {
                public void run() {
                    while (true) {
                        if (ofBridge.isBridgeOFConnectionWorking()) {
                            LOG.info("Switch openflow connection is working, now start test flowmod performance...");

                            //long startTime = date.getTime();
                            // Same date instance getTime call will return same timestamp, call System
                            // interface directly...
                            long startTime = System.currentTimeMillis();
                            LOG.info("FlowMod performance test start at " + startTime);
                            // If the connection is active, push flows to the switch...
                            final int FLOW_NUM = 100;
                            int i = 0;

                            for (; i < FLOW_NUM; i++) {
                                Flow flow = new Flow();
                                FlowKey flowKey = flow.getFlowKey();
                                flowKey.setMatch(new FlowMatchHelper().setInPort(i).toMatch());
                                ofBridge.addFlow(flow);
                            }

                            try {
                                ofBridge.barrier();
                                //long finishTime = date.getTime();
                                long finishTime = System.currentTimeMillis();
                                LOG.info("FlowMod performance test finished at " + finishTime);
                                LOG.info("FlowMod rate is " + 1000 * ((float) FLOW_NUM) / (float) (finishTime - startTime));
                            } catch (Exception e) {
                                LOG.error("Wait barrier reply timeout... can't obtain FlowMod performance");
                            }

                            break;
                        } else {
                            LOG.info("Switch openflow connection is not working, sleeping...");
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                LOG.error("FlowMod performance test is interruppted...");
                            }
                        }
                    }
                }
            });
        }


        // Start a runnable which will create 4k ports once connection active
        // We should wrap a single port initialization as a transaction which
        // will make 4k transactions in total, this will stress the library
        // and also conform to the real scenario.
        // And also we should wait monitor all created port for it's openflow
        // port.
        if (false) {
            pool.submit(new Runnable() {
                public void run() {
                    while (true) {
                        if (ovsdbManager.isOVSDBConnectionWorking()) {
                            final DatabaseSchema schema = null;//ovsdbConnectionManager.getSchema();
                            final OvsdbClient client = null;//ovsdbConnectionManager.getActiveOvsdbClient();
                            final int INTERNAL_PORT_NUM = 10;
                            final List<Interface> interfaceWithValidOfPortList = Lists.newArrayList();
                            final Map<UUID, Port> portMap = new HashMap<UUID, Port>();
                            final GenericTableSchema portTableSchema = schema.table("Port", GenericTableSchema.class);
                            final GenericTableSchema interfaceTableSchema = schema.table("Interface", GenericTableSchema.class);
                            long startTime = 0;
                            long finishTime = 0;

                            final MonitorCallBack callback = new MonitorCallBack() {
                                @Override
                                public void update(TableUpdates result, DatabaseSchema dbSchema) {
                                    Map<UUID, Port> portUpdates;
                                    Map<UUID, Interface> interfaceUpdates;
                                    portUpdates = TyperUtils.extractRowsUpdated(Port.class, result, dbSchema);
                                    interfaceUpdates = TyperUtils.extractRowsUpdated(Interface.class, result, dbSchema);

                                    for (Map.Entry<UUID, Port> entry : portUpdates.entrySet()) {
                                        String portName = entry.getValue().getNameColumn().getData();
                                        // Filter ports we do not care.
                                        if (portName.contains("odlperfp") && !portMap.containsKey(portName)) {
                                            portMap.put(entry.getKey(), entry.getValue());
                                            LOG.info("Got port create notify, port name " + portName);
                                        }
                                    }

                                    for (Map.Entry<UUID, Interface> entry : interfaceUpdates.entrySet()) {
                                        String interfaceName = entry.getValue().getNameColumn().getData();
                                        // Filter ports we do not care.
                                        if (!interfaceName.contains("odlperfp")) {
                                            continue;
                                        }
                                        long ofPort = 0;
                                        Set<Long> ofPorts = entry.getValue().
                                                getOpenFlowPortColumn().getData();
                                        if (ofPorts != null && !ofPorts.isEmpty()) {
                                            Iterator<Long> ofPortsIter = ofPorts.iterator();
                                            ofPort = ofPortsIter.next();
                                        }

                                        if (0L < ofPort) {
                                            LOG.info("Port " + interfaceName + "ofport " + ofPort + " obtained");
                                            interfaceWithValidOfPortList.add(entry.getValue());
                                        }
                                    }

                                    LOG.info("result = {}", result);
                                }

                                @Override
                                public void exception(Throwable t) {
                                    //monitor_results.add(t);
                                    LOG.warn("t = ", t);
                                }
                            };

                            LOG.info("The OVSDB connection and schema ready, now start test OVSDB performance...");

                            List<MonitorRequest> monitorRequests = Lists.newArrayList();
                            // By default library select nothing, so should add explicit select.
                            // And for test purpose, we do not want ports that already exists.
                            MonitorSelect selectAll = new MonitorSelect(false, true, true, true);
                            MonitorRequestBuilder monitorRequestBuilder = MonitorRequestBuilder.builder(interfaceTableSchema);
                            monitorRequestBuilder.addColumn("ofport");
                            monitorRequestBuilder.addColumn("name");
                            monitorRequests.add(monitorRequestBuilder.with(selectAll).build());
                            MonitorRequestBuilder portMonitorRequestBuilder = MonitorRequestBuilder.builder(portTableSchema);
                            portMonitorRequestBuilder.addColumn("name");
                            monitorRequests.add(portMonitorRequestBuilder.with(selectAll).build());

                            // Monitor itself is a synchronous interface, so we should try to update when the
                            // result come back, however, there should no result returned due to test not started yet.
                            // When developing agents, we should aware of this in order not block the calling
                            // thread...
                            callback.update(client.monitor(schema, monitorRequests, callback), schema);

                            //long startTime = date.getTime();
                            // Same date instance getTime call will return same timestamp, call System
                            // interface directly...
                            startTime = System.currentTimeMillis();
                            LOG.info("OVSDB performance test start at " + startTime);
                            int i = 0;
                            for (; i < INTERNAL_PORT_NUM; i++) {
                                TransactionBuilder tb = client.transactBuilder(schema);

                                String interfaceUuid = "Interface_" + i;
                                Interface ovsInterface =
                                        TyperUtils.getTypedRowWrapper(schema, Interface.class);
                                ovsInterface.setType("internal"); // We only need to create internal port...
                                ovsInterface.setName("odlperfp" + i);
                                tb.add(op.insert(ovsInterface).withId(interfaceUuid));

                                // External id mark, not set for test.
                                // stampInstanceIdentifier(transaction, entry.getKey(), ovsInterface.getName());

                                // Configure port with the above interface details
                                String portUuid = "Port_" + i;
                                Port port = TyperUtils.getTypedRowWrapper(schema, Port.class);
                                port.setName("odlperfp" + i);
                                port.setInterfaces(Sets.newHashSet(new UUID(interfaceUuid)));
                                tb.add(op.insert(port).withId(portUuid));

                                // Configure bridge with the above port details
                                Bridge bridge = TyperUtils.getTypedRowWrapper(schema, Bridge.class);
                                bridge.setName("br-int");
                                bridge.setPorts(Sets.newHashSet(new UUID(portUuid)));

                                tb.add(op.mutate(bridge)
                                        .addMutation(bridge.getPortsColumn().getSchema(),
                                                Mutator.INSERT, bridge.getPortsColumn().getData())
                                        .where(bridge.getNameColumn().getSchema()
                                                .opEqual(bridge.getNameColumn().getData())).build());

                                // We do not wait here, we will monitor the port openflow port  to ensure...
                                final ListenableFuture<List<OperationResult>> result = tb.execute();
                                result.addListener(new Runnable() {
                                    public void run() {
                                        try {
                                            List<OperationResult> results = result.get();
                                            for (OperationResult operationResult : results) {
                                                LOG.info("Add port transaction result: " + operationResult.getDetails());
                                            }
                                        } catch (InterruptedException e) {
                                            LOG.error("The transaction is interrupted...");
                                        } catch (ExecutionException e) {
                                            LOG.error("Exception in task");
                                        }
                                    }
                                }, pool);
                            }

                            // Wait until all ports have ofport valid.
                            while (true) {
                                if (interfaceWithValidOfPortList.size() == INTERNAL_PORT_NUM) {
                                    finishTime = System.currentTimeMillis();
                                    break;
                                } else {
                                    try {
                                        LOG.info("Test Ports not fully collected");
                                        Thread.sleep(50); // TODO: 50 ms second will downgrade performance....
                                    } catch (InterruptedException e) {
                                        LOG.info("OVSDB perf test is interrupted.");
                                        break;
                                    }
                                }
                            }

                            // Delete port asynchronously.
                            // Block until all ports get notified, then we can delete the ports.
                            while (true) {
                                if (portMap.size() != INTERNAL_PORT_NUM) {
                                    try {
                                        LOG.info("Test Ports not fully collected");
                                        Thread.sleep(50);
                                    } catch (InterruptedException e) {
                                        LOG.info("OVSDB perf test is interrupted.");
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }

                            // Delete all just added port
                            final List<Object> portRemovedList = Lists.newArrayList();
                            for (Map.Entry<UUID, Port> entry : portMap.entrySet()) {
                                TransactionBuilder tb = client.transactBuilder(schema);

                                UUID uuid = entry.getKey(); // Can't obtain uuid from port because it can be null.

                                // Configure a port which is only used to get ColumnSchema.
                                Port port = TyperUtils.getTypedRowWrapper(schema, Port.class, null);
                                tb.add(op.delete(portTableSchema)
                                        .where(port.getUuidColumn().getSchema().opEqual(uuid)).build());

                                // Configure bridge with the above port details
                                Bridge bridge = TyperUtils.getTypedRowWrapper(schema, Bridge.class, null);
                                tb.add(op.mutate(bridge)
                                        .addMutation(bridge.getPortsColumn().getSchema(),
                                                Mutator.DELETE, Sets.newHashSet(uuid))
                                        .where(bridge.getNameColumn().getSchema().opEqual("br-int")).build());

                                // We do not wait here, we will monitor the port openflow port  to ensure...
                                final ListenableFuture<List<OperationResult>> result = tb.execute();
                                result.addListener(new Runnable() {
                                    public void run() {
                                        portRemovedList.add(null); // just add null, we use list size to check completion
                                        try {
                                            List<OperationResult> results = result.get();
                                            for (OperationResult operationResult : results) {
                                                LOG.info("Delete port transaction result: " + operationResult.getDetails());
                                            }
                                        } catch (InterruptedException e) {
                                            LOG.error("The transaction is interrupted...");
                                        } catch (ExecutionException e) {
                                            LOG.error("Exception in task");
                                        }
                                    }
                                }, pool);
                            }


                            while (true) {
                                if (portRemovedList.size() != INTERNAL_PORT_NUM) {
                                    try {
                                        LOG.info("Test Ports not fully deleted");
                                        Thread.sleep(50);
                                    } catch (InterruptedException e) {
                                        LOG.info("OVSDB perf test is interrupted.");
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }

                            // Report test result.
                            LOG.info("OVSDB performance test finished at " + finishTime);
                            LOG.info("OVSDB port add performance test finished at " + finishTime);
                            LOG.info("OVSDB port add rate is " + 1000 * ((float) INTERNAL_PORT_NUM) / (float) (finishTime - startTime));

                            break;
                        } else {
                            LOG.info("OVSDB connection is not ready, sleeping...");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                LOG.error("OVSDB performance test is interruppted...");
                            }
                        }
                    }
                }
            });
        }

        while (true) {
            LOG.info("Master thread is running....");
            Thread.sleep(1000);
        }
    }

    static class ConnectionConfigurationImpl implements ConnectionConfiguration {

        private final InetAddress address;
        private final int port;
        private final TlsConfiguration tlsConfig;
        private final long switchIdleTimeout;
        private final boolean useBarrier;
        private Object transferProtocol;
        private ThreadConfiguration threadConfig;

        /**
         * Creates {@link ConnectionConfigurationImpl}
         *
         * @param address
         * @param port
         * @param protocol
         * @param switchIdleTimeout
         * @param useBarrier
         */
        public ConnectionConfigurationImpl(final InetAddress address, final int port, final TransportProtocol protocol,
                                           final long switchIdleTimeout, final boolean useBarrier) {
            this.address = address;
            this.port = port;
            this.transferProtocol = protocol;
            this.switchIdleTimeout = switchIdleTimeout;
            this.useBarrier = useBarrier;
            this.tlsConfig = null;
        }

        public InetAddress getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }

        public Object getTransferProtocol() {
            return transferProtocol;
        }

        /**
         * Used for testing - sets transport protocol
         *
         * @param protocol
         */
        public void setTransferProtocol(final TransportProtocol protocol) {
            this.transferProtocol = protocol;
        }

        public long getSwitchIdleTimeout() {
            return switchIdleTimeout;
        }

        public Object getSslContext() {
            // TODO Auto-generated method stub
            return null;
        }

        public TlsConfiguration getTlsConfiguration() {
            return tlsConfig;
        }

        public ThreadConfiguration getThreadConfiguration() {
            return threadConfig;
        }

        /**
         * @param threadConfig thread model configuration (configures threads used)
         */
        public void setThreadConfiguration(final ThreadConfiguration threadConfig) {
            this.threadConfig = threadConfig;
        }

        public boolean useBarrier() {
            return useBarrier;
        }
    }
}