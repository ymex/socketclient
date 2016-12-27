package cn.ymex.cute.socket.netty;

/**
 * Copyright (c) ymexc(www.ymex.cn)
 * Email:ymex@foxmail.com
 * date 2016/12/27
 *
 * @author ymexc
 */
public interface ConnectStatus {
    int PREPARE = 0x11; //准备状态
    int WAITING_LINK = 0x22; //连接中
    int WAITING_BREAK = 0x23;//断开中

    int BREAK = 0x44; //服务器断开
    int DISSCONNECT = 0x45;//本地主动断开

    int SUCCESS = 0x33;
    int FAILED = 0x55;
}
