package com.example.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;

public class ConnectPop extends Activity {

    @Override
    protected  void onCreate(Bundle savedInstanceState){
        super.onCreate((savedInstanceState));

        setContentView(R.layout.connectpopipwindow);

        DisplayMetrics met = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(met);
        int width = met.widthPixels;
        int height = met.heightPixels;

        getWindow().setLayout((int) (width*.8), (int) (height*.8));
    }
}
