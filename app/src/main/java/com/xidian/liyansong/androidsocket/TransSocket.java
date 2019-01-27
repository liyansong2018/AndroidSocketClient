package com.xidian.liyansong.androidsocket;

import android.app.Application;

import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by HackerLee on 2019/1/6.
 */

public class TransSocket extends Application {
    private Socket socket = null;

    public Socket getSocket(){
        return socket;
    }

    public void setSocket(Socket socket){
        this.socket = socket;
    }

    public String getRemoteHost(){
        return socket.getInetAddress().getHostAddress();
    }

    public String getLocalHost(){
        return socket.getLocalAddress().getHostAddress();
    }
}
