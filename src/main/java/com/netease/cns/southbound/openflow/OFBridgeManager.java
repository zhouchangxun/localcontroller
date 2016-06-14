package com.netease.cns.southbound.openflow;

import com.netease.cns.southbound.openflow.transport.UnixChannelInitializer;
import com.netease.cns.southbound.openflow.transport.UnixConnectionInitializer;
import io.netty.channel.epoll.EpollEventLoopGroup;
import org.opendaylight.openflowjava.protocol.impl.deserialization.DeserializationFactory;
import org.opendaylight.openflowjava.protocol.impl.deserialization.DeserializerRegistryImpl;
import org.opendaylight.openflowjava.protocol.impl.serialization.SerializationFactory;
import org.opendaylight.openflowjava.protocol.impl.serialization.SerializerRegistryImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hzzhangdongya on 16-6-13.
 */
public class OFBridgeManager {
    private final Map<String, OFBridge> bridgeMap = new HashMap<>();
    // EpollEventLoopGroup is required by Netty in order to use UnixChannel.
    private final EpollEventLoopGroup workerGroup = new EpollEventLoopGroup();
    private SerializationFactory serializationFactory  = null;
    private DeserializationFactory deserializationFactory  = null;

    public OFBridgeManager() {
        SerializerRegistryImpl serializerRegistry = new SerializerRegistryImpl();
        serializerRegistry.init();
        serializationFactory = new SerializationFactory();
        serializationFactory.setSerializerTable(serializerRegistry);
        DeserializerRegistryImpl deserializerRegistry = new DeserializerRegistryImpl();
        deserializerRegistry.init();
        deserializationFactory = new DeserializationFactory();
        deserializationFactory.setRegistry(deserializerRegistry);
    }

    // 1. get OFBridge instance

    public OFBridge getOFBridge(String bridgeName) {
        OFBridge ofBridge = bridgeMap.get(bridgeName);
        if (ofBridge == null) {
            ofBridge = new OFBridge(bridgeName, workerGroup, serializationFactory, deserializationFactory);
            bridgeMap.put(bridgeName, ofBridge);
        }

        return ofBridge;
    }

    public void delOFBridge(String bridgeName) {

    }
}
