package com.netease.cns.southbound.openflow.flow;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by hzzhangdongya on 16-6-15.
 */
public class FlowMatchHelperTest {
    @Test
    public void testFlowMatch() throws Exception {
        FlowMatchHelper fh1 = new FlowMatchHelper().setInPort(1);
        FlowMatchHelper fh2 = new FlowMatchHelper().setInPort(2);

        assertNotEquals(fh1.toMatch(), fh2.toMatch());

        FlowMatchHelper fh3 = new FlowMatchHelper().setInPort(3);
        FlowMatchHelper fh4 = new FlowMatchHelper().setInPort(3);

        // This equals because YANG generated class have implemented
        // equals method which can used to compare equality of
        // different Match object.
        assertEquals(fh3.toMatch(), fh4.toMatch());
    }
}