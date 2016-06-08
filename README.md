# Purpose of this project

* Test OpenFlow/OVSDB java native library performance of Open Daylight Controller.
* Add Unix Domain Socket transport support for OpenFlow

# Special Notes

## br-*.mgmt privilege issue

Make sure the program have privilege to access br-XXX.mgmt socket

sudo java -jar out/artifacts/localcontroller_jar/localcontroller.jar

## Unix Domain Socket active connect test running delay

Since OVS set a 60 seconds timer for echo request on br-*.mgmt OpenFlow connection, and I use the first
received EchoRequest as an indication of establishment of connection, so the test which depends on the
indication will run in 60 seconds later once you run the test controller.

