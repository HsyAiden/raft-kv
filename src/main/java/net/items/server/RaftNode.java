package net.items.server;

import lombok.extern.slf4j.Slf4j;
import net.items.server.config.RaftConfig;
import net.items.server.constant.NodeStatus;
import net.items.server.db.StateMachine;
import net.items.server.log.LogEntry;
import net.items.server.log.LogModule;
import net.items.server.pojo.*;
import net.items.server.rpc.RpcClient;
import net.items.server.rpc.RpcServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static net.items.server.constant.NodeStatus.*;


/**
 * @ Author Hsy
 * @ Date 2023/04/02
 * @ describe 集群节点
 **/
@Slf4j
public class RaftNode {

    /**
     * 心跳间隔时间
     */
    private int heartBeatInterval;

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
    private Long term;

    private volatile String votedFor;

    /**
     * 上一次接收到主节点的心跳时间
     */
    private Long preHeartBeatTime;

    /**
     * 上一次发生选举的时间
     */
    private Long preElectionTime;

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


    /** 线程池 */
    private ScheduledExecutorService ss;
    private ThreadPoolExecutor te;

    /** 定时任务 */
    HeartBeatTask heartBeatTask;
    ElectionTask electionTask;

    ScheduledFuture<?> heartBeatFuture;

    /**
     * RPC 客户端
     */
    private RpcClient rpcClient;

    /**
     * RPC 服务端
     */
    private RpcServer rpcServer;

    private boolean leaderInitializing;

    public RaftNode() {
        logModule = LogModule.getInstance();
        stateMachine = StateMachine.getInstance();
        setConfig();
        threadPoolInit();
        log.info("Raft node started successfully. The current term is {}", term);
    }

    private void setConfig() {
        heartBeatInterval = RaftConfig.heartBeatInterval;
        electionTimeout = RaftConfig.electionTimeout;
        updatePreElectionTime();
        preHeartBeatTime = System.currentTimeMillis();
        status = FOLLOWER;
        String port = System.getProperty("server.port");
        myAddr = "localhost:" + port;
        rpcServer = new RpcServer(Integer.parseInt(port), this);
        rpcClient = new RpcClient();
        peerAddrs = RaftConfig.getAddrs();
        peerAddrs.remove(myAddr);
        LogEntry last = logModule.getLast();
        if (last != null){
            term = last.getTerm();
        }
    }

    private void updatePreElectionTime() {
        preElectionTime = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(20) * 100;
    }

    private void threadPoolInit() {

        int cup = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = cup * 2;
        final int queueSize = 1024;
        final long keepTime = 1000 * 60;

        ss = new ScheduledThreadPoolExecutor(cup);
        te = new ThreadPoolExecutor(
                cup,
                maxPoolSize,
                keepTime,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(queueSize));
        heartBeatTask = new HeartBeatTask();
        electionTask = new ElectionTask();
        ss.scheduleAtFixedRate(electionTask, 3000, 100, TimeUnit.MILLISECONDS);

    }

    class ElectionTask implements Runnable{
        @Override
        public void run() {

            // leader状态下不允许发起选举
            if (status == LEADER){
                return;
            }

            // 判断是否超过选举超时时间
            long current = System.currentTimeMillis();
            if (current - preElectionTime < electionTimeout) {
                return;
            }

            status = CANDIDATE;
            // leader = "";
            term++;
            votedFor = myAddr;
            log.info("node become CANDIDATE and start election, its term : [{}], LastEntry : [{}]",
                    term, logModule.getLast());

            ArrayList<Future<VoteResult>> futureList = new ArrayList<>();

            // 计数器
            CountDownLatch latch = new CountDownLatch(peerAddrs.size());

            // 发送投票请求
            for (String peer : peerAddrs) {
                // 执行rpc调用并加入list；添加的是submit的返回值
                futureList.add(te.submit(() -> {
                    long lastLogTerm = 0L;
                    long lastLogIndex = 0L;
                    LogEntry lastLog = logModule.getLast();
                    if (lastLog != null) {
                        lastLogTerm = lastLog.getTerm();
                        lastLogIndex = lastLog.getIndex();
                    }

                    // 封装请求体
                    VoteParam voteParam = VoteParam.builder().
                            term(term).
                            candidateAddr(myAddr).
                            lastLogIndex(lastLogIndex).
                            lastLogTerm(lastLogTerm).
                            build();

                    Request request = Request.builder()
                            .cmd(Request.R_VOTE)
                            .obj(voteParam)
                            .url(peer)
                            .build();

                    try {
                        // rpc 调用
                        return rpcClient.send(request);
                    } catch (Exception e) {
                        log.error("ElectionTask RPC Fail , URL : " + request.getUrl());
                        return null;
                    } finally {
                        latch.countDown();
                    }
                }));
            }

            try {
                // 等待子线程完成选票统计
                latch.await(3000, MILLISECONDS);
            } catch (InterruptedException e) {
                log.warn("election task interrupted by main thread");
            }

            // 统计赞同票的数量
            int votes = 0;

            // 获取结果
            for (Future<VoteResult> future : futureList) {
                try {
                    VoteResult result = null;
                    if (future.isDone()){
                        result = future.get();
                    }
                    if (result == null) {
                        // rpc调用失败或任务超时
                        continue;
                    }

                    if (result.isVoteGranted()) {
                        votes++;
                    } else {
                        // 更新自己的任期
                        long resTerm =result.getTerm();
                        if (resTerm > term) {
                            term = resTerm;
                            status = FOLLOWER;
                        }
                    }
                } catch (Exception e) {
                    log.error("future.get() exception");
                }
            }

            // 如果投票期间有其他服务器发送 appendEntry , 就可能变成 follower
            if (status == FOLLOWER) {
                log.info("election stops with newer term {}", term);
                updatePreElectionTime();
                votedFor = "";
                return;
            }

            // 需要获得超过半数节点的投票
            if (votes * 2 >= peerAddrs.size()) {
                votedFor = "";
                status = LEADER;
                // 启动心跳任务
                heartBeatFuture = ss.scheduleWithFixedDelay(heartBeatTask, 0, heartBeatInterval, TimeUnit.MILLISECONDS);
                // 初始化
                leaderInit();
                log.warn("become leader with {} votes", votes);
            } else {
                // 重新选举
                votedFor = "";
                status = FOLLOWER;
                updatePreElectionTime();
                log.info("node {} election fail, votes count = {} ", myAddr, votes);
            }

        }
    }

