package com.funtoro.generalapk.service;

import static android.Manifest.permission.SET_TIME_ZONE;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.IOException;

public class MainService extends Service {
    private HttpServer mHttpServer = null;

    @Override
    public void onCreate() {
        //在这里开启HTTP Server。
        Log.i("MainService", "onCreate: 111111");
        if (!Settings.System.canWrite(this)) {
            requestWriteSettingsPermission();
        }

        mHttpServer = new HttpServer(8081,MainService.this);
        try {
            Log.i("MainService", "准备启动");
            mHttpServer.start();
            Log.i("MainService", "启动成功");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDestroy() {
        //在这里关闭HTTP Server
        if (mHttpServer != null)
            mHttpServer.stop();
    }

    private void requestWriteSettingsPermission()   //请求修改系统设置权限
    {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(android.net.Uri.parse("package:" + getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    // 添加请求 SET_TIME_ZONE 权限的方法



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}