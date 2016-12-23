package cn.ymex.cute.socket.netty;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
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
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
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

    private final int MIN_CLICK_DELAY_TIME = 1000 * 30; //设置心跳时间  开始


    private String host;
    private int port;

    private Bootstrap bootstrap;
    public SocketChannel socketChannel;
    private ClinetAdapterHandler clientAdapterHandler;
    private IdleStateHandler idleStateHandler;

    private List<SocketListener.OnDataReceiveListener> onDataReceiveListeners;
    private SocketListener.OnConnectStatusListener onConnectStatusListener;

    private HandlerThread mWorkThread = null;
    private Handler mWorkHandler = null;
    private Handler.Callback mWorkHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_INIT:
                    _init();
                    break;

                case MESSAGE_CONNECT://连接
                    _connect();
                    break;

                case MESSAGE_SEND://发送消息
                    _post(msg);
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
        init();
    }


    /**
     * 始化工作线程
     */
    private void init() {
        mWorkThread = new HandlerThread("droid_socket_client");
        mWorkThread.start();
        mWorkHandler = new Handler(mWorkThread.getLooper(), mWorkHandlerCallback);
        clientAdapterHandler = new ClinetAdapterHandler();
        idleStateHandler = new IdleStateHandler(10, 10, 0);
        mWorkHandler.sendEmptyMessage(MESSAGE_INIT);
    }

    /**
     * 初始化 client
     */
    private void _init() {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.group(eventLoopGroup);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline pipeline = socketChannel.pipeline();
                pipeline.addLast(idleStateHandler);//心跳设置
                pipeline.addLast(clientAdapterHandler);
                // 服务器和客户端编码解码需要统一不然会报错。
                //pipeline.addLast(new ObjectEncoder());//数据编码
                //pipeline.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));//数据解码解密
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

    /**
     * 重新连接
     */
    public void reconnect() {

    }

    /**
     * 断开连接
     */
    public void disconnect() {

    }

    /**
     * 销毁
     */
    public void destroy() {

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
                _onConnected(DroidSocketClient.this);
            } else { //连接失败
                _onConnectFailed();
            }
        } catch (Exception e) {//连接失败
            _onConnectFailed();
            e.printStackTrace();
        }
    }


    /**
     * 发送信息
     *
     * @param content
     */
    public void post(String content) {
        ByteBuf con = Unpooled.wrappedBuffer(content.getBytes(CharsetUtil.UTF_8));
        post(con);
    }

    /**
     * 发送信息
     *
     * @param content
     */
    public void post(ByteBuf content) {
        Message message = new Message();
        message.what = MESSAGE_SEND;
        message.obj = content;
        mWorkHandler.sendMessage(message);
    }


    /**
     * 发送消息
     */
    private void _post(Message message) {
        ByteBuf content = (ByteBuf) message.obj;
        try {
            socketChannel.writeAndFlush(content).sync();
            socketChannel.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    /**
     * 接收到数据回调
     *
     * @param message
     */
    private void _onDataReceive(final ByteBuf message) {
        String msg1 = message.toString(CharsetUtil.UTF_8).trim();
        System.out.println("收到的信息：" + msg1);
        ReferenceCountUtil.release(msg1);
        runInMainUiThread(new Runnable() {
            @Override
            public void run() {
                for (SocketListener.OnDataReceiveListener listener : getOnDataReceiveListeners()) {
                    if (listener != null) {
                        listener.onDataReceive(message);
                    }
                }
            }
        });

    }

    private void _onConnected(final DroidSocketClient client) {
        if (onConnectStatusListener != null) {
            onConnectStatusListener.onConnected();
        }
        System.out.println("连接成功");
    }

    private void _onDisconnected(final ChannelHandlerContext ctx) {
        if (onConnectStatusListener != null) {
            onConnectStatusListener.onDisconnected();
        }
        System.out.println("连接断开");
    }

    private void _onConnectFailed() {//连接失败
        if (onConnectStatusListener != null) {
            onConnectStatusListener.onConnectFailed();
        }
        System.out.println("连接失败");
    }


    public void setOnConnectStatusListener(SocketListener.OnConnectStatusListener onConnectStatusListener) {
        this.onConnectStatusListener = onConnectStatusListener;
    }

    /**
     * 注册数据接收监听
     *
     * @param listener
     */
    public void registerDataReceiveListener(SocketListener.OnDataReceiveListener listener) {
        if (listener == null
                || !(listener instanceof SocketListener.OnDataReceiveListener)
                || getOnDataReceiveListeners().contains(listener)) {
            return;
        }
        getOnDataReceiveListeners().add(listener);
    }

    /**
     * 注册数据接收监听
     *
     * @param listener
     */
    public void unregisterDataReceiveListener(SocketListener.OnDataReceiveListener listener) {
        if (listener == null || !getOnDataReceiveListeners().contains(listener)) {
            return;
        }
        getOnDataReceiveListeners().remove(listener);
    }

    private List<SocketListener.OnDataReceiveListener> getOnDataReceiveListeners() {
        if (onDataReceiveListeners == null) {
            onDataReceiveListeners = new ArrayList<>(4);
        }
        return onDataReceiveListeners;
    }

    private void runInMainUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }

    private class ClinetAdapterHandler extends ChannelInboundHandlerAdapter {
        private long lastClickTime = 0;//设置心跳时间   结束

        /**
         * 接受服务端发送过来的消息 调用
         */

        @Override
        public void channelRead(ChannelHandlerContext channelHandlerContext, Object baseMsg) throws Exception {

            System.out.println("channelRead：" + Thread.currentThread().getId());
            _onDataReceive((ByteBuf) baseMsg);

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
                            ByteBuf bb = Unpooled.wrappedBuffer("ping .......".getBytes(CharsetUtil.UTF_8));
                            ctx.writeAndFlush(bb);
                        }
                        break;
                }
            }
        }

        /**
         * 在连接被建立并且准备进行通信时被调用。
         */
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
        }


        /**
         * 连接断开要进行的操作
         *
         * @param ctx
         * @throws Exception
         */
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            _onDisconnected(ctx);
        }

        /**
         * 当Netty由于IO错误或者处理器在处理事件时抛出的异常时。
         * 在大部分情况下，捕获的异常应该被记录下来并且把关联的channel给关闭掉。
         *
         * @param ctx
         * @param cause
         * @throws Exception
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
            TimeUnit.SECONDS.sleep(10);
            cause.printStackTrace();
        }
    }

}
