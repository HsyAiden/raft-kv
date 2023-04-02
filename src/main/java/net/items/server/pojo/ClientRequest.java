package net.items.server.pojo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @ Author Hsy
 * @ Date 2023/04/02
 * @ describe 客户端请求信息
 **/
@Getter
@Setter
@ToString
@Builder
public class ClientRequest implements Serializable {

    public static int PUT = 0;

    public static int GET = 1;

    public static int DEL = 2;

    /**
     * 请求类型
     */
    private int type;

    private String key;

    private String value;

    /**
     *  请求唯一 id,保证幂等性
     */
    private String requestId;
}
