package cn.ymex.cute.socket;

/**
 * Created by ymexc on 2016/8/10.
 */
public class Status {//socketclient 状态
    public static final int CONNECT_PREPARE = 0x11; //socket开始连接
    public static final int CONNECT_WAITING = 0x22; //socket 连接中
    public static final int CONNECT_SUCCESS = 0x33;//socket 连接成功
    public static final int CONNECT_BREAK = 0x44;//socket 连接断开
    public static final int CONNECT_FAILED = 0x55; //socket 连接失败
    public static final int DIS_CONNECT = 0x66;// 主动断开连接
    public static final int MESSAGE_RECONNECT = 0x77;// 重新连接
}
