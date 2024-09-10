package com.huangdada.nettyrpc.nettyServer;

import com.huangdada.nettyrpc.protocols.RpcRequest;
import com.huangdada.nettyrpc.protocols.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.InvocationTargetException;

@Slf4j
//SimpleChannelInboundHandler简化了ChannelInboundHandler,入站操作方法类
/*
   当前客户端传过来的信息即HelloService, 我们使用它的实现类的方法,且它的实现类是被Spring管理的,所以需要用到Spring容器的上下文来获取它的实现类
    ApplicationContextAware 是 Spring 框架中的一个接口，用于获取 Spring 应用上下文。
    实现这个接口的类可以在 Spring 容器中被注入 ApplicationContext，从而获取到 Spring 管理的 Bean 或者其他资源。
 */
public class ServerHandler extends SimpleChannelInboundHandler<RpcRequest> implements ApplicationContextAware {
    private ApplicationContext applicationContext;

    //当客户端连接服务端时, 服务端触发入站操作, 即Read,此时自动触发入站处理器,如解码器
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcRequest rpcRequest) throws Exception {
        //需要返回响应对象
        RpcResponse rpcResponse = new RpcResponse();
        //将响应对象的请求id设置成当前传过来的请求id,保持一致后面客户端才知道是同一个请求返回的响应结果
        rpcResponse.setRequestId(rpcRequest.getRequestId());
        //处理信息
        try{
            //处理该rpcRequest中的HelloService的相关信息并做增强
            Object handlerMsg = handler(rpcRequest);
            log.info("查看的获取返回结果: {}", handlerMsg);
            //将处理好的数据包装在响应结果中传给客户端
            rpcResponse.setResult(handlerMsg);
        }catch (Throwable throwable){
            //如果出错就将出错的结果包装在响应结果传给客户端
            rpcResponse.setError(throwable.toString());
            log.error("rpcResponse设置错误信息: {}", throwable.getMessage());
        }
        //将响应结果传给客户端,执行出站操作,自动触发出站处理器,包括编码器
        channelHandlerContext.writeAndFlush(rpcResponse);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        //这里我们要不能拿nettyServer和serverHandler这两个bean,因为它们循环依赖且,且该类是ServerHandler类,没有初始化完不能拿到该bean的信息
        log.info("传过来的上下文信息:{}\n",applicationContext.getBean("helloServiceImpl"));
        this.applicationContext = applicationContext;
    }


    private Object handler(RpcRequest rpcRequest) throws ClassNotFoundException, InvocationTargetException {
        // 通过 forName 和 getClassName 获取HelloService类的基本信息
        Class<?> aClass = Class.forName(rpcRequest.getClassName()); //全限定名: interface com.huangdada.nettyrpcserver.HelloService
        String methodName = rpcRequest.getMethodName(); // 获取方法 hello
        Class<?>[] parameterTypes = rpcRequest.getParameterTypes(); // 获取类型class java.lang.String
        Object[] parameterValues = rpcRequest.getParameterValues(); //获取值zeolite
        log.info("'\n'aClass:{} '\n' methodName:{}'\n' parameterTypes:{}'\n' parameterValues: {}", aClass,methodName, parameterTypes, parameterValues);

        // 通过类名从 Spring 容器中获取目标服务类的 Bean 实例
        /*
            它会得到HelloServiceImpl, 因为getBean会找Spring中的是aClass也就是HelloService,但找不到这个接口因为它并没有Spring管理
            但getBean会继续找这个类的实现和继承类,查看这些子类是否有被Bean管理,有就返回这些实现实例
            如果只有一个实现类/继承类,那么getBean就直接返回一个
            如果有多个实现类/继承类且只需要返回想要的一个,要么指定实现类的名字,要么使用@Qualified(BeanName)注解,指定BeanName或者使用@Primary注解来使用,将注释放在需要被使用的实现类上面
         */
        Object serviceBean = applicationContext.getBean(aClass);
        log.info("serviceBean信息: {}", serviceBean);

        // 获取该 Bean 的 Class 信息,也就是HelloServiceImpl的信息
        Class<?> serviceBeanClass = serviceBean.getClass();
        log.info("serviceBeanClass信息:{}", serviceBeanClass);
        /*
        使用动态代理来增强,且使用CGLIB动态代理,因为这里我们是跟外部的类进行代理所以没有接口所以只能使用CGLIB
        为什么使用动态代理而不直接用 serviceBeanClass 直接增强 , 原因是反射性能消耗所以 CGLIB 库中提供的 FastClass 机制优化了代理方法调用。
        通过生成方法索引，可以通过索引直接调用方法，从而避免反射调用带来的性能损耗。
        */
        //根据HelloServiceImplClass创建代理类,这里反射得到了HelloServiceImpl
        FastClass fastClass = FastClass.create(serviceBeanClass);
        log.info("fastClass.getName()信息: {}", fastClass.getName());
        // 根据方法名和参数类型获取对应的HelloServiceImpl的方法hello
        FastMethod fastMethod = fastClass.getMethod(methodName, parameterTypes);
        log.info("fastMethod.getName()信息: {}", fastMethod.getName());
        log.info("开始调用 CGLIB 动态代理执行服务端方法...");
        // 通过 FastMethod 调用hello方法,参数是该方法的执行对象HelloServiceImpl和该方法的参数
        return fastMethod.invoke(serviceBean, parameterValues);
    }
}
