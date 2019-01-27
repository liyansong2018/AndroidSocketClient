package com.xidian.liyansong.androidsocket;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.xidian.liyansong.androidsocket.Utils.SoftKeyboardUtils;
import com.xidian.liyansong.androidsocket.Utils.StreamTool;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.PushbackInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.xidian.liyansong.androidsocket.R.styleable.MenuItem;

/**
 * Created by Liyansong on 2019/1/4.
 */

public class MainActivity extends AppCompatActivity {
    private EditText editIp, editPort;
    private Button btnConnect, btnBreak;
    private ImageButton connectState, menu;

//    private BufferedReader in = null;
//    private PrintWriter out = null;
    private static String ip = "192.168.42.166";
    private static int port = 8888;

    private Handler mMainHandler;// 线程池
    private ExecutorService mThreadPool;

    String response;
//    OutputStream outputStream;

    Socket socket = null;
    private LocalBroadcastManager localBroadcastManager;
    public static final String action = "server.message.broadcast.action";
    private final String stateKey = "9875a1b2957c223f33e67d6f9940c4d6";


    // 控件初始化
    private void init(){
        editIp = (EditText) findViewById(R.id.edit_ip);
        editPort = (EditText) findViewById(R.id.edit_port);
        btnConnect = (Button) findViewById(R.id.btn_connect);
        btnBreak = (Button) findViewById(R.id.btn_break);
        connectState = (ImageButton) findViewById(R.id.connect_state);
        menu = (ImageButton) findViewById(R.id.menu);

        mThreadPool = Executors.newCachedThreadPool();

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();


        // 实例化主线程，用于更新子线程传递的消息
        mMainHandler = new Handler() {
            ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case 0:
                        // 状态：已连接
                        setTitle(R.string.connected);
                        connectState.setImageResource(R.drawable.connected);
                        break;
                    case 1:
                        // 状态：未连接
                        setTitle(R.string.unconnected);
                        connectState.setImageResource(R.drawable.unconnected);
                        break;
                    case 2:
                        // 提示IP输入错误
                        String tmp = (String) getBaseContext().getResources().getText(R.string.ip_error);
                        Toast.makeText(MainActivity.this, tmp ,Toast.LENGTH_SHORT).show();
                        break;
                    case 3:
                        // 退出软键盘
                        if (SoftKeyboardUtils.isSoftShowing(MainActivity.this)){
                            SoftKeyboardUtils.showORhideSoftKeyboard(MainActivity.this);
                        }else {
                            onBackPressed();
                        }
                        break;
                    case 4:
                        // 服务器已连接
                        String tmp1 = (String) getBaseContext().getResources().getText(R.string.connected);
                        Toast.makeText(MainActivity.this, tmp1, Toast.LENGTH_SHORT).show();
                        break;
                    case 5:
                        // 服务器未连接
                        String tmp2 = (String) getBaseContext().getResources().getText(R.string.unconnected);
                        Toast.makeText(MainActivity.this, tmp2, Toast.LENGTH_SHORT).show();
                        break;
                    case 6:
                        // 服务器正在连接...
                        String tmp3 = (String) getBaseContext().getResources().getText(R.string.unconnected);
                        Toast.makeText(MainActivity.this, tmp3, Toast.LENGTH_SHORT).show();
                        break;
                    case 7:
                        // 连接失败！
                        String tmp7 = (String) getBaseContext().getResources().getText(R.string.unsuccessed);
                        Toast.makeText(MainActivity.this, tmp7, Toast.LENGTH_SHORT).show();
                        break;
                    case 8:
                        // 连接成功！
                        String tmp8 = (String) getBaseContext().getResources().getText(R.string.successed);
                        Toast.makeText(MainActivity.this, tmp8, Toast.LENGTH_SHORT).show();
                        break;
                    case 9:
                        // 加载动画
                        String tmp9 = (String) getBaseContext().getResources().getText(R.string.connecting);
                        // progressDialog.setTitle(tmp9);
                        progressDialog.setMessage(tmp9);
                        progressDialog.show();
                        break;
                    case 10:
                        Toast.makeText(MainActivity.this, "退出", Toast.LENGTH_SHORT).show();
                        break;
                    case 11:
                        progressDialog.dismiss();
                        break;

                }
            }
        };

        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                //创建弹出式菜单对象（最低版本11）
                android.widget.PopupMenu popup = new android.widget.PopupMenu(MainActivity.this, v);//第二个参数是绑定的那个view
                //获取菜单填充器
                MenuInflater inflater = popup.getMenuInflater();
                //填充菜单
                inflater.inflate(R.menu.main, popup.getMenu());
                //绑定菜单项的点击事件
                //显示(这一行代码不要忘记了)
                popup.show();

                popup.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(android.view.MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.menu_chat:
                                String tmp = (String)getBaseContext().getResources().getText(R.string.chat);
                                Toast.makeText(MainActivity.this, tmp, Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                                startActivity(intent);
                                break;
                            case R.id.menu_upload:
                                String tmp1 = (String)getBaseContext().getResources().getText(R.string.upload);
                                Toast.makeText(MainActivity.this, tmp1, Toast.LENGTH_SHORT).show();
                                Intent intent1 = new Intent(MainActivity.this, UploadActivity.class);
                                startActivity(intent1);
                                break;
                            case R.id.menu_help:
                                String tmp2 = (String)getBaseContext().getResources().getText(R.string.help);
                                Toast.makeText(MainActivity.this, tmp2, Toast.LENGTH_SHORT).show();
                                break;
                            default:
                                break;
                        }
                        return false;
                    }
                });
            }
        });


