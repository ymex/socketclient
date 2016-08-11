# socketclient
A simple NIO Socket Client for Android


###socket server's tool
项目中带有TCP/UDP socket工具在app 目录中
socketio/SocketTool.exe for window.

###android studio
```
compile 'cn.ymex:cute.socketclient:0.0.1'
```


### Demo
```
   public static int PORT = 60000;
    public static final String HOST = "192.168.6.20";


    private SocketClient socketClient;
    ClientConfig config = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

        socketClient = new SocketClient();
        config = new ClientConfig(HOST,PORT);
        config.setHeartBeatInterval(3 * 1000);
        //一次读取的字节数
        config.setAllocateBuffer(512);
        // 不加心跳包，默认无心跳
        config.setHeartbeatPacketData(new PacketData(ByteString.utf8("H-E-A-R-T-B-E-A-T")));
        socketClient.connect(config);
        socketClient.setOnReceiveListener(onReceiveListener);
        socketClient.setOnConnectListener(onConnectListener);
    }

    private Listener.OnConnectListener onConnectListener =new Listener.OnConnectListener() {
        @Override
        public void connectPrepare() {
            L.d("----connectPrepare------");
        }

        @Override
        public void connectWaiting() { //waiting 触发比较多就不打印了。
//            L.d("----connectWaiting------");
        }

        @Override
        public void connectSuccess(SocketClient socketClient) {
            L.d("----connectSuccess------");
            socketClient.send(new PacketData(ByteString.utf8("client ----connectSuccess------")));
        }

        @Override
        public void connectBreak() {
            L.d("-----connectBreak-----");

        }

        @Override
        public void connectFailed() {
            L.d("-----connectFailed-----");
        }
    };

    private Listener.OnReceiveListener onReceiveListener = new Listener.OnReceiveListener() {
        @Override
        public void receive(ResponsePacketData result) {
            String message = new String(result.untieData());
            tv_msg.setText(message);
        }
    };

```
具体见项目demo




###不支持

1。暂时不支持拆包处理[固长数据包除外]，后期会加上。
2。不支持图片字节流，视频字节流的上传与下载。


