package com.netease.cns.southbound.openflow.flow;

import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.grouping.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.grouping.MatchBuilder;

import java.util.ArrayList;

import static org.junit.Assert.*;

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