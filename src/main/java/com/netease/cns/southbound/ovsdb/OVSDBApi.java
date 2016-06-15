package com.netease.cns.southbound.ovsdb;

/**
 * Created by hzzhangdongya on 16-6-15.
 */
public interface OVSDBApi {
    // TODO:
    // define agent API here.
    // 1. bridge add/delete/query(by-name), should return a temporary object to keep cache internal.
    // 2. port add/delete/query(by-name).
    // transaction should be done transparently, agent does not need to know transaction exsists.
}
