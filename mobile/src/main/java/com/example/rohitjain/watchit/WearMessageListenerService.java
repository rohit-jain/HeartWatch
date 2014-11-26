package com.example.rohitjain.watchit;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by rohitjain on 11/26/14.
 */
public class WearMessageListenerService extends WearableListenerService {
    private String TAG = "Mobile";
    private String url = "http://54.148.164.29/emotimon/watch/store";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.v(TAG, "Received Message: " + messageEvent.getPath());
        new SensorServerLoggerTask().execute(messageEvent.getPath());
    }

    public Void POST(String data){
        InputStream inputStream = null;
        String result = "";


        try {
            // 1. create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // 2. make POST request to the given URL
            HttpPost httpPost = new HttpPost(url);

            // 3. build jsonObject
            // 4. convert JSONObject to JSON to String
            //        json = jsonObject.toString();
            String  json = data;
            // 5. set json to StringEntity
                  StringEntity se = new StringEntity(json);

            // 6. set httpPost Entity
            httpPost.setEntity(se);
            Log.v(TAG,json);

            // 7. Set some headers to inform server about the type of the content
            //httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-Type", "application/json");

            // 8. Execute POST request to the given URL
            Log.v(TAG,"Making post request");
            HttpResponse httpResponse = httpclient.execute(httpPost);
            //      HttpResponse httpResponse = httpclient.execute(new HttpGet(url));
            Log.v(TAG,"Response Length:"+httpResponse.getEntity().getContentLength());
            Log.v(TAG,"STATUS CODE:"+httpResponse.getStatusLine().getStatusCode());
            // 9. receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();
            Log.v(TAG, inputStream.toString());

            // 10. convert inputstream to string
            if(inputStream != null) {
                result = convertInputStreamToString(inputStream);
                Log.v(TAG, result);
            }
            else
                result = "Did not work!";

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


    private class SensorServerLoggerTask extends
            AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... jsonString) {
            //for (Float item : minuteFrame) {
            //    if(isConnected())Log.v(TAG,"Connected to internet");
            //    POST(minuteFrame[0]);
            POST(jsonString[0]);
            Log.v("Mobile", jsonString[0]);
            //}
            return null;
        }

        protected void onPostExecute() {

        }
    }
}
