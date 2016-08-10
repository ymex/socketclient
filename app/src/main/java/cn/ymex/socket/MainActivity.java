package cn.ymex.socket;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.nio.charset.Charset;

import cn.ymex.cute.socket.ClientConfig;
import cn.ymex.cute.socket.Listener;
import cn.ymex.cute.socket.PacketData;
import cn.ymex.cute.socket.ResponsePacketData;
import cn.ymex.cute.socket.SocketClient;
import okio.ByteString;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tv_msg = null;
    private EditText ed_msg = null;
    private Button btn_send = null;


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
        config.setAllocateBuffer(16);
        config.setHeartbeatPacketData(new PacketData("$H2B$".getBytes(Charset.forName("utf-8"))));
        socketClient.connect(config);
        socketClient.setOnReceiveListener(onReceiveListener);
    }



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
        socketClient.close();
    }
}
