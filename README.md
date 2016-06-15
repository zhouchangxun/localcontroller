# Purpose of this project

* Develop a framework which will ease local SDN agent development

# Special Notes

## Transport consideration

### OpenFlow

Since we do not have controller configured previously, if we set controller OVS will flush all
the flows on the bridge which will make traffic break, and is not accepted by us.

So we choose unix transport to emulate ovs-vsctl behavior, which manage OVS bridge by unix domain
socket (OVS listening on <br-name>.mgmt for each bridge).

### OVSDB

Since ODL ovsdb library does not provider unix domain socket transport, and using TCP will not have
impact on traffic, so we keep use TCP.

### KeepAlive

### OpenFlow

TODO: Need implement OnSwitchIdleEvent support which will send EchoRequest to peer and wait reply to check
if connection is still alive.

### OVSDB

TODO: Seems OVSDB active connection does not add idletimeout to netty channel pipeline, we should refer to
passive mode and send echo probe and detect lost of connection.


## <br-name>.mgmt privilege issue

Make sure the program have privilege to access br-XXX.mgmt socket

sudo java -jar out/artifacts/localcontroller_jar/localcontroller.jar