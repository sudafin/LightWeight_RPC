package com.huangdada.nettyrpcserver;

import com.huangdada.nettyrpc.nettyServer.NettyServer;
import com.huangdada.nettyrpc.nettyServer.ServerHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@Slf4j
public class NettyRpcServerApplicationContext {

    @Value("${netty.rpc.server.port}")
    private Integer port;
 
    public static void main(String[] args) throws Exception {
        SpringApplication.run(NettyRpcServerApplicationContext.class, args);
        log.info("服务端启动成功");
    }

    //启动类启动下面这两个Bean对象也会依次创建,在创建中NettyServer对象时会先创建
    //再创建中发现需要ServerHandler对象从而先创建出了ServerHandler bean对象
    //后面再继续创建NettyServer创建对象的过程
    @Bean
    public NettyServer nettyServer() {
        NettyServer nettyServer = new NettyServer(serverHandler(), port);
        log.info("NettyServer bean....启动");
        return nettyServer;
    }
 
    @Bean
    public ServerHandler serverHandler() {
        ServerHandler serverHandler = new ServerHandler();
        log.info("ServerHandler bean....启动");
        return serverHandler;
    }
}