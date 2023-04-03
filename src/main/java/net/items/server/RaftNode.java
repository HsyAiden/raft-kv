package net.items.server;

import net.items.server.constant.NodeStatus;
import net.items.server.db.StateMachine;
import net.items.server.log.LogModule;
import net.items.server.rpc.RpcClient;
import net.items.server.rpc.RpcServer;

import java.util.List;
import java.util.Map;


/**
 * @ Author Hsy
 * @ Date 2023/04/02
 * @ describe 集群节点
 **/
public class RaftNode {

    /**
     * 心跳间隔时间
     */
    private int heatBeatInterval;

    /**
     * 丛节点超时选举时间
     */
    private int electionTimeout;

    /**
     * 集群状态
     */
    private NodeStatus status;

    /**
     * 领导者地址
     */
    private String leader;

    /**
     * 选举期间，投票给谁
     */
    private String voteFor;

    /**
     * Node的任期
     */
    private int term;

    /**
     * 上一次接收到主节点的心跳时间
     */
    private int preHeatBeatTime;

    /**
     * 上一次发生选举的时间
     */
    private int preElectionTime;

    /**
     * 集群其它节点地址，格式："ip:port"
     */
    private List<String> peerAddrs;

    /**
     * 对于每一个服务器，需要发送给他的下一个日志条目的索引值
     */
    Map<String, Long> nextIndexes;

    /**
     * 当前节点地址
     */
    private String myAddr;

    /**
     * 日志模块
     */
    LogModule logModule;

    /**
     * 状态机
     */
    StateMachine stateMachine;

    /**
     * RPC 客户端
     */
    private RpcClient rpcClient;

    /**
     * RPC 服务端
     */
    private RpcServer rpcServer;

    public RaftNode() {

    }

}