    private void leaderInit() {
        leaderInitializing = true;
        nextIndexes = new ConcurrentHashMap<>();
        for (String peer : peerAddrs) {
            nextIndexes.put(peer, logModule.getLastIndex() + 1);
        }

        // no-op 空日志
        LogEntry logEntry = LogEntry.builder()
                .command(null)
                .term(term)
                .build();

        // 写入本地日志并更新logEntry的index
        logModule.write(logEntry);
        log.info("write no-op log success, log index: {}", logEntry.getIndex());

        List<Future<Boolean>> futureList = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(peerAddrs.size());

        //  复制到其他机器
        for (String peer : peerAddrs) {
            // 并行发起 RPC 复制并获取响应
            futureList.add(replication(peer, logEntry, latch));
        }

        try {
            // 等待replicationResult中的线程执行完毕
            latch.await(6000, MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("latch timeout in leaderInit()");
        }

        // 统计日志复制结果
        int success = getReplicationResult(futureList);

        //  响应客户端(成功一半及以上)
        if (success * 2 >= peerAddrs.size()) {
            // 提交旧日志并更新 commit index
            long nextCommit = getCommitIndex() + 1;
            while (nextCommit < logEntry.getIndex() && logModule.read(nextCommit) != null){
                stateMachine.apply(logModule.read(nextCommit));
                nextCommit++;
            }
            setCommitIndex(logEntry.getIndex());
            log.info("no-op successfully commit, log index: {}", logEntry.getIndex());
        } else {
            // 提交失败，删除日志，重新发起选举
            logModule.removeOnStartIndex(logEntry.getIndex());
            log.warn("no-op commit fail, election again");
            status = FOLLOWER;
            votedFor = "";
            updatePreElectionTime();
            stopHeartBeat();

        }
        leaderInitializing = false;
    }

    private void stopHeartBeat() {
        if(heartBeatFuture != null){
            heartBeatFuture.cancel(true);
            heartBeatFuture = null;
        }
    }


    private Future<Boolean> replication(String peer, LogEntry entry, CountDownLatch latch) {
        return te.submit(() -> {

            long start = System.currentTimeMillis();
            long end = start;

            // 1. 封装append entry请求基本参数
            AppendParam appendParam = AppendParam.builder()
                    .leaderId(myAddr)
                    .term(term)
                    .leaderCommit(getCommitIndex())
                    .serverId(peer)
                    .build();

            // 2. 生成日志数组
            Long nextIndex = nextIndexes.get(peer);
            List<LogEntry> logEntryList = new ArrayList<>();
            if (entry.getIndex() >= nextIndex) {
                // 把 nextIndex ~ entry.index 之间的日志都加入list
                for (long i = nextIndex; i <= entry.getIndex(); i++) {
                    LogEntry l = logModule.read(i);
                    if (l != null) {
                        logEntryList.add(l);
                    }
                }
            } else {
                logEntryList.add(entry);
            }

            // 3. 设置preLog相关参数，用于日志匹配
            LogEntry preLog = getPreLog(logEntryList.get(0));
            // preLog不存在时，下述参数会被设为-1
            appendParam.setPreLogTerm(preLog.getTerm());
            appendParam.setPrevLogIndex(preLog.getIndex());

            // 4. 封装RPC请求
            Request request = Request.builder()
                    .cmd(Request.A_ENTRIES)
                    .obj(appendParam)
                    .url(peer)
                    .build();

            // preLog 不匹配时重试；重试超时时间为 5s
            while (end - start < 5 * 1000L) {
                appendParam.setEntries(logEntryList.toArray(new LogEntry[0]));
                try {
                    // 5. 发送RPC请求；同步调用，阻塞直到得到返回值或超时
                    AppendResult result = rpcClient.send(request);
                    if (result == null) {
                        log.error("follower responses with null result, request URL : {} ", peer);
                        latch.countDown();
                        return false;
                    }
                    if (result.isSuccess()) {
                        log.info("append follower entry success, follower=[{}], entry=[{}]", peer, appendParam.getEntries());
                        // 更新索引信息
                        nextIndexes.put(peer, entry.getIndex() + 1);
                        latch.countDown();
                        return true;
                    } else  {
                        // 失败情况1：对方任期比我大，转变成跟随者
                        if (result.getTerm() > term) {
                            log.warn("follower [{}] term [{}], my term = [{}], so I will become follower",
                                    peer, result.getTerm(), term);
                            term = result.getTerm();
                            status = FOLLOWER;
                            stopHeartBeat();
                            latch.countDown();
                            return false;
                        } else {
                            // 失败情况2：preLog不匹配
                            nextIndexes.put(peer, Math.max(nextIndex - 1, 0));
                            log.warn("follower {} nextIndex not match, will reduce nextIndex and retry append, nextIndex : [{}]", peer,
                                    logEntryList.get(0).getIndex());

                            // 更新 preLog 和 logEntryList
                            LogEntry l = logModule.read(logEntryList.get(0).getIndex() - 1);
                            if (l != null){
                                logEntryList.add(0, l);
                            } else {
                                // l == null 说明前一次发送的 preLogIndex 已经来到 -1 的位置，正常情况下应该无条件匹配
                                log.error("log replication from the beginning fail");
                                latch.countDown();
                                return false;
                            }

                            preLog = getPreLog(logEntryList.get(0));
                            appendParam.setPreLogTerm(preLog.getTerm());
                            appendParam.setPrevLogIndex(preLog.getIndex());
                        }
                    }

                    end = System.currentTimeMillis();

                } catch (Exception e) {
                    log.error("Append entry RPC fail, request URL : {} ", peer);
                    latch.countDown();
                    return false;
                }

            }


            // 超时了
            log.error("replication timeout, peer {}", peer);
            latch.countDown();
            return false;
        });

    }



    class HeartBeatTask implements Runnable {

        @Override
        public void run() {
            if (status != LEADER) {
                return;
            }

            long current = System.currentTimeMillis();
            if (current - preHeartBeatTime < heartBeatInterval) {
                return;
            }

            preHeartBeatTime = System.currentTimeMillis();
            AppendParam param = AppendParam.builder()
                    .entries(null)// 心跳,空日志.
                    .leaderId(myAddr)
                    .term(term)
                    .leaderCommit(getCommitIndex())
                    .build();

            List<Future<Boolean>> futureList = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(peerAddrs.size());

            for (String peer : peerAddrs) {
                Request request = new Request(
                        Request.A_ENTRIES,
                        param,
                        peer);

                // 并行发起 RPC 复制并获取响应
                futureList.add(te.submit(() -> {
                    try {
                        AppendResult result = rpcClient.send(request);
                        long resultTerm = result.getTerm();
                        if (resultTerm > term) {
                            log.warn("follow new leader {}", peer);
                            term = resultTerm;
                            votedFor = "";
                            status = FOLLOWER;
                        }
                        latch.countDown();
                        return result.isSuccess();
                    } catch (Exception e) {
                        log.error("heartBeatTask RPC Fail, request URL : {} ", request.getUrl());
                        latch.countDown();
                        return false;
                    }
                }));
            }

            try {
                // 等待任务线程执行完毕
                latch.await(1000, MILLISECONDS);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }

            int success = getReplicationResult(futureList);
        }
    }

    private int getReplicationResult(List<Future<Boolean>> futureList) {
        log.info("node add {} return nums {} ", myAddr, futureList.size());
        int success = 0;
        for (Future<Boolean> future : futureList) {
            if (future.isDone()){
                try {
                    if (future.get()){
                        success++;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.error("future.get() error");
                }
            }
        }
        return success;
    }

    private long getCommitIndex() {
        return stateMachine.getCommit();
    }

    private void setCommitIndex(long index) {
        stateMachine.setCommit(index);
    }

    private LogEntry getPreLog(LogEntry logEntry) {
        LogEntry entry = logModule.read(logEntry.getIndex() - 1);

        if (entry == null) {
            log.info("preLog is null, parameter logEntry : {}", logEntry);
            entry = LogEntry.builder().index(-1L).term(-1).command(null).build();
        }
        return entry;
    }

}
