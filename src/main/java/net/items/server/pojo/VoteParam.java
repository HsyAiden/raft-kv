package net.items.server.pojo;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * @ Author Hsy
 * @ Date 2023/04/02
 * @ describe
 **/
@Getter
@Setter
@Builder
@Data
public class VoteParam {

    /**
     * node的任期
     */
    private long term;


//    private String peerAddr;

    /**
     * 候选人，发起投票的node
     */
    private String candidateAddr;

    /**
     * 最后一个日志的索引
     */
    private long lastLogIndex;

    /**
     * 最后一个日志的任期
     */
    private long lastLogTerm;

}
