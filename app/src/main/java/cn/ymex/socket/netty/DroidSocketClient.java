package cn.ymex.socket.netty;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

/**
 * Copyright (c) ymexc(www.ymex.cn)
 * Email:ymex@foxmail.com
 * date 2016/12/23
 *
 * @author ymexc
 */
public class DroidSocketClient {
    private static DroidSocketClient socketClient;

    private final int MESSAGE_INIT = 0x66;
    private final int MESSAGE_CONNECT = 0x67;
    private final int MESSAGE_SEND = 0x68;

    private String host;
    private int port;

    private Bootstrap bootstrap;
    public SocketChannel socketChannel;
    private ClinetAdapterHandler clientAdapterHandler;

    private List<SocketListener.OnDataReceiveListener> onDataReceiveListeners;
    private SocketListener.OnConnectStatusListener onConnectStatusListener;

    private HandlerThread mWorkThread = null;
    private Handler mWorkHandler = null;
    private Handler.Callback mWorkHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_INIT:
                    _initClient();
                    break;

                case MESSAGE_CONNECT://连接
                    _connect();
                    break;

                case MESSAGE_SEND://发送消息

                    break;
            }

            return true;
        }
    };

    public static DroidSocketClient getInstance() {
        if (socketClient == null) {
            socketClient = new DroidSocketClient();
        }
        return socketClient;
    }

    private DroidSocketClient() {
        initWork();
    }


    /**
     * 始化工作线程
     */
    private void initWork() {
        mWorkThread = new HandlerThread("droid_socket_client");
        mWorkThread.start();
        mWorkHandler = new Handler(mWorkThread.getLooper(), mWorkHandlerCallback);
        clientAdapterHandler = new ClinetAdapterHandler();
    }

    /**
     * 建立socket连接
     */
    private void _connect() {
        bootstrap.remoteAddress(host, port);
        try {
            ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port)).sync();
            if (future != null && future.isSuccess()) {//连接成功
                socketChannel = (SocketChannel) future.channel();
                if (onConnectStatusListener != null) {
                    onConnectStatusListener.onDisconnected();
                }
            } else { //连接失败

            }
        } catch (Exception e) {//连接失败

        }
    }

    /**
     * 初始化 client
     */
    private void _initClient() {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.group(eventLoopGroup);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline pipeline = socketChannel.pipeline();
                pipeline.addLast(new IdleStateHandler(20, 10, 0));//心跳设置
                // 服务器和客户端编码解码需要统一不然会报错。
                //pipeline.addLast(new ObjectEncoder());//数据编码
                //pipeline.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));//数据解码解密

                pipeline.addLast(clientAdapterHandler);
            }
        });
    }


    /**
     * @param host
     * @param port
     */
    public void connect(String host, int port) {
        this.host = host;
        this.port = port;
        mWorkHandler.sendEmptyMessage(MESSAGE_CONNECT);
    }

    public void setOnConnectStatusListener(SocketListener.OnConnectStatusListener onConnectStatusListener) {
        this.onConnectStatusListener = onConnectStatusListener;
    }

    public List<SocketListener.OnDataReceiveListener> getOnDataReceiveListeners() {
        if (onDataReceiveListeners == null) {
            onDataReceiveListeners = new ArrayList<>(4);
        }
        return onDataReceiveListeners;
    }


    private class ClinetAdapterHandler extends SimpleChannelInboundHandler<Object> {


        public static final int MIN_CLICK_DELAY_TIME = 1000 * 30; //设置心跳时间  开始
        private long lastClickTime = 0;//设置心跳时间   结束


        /**
         * 在连接被建立并且准备进行通信时被调用。
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {

            super.channelActive(ctx);
        }

        /**
         * 空闲发送心跳检测消息
         */

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent e = (IdleStateEvent) evt;
                switch (e.state()) {
                    case WRITER_IDLE:
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME) {
                            lastClickTime = System.currentTimeMillis();
                            ByteBuf bb = Unpooled.wrappedBuffer("ping".getBytes(CharsetUtil.UTF_8));
                            ctx.writeAndFlush(bb);
                            System.out.println("send ping to server----------");
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        /**
         * 接受服务端发送过来的消息 调用
         */

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object baseMsg) throws Exception {

            String msg1 = ((ByteBuf) baseMsg).toString(CharsetUtil.UTF_8).trim();
            ReferenceCountUtil.release(msg1);
        }

        NettyClientBootstrap nettyClient = new NettyClientBootstrap();

        /**
         * 连接断开要进行的操作
         *
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            System.out.println("重连了。---------");
            //这里最好暂停一下。不然会基本属于毫秒时间内执行很多次。
            //造成重连失败
            TimeUnit.SECONDS.sleep(5);
            nettyClient.startNetty();
            //ctx.channel().eventLoop().schedule();
        }

        /**
         * 事件处理方法是当出现Throwable对象才会被调用，
         * 即当Netty由于IO错误或者处理器在处理事件时抛出的异常时。
         * 在大部分情况下，捕获的异常应该被记录下来并且把关联的channel给关闭掉。
         *
         * @param ctx
         * @param cause
         * @throws Exception
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            System.out.println("出现异常了。。。。。。。。。。。。。");
            TimeUnit.SECONDS.sleep(10);
            nettyClient.startNetty();
            cause.printStackTrace();
        }
    }

}
