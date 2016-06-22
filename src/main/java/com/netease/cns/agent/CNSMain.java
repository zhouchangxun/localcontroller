package com.netease.cns.agent;

/**
 * Created by hzzhangdongya on 16-6-13.
 */
public class CNSMain {
    public static void main(String[] args) throws Exception {
        CNSAgent agent = CNSAgent.getInstance();
        agent.run();
    }
}
