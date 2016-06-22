/**
 * Created by hzzhangdongya on 16-6-6.
 */

package com.netease.cns.agent;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.netease.cns.agent.openflow.OFBridge;
import com.netease.cns.agent.openflow.OFBridgeManager;
import com.netease.cns.agent.openflow.flow.Flow;
import com.netease.cns.agent.openflow.flow.FlowKey;
import com.netease.cns.agent.openflow.flow.FlowMatchHelper;
import com.netease.cns.agent.ovsdb.OVSDBBridge;
import com.netease.cns.agent.ovsdb.OVSDBManager;
import com.netease.cns.agent.ovsdb.event.*;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class PerfTestLocalController {
    private static final Logger LOG = LoggerFactory.getLogger(PerfTestLocalController.class);
    private static ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

    public static void main(String[] args) throws Exception {
        // Start a runnable which will push 1k flows once connection active,
        // we will then benchmark performance by using barrier.
        if (false) {
            OFBridgeManager ofBridgeManager = new OFBridgeManager();
            final OFBridge ofBridge = ofBridgeManager.getOFBridge("br-int");
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

        // Start a runnable which will create 2k ports once connection active
        // We should wrap a single port initialization as a transaction which
        // will make 2k transactions in total, this will stress the library
        // and also conform to the real scenario.
        // And also we should wait monitor all created port for it's openflow
        // port.
        if (true) {
            int ovsdbPort = 6634;
            InetAddress ovsdbAddr = InetAddress.getByName("10.166.224.11");
            final OVSDBManager ovsdbManager = new OVSDBManager(ovsdbAddr, ovsdbPort);

            pool.submit(new Runnable() {
                public void run() {
                    while (true) {
                        if (ovsdbManager.isOVSDBConnectionWorking()) {
                            OVSDBBridge brInt = ovsdbManager.getOVSDBBridge("br-int");
                            int PORT_NUM = 2000;
                            Map<UUID, Object> ofPortMap = new HashMap<>();
                            Map<UUID, Object> addedPortMap = new HashMap<>();
                            Map<UUID, Object> removedPortMap = new HashMap<>();

                            final long[] addTime = {0, 0}; // 0 is start time, 1 is finish time.
                            final long[] removeTime = {0, 0}; // 0 is start time, 1 is finish time.

                            ovsdbManager.registerOVSDBChangeListener(new OVSDBChangeListener() {
                                @Override
                                public void notify(BaseEvent event) {
                                    if (event.getClass() == InterfaceOFPortAllocatedEvent.class) {
                                        InterfaceOFPortAllocatedEvent interfaceOfPortAllocatedEvent = ((InterfaceOFPortAllocatedEvent) event);
                                        LOG.info("New port have ofport allocated, port " +
                                                interfaceOfPortAllocatedEvent.getPortName() +
                                                ", ofport " + interfaceOfPortAllocatedEvent.getOfPort());

                                        ofPortMap.put(interfaceOfPortAllocatedEvent.getUuid(), null);
                                        LOG.info("Gather ofport num " + ofPortMap.size());
                                        if (ofPortMap.size() == PORT_NUM) {
                                            addTime[1] = System.currentTimeMillis();
                                        }


                                    } else if (event.getClass() == PortRemovedEvent.class) {
                                        PortRemovedEvent portRemovedEvent = ((PortRemovedEvent) event);
                                        LOG.info("Received port removed event, port " + portRemovedEvent.getPortName());
                                        removedPortMap.put(portRemovedEvent.getUuid(), null);
                                        if (removedPortMap.size() == PORT_NUM) {
                                            removeTime[1] = System.currentTimeMillis();
                                        }
                                    } else if (event.getClass() == PortAddedEvent.class) {
                                        PortAddedEvent portAddedEvent = ((PortAddedEvent) event);
                                        LOG.info("Received port added event, port " + portAddedEvent.getPortName());
                                        addedPortMap.put(portAddedEvent.getUuid(), null);
                                    }

                                }
                            });

                            LOG.info("OVSDB connection is ready, do test");

                            addTime[0] = System.currentTimeMillis();
                            LOG.info("OVSDB performance test start at " + addTime[0]);

                            for (int i = 0; i < PORT_NUM; i++) {
                                brInt.addPort("odlperftest" + i);
                            }

                            // Wait until all ports have ofport valid.
                            while (true) {
                                if ((addedPortMap.size() != PORT_NUM) || (ofPortMap.size() != PORT_NUM)) {
                                    try {
                                        LOG.info("Test Ports not fully collected");
                                        Thread.sleep(50); // TODO: 50 ms second will downgrade performance....
                                    } catch (InterruptedException e) {
                                        LOG.info("OVSDB perf test is interrupted.");
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }

                            removeTime[0] = System.currentTimeMillis();
                            LOG.info("Remove ports start at " + removeTime[0]);

                            // Remove the added port by uuid.
                            // HashMap entrySet is backed by HashMap itself, so add another removedPortMap
                            // to check port removal.
                            for (Map.Entry<UUID, Object> entry : addedPortMap.entrySet()) {
                                brInt.removePort(entry.getKey());
                            }

                            // Wait until all ports have been removed.
                            while (true) {
                                if (removedPortMap.size() != PORT_NUM) {
                                    try {
                                        LOG.info("Test Ports not fully removed");
                                        Thread.sleep(50); // TODO: 50 ms second will downgrade performance....
                                    } catch (InterruptedException e) {
                                        LOG.info("OVSDB perf test is interrupted.");
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            }

                            // Report test result.
                            LOG.info("OVSDB port add performance finished at " + addTime[1]);
                            LOG.info("OVSDB port add rate is " + 1000 * ((float) PORT_NUM) / (float) (addTime[1] - addTime[0]));
                            LOG.info("OVSDB port remove performance test finished at " + removeTime[1]);
                            LOG.info("OVSDB port remove rate is " + 1000 * ((float) PORT_NUM) / (float) (removeTime[1] - removeTime[0]));
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
            Thread.sleep(5000);
        }
    }
}