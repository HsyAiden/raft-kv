package net.items.server.rpc;

import com.alipay.remoting.exception.RemotingException;
import lombok.extern.slf4j.Slf4j;
import net.items.server.pojo.Request;
import net.items.server.pojo.Response;

/**
 * @ Author Hsy
 * @ Date 2023/04/02
 * @ describe
 **/
@Slf4j
public class RpcClient {

    private com.alipay.remoting.rpc.RpcClient CLIENT;

    public RpcClient(){
        CLIENT = new com.alipay.remoting.rpc.RpcClient();
        CLIENT.startup();
    }

    public <R> R send(Request request) throws RemotingException, InterruptedException {
        return send(request, 100);
    }

    public <R> R send(Request request, int timeout) throws RemotingException, InterruptedException {
        Response<R> result;
        result = (Response<R>) CLIENT.invokeSync(request.getUrl(), request, timeout);
        return result.getResult();
    }

    public void destroy() {
        CLIENT.shutdown();
        log.info("destroy success");
    }
}
