package com.example.rohitjain.watchit;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by rohitjain on 11/22/14.
 */
public class HeartBeatPullService extends Service implements SensorEventListener {
    private SensorManager mSensorManager;
    private String TAG = "Heart";


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something useful
        Log.v(TAG, "service started");
        mSensorManager = ((SensorManager)this.getSystemService(SENSOR_SERVICE));
        registerSensorManagerListeners();
        //return 1;
        return Service.START_NOT_STICKY;
    }

    public void registerSensorManagerListeners() {
        Log.v(TAG, "registering listener");
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(65562),
                3);

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.v(TAG, "sensor event: " + sensorEvent.accuracy + " = " + sensorEvent.values[0]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.v(TAG, "accuracy changed: " + i);
    }

    @Override
    public void onDestroy(){
        mSensorManager.unregisterListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }
}
