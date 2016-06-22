package com.netease.cns.agent.openflow.transport;

/**
 * Created by hzzhangdongya on 16-6-13.
 */
public class Utils {
    public static String makeOFUnixSocketPath(String bridgeName) {
        // TODO: make this string as a configuration item.
        return "/run/openvswitch/" + bridgeName + ".mgmt";
    }
}
