package com.netease.cns.southbound.ovsdb.event;

/**
 * Created by hzzhangdongya on 16-6-14.
 */
public interface OVSDBChangeListener {
    // TODO:
    // 1. notify ovsdb
    // 2. notify port ofport changes (seems we do not need to handle port add before ofport obtained)
    // 3. notify port removal
}
