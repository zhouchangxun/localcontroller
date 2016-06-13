package com.netease.cns.southbound.openflow.transport;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
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

    private static final Logger LOG = LoggerFactory
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
        // XXX: I copied the code from TCPConnectionInitializer, but it directly
        // call sync of the ChannelFuture object which does not conform to fully-async
        // philosophy of netty.
        // Change to code here.
        // Also netty ChannelFuture.sync seems have a bug which does raise exception like
        // ConnectException due to it define the interface specified only InterruptedException
        // will be raised....
        ChannelFuture f = b.connect(new DomainSocketAddress(unixSocketPath));
        f.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    LOG.error("Unable to initial connection due to " + future.cause().getMessage());
                }
            }
        });
    }

    /**
     * @param channelInitializer
     */
    public void setChannelInitializer(UnixChannelInitializer channelInitializer) {
        this.channelInitializer = channelInitializer;
    }
}
