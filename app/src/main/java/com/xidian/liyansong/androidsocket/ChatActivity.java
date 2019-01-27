package com.xidian.liyansong.androidsocket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Liyansong on 2019/1/5.
 */

public class ChatActivity extends AppCompatActivity {
    private List<Msg> msgList = new ArrayList<>();
    private EditText inputText;
    private Button send;
    private ImageButton connectState;
    private ImageButton menu, back;
    private TextView title;
    private RecyclerView msgRecyclerView;
    private MsgAdapter adapter;

    private IntentFilter intentFilter;
    private LocalReceiver localReceiver;
    private LocalBroadcastManager localBroadcastManager;

    private Socket socket;
    OutputStream outputStream;
    private Handler mMainHandler;// 线程池
    private ExecutorService mThreadPool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);
//        ActionBar actionBar = getSupportActionBar();
//        if(actionBar != null){
//            actionBar.hide();
//        }


        mThreadPool = Executors.newCachedThreadPool();
        inputText = (EditText) findViewById(R.id.input_text);
        send = (Button) findViewById(R.id.send);
        connectState = (ImageButton) findViewById(R.id.connect_state);
        back = (ImageButton) findViewById(R.id.button_backward);
        menu = (ImageButton) findViewById(R.id.menu);
        menu.setVisibility(View.GONE);
        back.setVisibility(View.VISIBLE);

        title = (TextView) findViewById(R.id.bar_title);
        msgRecyclerView = (RecyclerView) findViewById(R.id.msg_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        msgRecyclerView.setLayoutManager(layoutManager);
        adapter = new MsgAdapter(msgList);
        msgRecyclerView.setAdapter(adapter);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);    // 获取广播管理实例
        intentFilter = new IntentFilter();
        intentFilter.addAction(MainActivity.action);
        localReceiver = new LocalReceiver();
        localBroadcastManager.registerReceiver(localReceiver, intentFilter);    // 注册本地广播监听器

        String tmp = (String) getBaseContext().getResources().getText(R.string.unconnected);
        title.setText(tmp);

        // 实例化主线程，用于更新子线程传递的消息
        mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String tmp;
                switch (msg.what){
                    case 0:
                        // 已连接
                        InetAddress serverIp = socket.getInetAddress();
                        tmp = (String) getBaseContext().getResources().getText(R.string.state);
                        title.setText(serverIp.getHostAddress() + tmp);
                        connectState.setImageResource(R.drawable.connected);
                        break;
                    case 1:
                        // 未连接
                        tmp = (String) getBaseContext().getResources().getText(R.string.unconnected);
                        title.setText(tmp);
                        connectState.setImageResource(R.drawable.unconnected);
                        break;
                }
            }
        };

        socket = ((TransSocket)getApplication()).getSocket();

        if(sendHeartBeat(socket)){
            initMsgs();     // 初始化消息数据
            sendHandle(0);
        }

        send.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String content = inputText.getText().toString();
                if(!"".equals(content)){
                    Msg msg = new Msg(content, Msg.TYPE_SENT);
                    msgList.add(msg);
                    adapter.notifyItemInserted(msgList.size() - 1);     // 有消息时 刷新ListView
                    msgRecyclerView.scrollToPosition(msgList.size() - 1);          // 将ListView定位到最后一行
                    inputText.setText("");      // 清空输入框的内容

                    try{
                        if(socket != null && socket.isConnected()){
                            outputStream = socket.getOutputStream();
                            outputStream.write((content.toString()+"\n").getBytes("utf-8"));
                            outputStream.flush();
                        }
                    }catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        back.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                finish();
            }
        });
    }

    private void initMsgs(){

        Msg msg1 = new Msg("This is " + ((TransSocket)getApplication()).getRemoteHost(), Msg.TYPE_RECEIVED);
        msgList.add(msg1);

        Msg msg2 = new Msg("This is " + ((TransSocket)getApplication()).getLocalHost(), Msg.TYPE_SENT);
        msgList.add(msg2);
    }

    class LocalReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent){
            String content = intent.getStringExtra(Intent.EXTRA_TEXT);
            if(content.equals("9875a1b2957c223f33e67d6f9940c4d6")){
                sendHandle(1);
            }else {
                Msg msg = new Msg(content, Msg.TYPE_RECEIVED);
                msgList.add(msg);
                adapter.notifyItemInserted(msgList.size() - 1);     // 有消息时 刷新ListView
                msgRecyclerView.scrollToPosition(msgList.size() - 1);          // 将ListView定位到最后一行
            }

        }
    }

    protected void onDestory(){
        super.onDestroy();
        localBroadcastManager.unregisterReceiver(localReceiver);
    }

    // 心跳检测
    private boolean sendHeartBeat(Socket socket) {
        try {
            socket.sendUrgentData(0xFF);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // 给主线程发送消息，更新UI
    private void sendHandle(int what){
        Message msg = Message.obtain();
        msg.what = what;
        mMainHandler.sendMessage(msg);
    }
}