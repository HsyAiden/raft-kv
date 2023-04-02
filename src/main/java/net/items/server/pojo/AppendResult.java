package net.items.server.pojo;

/**
 * @ Author Hsy
 * @ Date 2023/04/02
 * @ describe
 **/
public class AppendResult {

    /**
     * 被请求方的任期号，用于领导人去更新自己
     */
    long term;

    /**
     * 跟随者包含了匹配上 prevLogIndex 和 prevLogTerm 的日志时为真
     */
    boolean success;

}
