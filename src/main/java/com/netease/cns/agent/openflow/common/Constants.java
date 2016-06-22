package com.netease.cns.agent.openflow.common;

import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.FlowModFlags;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.TableId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.grouping.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.grouping.MatchBuilder;

import java.util.concurrent.TimeUnit;

/**
 * Created by hzzhangdongya on 16-6-14.
 */
public class Constants {
    public final static short OF_VERSION = 4;
    public final static TableId TABLE_ID_0 = new TableId(0L);
    public final static FlowModFlags EMPTY_FLOWMOD_FLAGS = new FlowModFlags(false, false, false, false, false);
    public final static Match MATCH_ALL = new MatchBuilder().build();
    public final static long OFP_NO_BUFFER = 0xffffffffL;
    public final static long OFPP_ANY = 0xffffffffL;
    public final static long OFPG_ANY = 0xffffffffL;
    public final static long BARRIER_TIMEOUT = 3000;
    public final static TimeUnit BARRIER_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

}
