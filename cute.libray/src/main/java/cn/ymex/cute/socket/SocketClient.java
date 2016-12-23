package cn.ymex.cute.socket;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import cn.ymex.cute.log.L;

import static cn.ymex.cute.socket.Tools.checkNull;
import static cn.ymex.cute.socket.Tools.isNull;

public class SocketClient {
    private boolean activeBreak = false; //主动断开标识[主动断开后]
    private ClientConfig clientConfig;
    private SocketChannel socketChannel = null;
    private Selector selector = null;

    private int currentStatus = Status.CONNECT_PREPARE;


    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public SocketClient() {
    }

    public int getCurrentStatus() {
        return currentStatus;
    }

    /**
     * 重新连接
     */
    public synchronized void reconnect() {
        connect(this.clientConfig);
        setActiveBreak(false);
    }

    /**
     * 连接到服务器
     *
     * @param config
     */
    public synchronized void connect(ClientConfig config) {
        if (this.currentStatus == Status.CONNECT_SUCCESS
                || this.currentStatus == Status.CONNECT_WAITING) {
            L.w("socket is already runing...");
            return;
        }
        this.sendHandleMessage(Status.CONNECT_PREPARE);
        checkNull(config, "socket config is null");
        this.clientConfig = config;
        this.sendHandleMessage(Status.CONNECT_WAITING);
        new Thread(obtionConnectThread()).start();
    }

    /**
     * 发送一条信息
     *
     * @param packetDate
     */
    public synchronized void send(PacketData packetDate) {
        if (obtionPostDataThread() == null || !obtionPostDataThread().isAlive()) {
            System.out.println("PostDataThread  is null or PostDataThread is stop ");
            return;
        }
        checkNull(packetDate, "postData is null");
        obtionPostDataThread().put(packetDate);
    }

    /**
     * 关闭socketclient
     */
    private synchronized void close() {
        try {
            if (this.socketChannel != null ) {


                this.socketChannel.close();
                this.socketChannel = null;
            }

            if (this.selector != null) {
                this.selector.wakeup();
                this.selector.close();
                this.selector = null;
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 主动断开连接
     */
    public synchronized void disconnect() {
        setActiveBreak(true);
        close();
        this.sendHandleMessage(Status.DIS_CONNECT);
    }

    /**
     * 销毁SocketClient
     * 若调用了些方法，socketclient 不可再重用
     */
    public synchronized void destroy() {
        obtainHeartbeatThread().setStop(true);
        obtionPostDataThread().setStop(true);
        obtainReceiveDataThread().setStop(true);
        obtainDealReceiveDataThread().setStop(true);
        setActiveBreak(true);
        close();
    }

    /**
     * 连接 socket
     *
     * @param host
     * @param port
     * @throws IOException
     */
    private SocketChannel _connect(String host, int port, int timeout) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.socket().setTcpNoDelay(false);
        channel.socket().setKeepAlive(true);
        channel.socket().setSoTimeout(timeout);
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress(host, port));
        return channel;
    }

    private Selector regConnectSelector(SocketChannel channel) throws IOException {
        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);
        return selector;
    }


    private ConnectThreadRunable connectThreadRunable = null;

    private ConnectThreadRunable obtionConnectThread() {
        if (isNull(this.connectThreadRunable)) {
            this.connectThreadRunable = new ConnectThreadRunable();
        }
        return this.connectThreadRunable;
    }

    private class ConnectThreadRunable implements Runnable {
        @Override
        public void run() {
            try {
                sendHandleMessage(Status.CONNECT_WAITING);
                socketChannel = _connect(
                        clientConfig.getHost(),
                        clientConfig.getPort(),
                        clientConfig.getSoTimeout());


                while (true) {
                    if (socketChannel.finishConnect()) {
                        selector = regConnectSelector(socketChannel);
                        sendHandleMessage(Status.CONNECT_SUCCESS);
                        break;
                    }
                    sendHandleMessage(Status.CONNECT_WAITING);
                    Thread.sleep(100);//空出cpu
                }
            } catch (Exception e) {
                sendHandleMessage(Status.CONNECT_FAILED);
                e.printStackTrace();
            }
        }
    }

    private PostDataThread postDataThread = null;

    private PostDataThread obtionPostDataThread() {
        if (isNull(this.postDataThread)) {
            this.postDataThread = new PostDataThread();
        }
        return postDataThread;
    }

    private class PostDataThread extends Thread {
        private boolean stop = false;
        private LinkedBlockingQueue<PacketData> messageQueue;

        public boolean isStop() {
            return stop;
        }

        public void setStop(boolean stop) {
            this.stop = stop;
        }

        private LinkedBlockingQueue<PacketData> obainQueue() {
            if (null == this.messageQueue) {
                this.messageQueue = new LinkedBlockingQueue<>();
            }
            return this.messageQueue;
        }

