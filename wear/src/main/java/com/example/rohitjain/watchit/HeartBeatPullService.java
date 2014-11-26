package com.example.rohitjain.watchit;

import android.app.Activity;
import android.app.IntentService;
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
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by rohitjain on 11/22/14.
 */
public class HeartBeatPullService extends Service implements SensorEventListener {
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
    GoogleApiClient mGoogleApiClient;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something useful
        Log.v(TAG, "service started");
        mSensorManager = ((SensorManager)this.getSystemService(SENSOR_SERVICE));
        registerSensorManagerListeners();
        //return 1;
        return Service.START_STICKY;
    }

    public void registerSensorManagerListeners() {
        Log.v(TAG, "registering listener");
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(65562),
                3);

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
    public void onSensorChanged(SensorEvent sensorEvent) {
        int bufferSize = hb_sec_buffer.size();
        //Log.v(TAG, "sensor event: " + sensorEvent.accuracy + " = " + sensorEvent.values[0]);
        hb_sec_buffer.add(sensorEvent.values[0]);
        if (sensorEvent.values[0] > 0.1)
        {
            entries+=1;
            sum += sensorEvent.values[0];
        }
        if(bufferSize == 9)
        {
            if(entries !=0) {
                avg = (float) sum / entries;
            }
            else
            {
                avg=(float)0.0;
            }

            sum = (float)0.0;
            entries=0;
            hb_min_buffer.add(avg);
            if(hb_min_buffer.size()==5)
            {
                ArrayList<Float> copy = new ArrayList<Float>(hb_min_buffer.size());

                for (Float foo: hb_min_buffer) {
                    copy.add((Float)foo);
                }
                new SensorEventLoggerTask().execute(copy);
                hb_min_buffer.clear();
            }
            hb_sec_buffer.clear();

        }
    }

    public Void POST(ArrayList<Float> heartBeat){
        //InputStream inputStream = null;
        String result = "";
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        Date date = cal.getTime();
        String mDate = date.getMonth() + "/" + date.getDate() + "/" + (date.getYear()+1900);
        int mHour = date.getHours();
        int mMinute = date.getMinutes();
        Log.v(TAG,"Date: "+ mDate);

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", 1);
            jsonObject.put("heart_beat", heartBeat);
            jsonObject.put("timestamp", mHour+":"+mMinute);
            jsonObject.put("date",mDate);
            Log.v(TAG,jsonObject.toString());

            URL url = new URL("http://54.148.164.29/emotimon/watch/store");

            HttpURLConnection urlConnection = null;
            Log.v(TAG,"trying connection made");

            urlConnection = (HttpURLConnection) url.openConnection();
            Log.v(TAG,"connection open");

            urlConnection.setRequestMethod("GET");
            urlConnection.connect();
            Log.v(TAG,"connection made");
            // 1. create HttpClient
            //HttpClient httpclient = new DefaultHttpClient();

            // 2. make POST request to the given URL
//            HttpPost httpPost = new HttpPost(url);

            String json = "";
            String serverJsonResponse = null;
            BufferedReader reader = null;

            // 3. build jsonObject


            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                serverJsonResponse = null;
                Log.v(TAG,"response null");
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                serverJsonResponse = null;
            }
            serverJsonResponse = buffer.toString();
            Log.v(TAG, serverJsonResponse.toString());
            // 4. convert JSONObject to JSON to String
    //        json = jsonObject.toString();

            // 5. set json to StringEntity
      //      StringEntity se = new StringEntity(json);

            // 6. set httpPost Entity
//            httpPost.setEntity(se);
  //          Log.v(TAG,json);

            // 7. Set some headers to inform server about the type of the content
            //httpPost.setHeader("Accept", "application/json");
    //        httpPost.setHeader("Content-Type", "application/json");

            // 8. Execute POST request to the given URL
            //Log.v(TAG,"Making post request");
            //HttpResponse httpResponse = httpclient.execute(httpPost);
      //      HttpResponse httpResponse = httpclient.execute(new HttpGet(url));
            //Log.v(TAG,"Response Length:"+httpResponse.getEntity().getContentLength());
            //Log.v(TAG,"STATUS CODE:"+httpResponse.getStatusLine().getStatusCode());
            // 9. receive response as inputStream
            //inputStream = httpResponse.getEntity().getContent();
            //Log.v(TAG, inputStream.toString());

            // 10. convert inputstream to string
            //if(inputStream != null) {
            //    result = convertInputStreamToString(inputStream);
            //    Log.v(TAG, result);
            //}
            //else
            //    result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        // 11. return result
        //return result;
        return null;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    private class SensorEventLoggerTask extends
            AsyncTask<ArrayList<Float>, Void, Void> {
        @Override
        protected Void doInBackground(ArrayList<Float>... minuteFrame) {
            //for (Float item : minuteFrame) {
                if(isConnected())Log.v(TAG,"COnneted");
                POST(minuteFrame[0]);

                Log.v(TAG, minuteFrame[0].toString());
            //}
            return null;
        }

        protected void onPostExecute() {

        }
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
    }

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }
}
