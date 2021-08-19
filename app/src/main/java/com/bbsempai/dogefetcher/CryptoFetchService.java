package com.bbsempai.dogefetcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import org.json.JSONException;
import org.json.JSONObject;

public class CryptoFetchService extends Service {
    private final LocalBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public CryptoFetchService getService() {
            return CryptoFetchService .this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AndroidNetworking.initialize(getApplicationContext());
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            startMyOwnForeground();
        }
        else
            startForeground(1, new Notification());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        new ContinuousFetch().start();
        return android.app.Service.START_STICKY;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground()
    {
        String NOTIFICATION_CHANNEL_ID = "example.permanence";
        String channelName = "Background Service";
        int lowImportance = NotificationManager.IMPORTANCE_LOW;
        int highImportance = NotificationManager.IMPORTANCE_HIGH;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(chan);

            NotificationChannel channel = new NotificationChannel("MyNotification", "MyNotification", lowImportance);
            notificationManager.createNotificationChannel(channel);

            NotificationChannel singleChannel = new NotificationChannel("SingleNotification", "SingleNotification", highImportance);
            notificationManager.createNotificationChannel(singleChannel);
        }

        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_doge)
                .setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(1337, notification);
    }

    public void getCryptoData(Boolean silent){
        AndroidNetworking.get("https://api.wazirx.com/api/v2/tickers/")
                .setPriority(Priority.HIGH)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONObject DogeCoinData = (JSONObject) response.get("dogeinr");
                            String DogeCoinPrice = DogeCoinData.getString("last");
                            if(silent)
                                startNotification(DogeCoinPrice);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError anError) {
                        anError.printStackTrace();
                    }
                });
    }

    private void startNotification(String displayData) {
        Intent intent = new Intent(CryptoFetchService.this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(CryptoFetchService.this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "MyNotification")
                .setSmallIcon(R.drawable.ic_doge)
                .setContentTitle("DOGE/INR")
                .setContentText(displayData == null ? "Fetching Data..." : displayData)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(displayData == null ? "Fetching Data..." : displayData))
                .setOngoing(true)
                .setSubText("DogeCoin")
                .setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.doge_round))
                .setPriority(NotificationCompat.PRIORITY_MAX);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(3, builder.build());
    }

    private class ContinuousFetch extends Thread{
        @Override
        public void run() {
            while(MainActivity.fetchData){
                getCryptoData(true);
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class stopBackgroundNotifications{
        public void stop() {
            stopForeground(true);
        }
    }

    public void stopServiceNotification() {
        stopBackgroundNotifications stop = new stopBackgroundNotifications();
        stop.stop();
    }
}