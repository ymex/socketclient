package cn.ymex.socket.netty;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

/**
 * Copyright (c) ymexc(www.ymex.cn)
 * Email:ymex@foxmail.com
 * date 2016/12/23
 *
 * @author ymexc
 */
public class NettyClientBootstrap {

    public interface ClientIntface{
        void connect(String host, int port);//1. 建立连接
        void sendMessage(int mt, String msg, long delayed);//2. 发送消息
    }




    private int port=60000;
    private String host="192.168.6.111";

    public SocketChannel socketChannel;
    public  void startNetty() throws InterruptedException {
        System.out.println("长链接开始");
        if(start()){
            System.out.println("长链接成功");
            ByteBuf bb = Unpooled.wrappedBuffer(("tableIP=asdf".getBytes(CharsetUtil.UTF_8)));
            socketChannel.writeAndFlush(bb);
        }
    }
    private Boolean start() throws InterruptedException {
        EventLoopGroup eventLoopGroup=new NioEventLoopGroup();
        Bootstrap bootstrap=new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE,true);
        bootstrap.group(eventLoopGroup);
        bootstrap.remoteAddress(host, port);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline pipeline = socketChannel.pipeline();
                pipeline.addLast(new IdleStateHandler(20, 10, 0));
                //下面注释的两行是加密和解密。服务器和客户端需要统一不然会报错。所以我直接注释了。
                //pipeline.addLast(new ObjectEncoder());
                //pipeline.addLast(new ObjectDecoder(ClassResolvers.cacheDisabled(null)));
                pipeline.addLast(new NettyClientHandlerBootstrap());
            }
        });
        ChannelFuture future = null ;
        try {
            future =bootstrap.connect(new InetSocketAddress(host,port)).sync();
            if (future.isSuccess()) {
                socketChannel = (SocketChannel)future.channel();
                System.out.println("connect server  成功---------");
                return true;
            }else{
                System.out.println("connect server  失败---------");
                startNetty();
                return false;
            }
        } catch (Exception e) {
            System.out.println("无法连接----------------");
            //这里最好暂停一下。不然会基本属于毫秒时间内执行很多次。
            //造成重连失败
            TimeUnit.SECONDS.sleep(5);
            startNetty();
            return false;
        }
    }
}
