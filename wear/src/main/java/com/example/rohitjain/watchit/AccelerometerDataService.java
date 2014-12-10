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
 * Created by rohitjain on 12/9/14.
 */
public class AccelerometerDataService extends Service implements SensorEventListener,GoogleApiClient.ConnectionCallbacks{
    private SensorManager mSensorManager;
    private String TAG = "Accelerometer";
    String FILENAME = "accelerometer_log";
    ArrayList<Float[]> acc_sec_buffer = new ArrayList<Float[]>();
    ArrayList<Float[]> acc_min_buffer = new ArrayList<Float[]>();
    Float sum=new Float(0.0);
    int entries = 0;
    Float avg=new Float(0.0);
    Node node; // the connected device to send the message to
    private GoogleApiClient mApiClient;

    private class AccelerometerSensorData{
        ArrayList<Float> aziData;
        ArrayList<Float> rollData;
        ArrayList<Float> pitchData;

        String id;
        String timestamp;
        String date;

        public ArrayList<Float> getPitchData() {
            return pitchData;
        }

        public void setPitchData(ArrayList<Float> pitchData) {
            this.pitchData = pitchData;
        }

        public ArrayList<Float> getRollData() {
            return rollData;
        }

        public void setRollData(ArrayList<Float> rollData) {
            this.rollData = rollData;
        }

        public ArrayList<Float> getAziData() {
            return aziData;
        }

        public void setAziData(ArrayList<Float> aziData) {
            this.aziData = aziData;
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

        public void setAccData(ArrayList<Float[]> accData){
            //ArrayList<Float[]> copy = new ArrayList<Float[]>(acc_min_buffer.size());
            ArrayList<Float> temp_azi = new ArrayList<Float>();
            ArrayList<Float> temp_pitch = new ArrayList<Float>();
            ArrayList<Float> temp_roll = new ArrayList<Float>();

            for (Float foo[] : accData) {

                temp_azi.add(foo[0]);
                temp_roll.add(foo[1]);
                temp_pitch.add(foo[2]);
            }
            setRollData(temp_roll);
            setPitchData(temp_pitch);
            setAziData(temp_azi);
        }


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something useful
        Log.v(TAG, "accelerometer service started");
        Log.v(TAG, "Initiate connection with phone");
        initGoogleApiClient();
        mSensorManager = ((SensorManager)this.getSystemService(SENSOR_SERVICE));
        registerSensorManagerListeners();
        //return 1;
        return Service.START_STICKY;
    }

    public void registerSensorManagerListeners() {
        Log.v(TAG, "registering accelerometer listener");
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
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
                    //Log.v(TAG,"Length watch message: "+ path.length());
                    Log.v(TAG,"Sending message to "+node.getDisplayName()+ " " + node.getId());
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

    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Log.v(TAG, "sensor event: " + sensorEvent.accuracy + " = " + sensorEvent.values[0] + " = " + sensorEvent.values[1] + " = " + sensorEvent.values[2]);
        int bufferSize = acc_sec_buffer.size();
        //Log.v(TAG, "sensor event: " + sensorEvent.accuracy + " = " + sensorEvent.values[0]);
        final float alpha = 0.8f;
        Float[] gravity = new Float[3];
        Float[] acc = new Float[3];
        Float[] sum = new Float[3];
        Float[] avg = new Float[3];

        sum[0]=0f;
        sum[1]=0f;
        sum[2]=0f;

        gravity[0]=9.8f;
        gravity[1]=9.8f;
        gravity[2]=9.8f;

        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        acc[0] = event.values[0] - gravity[0];
        acc[1] = event.values[1] - gravity[1];
        acc[2] = event.values[2] - gravity[2];

        //put acceleration data in a buffer
        acc_sec_buffer.add(acc);

        //increase the number of entries
        entries += 1;
        sum[0] += acc[0];
        sum[1] += acc[1];
        sum[2] += acc[2];
        if (bufferSize == 179) {
            if (entries != 0) {
                avg[0] = (float) sum[0] / entries;
                avg[1] = (float) sum[1] / entries;
                avg[2] = (float) sum[2] / entries;
            } else {
                avg[0] = (float) 0.0;
                avg[1] = (float) 0.0;
                avg[2] = (float) 0.0;
            }


            sum[0]=0f;
            sum[1]=0f;
            sum[2]=0f;
            entries = 0;
            acc_min_buffer.add(avg);
            if (acc_min_buffer.size() == 60) {
                ArrayList<Float[]> copy = new ArrayList<Float[]>(acc_min_buffer.size());

                for (Float foo[] : acc_min_buffer) {
                    copy.add((Float[]) foo);
                }
                new SensorEventLoggerTask().execute(copy);
                acc_min_buffer.clear();
            }
            acc_sec_buffer.clear();

        }

    }


    private class SensorEventLoggerTask extends
            AsyncTask<ArrayList<Float[]>, Void, Void> {
        @Override
        protected Void doInBackground(ArrayList<Float[]>... minuteFrame) {
            String message = GetSerializedJSONObject(minuteFrame[0]);
            byte[] bytes = new byte[1];
            Arrays.fill(bytes, (byte) 1);
            sendMessage( message, bytes );
            //Log.v(TAG, minuteFrame[0].toString());
            return null;
        }

        protected void onPostExecute() {

        }
    }

    private String GetSerializedJSONObject(ArrayList<Float[]> accelerometer){
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        Date date = cal.getTime();
        String mDate = date.getMonth() + "/" + date.getDate() + "/" + (date.getYear()+1900);
        int mHour = date.getHours();
        int mMinute = date.getMinutes();
        AccelerometerSensorData data = new AccelerometerSensorData();

        data.setId("2");
        data.setAccData(accelerometer);
        data.setTimestamp(mHour+":"+mMinute);
        data.setDate(mDate);

        Gson gson = new Gson();
        Log.v(TAG,gson.toJson(data));

        return gson.toJson(data);
    }

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
