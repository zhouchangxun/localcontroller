package com.netease.cns.southbound;

/**
 * Created by hzzhangdongya on 16-6-13.
 */
public class RefactorSampleController {
    // 1. initialize bridge flows manager
    // 2. initialize ovsdb manager
    // 3. start core logic which handle all kinds of events like zk notify, ovsdb notify, and calculate
    //    flows which guide forwarding.

    // This sample controller only care 1./2. above, which focus on the southbound interactive with openvswitch.

}
