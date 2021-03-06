package cn.ymex.socket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import cn.ymex.cute.socket.netty.DroidSocketClient;
import cn.ymex.cute.socket.netty.SocketListener;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tv_msg = null;
    private EditText ed_msg = null;
    private Button btn_send = null;


    public static final String HOST = "192.168.6.112";
    public static int PORT = 60000;
    DroidSocketClient client = DroidSocketClient.getInstance();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        System.out.println("uiid:" + Thread.currentThread().getId());
        client.registerDataReceiveListener(new SocketListener.OnDataReceiveListener() {
            @Override
            public void onDataReceive(ByteBuf baseMsg) {
                System.out.println("mainaction thread " + Thread.currentThread().getId() + " :: " + baseMsg.toString(CharsetUtil.UTF_8).trim());
            }
        });
        client.connect(HOST, PORT);
    }

    private long TIME_TAG = 15 * 1000;

    @Override
    public void onClick(View v) {
//        String message = ed_msg.getText().toString();
//        client.post(message);

        handler.sendEmptyMessageDelayed(MESSAGE_DIS_CONNECT, TIME_TAG);

    }


    private final static int MESSAGE_CONNECT = 1;
    private final static int MESSAGE_DIS_CONNECT = 2;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_CONNECT:
                    client.reconnect();
                    break;
                case MESSAGE_DIS_CONNECT:
                    client.disconnect();
                    this.sendEmptyMessageDelayed(MESSAGE_CONNECT, TIME_TAG);
                    break;
            }
            client.reconnect();
        }
    };


    private void initView() {
        tv_msg = (TextView) findViewById(R.id.tv_text);
        ed_msg = (EditText) findViewById(R.id.et_edit);
        btn_send = (Button) findViewById(R.id.btn_send);
        btn_send.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
