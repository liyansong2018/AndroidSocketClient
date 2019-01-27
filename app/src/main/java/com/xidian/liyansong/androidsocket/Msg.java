package com.xidian.liyansong.androidsocket;

/**
 * Created by Liyansong on 2019/1/5.
 */

// 消息类实体
public class Msg {
    public static final int TYPE_RECEIVED = 0;
    public static final int TYPE_SENT = 1;
    private String content;
    private int type;

    public Msg(String content, int type){
        this.content = content;
        this.type = type;
    }

    public String getContent(){
        return content;
    }

    public int getType(){
        return type;
    }
}
