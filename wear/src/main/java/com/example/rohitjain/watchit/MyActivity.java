package com.example.rohitjain.watchit;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class MyActivity extends Activity{

    private TextView mTextView;
    private Sensor mHeartRateSensor;
    private String TAG = "Heart";
    private Intent mServiceIntent,mServiceAccIntent;
    private AlarmManager am;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        am=(AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        Log.v(TAG,"starting activity");
        mServiceIntent = new Intent(this, HeartBeatPullService.class);
        mServiceAccIntent = new Intent(this, AccelerometerDataService.class);

        //mServiceIntent.setData();
        //mSensorManager.registerListener( this,this.mHeartRateSensor, 3);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG,"trying to connect to phone");
//        int i=1;
//        while(i<100){
//            Log.v(TAG,"yo");
//            i++;
//        }
        this.startService(mServiceIntent);
        this.startService(mServiceAccIntent);
        //PendingIntent pi = PendingIntent.getService(this, 0, mServiceIntent, 0);
        //PendingIntent pi_1 = PendingIntent.getService(this, 0, mServiceAccIntent, 0);
        //am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 1 , pi);
        //am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 1 , pi_1);

        //registerSensorManagerListeners();

    }




    @Override
    protected void onResume() {
        super.onResume();
        //registerSensorManagerListeners();

    }

    @Override
    protected void onPause() {
        super.onPause();
        //mSensorManager.unregisterListener(this);
    }


    @Override
    protected void onStop() {
        super.onStop();

        //this.stopService(mServiceIntent);
        //mSensorManager.unregisterListener(this);
    }




}
