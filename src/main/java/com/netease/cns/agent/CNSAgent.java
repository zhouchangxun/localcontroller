package com.netease.cns.agent;

import com.netease.cns.agent.common.Constants;
import com.netease.cns.agent.ovsdb.OVSDBBridge;
import com.netease.cns.agent.ovsdb.OVSDBManager;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Created by hzzhangdongya on 16-6-21.
 */
public class CNSAgent {
    private static final Logger LOG = LoggerFactory.getLogger(CNSAgent.class);

    private static CNSAgent instance = null;

    protected CNSAgent() {
        // Exists only to defeat instantiation.
    }

    public static CNSAgent getInstance() {
        if (instance == null) {
            instance = new CNSAgent();
        }
        return instance;
    }

    // This sample controller only care 1./2. above, which focus on the southbound interactive with openvswitch.
    public void run() {
        // Init ZK and monitor neccesary datas??
        // Init ovsdbmanager and start ovsdb manager.
        // Init ofbr(try create br if not exists) and start connection, then monitor changes of ofport.

        // Currently only care about the zk schema and use that to caculate flows for br-int

        URL jaasURL = CNSMain.class.getClassLoader().getResource(Constants.JAAS_PROP_FILE_REL_PATH);
        System.setProperty(Constants.JAAS_PROP, jaasURL.getPath());
        CuratorFramework zkClient = CuratorFrameworkFactory.newClient(Constants.ZK_SERVER_HOST, new RetryNTimes(10, 5000));
        zkClient.start();

        int ovsdbPort = 6634;
        InetAddress ovsdbAddr;
        try {
            ovsdbAddr = InetAddress.getByName("10.166.224.11");
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        final OVSDBManager ovsdbManager = new OVSDBManager(ovsdbAddr, ovsdbPort);
        OVSDBBridge brInt = ovsdbManager.getOVSDBBridge("br-int1");
        brInt.ensureExisted();
        LOG.info("Ensure br-int1 finished");
    }
}
