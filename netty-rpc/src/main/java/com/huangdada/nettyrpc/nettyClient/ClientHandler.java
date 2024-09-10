package com.huangdada.nettyrpc.nettyClient;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import com.huangdada.nettyrpc.protocols.RpcRequest;
import com.huangdada.nettyrpc.protocols.RpcResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// ChannelDuplexHandler 结合了ChannelInboundHandler入站和ChannelOutboundHandler出站两个类的方法
public class ClientHandler extends ChannelDuplexHandler {

     // 使用Map维护请求对象ID与响应结果Future的映射关系
    private final Map<String ,DefaultFuture> futureMap = new ConcurrentHashMap<>();

    //将请求id和响应Future再发送前写入再Map中,这是 ChannelOutboundHandler出站的方法,这里是先触发了send方法中WriteAndFlush把RpcRequest放到中
    //它会处理器顺是先触发编码器 - write - 网络层运输
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof RpcRequest){
            RpcRequest rpcRequest = (RpcRequest)msg;
            //发送请求对象之前，先把请求ID保存下来，并构建一个与自定义响应Future的映射关系
            futureMap.putIfAbsent(rpcRequest.getRequestId(), new DefaultFuture());
        }
        super.write(ctx, msg, promise);
    }
    //入站方法, 获取服务端发送过来的消息数据
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        //判断这个消息数据是否是RpcResponse
        if(msg instanceof RpcResponse){
            RpcResponse rpcResponse =(RpcResponse)msg;
            //通过响应中的requestId拿到对应自定义异步响应结果DefaultFuture
            DefaultFuture future = futureMap.get(rpcResponse.getRequestId());
            //将响应结果结果写入DefaultFuture
            future.setRpcResponse(rpcResponse);
        }
        super.channelRead(ctx, msg);
    }


    public RpcResponse getRpcResponse(String requestId) {
        try {
            DefaultFuture future = futureMap.get(requestId);
            return future.getRpcResponse(10);
        }finally {
            //获取成功以后，从map中移除
            futureMap.remove(requestId);
        }
    }
}
