package mobimobz.json;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by D-Line on 11/18/14.
 * Includes an array of data considered. One array for each sensor input
 * Includes time of day of upload
 */

public class SensorDataJSON {

    private float[] aziData;
    private float[] pitchData;
    private float[] rollData;
    private float[] micData;
    private int id;
    private String time;
    private String date;
    private String emotion;


    public SensorDataJSON(){

    }

    public SensorDataJSON( float[] azi, float[] pitch, float[] roll, float[] mic, String emo){

        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        Date dateo = cal.getTime();

        date = dateo.getMonth() + "/" + dateo.getDate() + "/" + (dateo.getYear()+1900);
        int mHour = dateo.getHours();
        int mMinute = dateo.getMinutes();

        time = ""+mHour+":"+mMinute;

        aziData = azi.clone();
        pitchData = pitch.clone();
        rollData = roll.clone();
        micData = mic.clone();
        id = 1;

        emotion = emo;
    }



}
