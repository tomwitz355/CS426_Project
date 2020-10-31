package com.example.iotvoiceassistant;

public class Item {


    private int mImageResource;
    private String mText1;
    private String mText2;
    private String IP;
    private String Port;

    public Item(int imageResource, String text1, String text2) {
        mImageResource = imageResource;
        mText1 = text1;
        mText2 = text2;
        IP = mText1;
        Port = mText2;
    }

    public void changeText1(String text) {
        mText1 = text;
    }

    public void changeText2(String text) {
        mText2 = text;
    }

    public void changeIP(String text) {
        IP = text;
    }

    public void changePort(String text) {
        Port = text;
    }

    public int getImageResource() {
        return mImageResource;
    }

    public String getText1() {
        return mText1;
    }

    public String getText2() {
        return mText2;
    }

    public String getIP() {
        return IP;
    }

    public String getPort() {
        return Port;
    }

    public void setImResource(int i) {
        mImageResource = i;
    }
}
