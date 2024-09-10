package com.huangdada.nettyrpc.nettyClient;

import com.huangdada.nettyrpc.coder.RpcDecoder;
import com.huangdada.nettyrpc.coder.RpcEncoder;
import com.huangdada.nettyrpc.protocols.RpcResponse;
import com.huangdada.nettyrpc.serializer.JSONSerializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import com.huangdada.nettyrpc.protocols.RpcRequest;

import javax.annotation.PreDestroy;

@Slf4j
public class NettyClient {
    private EventLoopGroup workGroup;
    private Channel channel;
    private ClientHandler clientHandler;
    private final String host;
    private final Integer port;
    public NettyClient(String host, Integer port){
        this.host = host;
        this.port = port;
    }
    public void connect() throws InterruptedException {
        clientHandler =new ClientHandler();
        workGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                //启用Nagle算法。Nagle算法通过将小的数据块组合成更大的数据块来减少网络传输，从而提高效率
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        //不管是服务端和客户端, 编码器和解码器必须顺序在首位, 然后才到具体的任务处理器
                        //虽然调用时根据进站和出站分处理器, 但是顺序是一定的,如果出站把任务处理器放在前面的话,那么它会执行任务处理器,再执行编码,这是不允许
                        //因为只有先编码我们才能进行网络传输

                        //添加解码器,处理来自网络的入站字节流,发送请求是不会进行解码的
                        pipeline.addLast(new RpcDecoder(RpcResponse.class, new JSONSerializer()));
                        //添加编码器,处理出站发送到网络的出站对象
                        pipeline.addLast(new RpcEncoder(RpcRequest.class, new JSONSerializer()));

                        //添加任务处理器
                        pipeline.addLast(clientHandler);
                    }
                });
        //需要用同步,不能用默认的异步
        ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
        //获取SocketChannel
        channel = channelFuture.channel();
        channelFuture.addListener((ChannelFutureListener)future ->{
            if(future.isSuccess()){
                log.info("连接成功远程地址为:  {}" , future.channel().remoteAddress() );
            }else log.error("连接失败");
        });

    }
    //发送请求给服务端
    public RpcResponse send(final RpcRequest rpcRequest){
        try {
            //请求发送处理,这里将rpcRequest放到 ChannelPipeline管道后会触发管道处理器中的所有有关出站的类,如客户端的解码器和客户端任务处理器(这里是继承了两端结合所以说方法,如果不说方法可以直接继承 ChannelInboundHandler)中的write方法
            //知识点: 连接默认触发的是入站的处理器,如 ChannelOutboundHandler中Read, ReadComplete和ChannelActive ,这些入站的方法
            //          只要手动write才会触发出战的处理器
            channel.writeAndFlush(rpcRequest).await();  //await表示只有管道有关出站的处理器执行完才能往下走
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //得到客户端处理器已经处理过的获得的响应
        return clientHandler.getRpcResponse(rpcRequest.getRequestId());
    }
    //java自带的注解,结束执行方法
    @PreDestroy
    public void close(){
        workGroup.shutdownGracefully();
        channel.closeFuture().syncUninterruptibly();
    }
}
