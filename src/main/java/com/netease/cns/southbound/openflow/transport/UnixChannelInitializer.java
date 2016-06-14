package com.netease.cns.southbound.openflow.transport;

import io.netty.channel.Channel;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.group.DefaultChannelGroup;
import org.opendaylight.openflowjava.protocol.impl.core.*;
import org.opendaylight.openflowjava.protocol.impl.core.connection.ConnectionAdapterFactory;
import org.opendaylight.openflowjava.protocol.impl.core.connection.ConnectionAdapterFactoryImpl;
import org.opendaylight.openflowjava.protocol.impl.core.connection.ConnectionFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Created by hzzhangdongya on 16-6-8.
 */
public class UnixChannelInitializer extends ProtocolChannelInitializer<EpollDomainSocketChannel> {

    private static final Logger LOG = LoggerFactory
            .getLogger(org.opendaylight.openflowjava.protocol.impl.core.TcpChannelInitializer.class);
    private final DefaultChannelGroup allChannels;
    private final ConnectionAdapterFactory connectionAdapterFactory;

    /**
     * default ctor
     */
    public UnixChannelInitializer() {
        this(new DefaultChannelGroup("netty-receiver", null), new ConnectionAdapterFactoryImpl());
    }

    /**
     * Testing Constructor
     */
    protected UnixChannelInitializer(final DefaultChannelGroup channelGroup, final ConnectionAdapterFactory connAdaptorFactory) {
        allChannels = channelGroup;
        connectionAdapterFactory = connAdaptorFactory;
    }

    @Override
    protected void initChannel(EpollDomainSocketChannel ch) throws Exception {
        LOG.debug("Connection to unix domain socket established - building pipeline");
        allChannels.add(ch);
        ConnectionFacade connectionFacade = null;
        connectionFacade = connectionAdapterFactory.createConnectionFacade(ch, null, useBarrier());
        try {
            LOG.debug("calling plugin: {}", getSwitchConnectionHandler());
            getSwitchConnectionHandler().onSwitchConnected(connectionFacade);
            connectionFacade.checkListeners();
            ch.pipeline().addLast(PipelineHandlers.IDLE_HANDLER.name(), new IdleHandler(getSwitchIdleTimeout(), TimeUnit.MILLISECONDS));
            ch.pipeline().addLast(PipelineHandlers.OF_FRAME_DECODER.name(),
                    new OFFrameDecoder(connectionFacade, false/* No TLS present. */));
            ch.pipeline().addLast(PipelineHandlers.OF_VERSION_DETECTOR.name(), new OFVersionDetector());
            final OFDecoder ofDecoder = new OFDecoder();
            ofDecoder.setDeserializationFactory(getDeserializationFactory());
            ch.pipeline().addLast(PipelineHandlers.OF_DECODER.name(), ofDecoder);
            final OFEncoder ofEncoder = new OFEncoder();
            ofEncoder.setSerializationFactory(getSerializationFactory());
            ch.pipeline().addLast(PipelineHandlers.OF_ENCODER.name(), ofEncoder);
            ch.pipeline().addLast(PipelineHandlers.DELEGATING_INBOUND_HANDLER.name(), new DelegatingInboundHandler(connectionFacade));
            connectionFacade.fireConnectionReadyNotification();
        } catch (final Exception e) {
            LOG.warn("Failed to initialize channel", e);
            ch.close();
        }
    }

    /**
     * @return iterator through active connections
     */
    public Iterator<Channel> getConnectionIterator() {
        return allChannels.iterator();
    }

    /**
     * @return amount of active channels
     */
    public int size() {
        return allChannels.size();
    }

}
