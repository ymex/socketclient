package cn.ymex.cute.socket.netty;

import io.netty.buffer.ByteBuf;

/**
 * Copyright (c) ymexc(www.ymex.cn)
 * Email:ymex@foxmail.com
 * date 2016/12/23
 *
 * @author ymexc
 */
public class SocketListener {
    /**
     * 数据接收监听
     */
    public interface OnDataReceiveListener {
        void onDataReceive(ByteBuf baseMsg);
    }

    /**
     * SOCKET连接状态监听
     */
    public interface OnConnectStatusListener {
        void onConnected();

        void onDisconnected();

        void onConnectFailed();
    }
}
