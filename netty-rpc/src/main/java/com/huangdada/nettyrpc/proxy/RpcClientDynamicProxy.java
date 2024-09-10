package com.huangdada.nettyrpc.proxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.cglib.proxy.Proxy;
import com.huangdada.nettyrpc.nettyClient.NettyClient;
import com.huangdada.nettyrpc.protocols.RpcRequest;
import com.huangdada.nettyrpc.protocols.RpcResponse;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

@Slf4j
//这里客户端用的JDK动态代理, 服务端用CGLIB,因为是客户端是通过接口方式来处理所以可以采取JDK,服务端处理是没用接口所以只能用CGLIB
public class RpcClientDynamicProxy<T> implements InvocationHandler {
    private final Class<T> interfaceClazz;
 
    private final String host;
 
    private final Integer port;
 
    public RpcClientDynamicProxy(Class<T> interfaceClazz, String host, Integer port) {
        this.interfaceClazz = interfaceClazz;
        this.host = host;
        this.port = port;
    }
 
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //设置请求统一协议的参数
        RpcRequest request = new RpcRequest();
        String requestId = UUID.randomUUID().toString();
        String className = method.getDeclaringClass().getName();
        String methodName = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        request.setRequestId(requestId);
        request.setClassName(className);
        request.setMethodName(methodName);
        request.setParameterTypes(parameterTypes);
        request.setParameterValues(args);
        log.info("请求内容: {}", request);
        //开启Netty客户端，直连
        //这里直接指定了server的host和port，正常的RPC框架会从注册中心获取
        NettyClient nettyClient = new NettyClient(host, port);
        log.info("开始连接服务端：{}", new Date());
        nettyClient.connect();
        //调用send方法将请求发送给服务端
        RpcResponse send = nettyClient.send(request);
        log.info("请求调用返回结果：{}", send.getResult());
        //最后将结果return,就能得到hello的结果
        return send.getResult();
    }
 
    @SuppressWarnings("unchecked")
    public T getProxy() {
        return (T) Proxy.newProxyInstance(
                interfaceClazz.getClassLoader(),
                new Class<?>[]{interfaceClazz},
                this
        );
    }

}