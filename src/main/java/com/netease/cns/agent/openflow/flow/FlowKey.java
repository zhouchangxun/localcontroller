package com.netease.cns.agent.openflow.flow;

import com.netease.cns.agent.openflow.common.Constants;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.common.types.rev130731.TableId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflow.oxm.rev150225.match.grouping.Match;

/**
 * Created by hzzhangdongya on 16-6-14.
 */
public class FlowKey {
    private java.lang.Integer priority;
    private TableId tableId;
    private Match match;

    public FlowKey() {
        priority = java.lang.Integer.valueOf(0);
        tableId = Constants.TABLE_ID_0;
        match = Constants.MATCH_ALL;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final FlowKey that = (FlowKey) o;

        if (priority != that.priority) {
            return false;
        }
        if (tableId != that.tableId) {
            return false;
        }
        return match.equals(that.match);

    }

    @Override
    public int hashCode() {
        int result = tableId.getValue().intValue();
        result = 31 * result + priority;
        result = 31 * result + match.hashCode();
        return result;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public TableId getTableId() {
        return tableId;
    }

    public void setTableId(TableId tableId) {
        this.tableId = tableId;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

}