        /**
         * 发送信息
         *
         * @param data
         */
        public void put(PacketData data) {
            obainQueue().offer(data);
            synchronized (this) {
                notify();
            }
        }

        @Override
        public void run() {
            while (!isStop()) {
                if (this.obainQueue().size() <= 0) {
                    synchronized (this) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    continue;
                }
                PacketData packetData = this.obainQueue().poll();
                if (packetData == null) {
                    continue;
                }
                //发送时加锁
                if (socketChannel != null
                        && socketChannel.isConnected()
                        && socketChannel.isOpen()) {
                    this._send(packetData);
                }
            }
        }

        private synchronized void _send(PacketData packetData) {
            if (socketChannel == null) {
                System.out.println("post out is null ");
                return;
            }

            try {

                if (!socketChannel.isConnected()
                        || !socketChannel.finishConnect()
                        || !socketChannel.socket().isConnected() ||
                        socketChannel.socket().isClosed()) {
                    System.out.println("socket not connect or close ");
                    return;
                }
                ByteBuffer buffer = ByteBuffer.wrap(packetData.warpData());
                socketChannel.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                L.e(e.getMessage());
                sendHandleMessage(Status.CONNECT_BREAK);
            }
        }
    }

    private void sendHandleMessage(int what) {
        sendHandleMessage(what, null);
    }

    /**
     * 向EventBusHandler 发送事件
     *
     * @param what
     * @param obj
     */
    private void sendHandleMessage(int what, Object obj) {

        Message message = this.obtianEventBusHandler().obtainMessage();
        message.what = what;
        if (!isNull(obj)) {
            message.obj = obj;
        }
        message.sendToTarget();
    }

    /**
     * 发送延时事件
     *
     * @param what
     * @param arg1
     * @param delayMillis
     */
    private void sendDelayedMessage(int what, int arg1, long delayMillis) {
        Message message = new Message();
        message.what = what;
        message.arg1 = arg1;
        this.obtianEventBusHandler().sendMessageDelayed(message, delayMillis);
    }

    private EventBusHandler eventBusHandler = null;

    public EventBusHandler obtianEventBusHandler() {
        if (isNull(this.eventBusHandler)) {
            this.eventBusHandler = new EventBusHandler(this);
        }
        return eventBusHandler;
    }

    private static class EventBusHandler extends Handler {
        private static final int HEART_MESSAGE = 0x777;
        private static final int DATA_MESSAGE = 0x788;


        private WeakReference<SocketClient> referenceTcpClient = null;

        public EventBusHandler(SocketClient socketClient) {
            super(Looper.getMainLooper());
            referenceTcpClient = new WeakReference<SocketClient>(socketClient);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (this.referenceTcpClient == null || referenceTcpClient.get() == null) {
                return;
            }
            switch (msg.what) {
                case HEART_MESSAGE://心跳包
                    referenceTcpClient.get().messageOnHeartbeat();
                    break;
                case DATA_MESSAGE://数据包
                    referenceTcpClient.get().messageOnReceiveData((ResponsePacketData) msg.obj);
                    break;
                case Status.CONNECT_PREPARE:
                    referenceTcpClient.get().messageOnConnectPrepare();
                    break;
                case Status.CONNECT_WAITING:
                    referenceTcpClient.get().messageOnConnectWaiting();
                    break;
                case Status.CONNECT_SUCCESS:
                    referenceTcpClient.get().messageOnConnectSuccess();
                    break;
                case Status.CONNECT_FAILED:
                    referenceTcpClient.get().messageConnectFailed();
                    break;
                case Status.CONNECT_BREAK:
                    referenceTcpClient.get().messageConnectBreak();
                    break;
                case Status.DIS_CONNECT:
                    referenceTcpClient.get().messageOnDiscontent();
                    break;
                case Status.MESSAGE_RECONNECT:
                    referenceTcpClient.get().messageReconnect(msg.arg1);
                    break;
            }
        }
    }

    private void messageOnHeartbeat() {

    }

    private void messageOnReceiveData(ResponsePacketData responsePacketData) {
        if (this.onReceiveListeners != null && this.onReceiveListeners.size() > 0) {
            for (Listener.OnReceiveListener onrec : this.onReceiveListeners) {
                if (onrec != null) {
                    onrec.receive(responsePacketData);
                }
            }
        }
    }

    private void messageOnConnectPrepare() {
        this.currentStatus = Status.CONNECT_PREPARE;
        if (!isNull(this.onConnectListener)) {
            this.onConnectListener.connectPrepare();
        }
    }

