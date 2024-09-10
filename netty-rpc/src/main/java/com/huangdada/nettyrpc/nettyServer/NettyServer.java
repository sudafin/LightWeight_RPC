package com.huangdada.nettyrpc.nettyServer;

import com.huangdada.nettyrpc.coder.RpcDecoder;
import com.huangdada.nettyrpc.coder.RpcEncoder;
import com.huangdada.nettyrpc.protocols.RpcResponse;
import com.huangdada.nettyrpc.serializer.JSONSerializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import com.huangdada.nettyrpc.protocols.RpcRequest;

import javax.annotation.PreDestroy;
import java.util.Objects;

@Slf4j
/*
    InitializingBean 接口的作用是让你在 Spring 容器初始化该类的 Bean 后，
     执行自定义的初始化逻辑。实现了该接口后，Spring 会自动调用 afterPropertiesSet() 方法。
 */
public class NettyServer implements InitializingBean {
    private final ServerHandler serverHandler;
 
    private EventLoopGroup boss;
    private EventLoopGroup worker;
 
    private final Integer serverPort;
 
    public NettyServer(ServerHandler serverHandler, Integer serverPort) {
        this.serverHandler = serverHandler;
        this.serverPort = serverPort;
    }
 
    @Override
    //nettyServer 的bean依赖注入执行完后自动执行
    public void afterPropertiesSet() throws Exception {
        //使用zookeeper做注册中心，本文不涉及，可忽略
        ServiceRegistry registry = null;
        if (Objects.nonNull(serverPort)) {
            start(registry);
        }
    }
 
    public void start(ServiceRegistry registry) throws Exception {
        //负责处理客户端连接的线程池
        boss = new NioEventLoopGroup();
        //负责处理读写操作的线程池
        worker = new NioEventLoopGroup();
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        //添加编码器
                        pipeline.addLast(new RpcEncoder(RpcResponse.class, new JSONSerializer()));
                        //添加解码器
                        pipeline.addLast(new RpcDecoder(RpcRequest.class, new JSONSerializer()));
                        //添加请求处理器
                        pipeline.addLast(serverHandler);
                    }
                });
        bind(serverBootstrap, serverPort);
    }
 
    /**
     * 如果端口绑定失败，端口数+1，重新绑定
     */
    public void bind(final ServerBootstrap serverBootstrap, int port) {
        serverBootstrap.bind(port).addListener(future -> {
            if (future.isSuccess()) {
                log.info("端口[ {} ] 绑定成功", port);
            } else {
                log.error("端口[ {} ] 绑定失败", port);
                bind(serverBootstrap, port + 1);
            }
        });
    }
 
    @PreDestroy
    public void close() throws InterruptedException {
        boss.shutdownGracefully().sync();
        worker.shutdownGracefully().sync();
        log.info("关闭Netty");
    }
}