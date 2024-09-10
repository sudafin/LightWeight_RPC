package com.huangdada.nettyrpc.protocols;

import lombok.Data;
import lombok.ToString;

//通信协议的请求协议
@Data
@ToString
public class RpcRequest{
    //请求对象的Id, 客户端用于验证服务器的请求和响应是否相匹配,这里是两个requestId是否相等
    private String requestId;

    //该请求方法的对象类名
    private String className;

    //该请求方法的方法名
    private String methodName;

    //该请求方法的参数类型,因为传过来的数据可能是动态代理的,所以不知道具体的类型所以用反射来获取,Object无法确定参数类型
    private Class<?> [] parameterTypes;

    //该请求方法的参数数据, 用Object来接收, 因为传过来的数据是确定的,且Object是所有类的父类,可以直接接收
    private Object[] parameterValues;
}