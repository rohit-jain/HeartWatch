package com.example.rohitjain.watchit;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by rohitjain on 11/26/14.
 */
public class WearMessageListenerService extends WearableListenerService {
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.v("Mobile", messageEvent.getPath());
        //if( messageEvent.getPath().equalsIgnoreCase( START_ACTIVITY ) ) {
        //    Intent intent = new Intent( this, MainActivity.class );
        //    intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
        //    startActivity( intent );
        //} else {
        //    super.onMessageReceived(messageEvent);
        //}
    }
}
