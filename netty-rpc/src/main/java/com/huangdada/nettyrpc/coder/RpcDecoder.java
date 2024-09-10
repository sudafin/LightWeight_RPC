package com.huangdada.nettyrpc.coder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import com.huangdada.nettyrpc.serializer.Serializer;

import java.util.List;

//用Netty框架的解码类来接码消息对象
public class RpcDecoder extends ByteToMessageDecoder {
    private final Class<?> aClass;
    private final Serializer serializer;

    public RpcDecoder(Class<?> aClass, Serializer serializer) {
        this.aClass = aClass;
        this.serializer = serializer;
    }


    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if(byteBuf.readableBytes() < 4){
            //如果 ByteBuf 中的可读字节数小于4字节，说明还没有足够的数据来读取数据长度, 因为读取数据长度需要4字节（int 类型),在编码的时候定义的方法
            return; // 返回，等待更多数据到达
        }
        //标记当前读的位置,以防后续有数据来时重新从0开始读数据
        byteBuf.markReaderIndex();
        //读取接下来的 4 个字节作为数据长度，dataLength 表示接下来要读取的字节数。这个长度是在编码时写入的，用于告知解码器接下来的数据长度。
        int dataLength = byteBuf.readInt();
        if(byteBuf.readableBytes() < dataLength){
            // 如果 ByteBuf 中的可读字节数小于 dataLength，说明数据不完整
            // 还需要更多数据来完成当前的数据段
            byteBuf.resetReaderIndex(); // 重置读索引到标记点
            return; // 返回，等待更多数据到达
        }
        //如果数据刚好,创建一个字节数组 data，大小为 dataLength
        byte[] data = new byte[dataLength];
        //然后从 ByteBuf 中读取 dataLength 字节的数据到 data 数组中。完成从 ByteBuf 中提取数据的过程
        byteBuf.readBytes(data);
        //将字节数组反序列化为对象
        Object object = serializer.deserialize(aClass, data);
        //将解码后的对象添加到列表中
        list.add(object);
    }
}