    private void messageOnConnectWaiting() {
        this.currentStatus = Status.CONNECT_WAITING;
        if (!isNull(this.onConnectListener)) {
            this.onConnectListener.connectWaiting();
        }
    }

    private void messageOnConnectSuccess() {
        this.currentStatus = Status.CONNECT_SUCCESS;
        if (this.obtainReceiveDataThread().getState() == Thread.State.NEW) {
            this.obtainReceiveDataThread().start();
        }
        if (this.obtionPostDataThread().getState() == Thread.State.NEW) {
            this.obtionPostDataThread().start();
        }
        if (this.obtainHeartbeatThread().getState() == Thread.State.NEW
                && !isNull(this.clientConfig.getHeartbeatPacketData())) {
            this.obtainHeartbeatThread().start();
        }
        if (this.obtainDealReceiveDataThread().getState() == Thread.State.NEW) {
            this.obtainDealReceiveDataThread().start();
        }
        synchronized (obtainReceiveDataThread()) {
            obtainReceiveDataThread().notify();
        }
        if (!isNull(this.onConnectListener)) {
            this.onConnectListener.connectSuccess(this);
        }
    }


    private void messageConnectBreak() {
        this.currentStatus = Status.CONNECT_BREAK;
        if (!isNull(this.onConnectListener)) {
            this.onConnectListener.connectBreak();
        }
        this.close();
        sendDelayedMessage(Status.MESSAGE_RECONNECT, Status.CONNECT_BREAK, clientConfig.getAutoConnectdelayMillis());

    }

    private void messageConnectFailed() {
        this.currentStatus = Status.CONNECT_FAILED;
        if (!isNull(this.onConnectListener)) {
            this.onConnectListener.connectFailed(this);
        }
        close();
        sendDelayedMessage(Status.MESSAGE_RECONNECT, Status.CONNECT_FAILED, clientConfig.getAutoConnectdelayMillis());

    }

    private void messageOnDiscontent() {
        this.currentStatus = Status.DIS_CONNECT;
        if (!isNull(this.onConnectListener)) {
            this.onConnectListener.disconnect();
        }
    }


    private synchronized void messageReconnect(int type) {
        switch (type) {
            case Status.CONNECT_BREAK:
                if (!isActiveBreak() && clientConfig.isAutoConnectWhenBreak()) {
                    this.reconnect();
                }
                break;
            case Status.CONNECT_FAILED:
                if (!isActiveBreak() && clientConfig.isAutoConnectWhenFailed()) {
                    this.reconnect();
                }
                break;
        }
    }


    public boolean isActiveBreak() {
        return activeBreak;
    }

    public void setActiveBreak(boolean activeBreak) {
        this.activeBreak = activeBreak;
    }


    public static class Builder {
        SocketClient socketClient = null;
        ClientConfig config;

        public Builder() {
            config = new ClientConfig();
        }

        public Builder setHost(String host) {
            this.config.setHost(host);
            return this;
        }

        public Builder setPort(int port) {
            this.config.setPort(port);
            return this;
        }

        public Builder setHeartPacketData(PacketData packetData) {
            this.config.setHeartbeatPacketData(packetData);
            return this;
        }

        public Builder setHeartBeatInterval(long time) {
            this.config.setHeartBeatInterval(time);
            return this;
        }

        public Builder setSoTimeout(int time) {
            this.config.setSoTimeout(time);
            return this;
        }

        public Builder setAllocateBuffer(int allocateBuffer) {
            this.config.setAllocateBuffer(allocateBuffer);
            return this;
        }


        public SocketClient build() {
            if (socketClient == null) {
                socketClient = new SocketClient();
            }
            return socketClient;
        }

    }

    private ReceiveDataThread receiveDataThread;

    public ReceiveDataThread obtainReceiveDataThread() {
        if (isNull(this.receiveDataThread)) {
            this.receiveDataThread = new ReceiveDataThread();
        }
        return receiveDataThread;
    }

    private class ReceiveDataThread extends Thread {

        private boolean stop = false;

        public boolean isStop() {
            return stop;
        }

        public void setStop(boolean stop) {
            this.stop = stop;
        }

