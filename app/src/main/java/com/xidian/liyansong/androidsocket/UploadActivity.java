package com.xidian.liyansong.androidsocket;

import java.io.File;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.xidian.liyansong.androidsocket.service.UploadHelper;
import com.xidian.liyansong.androidsocket.Utils.StreamTool;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.LocalSocketAddress;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class UploadActivity extends AppCompatActivity {

    private EditText editFname;
    private Button btnUpload;
    private Button btnStop, btnSelect;
    private ImageButton menu, back;
    private ImageButton connectState;
    private ProgressBar pgbar;
    private TextView txtResult, uploadHost, downloadHost;
    private TextView title;

    private UploadHelper upHelper;
    private boolean flag = true;
    private String position = null;
    private static final int port = 9999;
    private int length;
    private Socket socket;
    private Socket newSocket;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    // 更新UI
//                    pgbar.setProgress(msg.getData().getInt("length"));
                    pgbar.setProgress(length);
                    float num = (float) pgbar.getProgress() / (float) pgbar.getMax();
                    int result = (int) (num * 100);
                    txtResult.setText(result + "%");
                    if (pgbar.getProgress() == pgbar.getMax()) {
                        Toast.makeText(UploadActivity.this, getBaseContext().getResources().getText(R.string.upload_success), Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 1:
                    // 更新用户名
                    uploadHost.setText(((TransSocket)getApplication()).getLocalHost());
                    downloadHost.setText(((TransSocket)getApplication()).getRemoteHost());
                    break;
                case 2:
                    // 上次已上传成功
                    Toast.makeText(UploadActivity.this, getBaseContext().getResources().getText(R.string.uploaded), Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    // 警告
                    AlertDialog.Builder dialog = new AlertDialog.Builder(UploadActivity.this);
                    dialog.setTitle(getBaseContext().getResources().getText(R.string.alert));
                    dialog.setMessage(getBaseContext().getResources().getText(R.string.no_socket));
                    dialog.setCancelable(false);
                    dialog.setPositiveButton(getBaseContext().getResources().getText(R.string.go_to_connect), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    });
                    dialog.setNegativeButton(getBaseContext().getResources().getText(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ;
                        }
                    });
                    dialog.show();
                    break;
                case 4:
                    // 更新标题栏图标
                    connectState.setImageResource(R.drawable.connected);
                    break;
                case 5:
                    // 更新标题栏图标
                    connectState.setImageResource(R.drawable.unconnected);
                    break;
            }
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload);

        // 修改标题栏
        back = (ImageButton) findViewById(R.id.button_backward);
        menu = (ImageButton) findViewById(R.id.menu);
        connectState = (ImageButton) findViewById(R.id.connect_state);
        title = (TextView) findViewById(R.id.bar_title);
        menu.setVisibility(View.GONE);
        back.setVisibility(View.VISIBLE);
        title.setText(getBaseContext().getResources().getText(R.string.upload_title));


        socket = ((TransSocket)getApplication()).getSocket();
        if(socket == null || !sendHeartBeat(socket)){
            sendHandle(3);  // 警告
            sendHandle(5);  // 更新标题栏图标 未连接
        }else {
            sendHandle(1);  // 更新用户名
            sendHandle(4);  // 更新标题栏图标 已连接
        }

        bindViews();
        upHelper = new UploadHelper(this);
    }

    private void bindViews() {
        editFname = (EditText) findViewById(R.id.edit_fname);
        btnUpload = (Button) findViewById(R.id.btn_upload);
        btnStop = (Button) findViewById(R.id.btn_stop);
        btnSelect = (Button) findViewById(R.id.btn_select);
        pgbar = (ProgressBar) findViewById(R.id.pgbar);
        txtResult = (TextView) findViewById(R.id.txt_result);
        uploadHost = (TextView) findViewById(R.id.upload_host);
        downloadHost = (TextView) findViewById(R.id.download_host);

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String filename = editFname.getText().toString();
                File file = new File(filename);
                if(file.exists()){
                    pgbar.setMax((int) file.length());
                    uploadFile(file);
                }else {
                    String temp = (String) getBaseContext().getResources().getText(R.string.file_no_texsit);
                    Toast.makeText(UploadActivity.this, temp, Toast.LENGTH_SHORT).show();
                }
                flag = true;
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = false;
            }
        });

        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");//设置类型，我这里是任意类型，任意后缀的可以这样写。
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent,1);
            }
        });

        back.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                finish();
            }
        });
    }


    // 文件选择器的回调函数
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {         // 是否选择，没选择就不会继续
            Uri uri = data.getData();                    // 得到uri，后面就是将uri转化成file的过程。
            File file = new File(uri.getPath());
            editFname.setText(file.toString());
            Toast.makeText(UploadActivity.this, file.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadFile(final File file) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String sourceid = upHelper.getBindId(file);
//                    Socket socket = ((TransSocket)getApplication()).getSocket();
//                    InetAddress serverIp = socket.getInetAddress();
//                    // 开辟新的socket连接
//                    Socket newSocket = new Socket(serverIp, port);
                    newSocket = new Socket(socket.getInetAddress(), port);
                    OutputStream outStream = newSocket.getOutputStream();
                    String head = "Content-Length=" + file.length() + ";filename=" + file.getName()
                            + ";sourceid=" + (sourceid != null ? sourceid : "") + "\r\n";
                    outStream.write(head.getBytes());

                    // 等待服务器返回数据
                    Thread.currentThread().sleep(1000);
                    PushbackInputStream inStream = new PushbackInputStream(newSocket.getInputStream());
                    position = StreamTool.readLine(inStream);
                    length = Integer.valueOf(position);
                    if (length == file.length()) {
                        upHelper.delete(file);
                        sendHandle(2);
                    }

                    RandomAccessFile fileOutStream = new RandomAccessFile(file, "r");
                    fileOutStream.seek(length);
                    byte[] buffer = new byte[40960];
                    int len = -1;

                    while (flag && (len = fileOutStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, len);
                        length += len;                      //  累加已经上传的数据长度
                        sendHandle(0);
//                        Message msg = new Message();
//                        msg.getData().putInt("length", length);
//                        handler.sendMessage(msg);
                    }
                    if (length == file.length()) {
                        upHelper.delete(file);
                    }
                    String end = "break...";
                    outStream.write(end.getBytes());
                    fileOutStream.close();
                    outStream.close();
                    newSocket.close();
                } catch (Exception e) {
                    Toast.makeText(UploadActivity.this, getBaseContext().getResources().getText(R.string.upload_exception), Toast.LENGTH_SHORT).show();
                }
            }
        }).start();
    }

    // 给主线程发送消息，更新UI
    private void sendHandle(int what){
        Message msg = Message.obtain();
        msg.what = what;
        handler.sendMessage(msg);
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
}
