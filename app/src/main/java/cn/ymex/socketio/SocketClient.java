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

/**
 * Created by ymexc on 2016/8/5.
 */
public class SocketClient implements Runnable {


    private SocketConfig socketConfig;

    private PostDataRunnable postDataRunnable;
    private ReceiveDataRunnable receiveDataRunnable;
    private HeartbeatRunnable heartbeatRunnable;

    private DealHander dealHander = null;
    private SocketChannel channel = null;
    private Selector selector = null;

    private int currentStatus = Status.CONNECT_PREPARE;

    public int getCurrentStatus() {
        return currentStatus;
    }

    /**
     * socket 是否连接
     *
     * @return
     */
    public boolean isConnected() {
        if (channel == null) {
            return false;
        }
        return channel.isConnected();//&& receiveDataRunnable.isConnected();
    }

    public void reConnect() {
        if (postDataRunnable.isStop()) {
            new Thread(postDataRunnable).start();
        }
        if (receiveDataRunnable.isStop()) {
            new Thread(receiveDataRunnable).start();
        }
        try {
            close();
            new Thread(this).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DealHander getDealHander() {
        if (dealHander == null) {
            dealHander = new DealHander(this);
        }
        return dealHander;
    }

    public static class SocketConfig {
        private String host;
        private int port;
        private PacketData heartbeatPacketData;
        private long heartBeatInterval = 30 * 1000;
        private int soTimeout = 1 * 1000;
        private int allocateBuffer = 512;

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

    public static class Builder {
        SocketClient socketClient = null;
        SocketConfig config;

        public Builder() {
            config = new SocketConfig();
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
                socketClient = new SocketClient(config);
            }
            return socketClient;
        }

    }

    private SocketClient(SocketConfig config) {
        this.socketConfig = config;
        new Thread(this).start();
    }


    public void close() throws IOException {
        if (channel != null) {
            channel.close();
        }
        if (selector != null) {
            channel.close();
        }
        if (receiveDataRunnable != null) {
            receiveDataRunnable.setStop(true);
        }
        if (postDataRunnable != null) {
            postDataRunnable.setStop(true);
        }
        if (heartbeatRunnable != null) {
            heartbeatRunnable.setStop(true);
        }
    }

    public static class Status {
        public static final int CONNECT_PREPARE = 0x11; //socket开始连接
        public static final int CONNECT_WAITING = 0x22; //socket 连接中
        public static final int CONNECT_SUCCESS = 0x33;//socket 连接成功
        public static final int CONNECT_BREAK = 0x44;//socket 连接断开
        public static final int CONNECT_FAILED = 0x55; //socket 连接失败
    }

    @Override
    public void run() {

        try {
            channel = configConnect(socketConfig.getHost(), socketConfig.getPort());
            selector = obtainSelecter(channel);

            receiveDataRunnable = new ReceiveDataRunnable(selector, getDealHander());
            receiveDataRunnable.setAllocateBuffer(socketConfig.getAllocateBuffer());
            postDataRunnable = new PostDataRunnable(channel);

            new Thread(receiveDataRunnable).start();
            new Thread(postDataRunnable).start();
            if (socketConfig.getHeartbeatPacketData() != null) {
                heartbeatRunnable = new HeartbeatRunnable(socketConfig.getHeartbeatPacketData());
                new Thread(heartbeatRunnable).start();
            }
            while (true) {
                if (channel.finishConnect()) {
                    currentStatus = Status.CONNECT_SUCCESS;
                    break;
                }
                currentStatus = Status.CONNECT_WAITING;
            }
        } catch (IOException e) {
            try {
                if (selector != null) {
                    selector.close();
                }
                if (channel != null) {
                    channel.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            currentStatus = Status.CONNECT_FAILED;
            DealHander.sendMessage(getDealHander(), Status.CONNECT_FAILED, null);
        }
    }

    private Selector obtainSelecter(SocketChannel channel) throws IOException {
        if (channel == null) {
            throw new IOException("channel is null");
        }
        Selector selector = Selector.open();
        if (selector != null) {
            channel.register(selector, SelectionKey.OP_READ);
        } else {
            throw new IOException("selector is null");
        }

        return selector;
    }

    private SocketChannel configConnect(String host, int port) throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.socket().setTcpNoDelay(false);
        socketChannel.socket().setKeepAlive(true);
        socketChannel.socket().setSoTimeout(socketConfig.getSoTimeout());
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(host, port));
        return socketChannel;
    }


    private class ReceiveDataRunnable implements Runnable {

        private Selector selector;
        private boolean stop = false;
        private DealMessageRunable messageRunable;
        private int allocateBuffer = 512;
        private Handler handler;

        public ReceiveDataRunnable(Selector selector, DealHander dealHander) {
            this.selector = selector;
            this.messageRunable = new DealMessageRunable();
            this.handler = dealHander;
            new Thread(messageRunable).start();
        }


        public void setAllocateBuffer(int allocateBuffer) {
            this.allocateBuffer = allocateBuffer;
        }

        public boolean isStop() {
            return stop;
        }

        public void setStop(boolean stop) {
            this.stop = stop;
        }

        @Override
        public void run() {
            boolean dis = true;
            List<Byte> tempList = new ArrayList<>(allocateBuffer);
            while (!isStop()) {
                try {
                    if (!selector.isOpen()) {
                        continue;
                    }
                    while (selector.select() > 0) {
                        for (SelectionKey sk : selector.selectedKeys()) {
                            if (sk.isReadable()) {
                                SocketChannel channel = (SocketChannel) sk.channel();
                                if (!channel.isConnected()) {
                                    continue;
                                }
                                ByteBuffer buffer = ByteBuffer.allocate(allocateBuffer);
                                int len = channel.read(buffer);
                                L.d(";;;;;;;;;;;;;;;--: "+len);
                                buffer.flip();
                                if (len > 0) {
                                    dis = false;
                                    tempList.clear();
                                    while (buffer.hasRemaining()) {
                                        tempList.add(buffer.get());
                                    }
                                    byte[] dates = new byte[tempList.size()];
                                    for (int i = 0; i<dates.length; i++){
                                        dates[i]=tempList.get(i);
                                    }
                                    dealReceiveData(dates);
                                } else {
                                    if (len !=0 && dis == false) {
                                        DealHander.sendMessage(handler, Status.CONNECT_BREAK, null);
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
         *
         */
        private void dealReceiveData(byte[] datas) {
//            byte[] dates = buffer.array();
            if (datas == null || datas.length <= 0) {
                return;
            }
//            this.messageRunable.put(Arrays.copyOf(datas, datas.length));
            this.messageRunable.put(datas);
        }
    }


    private class DealMessageRunable implements Runnable {
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
            callback(datas);
        }

        private void callback(byte[] datas){
            Message message = getDealHander().obtainMessage();
            message.what = DealHander.DATA_MESSAGE;
            message.obj = new ResponsePacketData(Arrays.copyOf(datas, datas.length));
            message.sendToTarget();
        }

        public  String bytesToHexString(byte[] src) {
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

        public boolean isStop() {
            return stop;
        }

        public void setStop(boolean stop) {
            this.stop = stop;
        }
    }

    private class PostDataRunnable implements Runnable {
        private boolean stop = false;
        private SocketChannel postChannel;
        private LinkedBlockingQueue<PacketData> messageQueue;

        public PostDataRunnable(SocketChannel channel) {
            this.postChannel = channel;
            obtainQueue();
        }

        public boolean isStop() {
            return stop;
        }

        public void setStop(boolean stop) {
            this.stop = stop;
        }

        private LinkedBlockingQueue<PacketData> obtainQueue() {
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
        public void putMessage(PacketData data) {
            obtainQueue().offer(data);
        }

        @Override
        public void run() {
            while (!isStop()) {
                if (obtainQueue().size() <= 0) {
                    continue;
                }
                PacketData packetData = obtainQueue().poll();
                if (packetData == null) {
                    continue;
                }
                if (postChannel.isConnected() && postChannel.isOpen()) {
                    this.post(packetData);
                } else {
                    L.d("------- this.post(packetData);----------");
                }
            }
        }

        private void post(PacketData packetData) {
            if (this.postChannel == null) {
                System.out.println("post out is null ");
                return;
            }

            try {

                if (!this.postChannel.isConnected()
                        || !this.postChannel.finishConnect()
                        || !this.postChannel.socket().isConnected() ||
                        this.postChannel.socket().isClosed()) {
                    System.out.println("socket not connect or close ");
                    return;
                }
                ByteBuffer buffer = ByteBuffer.wrap(packetData.warpData());
                this.postChannel.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
                L.e(e.getMessage());
            }
        }
    }

    private class HeartbeatRunnable implements Runnable {
        private boolean stop = false;
        public HeartbeatRunnable(PacketData packetData) {
            this.heartPackDate = packetData;
        }

        private PacketData heartPackDate = null;

        public void setHeartPackDate(PacketData heartPackDate) {
            this.heartPackDate = heartPackDate;
        }

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
                    Thread.sleep(socketConfig.getHeartBeatInterval());
                    if (getCurrentStatus() != Status.CONNECT_SUCCESS) {
                        System.out.println("if (getCurrentStatus() == Status.CONNECT_SUCCESS) {");
                        continue;
                    }
                    post(heartPackDate);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void post(PacketData packetDate) {
        if (postDataRunnable == null) {
            System.out.println("postDataRunnable out is null ");
            return;
        }
        if (packetDate == null) {
            System.out.println("postData  is null ");
            return;
        }
        postDataRunnable.putMessage(packetDate);
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


    private static class DealHander extends Handler {
        private static final int HEART_MESSAGE = 0x00;
        private static final int DATA_MESSAGE = 0x11;


        private static void sendMessage(Handler handler, int what, Object obj) {
            Message message = handler.obtainMessage();
            message.what = what;
            message.obj = obj;
            message.sendToTarget();
        }

        private WeakReference<SocketClient> referenceTcpClient = null;

        public DealHander(SocketClient socketClient) {
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

                    break;
                case DATA_MESSAGE://数据包
                    if (referenceTcpClient.get().onReceiveListeners != null && referenceTcpClient.get().onReceiveListeners.size() > 0) {
                        for (OnReceiveListener onrec : referenceTcpClient.get().onReceiveListeners) {
                            if (onrec != null) {
                                onrec.receive((ResponsePacketData) msg.obj);
                            }
                        }
                    }
                    break;

                case Status.CONNECT_FAILED:
                    if (referenceTcpClient.get().onConnectListener != null) {
                        referenceTcpClient.get().onConnectListener.connectFailed();
                    }
                    break;
                case Status.CONNECT_SUCCESS:
                    if (referenceTcpClient.get().onConnectListener != null) {
                        referenceTcpClient.get().onConnectListener.connectSuccess();
                    }
                    break;
                case Status.CONNECT_BREAK:
                    if (referenceTcpClient.get().onConnectListener != null) {
                        referenceTcpClient.get().onConnectListener.connectBreak();
                    }
                    break;
            }
        }
    }
}
