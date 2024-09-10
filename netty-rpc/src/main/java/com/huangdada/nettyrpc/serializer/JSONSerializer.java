package com.huangdada.nettyrpc.serializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;

public class JSONSerializer implements Serializer{
 
    @Override
    public byte[] serialize(Object object) {
        //使用阿里的fastjson来序列化消息对象
        return JSON.toJSONBytes(object);
    }
 
    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        //用阿里的fastjson来反序列消息对象
        return JSON.parseObject(bytes, clazz, JSONReader.Feature.SupportClassForName);
    }
}