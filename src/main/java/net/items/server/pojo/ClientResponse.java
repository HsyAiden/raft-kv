package net.items.server.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @ Author Hsy
 * @ Date 2023/04/02
 * @ describe 客户端响应信息
 **/
@Getter
@Setter
@ToString
public class ClientResponse {

    /**
     * 响应状态码
     * 0 -- ok
     * -1 -- fail
     * 1 -- redirect
     */
    private int code;


    private Object result;

    public ClientResponse(int code, Object result) {
        this.code = code;
        this.result = result;
    }

    public static ClientResponse ok() {
        return new ClientResponse(0, null);
    }

    public static ClientResponse ok(Object result) {
        return new ClientResponse(0, result);
    }

    public static ClientResponse fail(Object result) {
        return new ClientResponse(-1, result);
    }
}
