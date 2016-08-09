package cn.ymex.socketio;

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

public class SocketClient {
    private SocketClient client = this;
    private ClientConfig clientConfig;
    private SocketChannel socketChannel = null;
    private Selector selector = null;

    private int currentStatus = Status.CONNECT_PREPARE;


    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    public SocketClient() {
    }


    /**
     * 连接到服务器
     *
     * @param config
     */
    public void connect(ClientConfig config) {
        if (this.currentStatus == Status.CONNECT_SUCCESS
                || this.currentStatus == Status.CONNECT_WAITING) {
            L.w("socket is already runing...");
            return;
        }
        checkNull(config, "socket config is null");
        this.clientConfig = config;
        obtionConnectThread().start();
    }

    /**
     * 发送一条信息
     *
     * @param packetDate
     */
    public void send(PacketData packetDate) {
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
    public void close() {

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


    private ConnectThread connectThread = null;

    private ConnectThread obtionConnectThread() {
        if (isNull(this.connectThread)) {
            this.connectThread = new ConnectThread();
        }
        return this.connectThread;
    }

    private class ConnectThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                client.sendHandleMessage(Status.CONNECT_WAITING);
                client.socketChannel = client._connect(
                        client.clientConfig.getHost(),
                        client.clientConfig.getPort(),
                        client.clientConfig.getSoTimeout());
                client.selector = regConnectSelector(client.socketChannel);

                while (true) {
                    if (client.socketChannel.finishConnect()) {
                        client.sendHandleMessage(Status.CONNECT_SUCCESS);
                        break;
                    }
                    client.sendHandleMessage(Status.CONNECT_WAITING);
                }
            } catch (IOException e) {
                client.sendHandleMessage(Status.CONNECT_FAILED);
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
        }

        @Override
        public void run() {
            while (!isStop()) {
                if (this.obainQueue().size() <= 0) {
                    continue;
                }
                PacketData packetData = this.obainQueue().poll();
                if (packetData == null) {
                    continue;
                }
                if (client.socketChannel.isConnected() && client.socketChannel.isOpen()) {
                    this._send(packetData);
                }
            }
        }

        private void _send(PacketData packetData) {
            if (client.socketChannel == null) {
                System.out.println("post out is null ");
                return;
            }

            try {

                if (!client.socketChannel.isConnected()
                        || !client.socketChannel.finishConnect()
                        || !client.socketChannel.socket().isConnected() ||
                        client.socketChannel.socket().isClosed()) {
                    System.out.println("socket not connect or close ");
                    return;
                }
                ByteBuffer buffer = ByteBuffer.wrap(packetData.warpData());
                client.socketChannel.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                L.e(e.getMessage());
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

    private EventBusHandler eventBusHandler = null;

    public EventBusHandler obtianEventBusHandler() {
        if (isNull(this.eventBusHandler)) {
            this.eventBusHandler = new EventBusHandler(this);
        }
        return eventBusHandler;
    }

    private static class EventBusHandler extends Handler {
        private static final int HEART_MESSAGE = 0x77;
        private static final int DATA_MESSAGE = 0x88;


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
            }
        }
    }

    private void messageOnHeartbeat() {

    }

    private void messageOnReceiveData(ResponsePacketData responsePacketData) {
        if (this.onReceiveListeners != null && this.onReceiveListeners.size() > 0) {
            for (OnReceiveListener onrec : this.onReceiveListeners) {
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

        this.obtainDealReceiveDataThread().start();
        this.obtainReceiveDataThread().start();
        this.obtionPostDataThread().start();
        if (!isNull(this.clientConfig.getHeartbeatPacketData())) {
            this.obtainHeartbeatThread().start();
        }

        if (!isNull(this.onConnectListener)) {
            this.onConnectListener.connectSuccess();
        }
    }


    private void messageConnectBreak() {
        this.currentStatus = Status.CONNECT_BREAK;
        if (!isNull(this.onConnectListener)) {
            this.onConnectListener.connectBreak();
        }
    }

    private void messageConnectFailed() {
        this.currentStatus = Status.CONNECT_FAILED;
        if (!isNull(this.onConnectListener)) {
            this.onConnectListener.connectFailed();
        }
        //关闭 操作
    }

    public static class Status {//socketclient 状态
        public static final int CONNECT_PREPARE = 0x11; //socket开始连接
        public static final int CONNECT_WAITING = 0x22; //socket 连接中
        public static final int CONNECT_SUCCESS = 0x33;//socket 连接成功
        public static final int CONNECT_BREAK = 0x44;//socket 连接断开
        public static final int CONNECT_FAILED = 0x55; //socket 连接失败
    }

    /**
     * socket 配置
     */
    public static class ClientConfig {
        private String host;
        private int port;
        private PacketData heartbeatPacketData;
        private long heartBeatInterval = 30 * 1000;
        private int soTimeout = 1 * 1000;
        private int allocateBuffer = 512;

        public ClientConfig() {

        }

        public ClientConfig(String host, int port) {
            this.host = host;
            this.port = port;
        }

        public int getAllocateBuffer() {
            return allocateBuffer;
        }

        public void setAllocateBuffer(int allocateBuffer) {
            this.allocateBuffer = allocateBuffer;
        }

        public int getSoTimeout() {
            return soTimeout;
        }

        public void setSoTimeout(int soTimeout) {
            this.soTimeout = soTimeout;
        }

        public long getHeartBeatInterval() {
            return heartBeatInterval;
        }

        public void setHeartBeatInterval(long heartBeatInterval) {
            this.heartBeatInterval = heartBeatInterval;
        }

        public PacketData getHeartbeatPacketData() {
            return heartbeatPacketData;
        }

        public void setHeartbeatPacketData(PacketData heartbeatPacketData) {
            this.heartbeatPacketData = heartbeatPacketData;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

    }

    /**
     * 检查对象是否为空，为空抛出 NullPointerException
     *
     * @param notice
     * @param t
     * @param <T>
     * @return
     */
    private <T> T checkNull(T t, String notice) {
        if (null == t) {
            throw new NullPointerException(notice == null ? "" : notice);
        }
        return t;
    }

    /**
     * 检查对象是否为空，为空抛出 NullPointerException
     *
     * @param t
     * @param <T>
     * @return
     */
    private <T> T checkNull(T t) {
        return checkNull(t, null);
    }

    /**
     * 判断对象是否为空
     *
     * @param t
     * @param <T>
     * @return
     */
    private <T> boolean isNull(T t) {
        if (null == t) {
            return true;
        }
        return false;
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
            this.config.allocateBuffer = allocateBuffer;
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
            boolean dis = true;
            List<Byte> tempList = new ArrayList<>(client.clientConfig.getAllocateBuffer());
            while (!isStop()) {
                try {
                    if (isNull(client.selector) || !client.selector.isOpen()) {
                        continue;
                    }
                    while (selector.select() > 0) {
                        for (SelectionKey sk : selector.selectedKeys()) {
                            if (sk.isReadable()) {
                                SocketChannel channel = (SocketChannel) sk.channel();
                                if (!channel.isConnected()) {
                                    continue;
                                }
                                ByteBuffer buffer = ByteBuffer.allocate(client.clientConfig.getAllocateBuffer());
                                int len = channel.read(buffer);
                                buffer.flip();
                                if (len > 0) {
                                    dis = false;
                                    tempList.clear();
                                    while (buffer.hasRemaining()) {
                                        tempList.add(buffer.get());
                                    }
                                    byte[] dates = new byte[tempList.size()];
                                    for (int i = 0; i < dates.length; i++) {
                                        dates[i] = tempList.get(i);
                                    }
                                    dealReceiveData(dates);
                                } else {
                                    if (len != 0 && dis == false) {
                                        client.sendHandleMessage(Status.CONNECT_BREAK);
                                        dis = true;
                                    }
                                }
                                buffer.clear();
                                sk.interestOps(SelectionKey.OP_READ);
                                selector.selectedKeys().remove(sk);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
        }

        @Override
        public void run() {
            while (!isStop()) {
                if (obtainQueue().size() <= 0) {
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
            for (byte b : datas) {
                System.out.println("------------------::" + b);
                if (b == 0x13) {

                }
            }
            client.sendHandleMessage(EventBusHandler.DATA_MESSAGE, new ResponsePacketData(Arrays.copyOf(datas, datas.length)));
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
            try {
                while (!isStop()) {
                    Thread.sleep(client.clientConfig.getHeartBeatInterval());
                    send(client.clientConfig.getHeartbeatPacketData());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public interface OnWarpPacketData {
        byte[] warpData(byte[] rawData);
    }

    public static class PacketData {
        private byte[] rawData;

        public PacketData(byte[] rawData) {
            this.rawData = rawData;
        }

        public byte[] warpData() {
            if (onWarpPacketData == null) {
                return this.rawData;
            }
            return onWarpPacketData.warpData(this.rawData);
        }

        private OnWarpPacketData onWarpPacketData;

        public void setOnWarpPacketData(OnWarpPacketData onWarpPacketData) {
            this.onWarpPacketData = onWarpPacketData;
        }
    }

    public interface OnUntiePacketDate {
        byte[] untieData(byte[] resultData);
    }

    public static class ResponsePacketData {
        private int dataLength = 1024;
        private byte[] resultData;

        public ResponsePacketData() {
        }

        public ResponsePacketData(byte[] resultData) {
            this.resultData = resultData;
        }


        public int getDataLength() {
            return dataLength;
        }

        public void setDataLength(int dataLength) {
            this.dataLength = dataLength;
        }

        public byte[] untieData() {
            if (onUntiePacketDate == null) {
                return this.resultData;
            }
            return onUntiePacketDate.untieData(resultData);
        }

        private OnUntiePacketDate onUntiePacketDate;

        public void setOnUntiePacketDate(OnUntiePacketDate onUntiePacketDate) {
            this.onUntiePacketDate = onUntiePacketDate;
        }
    }


    public interface OnReceiveListener {
        /**
         * 接收服务器socket 返回数据，每有数据返回时都会调用。
         *
         * @param result
         */
        void receive(ResponsePacketData result);
    }

    public interface OnConnectListener {
        void connectPrepare();

        void connectWaiting();

        void connectSuccess();

        void connectBreak();

        void connectFailed();
    }

    private ArrayList<OnReceiveListener> onReceiveListeners = null;
    private OnConnectListener onConnectListener = null;

    /**
     * 添加消息接收回调
     *
     * @param onReceiveListener
     */
    public void setOnReceiveListener(OnReceiveListener onReceiveListener) {
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
    public void removeOnreceiveListener(OnReceiveListener onReceiveListener) {
        if (null != onReceiveListeners && onReceiveListeners.contains(onReceiveListener)) {
            onReceiveListeners.remove(onReceiveListener);
        }
    }

    public void setOnConnectListener(OnConnectListener onConnectListener) {
        this.onConnectListener = onConnectListener;
    }

    public String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv + " ");
        }
        return stringBuilder.toString();
    }
}
