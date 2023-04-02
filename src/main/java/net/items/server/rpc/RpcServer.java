package net.items.server.rpc;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.protocol.SyncUserProcessor;
import lombok.extern.slf4j.Slf4j;
import net.items.server.RaftNode;
import net.items.server.pojo.Request;

/**
 * @ Author Hsy
 * @ Date 2023/04/02
 * @ describe
 **/
@Slf4j
public class RpcServer {

    private final RaftNode node;

    private final com.alipay.remoting.rpc.RpcServer SERVER;

    public RpcServer(int port, RaftNode node) {

        // 初始化rpc服务端
        SERVER = new com.alipay.remoting.rpc.RpcServer(port, false, false);

        // 实现用户请求处理器
        SERVER.registerUserProcessor(new SyncUserProcessor<Request>() {

            @Override
            public Object handleRequest(BizContext bizContext, Request request) throws Exception {
                return null;
            }

            @Override
            public String interest() {
                return Request.class.getName();
            }
        });
        this.node = node;
        SERVER.startup();
    }


    public void destroy() {
        log.info("destroy success");
    }
}
