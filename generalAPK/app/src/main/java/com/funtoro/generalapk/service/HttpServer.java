package com.funtoro.generalapk.service;

//import static com.funtoro.generalapk.service.constant.ERR_WRONG_TIMEMODE;
//import static com.funtoro.generalapk.service.constant.INCORREC_TIME_DATA_FORMAT;


import static com.funtoro.generalapk.service.constant.DEFAULT_LANGUAGE;
import static com.funtoro.generalapk.service.constant.ERR_WRONG_TIMEMODE;
import static com.funtoro.generalapk.service.constant.INCORREC_TIMEZONE_CODE;
import static com.funtoro.generalapk.service.constant.INCORREC_TIME_DATA_FORMAT;

import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.TimeZone;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;


import javax.security.auth.login.LoginException;

import fi.iki.elonen.NanoHTTPD;


public class HttpServer extends NanoHTTPD {
private Context context;
private constant constant;
public int port;
public int CurrentHourFormat;
public String result;
public  Socket mySocket=new Socket();
public Boolean isConnect=false;
    ServerSocket serverSocket;
    DataOutputStream output;   //向客户端发送信息的OutPutStream
ServerSocketThread serverSocketThread=new ServerSocketThread();

    AudioManager audioManager;
    String TAG = "HttpServer";


