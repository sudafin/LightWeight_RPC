package com.huangdada.nettyrpc.coder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import com.huangdada.nettyrpc.serializer.Serializer;

//使用Netty框架的编码类来编码消息
public class RpcEncoder extends MessageToByteEncoder {
    private final Class<?> aClass;
    private final Serializer serializer;
    public RpcEncoder(Class<?> aClass, Serializer serializer){
        this.aClass = aClass;
        this.serializer = serializer;
    }

    //编码: 将序列化二进制的消息对象转为netty框架内中网络传输的格式ByteBuf,用来高效,灵活处理二进制数据
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
        //如果得到的类不为空且是属于该对象
        if(aClass != null && aClass.isInstance(o)){
            //序列化对象
            byte[] byteObject = serializer.serialize(o);
            //这里用Unpooled可以一次性传输,适用于数据小的
//            Unpooled.copiedBuffer(byteObject);
            //可以分段传输,适合数据量大的
            byteBuf.writeInt(byteObject.length); // 可以设定要分段的字节大小,这里使用int类型所以一次写入4个字节
            byteBuf.writeBytes(byteObject); //写入实际内容
            /*
             // 定义分段大小 (例如1024字节)
             int CHUNK_SIZE = 1024;

             // 获取数据总长度
             int totalLength = byteObject.length;
             int offset = 0; // 当前分段传输的位置

             // 当还有未传输的数据时，继续分段传输
             while (offset < totalLength) {
             // 计算本次要传输的长度
             int remainingLength = totalLength - offset; // 剩余未传输的数据长度
             int sendLength = Math.min(remainingLength, CHUNK_SIZE); // 每次最多发送 CHUNK_SIZE 字节

             // 写入本段数据长度到 ByteBuf
             byteBuf.writeInt(sendLength); // 写入本次要传输的数据长度
             // 写入实际的数据片段
             byteBuf.writeBytes(byteObject, offset, sendLength);

             // 更新偏移量，准备发送下一个片段
             offset += sendLength;
             }

             // 将已写入的 ByteBuf 发送到客户端
             channelHandlerContext.writeAndFlush(byteBuf);
             */
        }
    }
}
