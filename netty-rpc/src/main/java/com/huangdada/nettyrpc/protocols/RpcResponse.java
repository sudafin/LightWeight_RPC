package com.huangdada.nettyrpc.protocols;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class RpcResponse {
    //响应的对象id,用来判断与服务端是否是同一个请求
    private String requestId;

    //错误信息
    private String error;

    //返回的结果
    private Object result;
}
