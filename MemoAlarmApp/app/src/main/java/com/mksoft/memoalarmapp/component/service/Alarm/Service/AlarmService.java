package com.mksoft.memoalarmapp.component.service.Alarm.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.mksoft.memoalarmapp.DB.MemoReposityDB;
import com.mksoft.memoalarmapp.DB.data.OptionData;
import com.mksoft.memoalarmapp.R;
import com.mksoft.memoalarmapp.DB.data.MemoData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import dagger.android.AndroidInjection;

public class AlarmService extends Service {
    NotificationManager Notifi_M;
    AlarmServiceThread thread;
    Notification notification;
    NotificationChannel notificationChannel;

    private List<MemoData> memoDataList;
    //private MainActivity activity;
    //private ArrayList<String> tempRandomTime;
    SimpleDateFormat mFormat;
    String time;
    NotificationCompat.Builder notificationFake = null;

    OptionData optionData;

    @Inject
    MemoReposityDB memoReposityDB;

    @Override
    public void onCreate() {
        super.onCreate();
        this.configureDagger();
    }

    private void configureDagger(){

        AndroidInjection.inject(this);//서비스 주입입니당~
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notifi_M = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mFormat=new SimpleDateFormat("yyMMddkkmm");//날짜 형식 지정

        myServiceHandler handler = new myServiceHandler();

        thread = new AlarmServiceThread(handler);
        thread.start();

        Log.d("test","fine");
        return START_STICKY;
    }

    //서비스가 종료될 때 할 작업

    public void onDestroy() {
        thread.stopForever();
        thread = null;//쓰레기 값을 만들어서 빠르게 회수하라고 null을 넣어줌.
    }

    class myServiceHandler extends Handler {

        private NotificationCompat.Builder makeFakeNotification(){
            if(notificationFake != null)
                return notificationFake;
            String fakeCH = "-1";
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(fakeCH, "-1", NotificationManager
                .IMPORTANCE_DEFAULT);
                ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
                notificationFake = new NotificationCompat.Builder(AlarmService.this, fakeCH);
            }
            notificationFake.setSmallIcon(R.drawable.ic_announcement_black_24dp)
                    .setContentText("fake")
                    .setContentTitle("fake");



            return notificationFake;
        }
        private boolean CheckComfort(int hour, int comfortA, int comfortB) {
            if (comfortA <= comfortB)
                return comfortA <= hour && hour < comfortB;
            else
                return comfortA <= hour || hour < comfortB;
        }

        public void checkNotify(MemoData memoData){
            String tempRandomTime = memoData.getRandomTime().substring(memoData.getRandomTime().length()-10,memoData.getRandomTime().length());
            Log.d("tempRT", tempRandomTime);
            Log.d("tempRTraw", memoData.getRandomTime());
            if(tempRandomTime.length() != 0){

                Calendar calendar = Calendar.getInstance();
                time=mFormat.format(calendar.getTime());



                Log.d("tempRT", time);
                if(tempRandomTime.equals(time))
                {
                    if(optionData != null){
                        if (CheckComfort(Integer.parseInt(time.substring(6,8)),
                                optionData.getSleepStartTime(), optionData.getSleepEndTime())){
                            Notifi_M.notify(memoData.getId(), notification);
                            memoData.setRandomTime(memoData.getRandomTime().substring(0, memoData.getRandomTime().length()-10));
                            //방해금지 설정이 되어있고, 방해금지 설정 시간에 들어가면 노티파이를 소리없이 뛰어줌
                        }else{
                            notification.defaults = Notification.DEFAULT_SOUND;
                            Log.d("testHandler", "pass~");
                            Notifi_M.notify(memoData.getId(), notification);
                            memoData.setRandomTime(memoData.getRandomTime().substring(0, memoData.getRandomTime().length()-10));
                        }//방해금지 설정이 되어있고, 방해금지 설정 시간에 들어가지 않는 경우

                    }else{
                        notification.defaults = Notification.DEFAULT_SOUND;
                        Log.d("testHandler", "pass~");
                        Notifi_M.notify(memoData.getId(), notification);
                        memoData.setRandomTime(memoData.getRandomTime().substring(0, memoData.getRandomTime().length()-10));
                    }//방해금지 설정이 되어있지 않음.

                    memoReposityDB.insertMemo(memoData);
                    //스트링 값을 갱신한 메모데이터를 insert하자.

                }else{
                    Date current = null;
                    Date RT = null;
                    try {
                        current = mFormat.parse(time);
                        RT = mFormat.parse(tempRandomTime);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    int compare = current.compareTo(RT);
                    if(compare>0){
                        memoData.setRandomTime(memoData.getRandomTime().substring(0, memoData.getRandomTime().length()-10));
                        //이미 지난 시간....
                        memoReposityDB.insertMemo(memoData);
                        //스트링 값을 갱신한 메모데이터를 insert하자.

                    }else{
                        Log.d("tempRT", "finePass");
                    }


                }
                //startForeground(-1,null);
                //stopForeground(true);

                if(memoData.getRandomTime().length() == 0){

                    //디비에서 지우기

                    Log.d("DBdel", "it");
                    memoReposityDB.deleteMemo(memoData);


                }
            }

        }
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void handleMessage(android.os.Message msg) {
            Log.d("testHandler", "running");
            memoDataList = memoReposityDB.getStaticMemoDataList();
            optionData = memoReposityDB.getOptionData();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                startForeground(-1, makeFakeNotification().build());
                stopForeground(true);
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){



                for(int i =0; i<memoDataList.size(); i++){
                    MemoData memoData = memoDataList.get(i);

                    notificationChannel = new NotificationChannel(String.valueOf(memoData.getId()), "channel1", NotificationManager.IMPORTANCE_DEFAULT);
                    notificationChannel.setDescription("description");
                    notificationChannel.enableLights(true);
                    notificationChannel.setLightColor(Color.GREEN);
                    notificationChannel.enableVibration(true);
                    notificationChannel.setVibrationPattern(new long[]{100, 200, 100, 200});
                    notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                    Notifi_M.createNotificationChannel(notificationChannel);
                    //인텐트에서 받은 메모 초기화 과정...
                    notification = new Notification.Builder(getApplicationContext(), String.valueOf(memoData.getId()))
                            .setContentTitle(memoData.getMemoTitle())
                            .setContentText(memoData.getMemoText())
                            .setSmallIcon(R.drawable.ic_announcement_black_24dp)
                            .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.ic_event_black_24dp))
                            .setAutoCancel(true)
                            .build();


                    checkNotify(memoData);


                }

            }else{

                for(int i =0; i<memoDataList.size(); i++){
                    MemoData memoData = memoDataList.get(i);
                    notification = new Notification.Builder(getApplicationContext())
                            .setContentTitle(memoData.getMemoTitle())
                            .setContentText(memoData.getMemoText())
                            .setTicker("알림!!!")
                            .setSmallIcon(R.drawable.ic_announcement_black_24dp)
                            .build();



                    //알림 소리를 한번만 내도록
                    notification.flags = Notification.FLAG_ONLY_ALERT_ONCE;

                    //확인하면 자동으로 알림이 제거 되도록
                    notification.flags = Notification.FLAG_AUTO_CANCEL;
                    checkNotify(memoData);
                }
            }

        }
    }

}
