package com.huangdada.nettyrpc.nettyClient;

import com.huangdada.nettyrpc.protocols.RpcResponse;

public class DefaultFuture {
    private RpcResponse rpcResponse;
    private volatile boolean isSucceed = false;
    private final Object object = new Object();

    public RpcResponse getRpcResponse(int timeout) {
        //设置锁, 同步设置, 防止多线程导致的并发问题
        synchronized (object){
            //拿到锁的线程是否已经成功, 如果成功就直接返回该对象, 如果没有就等待时间, 因为可能这个rpcResponse还在入站,需要等待
            while (!isSucceed){
                try {
                    object.wait(timeout);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return rpcResponse;
        }
    }

    public void setRpcResponse(RpcResponse rpcResponse) {
        //防止其他线程并发设置, 如果已经成功了直接结束方法
        if(isSucceed){
            return;
        }
        //如果没有成功,, 就进去同步锁
        synchronized (object){
            this.rpcResponse = rpcResponse;
            this.isSucceed = true;
            //如果现在有线程在获取资源, 需要唤醒该对象锁
            object.notify();
        }
    }
}
