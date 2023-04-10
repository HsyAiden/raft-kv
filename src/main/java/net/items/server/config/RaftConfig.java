package net.items.server.config;

import java.util.ArrayList;
import java.util.List;

/**
 * @ Author Hsy
 * @ Date 2023/04/01
 * @ describe
 **/
public class RaftConfig {
    /**
     * 心跳检测间隔时间
     */
    public static final int heartBeatInterval = 300;

    /**
     * 选举触发时间
     * 在该时间段内未收到心跳检测的节点将发起选举
     */
    public static final int electionTimeout = 3 * 1000;

    /**
     * 存储其他节点的地址
     */
    private static List<String> addrs = new ArrayList<>();

    public static List<String> getAddrs() {
        return addrs;
    }

}
