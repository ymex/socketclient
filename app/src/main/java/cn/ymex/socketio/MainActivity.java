package cn.ymex.socketio;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.nio.charset.Charset;

import cn.ymex.cute.log.L;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tv_msg = null;
    private EditText ed_msg = null;
    private Button btn_send = null;


    public static int PORT = 60000;
    public static final String HOST = "192.168.6.20";


    private SocketClient socketClient;
    SocketClient.ClientConfig config = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        socketClient = new SocketClient();
        config = new SocketClient.ClientConfig(HOST,PORT);
        config.setHeartBeatInterval(3 * 1000);
        config.setHeartbeatPacketData(new SocketClient.PacketData("$H2B$".getBytes(Charset.forName("utf-8"))));
        socketClient.connect( config);
        socketClient.setOnReceiveListener(onReceiveListener);
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

        final String text = ed_msg.getText().toString();
        socketClient.send(new SocketClient.PacketData(text.getBytes()));
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
