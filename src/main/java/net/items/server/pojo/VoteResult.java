package net.items.server.pojo;

import lombok.Getter;
import lombok.Setter;

/**
 * @ Author Hsy
 * @ Date 2023/04/02
 * @ describe
 **/
@Setter
@Getter
public class VoteResult {

    /**
     * 当前任期号
     */
    private int term;

    /**
     * 是否统一投票
     */
    private boolean voteGranted;


}
