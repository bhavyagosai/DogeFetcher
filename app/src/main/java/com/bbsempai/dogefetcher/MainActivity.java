package com.bbsempai.dogefetcher;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    static Boolean fetchData = false;

    private TextView displayText,terminateApp, getNotification, stopNotification, updateNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AndroidNetworking.initialize(getApplicationContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int highImportance = NotificationManager.IMPORTANCE_HIGH;
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            NotificationChannel singleChannel = new NotificationChannel("SingleNotification", "SingleNotification", highImportance);
            notificationManager.createNotificationChannel(singleChannel);
        }

        displayText = (TextView)findViewById(R.id.displayText);

        getNotification = (Button)findViewById(R.id.button1);
        getNotification.setOnClickListener(this);

        stopNotification = (Button)findViewById(R.id.button2);
        stopNotification.setOnClickListener(this);

        updateNotification = (Button)findViewById(R.id.button3);
        updateNotification.setOnClickListener(this);

        terminateApp = (Button)findViewById(R.id.terminateButton);
        terminateApp.setOnClickListener(this);


        if (!isMyServiceRunning(CryptoFetchService.class))
            displayText.setText("Not fetching any data");
        else
            displayText.setText("Data is being fetched in the background");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button1:
                fetchData = true;
//                if (!isMyServiceRunning(CryptoFetchService.class))
                    startService(new Intent(this, CryptoFetchService.class));
//                else {
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//                        startForegroundService(new Intent(this, CryptoFetchService.class));
//                    else
//                        startService(new Intent(this, CryptoFetchService.class));
//                }
                displayText.setText("Data fetching started!\nCheck notifications for updates\n\nYou can close me now if you want :)");
                Toast.makeText(MainActivity.this, "Initiating fetching data", Toast.LENGTH_LONG).show();
                break;
            case R.id.button2:
                getCryptoData(false);
                break;
            case R.id.button3:
                fetchData = false;
                stopNotification();
//                new CryptoFetchService().stopServiceNotification();
                stopService(new Intent(this, CryptoFetchService.class));
                displayText.setText("Data fetching is aborted");
                Toast.makeText(MainActivity.this, "Aborting fetching data", Toast.LENGTH_LONG).show();
                break;
            case R.id.terminateButton:
//                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
                break;
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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
                            if(!silent)
                                displaySingleNotification(DogeCoinPrice);
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

    private void displaySingleNotification(String displayData){
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "SingleNotification")
                .setSmallIcon(R.drawable.ic_doge)
                .setContentTitle("DOGE/INR")
                .setContentText(displayData == null ? "Fetching Data..." : displayData)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(displayData == null ? "Fetching Data..." : displayData))
                .setOngoing(false)
                .setSubText("DogeCoin")
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.doge_round))
                .setPriority(NotificationCompat.PRIORITY_MAX);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(1, builder.build());
    }

    public void stopNotification() {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.cancelAll();
    }
}