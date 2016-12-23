package cn.ymex.socket.netty;

import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

/**
 * Copyright (c) ymexc(www.ymex.cn)
 * Email:ymex@foxmail.com
 * date 2016/12/23
 *
 * @author ymexc
 */
public class NettyClientHandlerBootstrap extends SimpleChannelInboundHandler<Object> {
    //设置心跳时间  开始
    public static final int MIN_CLICK_DELAY_TIME = 1000 * 30;
    private long lastClickTime = 0;
    //设置心跳时间   结束


    /**
     * 在连接被建立并且准备进行通信时被调用。
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        super.channelActive(ctx);
    }

    //利用写空闲发送心跳检测消息
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
        System.out.println("1111111111111111------------------------" + msg1);
        ReferenceCountUtil.release(msg1);
    }

    NettyClientBootstrap nettyClient = new NettyClientBootstrap();

    /**
     * 这里是断线要进行的操作
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
