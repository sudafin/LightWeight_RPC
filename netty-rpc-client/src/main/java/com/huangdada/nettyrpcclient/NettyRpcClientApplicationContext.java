package com.huangdada.nettyrpcclient;

import com.huangdada.nettyrpc.proxy.RpcClientDynamicProxy;
import com.huangdada.nettyrpcserver.HelloService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class NettyRpcClientApplicationContext {
    public static void main(String[] args) throws Exception {
        //客户端等待服务端启动
        int countdown = 5;  // 倒计时 5 秒
        System.out.println("等待服务端启动...");

        for (int i = countdown; i > 0; i--) {
            System.out.println("客户端还剩 " + i + " s启动...");
            Thread.sleep(1000);  // 每次延时 1 秒
        }
        log.info("客户端启动成功");
        //执行操作
        SpringApplication.run(NettyRpcClientApplicationContext.class, args);
        //这里初始化动态代理,将代理对象设置为HelloService
        HelloService helloService = new RpcClientDynamicProxy<>(HelloService.class, "127.0.0.1", 3663).getProxy();
        //执行动态代理中的invoke方法
        String result = helloService.hello("zeolite");
        log.info("响应结果“: {}", result);
    }
}