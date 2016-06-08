package com.netease.cns.transport;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import org.opendaylight.openflowjava.protocol.api.connection.ThreadConfiguration;
import org.opendaylight.openflowjava.protocol.impl.core.ServerFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes (UnixDomain) connection to device
 * Created by hzzhangdongya on 16-6-8.
 */
public class UnixConnectionInitializer implements ServerFacade {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(UnixConnectionInitializer.class);
    private EpollEventLoopGroup workerGroup;
    private ThreadConfiguration threadConfig;

    private UnixChannelInitializer channelInitializer;
    private Bootstrap b;

    /**
     * Constructor
     *
     * @param workerGroup - shared worker group
     *                    For unix domain socket, EpollEventLoopGroup is required by netty.
     */
    public UnixConnectionInitializer(EpollEventLoopGroup workerGroup) {
        Preconditions.checkNotNull(workerGroup, "WorkerGroup can't be null");
        this.workerGroup = workerGroup;
    }

    public void run() {
        b = new Bootstrap();
        b.group(workerGroup).channel(EpollDomainSocketChannel.class)
                .handler(channelInitializer);
    }

    public ListenableFuture<Boolean> shutdown() {
        final SettableFuture<Boolean> result = SettableFuture.create();
        workerGroup.shutdownGracefully();
        return result;
    }

    public ListenableFuture<Boolean> getIsOnlineFuture() {
        return null;
    }

    public void setThreadConfig(ThreadConfiguration threadConfig) {
        this.threadConfig = threadConfig;
    }

    public void initiateConnection(String unixSocketPath) {
        try {
            b.connect(new DomainSocketAddress(unixSocketPath)).sync();
        } catch (InterruptedException e) {
            LOGGER.error("Unable to initiate connection", e);
        }
    }

    /**
     * @param channelInitializer
     */
    public void setChannelInitializer(UnixChannelInitializer channelInitializer) {
        this.channelInitializer = channelInitializer;
    }
}
