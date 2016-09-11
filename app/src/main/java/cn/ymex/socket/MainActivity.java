package cn.ymex.socket;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import cn.ymex.cute.log.L;
import cn.ymex.cute.socket.ByteString;
import cn.ymex.cute.socket.ClientConfig;
import cn.ymex.cute.socket.Listener;
import cn.ymex.cute.socket.PacketData;
import cn.ymex.cute.socket.ResponsePacketData;
import cn.ymex.cute.socket.SocketClient;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tv_msg = null;
    private EditText ed_msg = null;
    private Button btn_send = null;


    public static final String HOST = "192.168.0.105";
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


    @Override
    public void onClick(View v) {

        final String text = ed_msg.getText().toString();
        socketClient.send(new PacketData(text.getBytes()));
//        socketClient.disconnect();
    }

    private void initView() {
        tv_msg = (TextView) findViewById(R.id.tv_text);
        ed_msg = (EditText) findViewById(R.id.et_edit);
        btn_send = (Button) findViewById(R.id.btn_send);
        btn_send.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socketClient.destroy();
    }
}
