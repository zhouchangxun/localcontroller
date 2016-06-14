package com.netease.cns.southbound.openflow.flow;

import com.google.common.collect.Lists;
import com.netease.cns.southbound.openflow.common.Constants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.InPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.OpenflowBasicClass;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.OxmMatchType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.entries.grouping.MatchEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.entries.grouping.MatchEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.entry.value.grouping.match.entry.value.InPortCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.entry.value.grouping.match.entry.value.in.port._case.InPortBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.grouping.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.grouping.MatchBuilder;

import java.util.ArrayList;

/**
 * Created by hzzhangdongya on 16-6-14.
 */
public class FlowMatchHelper {
    private long inPort = Constants.OFPP_ANY;

    // TODO: add more fields.

    public long getInPort() {
        return inPort;
    }

    public FlowMatchHelper setInPort(long inPort) {
        this.inPort = inPort;
        return this;
    }

    public Match toMatch() {
        ArrayList<MatchEntry> matchEntryList = Lists.newArrayList();

        if (inPort != Constants.OFPP_ANY) {
            InPortBuilder inPortBuilder = new InPortBuilder();
            inPortBuilder.setPortNumber(new PortNumber(inPort));
            InPortCaseBuilder caseBuilder = new InPortCaseBuilder()
                    .setInPort(inPortBuilder.build());
            MatchEntryBuilder matchEntryBuilder = new MatchEntryBuilder()
                    .setOxmClass(OpenflowBasicClass.class)
                    .setOxmMatchField(InPort.class)
                    .setHasMask(false)
                    .setMatchEntryValue(caseBuilder.build());

        }

        MatchBuilder matchBuilder = new MatchBuilder()
                .setType(OxmMatchType.class)
                .setMatchEntry(matchEntryList);

        return matchBuilder.build();
    }
}
