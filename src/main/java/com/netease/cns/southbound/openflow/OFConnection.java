package com.netease.cns.southbound.openflow;

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
public class OFConnection implements OpenflowProtocolListener, SwitchConnectionHandler,
        SystemNotificationsListener, ConnectionReadyListener {
    private static final Logger LOG = LoggerFactory.getLogger(OFConnection.class);
    private ConnectionAdapter connectionAdapter = null;
    private boolean working = false;
    // TODO: thread safety or atomic integer?
    private long xid = -1; // OpenFlow xid is 32 bit unsigned integer.

    public long nextXid() {
        long _xid = this.xid + 1;
        if (_xid > 4294967295L) {
            _xid = this.xid = 0;
        }

        return _xid;
    }

    @Override
    public void onConnectionReady() {
        // We send one hello immediately, and due to the simplicity of our
        // scenario, we can make sure our handshake with peer will succeed,
        // and we will make this connection liave
        HelloInputBuilder helloInputbuilder = new HelloInputBuilder();
        // OpenFlow 1.3, we can use 1.3 with NXM extensions to program
        // the OpenFlow rules we programmed by ovs-ofctl...
        helloInputbuilder.setVersion(new Short((short) 4));
        helloInputbuilder.setXid(nextXid());
        HelloInput helloInput = helloInputbuilder.build();
        // Not care result currently.
        connectionAdapter.hello(helloInput);
    }

    // This is called when underlying socket connected but decoder/encoder not
    // added to the pipeline, so never try to send any packet here, do it after
    // got onConnectionReady notification.
    @Override
    public void onSwitchConnected(ConnectionAdapter connectionAdapter) {
        LOG.info("A openflow switch connected...");
        connectionAdapter.setConnectionReadyListener(this);
        connectionAdapter.setMessageListener(this);
        connectionAdapter.setSystemListener(this);
        this.connectionAdapter = connectionAdapter;
    }

    @Override
    public boolean accept(InetAddress inetAddress) {
        return true;
    }

    @Override
    public void onMultipartReplyMessage(MultipartReplyMessage multipartReplyMessage) {

    }

    @Override
    public void onExperimenterMessage(ExperimenterMessage experimenterMessage) {

    }

    @Override
    public void onPortStatusMessage(PortStatusMessage portStatusMessage) {

    }

    @Override
    public void onErrorMessage(ErrorMessage errorMessage) {

    }

    @Override
    public void onPacketInMessage(PacketInMessage packetInMessage) {

    }

    @Override
    public void onHelloMessage(HelloMessage helloMessage) {
        // No handshake is needed in our scenario, directly shift to working.
        working = true;
    }

    @Override
    public void onEchoRequestMessage(EchoRequestMessage echoRequestMessage) {
        // Simply reply a EchoReply to make connection alive.
        EchoReplyInputBuilder echoReplyInputBuilder = new EchoReplyInputBuilder();
        // OpenFlow 1.3, we can use 1.3 with NXM extensions to program
        // the OpenFlow rules we programmed by ovs-ofctl...
        echoReplyInputBuilder.setVersion(new Short((short)4));
        echoReplyInputBuilder.setXid(echoRequestMessage.getXid());
        EchoReplyInput echoReplyInput = echoReplyInputBuilder.build();
        // Not care result currently.
        connectionAdapter.echoReply(echoReplyInput);
    }

    @Override
    public void onFlowRemovedMessage(FlowRemovedMessage flowRemovedMessage) {

    }

    @Override
    public void onSwitchIdleEvent(SwitchIdleEvent switchIdleEvent) {
        // Handle idle timeout logic here.
    }

    @Override
    public void onDisconnectEvent(DisconnectEvent disconnectEvent) {
        connectionAdapter = null;
    }

    public boolean isConnectionWorking() {
        return working;
    }

    public ConnectionAdapter getConnectionAdapter() {
        return connectionAdapter;
    }
}