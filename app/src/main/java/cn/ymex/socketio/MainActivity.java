package cn.ymex.socketio;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import cn.ymex.cute.log.L;
import cn.ymex.socketio.R;
import cn.ymex.socketio.SocketClient;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tv_msg = null;
    private EditText ed_msg = null;
    private Button btn_send = null;


    public static int PORT = 60000;
    public static final String HOST = "192.168.1.120";


    private SocketClient socketClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initTcp();
    }

    private void initTcp() {
        if (socketClient != null) {
            return;
        }
        socketClient = new SocketClient.Builder()
                .setHost(HOST)
                .setPort(PORT)
                .setHeartBeatInterval(1000 * 10)
                .setHeartPacketData(new SocketClient.PacketData("0000".getBytes()))
                .setAllocateBuffer(8).build();

        socketClient.setOnReceiveListener(onReceiveListener);
        socketClient.setOnConnectListener(new SocketClient.OnConnectListener() {

            @Override
            public void connectPrepare() {
                L.d("connectPrepare");
            }

            @Override
            public void connectWaiting() {
                L.d("connectWaiting");
            }

            @Override
            public void connectSuccess() {
                L.d("connectSuccess");
            }

            @Override
            public void connectBreak() {
                L.d("connectBreak");
            }

            @Override
            public void connectFailed() {
                L.d("connectFailed");
            }
        });
    }


    private SocketClient.OnReceiveListener onReceiveListener = new SocketClient.OnReceiveListener() {
        @Override
        public void receive(SocketClient.ResponsePacketData result) {
            String message = new String(result.untieData());
            L.d(message);
            tv_msg.setText(message);
        }
    };

    @Override
    public void onClick(View v) {
        if (!socketClient.isConnected()) {

        }
        final String text = ed_msg.getText().toString();
        socketClient.post(new SocketClient.PacketData(text.getBytes()));
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
        try {
            socketClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
