package com.netease.cns.agent.openflow.flow;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by hzzhangdongya on 16-6-15.
 */
public class FlowKeyTest {
    @Test
    public void testFlowKeyEqual() throws Exception {
        FlowKey fk1 = new FlowKey();
        FlowKey fk2 = new FlowKey();

        // This equals because they use the same Match object.
        assertEquals(fk1.getMatch(), fk2.getMatch());
    }
}