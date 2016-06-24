package com.netease.cns.agent.dstore;


import com.google.common.util.concurrent.AbstractService;
import com.netease.cns.agent.CNSMain;
import com.netease.cns.agent.common.Constants;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action1;

import java.net.URL;

/**
 * Created by hzzhangdongya on 16-6-24.
 */
public class ZKBackend extends AbstractService {
    private static final Logger LOG = LoggerFactory.getLogger(ZKBackend.class);
    CuratorFramework zkClient;

    @Override
    protected void doStart() {
        URL jaasURL = CNSMain.class.getClassLoader().getResource(Constants.JAAS_PROP_FILE_REL_PATH);
        System.setProperty(Constants.JAAS_PROP, jaasURL.getPath());
        CuratorFramework zkClient = CuratorFrameworkFactory.newClient(Constants.ZK_SERVER_HOST, new RetryNTimes(10, 5000));
        zkClient.start();

        notifyStarted();
        LOG.info("ZKBackend started...");

        ObservablePathChildrenCache netCache = ObservablePathChildrenCache.getInstance(zkClient, "/cns/topologies/networks");
        netCache.subscribe(new Action1<ChildData>() {
            @Override
            public void call(ChildData childData) {
                LOG.info("received network child event {} from subscriber", childData);
                // TODO: now we can transfer event to core logic ACTOR if we use full event-driven scheme....
            }
        });

        ObservablePathChildrenCache agentCache = ObservablePathChildrenCache.getInstance(zkClient, "/cns/agents/ovs");
        agentCache.subscribe(new Action1<ChildData>() {
            @Override
            public void call(ChildData childData) {
                LOG.info("received ovs agent child event {} from subscriber", childData);
                // TODO: now we can transfer event to core logic ACTOR if we use full event-driven scheme....
            }
        });
    }

    @Override
    protected void doStop() {
        zkClient.close();
        notifyStopped();
    }
}