//        btnChat.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v){
//                mThreadPool.execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
//                        startActivity(intent);
//                    }
//                });
//            }
//        });

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                /*
                * 使用线程池创建一个线程，连接服务器，使用死循环等待服务器发送来的数据
                * 读取服务器发送来的数据，并且通过Handler发送给UI线程
                */

                mThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        if(!TextUtils.isEmpty(editIp.getText()) && !TextUtils.isEmpty(editPort.getText())){
                            ip = editIp.getText().toString().trim();
                            String portOringal = editPort.getText().toString().trim();
                            port = Integer.valueOf(portOringal);
                        }

//                        Message msgKeyboard = Message.obtain();
//                        msgKeyboard.what = 3;
//                        mMainHandler.sendMessage(msgKeyboard);

                        if(isIp(ip)){
                            try{
                                socket = new Socket();      // 连接服务器
                                SocketAddress socketAddress = new InetSocketAddress(ip, port);
                                sendHandle(9);      // 开启动画
                                socket.connect(socketAddress, 3000);        // 设置超时时间
                                sendHandle(11);
                                if (sendHeartBeat(socket)){
                                    sendHandle(0);      // 修改图标
                                    sendHandle(8);      // 弹框提示 连接成功！
                                }
                            }catch (Exception ex){
                                ex.printStackTrace();
                            }
                        }else {
                            sendHandle(2);
                            Thread.currentThread().interrupt();
                        }

                        ((TransSocket)getApplication()).setSocket(socket);

                        try{
                            while (true) {                      // 死循环守护，监控服务器发来的消息
                                if (sendHeartBeat(socket)) {       // 如果服务器没有关闭
                                    if (!socket.isInputShutdown()) {  // 如果输入流没有断开
                                        PushbackInputStream inStream = new PushbackInputStream(socket.getInputStream());
                                        String response = StreamTool.readLine(inStream);
                                        sendBroadcast(response);    // 传消息
                                    }
                                }else {
                                    break;
                                }
                            }
                        }catch (Exception ex){
                            ex.printStackTrace();
                        }

                        if (!sendHeartBeat(socket)){
                            sendHandle(11);     // 关闭动画
                            sendHandle(1);
                            sendHandle(7);

                            // 传递给聊天活动，socket已关闭
                            sendBroadcast(stateKey);
                        }
                    }
                });
            }
        });

        btnBreak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(socket != null && socket.isConnected()){
                    try{
                        // 应服务器要求，给服务器发送关闭消息
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(("break connect......").getBytes("utf-8"));
                        outputStream.flush();
                        socket.close();
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }else {
                    sendHandle(5);
                }
            }
        });

        // 连接状态修改弹窗
        connectState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                mThreadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        if(sendHeartBeat(socket)){
                            sendHandle(4);
                        }else {
                            sendHandle(5);
                        }
                    }
                });
            }
        });

        // 监听输入是否为空
        editIp.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(editIp.getText()) || TextUtils.isEmpty(editPort.getText())) {
                    btnConnect.setEnabled(Boolean.FALSE);
                } else {
                    btnConnect.setEnabled(Boolean.TRUE);
                }

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        editPort.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (TextUtils.isEmpty(editIp.getText()) || TextUtils.isEmpty(editPort.getText())) {
                    btnConnect.setEnabled(Boolean.FALSE);
                } else {
                    btnConnect.setEnabled(Boolean.TRUE);
                }

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

    }


    // 检查IP合法性
    private boolean isIp(String str){
        String ip = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pattern = Pattern.compile(ip);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
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

    private void sendBroadcast(String message){
        Intent intent = new Intent(action);
        intent.putExtra(Intent.EXTRA_TEXT, message);
        localBroadcastManager.sendBroadcast(intent);
    }

}
