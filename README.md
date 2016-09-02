# socketclient

A simple NIO Socket Client for Android


### socket server's tool

项目assets中附有：TCP/UDP socket服务器端工具 SocketTool.exe。

### use socketclient for android studio

```
compile 'cn.ymex:cute.socketclient:0.0.6'
```


### code
```
    public static final String HOST = "192.168.1.120";
        public static int PORT = 60000;


        private SocketClient socketClient;
        ClientConfig config = null;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            initView();

            socketClient = new SocketClient();
            config = new ClientConfig(HOST, PORT);
            config.setHeartBeatInterval(3 * 1000);
            //一次读取的字节数
            config.setAllocateBuffer(512);
            // 不加心跳包，默认无心跳
            config.setHeartbeatPacketData(new PacketData(ByteString.utf8("H-E-A-R-T-B-E-A-T")));
            config.setAutoConnectWhenBreak(true);
            socketClient.connect(config);
            socketClient.setOnReceiveListener(onReceiveListener);
            socketClient.setOnConnectListener(onConnectListener);
        }

        private Listener.OnConnectListener onConnectListener = new Listener.SampleOnConnectLister() {

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
            public void connectFailed(SocketClient client) {
                L.d("-----connectFailed-----");
            }

            @Override
            public void disconnect() {
                super.disconnect();
                L.d("-----disconnect-----");
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

[code in app](https://github.com/ymex/socketclient/blob/master/app/src/main/java/cn/ymex/socket/MainActivity.java)


###不支持

1。暂时不支持拆包处理[固长数据包除外]，计划中...
2。不支持图片字节流，视频字节流的上传与下载。


