package com.netease.cns.southbound.openflow;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hzzhangdongya on 16-6-13.
 */
public class OFBridgeManager {
    private final Map<String, OFBridge> bridgeMap = new HashMap<>();

    // 1. get OFBridge instance

    public OFBridge getOFBridge(String bridgeName) {
        OFBridge ofBridge = bridgeMap.get(bridgeName);
        if (ofBridge == null) {
            ofBridge = new OFBridge();
            bridgeMap.put(bridgeName, ofBridge);
        }

        return ofBridge;
    }

    public void delOFBridge(String bridgeName) {

    }
}
