package cn.ymex.socket;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

import cn.ymex.cute.log.L;
import cn.ymex.cute.socket.ByteString;
import cn.ymex.cute.socket.ClientConfig;
import cn.ymex.cute.socket.Listener;
import cn.ymex.cute.socket.PacketData;
import cn.ymex.cute.socket.ResponsePacketData;
import cn.ymex.cute.socket.SocketClient;
import cn.ymex.cute.socket.Status;
import cn.ymex.socket.netty.NettyClientBootstrap;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tv_msg = null;
    private EditText ed_msg = null;
    private Button btn_send = null;


    public static final String HOST = "192.168.6.111";
    public static int PORT = 60000;

    NettyClientBootstrap nettyStart=new NettyClientBootstrap();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();


    }


    @Override
    public void onClick(View v) {
        try {
            nettyStart.startNetty();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
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
