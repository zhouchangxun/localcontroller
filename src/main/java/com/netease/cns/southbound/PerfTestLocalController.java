/**
 * Created by hzzhangdongya on 16-6-6.
 */

package com.netease.cns.southbound;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.netease.cns.southbound.openflow.OFBridge;
import com.netease.cns.southbound.openflow.OFBridgeManager;
import com.netease.cns.southbound.openflow.flow.Flow;
import com.netease.cns.southbound.openflow.flow.FlowKey;
import com.netease.cns.southbound.openflow.flow.FlowMatchHelper;
import com.netease.cns.southbound.ovsdb.OVSDBManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.Executors;

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
        if (true) {
            pool.submit(new Runnable() {
                public void run() {
                    while (true) {
                        if (ovsdbManager.isOVSDBConnectionWorking()) {
                            // TODO: basic logic here.

                            // 1. add 2k ports
                            // 2. subscribe to port changes by registering to changes event.
                            // 3. delete all ports and check local cache empty.
                            // 4. report data and finish test.

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