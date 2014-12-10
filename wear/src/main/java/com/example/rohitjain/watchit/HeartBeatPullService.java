package com.example.rohitjain.watchit;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by rohitjain on 11/22/14.
 */
public class HeartBeatPullService extends Service implements SensorEventListener,GoogleApiClient.ConnectionCallbacks {
    private SensorManager mSensorManager;
    private String TAG = "Heart";
    String FILENAME = "heart_beat_log";
    String string = "hello world!";
    //int[] temp_hb_buffer = new int[1000];
    ArrayList<Float> hb_sec_buffer = new ArrayList<Float>();
    ArrayList<Float> hb_min_buffer = new ArrayList<Float>();
    Float sum=new Float(0.0);
    int entries = 0;
    Float avg=new Float(0.0);
    Node node; // the connected device to send the message to
    private GoogleApiClient mApiClient;

    private class SensorData{
        ArrayList<Float> heart_beat;
        String id;
        String timestamp;
        String date;


        public ArrayList<Float> getHeart_beat() {
            return heart_beat;
        }

        public void setHeart_beat(ArrayList<Float> heart_beat) {
            this.heart_beat = heart_beat;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }



    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something useful
        Log.v(TAG, "service started");
        Log.v(TAG, "Initiate connection with phone");
        initGoogleApiClient();
        mSensorManager = ((SensorManager)this.getSystemService(SENSOR_SERVICE));
        registerSensorManagerListeners();
        //return 1;
        return Service.START_STICKY;
    }

    public void registerSensorManagerListeners() {
        Log.v(TAG, "registering heart beat listener");
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(65562),
                3);
    }

    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addConnectionCallbacks(this)
                .build();
        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) ) {
            Log.v(TAG,"Invoking client connect function");
            mApiClient.connect();
        }

    }

    private void sendMessage( final String path, final byte[] bytes ) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    //Log.v(TAG,"Sending message to "+node.getDisplayName()+ " " + node.getId());
                    //Log.v(TAG,"Length watch message: "+ path.length());
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, bytes ).await();
                }

            }
        }).start();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v(TAG,"Connected to phone");
        //sendMessage( "path", "text" );
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                //Log.v(TAG, "sensor event: " + sensorEvent.accuracy + " = " + sensorEvent.values[0] + " = " + sensorEvent.values[1] + " = " + sensorEvent.values[2]);
                break;

            case 65562:

                int bufferSize = hb_sec_buffer.size();
                //Log.v(TAG, "sensor event: " + sensorEvent.accuracy + " = " + sensorEvent.values[0]);
                hb_sec_buffer.add(sensorEvent.values[0]);
                if (sensorEvent.values[0] > 0.1) {
                    entries += 1;
                    sum += sensorEvent.values[0];
                }
                if (bufferSize == 9) {
                    if (entries != 0) {
                        avg = (float) sum / entries;
                    } else {
                        avg = (float) 0.0;
                    }

                    sum = (float) 0.0;
                    entries = 0;
                    hb_min_buffer.add(avg);
                    if (hb_min_buffer.size() == 60) {
                        ArrayList<Float> copy = new ArrayList<Float>(hb_min_buffer.size());

                        for (Float foo : hb_min_buffer) {
                            copy.add((Float) foo);
                        }
                        new SensorEventLoggerTask().execute(copy);
                        hb_min_buffer.clear();
                    }
                    hb_sec_buffer.clear();

                }
                break;
        }
    }


    private class SensorEventLoggerTask extends
            AsyncTask<ArrayList<Float>, Void, Void> {
        @Override
        protected Void doInBackground(ArrayList<Float>... minuteFrame) {
            String message = GetSerializedJSONObject(minuteFrame[0]);
            byte[] bytes_heart = new byte[1];
            Arrays.fill( bytes_heart, (byte) 0 );
            sendMessage( message, bytes_heart );
            Log.v(TAG, minuteFrame[0].toString());
            return null;
        }

        protected void onPostExecute() {

        }
    }

    private String GetSerializedJSONObject(ArrayList<Float> heartBeat){
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        Date date = cal.getTime();
        String mDate = date.getMonth() + "/" + date.getDate() + "/" + (date.getYear()+1900);
        int mHour = date.getHours();
        int mMinute = date.getMinutes();
        SensorData data = new SensorData();

        data.setId("2");
        data.setHeart_beat(heartBeat);
        data.setTimestamp(mHour+":"+mMinute);
        data.setDate(mDate);

        Gson gson = new Gson();
        Log.v(TAG,gson.toJson(data));

        return gson.toJson(data);
    }

    //code to write sensor data to file on watch
    /*
    private class SensorEventFileLoggerTask extends
            AsyncTask<SensorEvent, Void, Void> {
        @Override
        protected Void doInBackground(SensorEvent... events) {
            SensorEvent event = events[0];
            try {
                FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_APPEND);
                Long tsLong = System.currentTimeMillis();
                String ts = tsLong.toString();
                ts += ","+event.values[0];
                Log.v(TAG,"writing value "+ts);
                fos.write(ts.getBytes());
                fos.write(System.getProperty("line.separator").getBytes());
                fos.close();
            }
            catch(Exception e){
                Log.v(TAG,e.toString());
            }
            // log the value
            return null;
        }

        protected void onPostExecute() {

        }
    }
    */

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.v(TAG, "accuracy changed: " + i);
    }

    @Override
    public void onDestroy(){
        Log.v(TAG,"Service Killed");
        mSensorManager.unregisterListener(this);
        if( mApiClient != null ) {
            Log.v(TAG,"Disconnecting");
            mApiClient.unregisterConnectionCallbacks(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
