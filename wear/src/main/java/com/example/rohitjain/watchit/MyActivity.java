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
    private Intent mServiceIntent;
    private AlarmManager am;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        am=(AlarmManager)this.getSystemService(Context.ALARM_SERVICE);

        mServiceIntent = new Intent(this, HeartBeatPullService.class);
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
        this.startService(mServiceIntent);
        //PendingIntent pi = PendingIntent.getService(this, 0, mServiceIntent, 0);
        //am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * 1 , pi);

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
