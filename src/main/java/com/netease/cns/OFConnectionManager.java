package com.netease.cns;

import org.opendaylight.openflowjava.protocol.api.connection.ConnectionAdapter;
import org.opendaylight.openflowjava.protocol.api.connection.ConnectionReadyListener;
import org.opendaylight.openflowjava.protocol.api.connection.SwitchConnectionHandler;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.protocol.rev130731.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.system.rev130927.DisconnectEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.system.rev130927.SwitchIdleEvent;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.system.rev130927.SystemNotificationsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

/**
 * Created by hzzhangdongya on 16-6-7.
 */
public class OFConnectionManager implements OpenflowProtocolListener, SwitchConnectionHandler,
        SystemNotificationsListener, ConnectionReadyListener {
    private static final Logger LOG = LoggerFactory.getLogger(OFConnectionManager.class);
    private ConnectionAdapter activeConnectionAdapter = null;
    private boolean connectionActive = false;

    public void onConnectionReady() {

    }

    public void onSwitchConnected(ConnectionAdapter connectionAdapter) {
        LOG.info("A openflow switch connected...");
        connectionAdapter.setConnectionReadyListener(this);
        connectionAdapter.setMessageListener(this);
        connectionAdapter.setSystemListener(this);
        activeConnectionAdapter = connectionAdapter;
    }

    public boolean accept(InetAddress inetAddress) {
        return true;
    }

    public void onMultipartReplyMessage(MultipartReplyMessage multipartReplyMessage) {

    }

    public void onExperimenterMessage(ExperimenterMessage experimenterMessage) {

    }

    public void onPortStatusMessage(PortStatusMessage portStatusMessage) {

    }

    public void onErrorMessage(ErrorMessage errorMessage) {

    }

    public void onPacketInMessage(PacketInMessage packetInMessage) {

    }

    public void onHelloMessage(HelloMessage helloMessage) {
        // Simply reply a hello message to the switch, later we may add
        // a HandshakeManager as openflowplugin does.
        if (activeConnectionAdapter != null ) {
            HelloInputBuilder helloInputbuilder = new HelloInputBuilder();
            // OpenFlow 1.3, we can use 1.3 with NXM extensions to program
            // the OpenFlow rules we programmed by ovs-ofctl...
            helloInputbuilder.setVersion(new Short((short)4));
            helloInputbuilder.setXid(helloMessage.getXid());
            HelloInput helloInput = helloInputbuilder.build();
            // Not care result currently.
            activeConnectionAdapter.hello(helloInput);
        }
    }

    public void onEchoRequestMessage(EchoRequestMessage echoRequestMessage) {
        // Simply reply a EchoReply to make connection alive.
        if (activeConnectionAdapter != null ) {
            EchoReplyInputBuilder echoReplyInputBuilder = new EchoReplyInputBuilder();
            // OpenFlow 1.3, we can use 1.3 with NXM extensions to program
            // the OpenFlow rules we programmed by ovs-ofctl...
            echoReplyInputBuilder.setVersion(new Short((short)4));
            echoReplyInputBuilder.setXid(echoRequestMessage.getXid());
            EchoReplyInput echoReplyInput = echoReplyInputBuilder.build();
            // Not care result currently.
            activeConnectionAdapter.echoReply(echoReplyInput);

            // Set indication of connection ready.
            connectionActive = true;
        }
    }

    public void onFlowRemovedMessage(FlowRemovedMessage flowRemovedMessage) {

    }

    public void onSwitchIdleEvent(SwitchIdleEvent switchIdleEvent) {

    }

    public void onDisconnectEvent(DisconnectEvent disconnectEvent) {
        activeConnectionAdapter = null;
    }

    public boolean isConnectionActive() {
        return connectionActive;
    }

    public ConnectionAdapter getActiveConnectionAdapter() {
        return activeConnectionAdapter;
    }
}