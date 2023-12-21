package com.funtoro.generalapk;




import androidx.appcompat.app.AppCompatActivity;


import android.annotation.SuppressLint;

import android.content.Intent;
import android.os.Bundle;

import com.funtoro.generalapk.service.MainService;


public class MainActivity extends AppCompatActivity {

    /**
     * adb授予权限
     * adb shell pm grant com.funtoro.generalapk android.permission.WRITE_SECURE_SETTINGS
     */
    public String is24Hour;
    @SuppressLint("Range")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent serviceIntent = new Intent(this, MainService.class);
        startService(serviceIntent);

    }






}