        @Override
        public void run() {
            List<Byte> tempList = new ArrayList<>(clientConfig.getAllocateBuffer());
            while (!isStop()) {
                if (getCurrentStatus()!= Status.CONNECT_SUCCESS) {
                    synchronized (obtainReceiveDataThread()) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    if (isNull(selector) || !selector.isOpen()) {
                        continue;
                    }
                    selector.wakeup();
                    while (selector.select() > 0) {
                        for (SelectionKey sk : selector.selectedKeys()) {
                            if (sk.isReadable()) {
                                SocketChannel channel = (SocketChannel) sk.channel();
                                if (!channel.isConnected()) {
                                    continue;
                                }
                                ByteBuffer buffer = ByteBuffer.allocate(clientConfig.getAllocateBuffer());
                                int len = channel.read(buffer);
                                buffer.flip();
                                if (len > 0) {
                                    tempList.clear();
                                    while (buffer.hasRemaining()) {
                                        tempList.add(buffer.get());
                                    }
                                    byte[] dates = new byte[tempList.size()];
                                    for (int i = 0; i < dates.length; i++) {
                                        dates[i] = tempList.get(i);
                                    }
                                    dealReceiveData(dates);
                                } else if (len <= -1) {
                                    buffer.clear();
                                    selector.selectedKeys().remove(sk);
                                    sk.channel().close();
                                    selector.selectNow();
                                    sk.cancel();
                                    sendHandleMessage(Status.CONNECT_BREAK);
                                    break;
                                }
                                buffer.clear();
                                sk.interestOps(SelectionKey.OP_READ);
                                selector.selectedKeys().remove(sk);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sendHandleMessage(Status.CONNECT_BREAK);
                }
            }
        }


        /**
         * 处理接受数据
         */
        private void dealReceiveData(byte[] datas) {
            if (datas == null || datas.length <= 0) {
                return;
            }
            obtainDealReceiveDataThread().put(datas);
        }
    }


    private DealReceiveDataThread dealReceiveDataThread;

    public DealReceiveDataThread obtainDealReceiveDataThread() {
        if (isNull(dealReceiveDataThread)) {
            this.dealReceiveDataThread = new DealReceiveDataThread();
        }
        return dealReceiveDataThread;
    }

    private class DealReceiveDataThread extends Thread {
        private LinkedBlockingQueue<byte[]> queue;
        private List<Byte> packetRawDatas;
        private boolean stop = false;

        private LinkedBlockingQueue<byte[]> obtainQueue() {
            if (null == this.queue) {
                this.queue = new LinkedBlockingQueue<>();
            }
            return this.queue;
        }

        private List<Byte> obtainPacketRawDatas() {
            if (this.packetRawDatas == null) {
                this.packetRawDatas = new ArrayList<>();
            }
            return this.packetRawDatas;
        }

        public void put(byte[] datas) {
            obtainQueue().offer(datas);
            synchronized (this) {
                notify();
            }
        }

        @Override
        public void run() {
            while (!isStop()) {
                if (obtainQueue().size() <= 0) {
                    synchronized (this) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    continue;
                }
                byte[] dates = obtainQueue().poll();
                if (dates == null) {
                    continue;
                }
                deal(dates);
            }
        }

        /**
         * 处理接受到的原始数据
         * 暂时没有处理， 只是简单的返回给接收者
         *
         * @param datas
         */
        private void deal(byte[] datas) {
            //todo 拆包代码处理
            //todo 拆包代码处理
            //todo 拆包代码处理
            sendHandleMessage(EventBusHandler.DATA_MESSAGE, new ResponsePacketData(Arrays.copyOf(datas, datas.length)));
        }


        public boolean isStop() {
            return stop;
        }

        public void setStop(boolean stop) {
            this.stop = stop;
        }
    }


    private HeartbeatThread heartbeatThread;

    public HeartbeatThread obtainHeartbeatThread() {
        if (isNull(this.heartbeatThread)) {
            this.heartbeatThread = new HeartbeatThread();
        }
        return this.heartbeatThread;
    }

    private class HeartbeatThread extends Thread {
        private boolean stop = false;


        public boolean isStop() {
            return stop;
        }

        public void setStop(boolean stop) {
            this.stop = stop;
        }

        @Override
        public void run() {
            while (!isStop()) {
                try {
                    Thread.sleep(clientConfig.getHeartBeatInterval());
                    send(clientConfig.getHeartbeatPacketData());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private ArrayList<Listener.OnReceiveListener> onReceiveListeners = null;
    private Listener.OnConnectListener onConnectListener = null;

    /**
     * 添加消息接收回调
     *
     * @param onReceiveListener
     */
    public void setOnReceiveListener(Listener.OnReceiveListener onReceiveListener) {
        if (onReceiveListeners == null) {
            onReceiveListeners = new ArrayList<>();
        }
        if (onReceiveListeners.contains(onReceiveListener)) {
            return;
        }
        onReceiveListeners.add(onReceiveListener);
    }

    /**
     * 移除消息接收回调
     *
     * @param onReceiveListener
     */
    public void removeOnreceiveListener(Listener.OnReceiveListener onReceiveListener) {
        if (null != onReceiveListeners && onReceiveListeners.contains(onReceiveListener)) {
            onReceiveListeners.remove(onReceiveListener);
        }
    }

    public void setOnConnectListener(Listener.OnConnectListener onConnectListener) {
        this.onConnectListener = onConnectListener;
    }
}
