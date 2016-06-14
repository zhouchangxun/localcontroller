package com.netease.cns.southbound.openflow;

import com.netease.cns.southbound.openflow.common.Constants;
import com.netease.cns.southbound.openflow.flow.Flow;
import com.netease.cns.southbound.openflow.flow.FlowKey;
import com.netease.cns.southbound.openflow.transport.UnixChannelInitializer;
import com.netease.cns.southbound.openflow.transport.UnixConnectionInitializer;
import com.netease.cns.southbound.openflow.transport.Utils;
import com.romix.scala.collection.concurrent.TrieMap;
import io.netty.channel.epoll.EpollEventLoopGroup;
import org.opendaylight.openflowjava.protocol.impl.deserialization.DeserializationFactory;
import org.opendaylight.openflowjava.protocol.impl.serialization.SerializationFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.FlowModCommand;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.BarrierInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.BarrierOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.FlowModInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Created by hzzhangdongya on 16-6-13.
 */
public class OFBridge {
    private static final Logger LOG = LoggerFactory.getLogger(OFBridge.class);
    // Store flows for reconciliation.
    private static final Map flowStore = new TrieMap<FlowKey, Flow>();

    // 1. add/delete/mod flow
    // 2. maintain a flow registry to handle connect or disconnect, which hide the complexity from
    //    core logic.
    // 3. reconnect logic to handle connect failure.
    // 4. we should also provide some interface which does not persistent like delete flows by cookieMask...
    private String bridgeName = null;
    private OFConnection ofConnection = null;

    public OFBridge(String bridgeName,
                    EpollEventLoopGroup workerGroup,
                    SerializationFactory serializationFactory,
                    DeserializationFactory deserializationFactory) {
        this.bridgeName = bridgeName;

        UnixConnectionInitializer unixConnectionInitializer = new UnixConnectionInitializer(workerGroup);
        UnixChannelInitializer unixChannelInitializer = new UnixChannelInitializer();
        unixConnectionInitializer.setChannelInitializer(unixChannelInitializer);
        ofConnection = new OFConnection();
        unixChannelInitializer.setSwitchConnectionHandler(ofConnection);
        unixChannelInitializer.setSerializationFactory(serializationFactory);
        unixChannelInitializer.setDeserializationFactory(deserializationFactory);

        // Try connect immediately.
        unixConnectionInitializer.run();
        unixConnectionInitializer.initiateConnection(Utils.makeOFUnixSocketPath(bridgeName));

        // TODO: start a timer to reconnect.
    }

    // Used for explicit checking connection state.
    public boolean isBridgeOFConnectionWorking() {
        return ofConnection.isConnectionWorking();
    }

    // TODO: throw exception?
    public void addFlow(Flow flow) {
        // 1. store flow to registry
        // 2. try to add if connection is working.

        if (null != flowStore.get(flow.getFlowKey())) {
            // TODO: throw invalid parameter error, flow already added, should call modify
            return;
        }

        flowStore.put(flow.getFlowKey(), flow);

        if (ofConnection.isConnectionWorking()) {
            FlowModInputBuilder fmInputBuild = new FlowModInputBuilder();
            fmInputBuild.setXid(ofConnection.nextXid());
            fmInputBuild.setCookie(flow.getCookie());
            fmInputBuild.setCookieMask(flow.getCookieMask());
            // We should really provide an API that set default entries here, this is TOO
            // time-consuming to prevent NullPointerException in the serialization logic...
            fmInputBuild.setMatch(flow.getFlowKey().getMatch());
            fmInputBuild.setCommand(FlowModCommand.OFPFCADD);
            fmInputBuild.setVersion(Constants.OF_VERSION);
            fmInputBuild.setTableId(flow.getFlowKey().getTableId());
            fmInputBuild.setBufferId(Constants.OFP_NO_BUFFER);
            fmInputBuild.setFlags(flow.getFlags());
            fmInputBuild.setHardTimeout(flow.getHardTimeout());
            fmInputBuild.setIdleTimeout(flow.getIdleTimeout());
            fmInputBuild.setPriority(flow.getFlowKey().getPriority());
            fmInputBuild.setOutGroup(Constants.OFPG_ANY);
            fmInputBuild.setOutPort(new PortNumber(Constants.OFPP_ANY));
            // XXX: Perhaps here we should have a lock when operating on the connection adapter...
            // For perftest this is enough.
            // TODO: we should check the future of this flow mod to ensure that it's pushed
            // to the socket buffer.
            ofConnection.getConnectionAdapter().flowMod(fmInputBuild.build());
        } else {
            LOG.info("Connection is not ready for switch " + bridgeName + ", will program flow later");
        }
    }

    public boolean barrier() throws InterruptedException, ExecutionException, TimeoutException {
        if (ofConnection.isConnectionWorking()) {
            // Send barrier request and wait for reply to ensure flow programmed.
            BarrierInputBuilder barrierInputBuilder = new BarrierInputBuilder();
            barrierInputBuilder.setVersion(Constants.OF_VERSION);
            barrierInputBuilder.setXid(ofConnection.nextXid());
            Future<RpcResult<BarrierOutput>> barrierResult =
                    ofConnection.getConnectionAdapter().barrier(barrierInputBuilder.build());
            try {
                RpcResult<BarrierOutput> output = barrierResult.get(Constants.BARRIER_TIMEOUT, Constants.BARRIER_TIMEOUT_UNIT);
                return output.isSuccessful();
            } catch (Exception e) {
                LOG.error("Wait barrier reply timeout... can't obtain FlowMod performance");
                return false;
            }
        } else {
            LOG.info("Connection is not ready for switch " + bridgeName + ", can't do barrier.");
            return false;
        }
    }
}