    public HttpServer(int port,Context context){
        super(port);
        this.port = port;
        this.context=context;
        Log.i("HttpServer", "HttpServer启动成功: ");
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void stop() {
        super.stop();

    }

    class ServerSocketThread extends Thread {

        @Override
        public void run() {
            try {
                // 创建ServerSocket
                System.out.println("--开启服务器，监听端口 8090--");

               serverSocket = new ServerSocket(8090);


                // 监听端口，等待客户端连接

                    System.out.println("--等待客户端连接--");
                    mySocket = serverSocket.accept(); //等待客户端连接
//                    isConnect=mySocket.isConnected();
                    System.out.println(mySocket.isConnected());
                    System.out.println("得到客户端连接：" + mySocket);
                    DataInputStream dis = new DataInputStream(mySocket.getInputStream());
                    String msgRecv = dis.readUTF(); // 会出现阻塞状态
                    System.out.println("msg from client:"+msgRecv);

                output = new DataOutputStream(mySocket.getOutputStream()); // 用于向客户端发送数据

                // 发送自定义消息到客户端
                output.writeUTF("连接成功");
                output.flush();



            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        // 处理HTTP请求
        Log.i(TAG, "开始处理http请求: ");
        Method method = session.getMethod();
        String uri = session.getUri();
        System.out.println("Received " + method + " request to: " + uri);
        Log.i(uri, "Received " + method + " request to: " + uri);


        if(uri.equals("/general/timeInfo"))
        {
            Log.i("requset", "执行时间信息查找");
            String timeInfo = GetTimeInfo();
            return newFixedLengthResponse(timeInfo);
        }

        else if(uri.equals("/general/setTime"))
        {
            Map<String, String> parms = session.getParms();

            try {


                if (parms.get("isntp").equals("1")) {
                    Log.i("requset", "执行自动时间修改");
                    String hourFormat = parms.get("hourFormat");
                    String NTPHost = parms.get("ntpHost");
                    String language = parms.get("language");
                    String result = UseNTP(hourFormat, NTPHost, language);
                    return newFixedLengthResponse(result);
                } else if (parms.get("isntp").equals("0")) {
                    Log.i("requset", "执行手动时间修改");
                    String date = parms.get("date");
                    String time = parms.get("time");
                    String timeZone = parms.get("timeZone");
                    String hourFormat = parms.get("hourFormat");
                    String language = parms.get("language");
                    String setTime = SetTime(date, time, timeZone, hourFormat, language);
                    return newFixedLengthResponse(setTime);
                }
            }catch (NullPointerException e)
            {
                Log.w(TAG, "isNTP", e);
                String ErrTimeMode=TiptoJson("IsNTP Parameter Error only support 0=manual 1=use ntp",ERR_WRONG_TIMEMODE);
                return newFixedLengthResponse(ErrTimeMode);
            }
        }


        else if (uri.equals("/general/reset")) {
            Map<String, String> parms = session.getParms();
            Boolean wipeSDCard = Boolean.valueOf(parms.get("aaa").equals("1"));
            String result=resetSystem(wipeSDCard);
            return newFixedLengthResponse(result);
        }
        else if(uri.equals("/general/reboot"))
        {
            String result = reboot();
            return newFixedLengthResponse(result);
        }

        else if (uri.equals("/send"))
        {
            Map<String, String> parms = session.getParms();
            String message = parms.get("message");
            Log.i("send", "像前端发送的是"+message);
            sendMessage(message);
            return newFixedLengthResponse(result);
        }
        else if (uri.equals("/getVolume"))
        {
            String result=getVolume();
            return newFixedLengthResponse(result);
        }
        else if (uri.equals("/setVolume"))
        {
            Map<String, String> parms = session.getParms();
            String RingVolume = parms.get("ringVolume");
            String AlarmVolume = parms.get("alarmVolume");
            String musicVolume = parms.get("musicVolume");
            String callVolume = parms.get("callVolume");
            String result=setVolume(RingVolume,AlarmVolume,musicVolume,callVolume);
            return newFixedLengthResponse(result);
        }
        else if (uri.equals("/getAudio"))
        {
            String result=getAudio();
            return newFixedLengthResponse(result);
        } else if (uri.equals("/setHeadset")) {
            changeToHeadset();
            return newFixedLengthResponse("setAudio");
        }
        else if (uri.equals("/setBlueTooth")) {
            changeToBlueTooth();
            return newFixedLengthResponse("setAudio");
        }
        else if (uri.equals("/setSpeakerphone")) {
            changeToSpeakerphone();
            return newFixedLengthResponse("setAudio");
        }



        Log.i("false", "无法匹配路径404");
        return newFixedLengthResponse("404");
    }

    private String setVolume(String RingVolume,String AlarmVolume,String musicVolume,String callVolume)
    {
        if (!(RingVolume==null))
        {audioManager.setStreamVolume(AudioManager.STREAM_RING, Integer.parseInt(RingVolume), AudioManager.FLAG_PLAY_SOUND);}
        if (!(AlarmVolume==null))
        {audioManager.setStreamVolume(AudioManager.STREAM_ALARM, Integer.parseInt(AlarmVolume), AudioManager.FLAG_PLAY_SOUND);}
        if (!(musicVolume==null))
        {audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, Integer.parseInt(musicVolume), AudioManager.FLAG_PLAY_SOUND);}
        if (!(callVolume==null))
        {audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, Integer.parseInt(callVolume), AudioManager.FLAG_PLAY_SOUND);}
        return TiptoJson("setVolume success",200);
    }

    private String getVolume(){
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        int callVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);  //Max=5
        int musicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);      //Max=15
        int RingVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);       //Max=7
        int AlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);      //Max=7



        JSONObject data= new JSONObject();
        try {
            data.put("ringVolume",RingVolume);
            data.put("alarmVolume",AlarmVolume);
            data.put("musicVolume",musicVolume);
            data.put("callVolume",callVolume);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return toJson(data,200);
    }
 private String getAudio()
 {
     AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
     JSONArray data= new JSONArray();
     for (AudioDeviceInfo device : devices) {
         int deviceType = device.getType();
         String deviceProductName = device.getProductName().toString();
         int deviceId = device.getId();
         // 还可以获取其他设备信息，具体取决于你的需求

         Log.d(TAG, "AudioDeviceId: "+ deviceId+", Device Type: " + deviceType + ", Device Name: " + deviceProductName);
         JSONObject JSdevice= new JSONObject();
         try {
             JSdevice.put("AudioDeviceId",deviceId);
             JSdevice.put("DeviceType",deviceType);
             JSdevice.put("DeviceName",deviceProductName);
         } catch (JSONException e) {
             throw new RuntimeException(e);
         }
         data.put(JSdevice);
     }

     JSONObject result= new JSONObject();
     try {
         result.put("msg","success");
         result.put("code",200);
         result.put("data",data);
     } catch (JSONException e) {
         throw new RuntimeException(e);
     }
     return result.toString();

 }

    /**
     * 切换至头戴式耳机（非蓝牙、扬声器）
     */
    private void changeToHeadset() {

        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        audioManager.setBluetoothA2dpOn(false);
//        audioManager.setBluetoothScoOn(true);
//        audioManager.startBluetoothSco();
        audioManager.setSpeakerphoneOn(false);

        AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

    }

    /**
     * 切换至扬声器
     */
    private void changeToSpeakerphone() {

        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        audioManager.setBluetoothA2dpOn(false);
//        audioManager.setBluetoothScoOn(true);
//        audioManager.startBluetoothSco();
        audioManager.setSpeakerphoneOn(true);



    }

    /**
     * 切换至蓝牙A2DP
     */
    private void changeToBlueTooth() {
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setBluetoothScoOn(false);
        audioManager.stopBluetoothSco();
        audioManager.setBluetoothA2dpOn(true);
        audioManager.setSpeakerphoneOn(false);
    }



    private String GetTimeInfo() {

            ContentResolver cv = context.getContentResolver();
            //get time format
            String format = android.provider.Settings.System.getString(cv, android.provider.Settings.System.TIME_12_24);
            if(format.equals(null))
            {format="24";}
            //get time zone
            TimeZone Zone = TimeZone.getDefault();
//            String timeZone=Zone.getDisplayName();
            String timeZone=Zone.getID();
            //get date and time
            long timecurrentTimeMillis = System.currentTimeMillis();
            SimpleDateFormat dateFormat =new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault());
            SimpleDateFormat timeFormat =new SimpleDateFormat("HH:mm:ss",Locale.getDefault());
            String date = dateFormat.format(timecurrentTimeMillis);
            String time = timeFormat.format(timecurrentTimeMillis);
            //get language
            SharedPreferences preferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
            String language = preferences.getString("language", DEFAULT_LANGUAGE); // 默认值是空字符串

            Log.i(TAG, "GetTimeFormat: "+format);
            Log.i(TAG, "GetTimeZone: "+timeZone);
            Log.i(TAG, "GetTime: "+date+" "+time);
            Log.i(TAG, "GetLanguage: "+language);
            JSONObject data= new JSONObject();
        try {
            data.put("timeformat",format);
            data.put("timezone",timeZone);
            data.put("date",date);
            data.put("time",time);
            data.put("language",language);
            return toJson(data,200);
        } catch (Exception e) {
            e.printStackTrace();
            return toJson(null,500);
        }
    }





    /**
     * @param date
     * @param time
     * @param timeZone
     * @param hourFormat
     * @param language
     */
    private String SetTime(String date,String time,String timeZone,String hourFormat, String language)
    {
        String dateAndTime= date + " " + time;
        try {
            //set time
            long timecurrentTimeMillis = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            timecurrentTimeMillis = sdf.parse(dateAndTime).getTime();
            Settings.Global.putInt(context.getContentResolver(), Settings.Global.AUTO_TIME, 0);
            Settings.Global.putInt(context.getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 0);
            SystemClock.setCurrentTimeMillis(timecurrentTimeMillis);
        }
            catch (Exception e) {
                Log.w(TAG, "SetTime: ", e);
                return TiptoJson("The wrong time format is provided",INCORREC_TIME_DATA_FORMAT);
            }
            //set time zone
            AlarmManager am = (AlarmManager) ContextCompat.getSystemService(context, AlarmManager.class);

        try {
            am.setTimeZone(timeZone);
            Log.i(TAG, "SetTimeZone: " + timeZone);
        }catch (Exception e)
        {
            Log.w(TAG, "SetTime: ", e);
            return TiptoJson("The wrong Time Zone Code is provided",INCORREC_TIMEZONE_CODE);
        }

            //set hour format
            Settings.System.putInt(context.getContentResolver(), Settings.System.TIME_12_24, Integer.valueOf(hourFormat));
            Log.i(TAG, "SetTime: "+dateAndTime);
            Log.i(TAG, "SetHourFormat: "+hourFormat);
            SetLanauage(language);
            return toJson(null,200);


    }
    /**
     * @param hourFormat
     * @param NTPHost
     * NTPHost: cn.pool.ntp.org
     */
    private String UseNTP(String hourFormat,String NTPHost,String language)
    {
        try {
            //use NTP to set time
            Settings.Global.putInt(context.getContentResolver(), Settings.Global.AUTO_TIME, 1);
            Settings.Global.putInt(context.getContentResolver(), Settings.Global.AUTO_TIME_ZONE, 1);
            Settings.System.putInt(context.getContentResolver(), Settings.System.TIME_12_24, Integer.valueOf(hourFormat));
            new Thread(){
                @Override
                public void run() {
                    SntpClient sntpClient = new SntpClient();
                    if (sntpClient.requestTime(NTPHost, 8000)) {
                        long now = sntpClient.getNtpTime() + SystemClock.elapsedRealtime() - sntpClient.getNtpTimeReference();
                        Date current = new Date(now);
                        Log.d(TAG, current.toString());
                    }
                }
            }.start();
            Log.i("UseNTP", "UseNTP: "+"success");
            SetLanauage(language);
            Log.i(TAG, "SetLanauage: "+"success");
            return toJson(null,200);
        }
        catch (Exception e) {
            e.printStackTrace();
            return toJson(null,500);
        }

    }
    private void SetLanauage(String language)
    {
        SharedPreferences preferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("language", language); // 保存一个字符串
        editor.apply();
    }


    public String resetSystem(Boolean wipeSDCard) {
        Intent intent = new Intent("android.intent.action.MASTER_CLEAR");
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.setPackage("android");
        intent.putExtra("android.intent.extra.REASON", "FactoryMode");
        //Erase the SdCard
        intent.putExtra("android.intent.extra.WIPE_EXTERNAL_STORAGE", wipeSDCard);
        //Erase the SIMCard
        intent.putExtra("android.intent.extra.EXTRA_WIPE_ESIMS", false);
        //Erases all user data, application data, system Settings, and external storage
        intent.putExtra("android.intent.extra.EXTRA_WIPE_ALL", true);
        //Force the master clear
        intent.putExtra("android.intent.extra.FORCE_MASTER_CLEAR", false);
        context.sendBroadcast(intent);
        Log.i(TAG, "resetSystem success");
        return TiptoJson("resetSystem success",200);
    }

    public String reboot(){
        Intent reboot = new Intent(Intent.ACTION_REBOOT);
        reboot.putExtra("nowait", 1);
        reboot.putExtra("interval", 1);
        reboot.putExtra("window", 0);
        context.sendBroadcast(reboot);
        Log.i(TAG,"rebootSystem success");
        return TiptoJson("rebootSystem success",200);
    }

    public void resetSetting(){
        ContentResolver contentResolver = context.getContentResolver();

        // 手动重设系统设置
        Settings.System.putInt(contentResolver, Settings.Global.AUTO_TIME, 1);
        Settings.System.putInt(contentResolver, Settings.Global.AUTO_TIME_ZONE, 1);
        Settings.System.putInt(contentResolver, Settings.System.TIME_12_24, 24);

    }


    /**
     *传入json数据以及状态码，返回封装后的String
     */
    public String toJson(JSONObject data,int code)
    {
            JSONObject result = new JSONObject();

            try {
                if (code == 200) {
                    result.put("msg", "success");
                } else {
                    result.put("msg", "Unknown exception occurs");
                }
                result.put("code", code);
                result.put("data", data);
            }  catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return result.toString();
    }
    public String TiptoJson(String err,int code)
    {
        JSONObject result = new JSONObject();

        try {
            result.put("msg", err);

            result.put("code", code);

        }  catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return result.toString();
    }


    public void sendMessage(String message) {
        if (!isConnect) {
            isConnect=true;
            ServerSocketThread serverSocketThread = new ServerSocketThread();
            serverSocketThread.start();
            result="正在连接";
        } else if(mySocket.isConnected()){

            try {
                // 用于向客户端发送数据
                // 发送自定义消息到客户端
                output.writeUTF(message);
                output.flush();
                Log.i("send","发送消息:"+message);
                result="发送消息:"+message;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        else{
            result="客户端未连接";
        }
    }

}
