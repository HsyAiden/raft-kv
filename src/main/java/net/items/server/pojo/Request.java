package net.items.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

/**
 * @ Author Hsy
 * @ Date 2023/04/02
 * @ describe RPC请求体
 **/
@Getter
@Setter
@AllArgsConstructor
@Builder
public class Request {

    /**
     * 请求投票
     */
    public static final int R_VOTE = 0;
    /**
     * 附加日志
     */
    public static final int A_ENTRIES = 1;
    /**
     * 客户端
     */
    public static final int CLIENT_REQ = 2;
    /**
     * 配置变更. add
     */
    public static final int CHANGE_CONFIG_ADD = 3;
    /**
     * 配置变更. remove
     */
    public static final int CHANGE_CONFIG_REMOVE = 4;

    /**
     * 请求类型
     */
    private int cmd = -1;

    /**
     * 附带请求参数
     */
    private Object obj;

    /**
     * 目标地址
     */
    private String url;

}
