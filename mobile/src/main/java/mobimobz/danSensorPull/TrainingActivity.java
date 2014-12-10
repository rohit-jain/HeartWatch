package mobimobz.danSensorPull;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.rohitjain.watchit.R;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.gson.Gson;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.*;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.URISyntaxException;
import java.text.DecimalFormat;

//import mobimobz.interfaceAdapter.TabsPagerAdapter;
import mobimobz.json.SensorDataJSON;
import mobimobz.util.SensorFusion;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.ActivityRecognitionClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import android.os.StrictMode;

public class TrainingActivity extends FragmentActivity implements SensorEventListener, View.OnClickListener//, ActionBar.TabListener//, GooglePlayServicesClient.ConnectionCallbacks,GooglePlayServicesClient.OnConnectionFailedListener {
{
    private SensorFusion sensorFusion;
    private SensorManager sensorManager = null;
    private TextView azimuthText, pithText, rollText;
    private DecimalFormat d = new DecimalFormat("#.##");

    private GraphViewSeries azimuthSeries,pitchSeries,rollSeries;
    private GraphView sensorGraph;
    private GraphViewData[] azimuthData, pitchData, rollData;

    private GraphViewSeries micSeries;
    private GraphViewData[] micData;

    private int numHistory = 100;

    //Microphone
    private MediaRecorder myRecorder;
    private MediaPlayer myPlayer;
    private String outputFile = null;
    private TextView text;

    private int updateRepeat = 1000;
    private int updateCount = 0;

    // Audio Recording
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private float presentVal;
    private float[] presentVals;

    //Frames
    private float[] azimuthFrame;
    private float[] pitchFrame;
    private float[] rollFrame;
    private float[] micFrame;

    private float[] azimuthAvg, pitchAvg, rollAvg, micAvg;

    private Thread jsonThread = null;


    int thouCount = 0;
    int minCount = 0;
    int timeID = 0;

    private String emotion = "unknown";


    /** THIS IS THE DETECT ACTIVITY SECTION **/


    /** END OF DETECT ACTIVITY SECTION **/

    /** THIS IS TABBED SECTION **/
    private ViewPager viewPager;
    //private TabsPagerAdapter mAdapter;
    private ActionBar actionBar;
    // Tab titles
    private String[] tabs = { "PAM", "Sensors"};
    /** THIS IS THE END OF THE TABBED SECTION **/




    /** Sensor Specific Functions **/

    public void registerSensorManagerListeners() {
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerSensorManagerListeners();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                sensorFusion.setAccel(event.values);
                sensorFusion.calculateAccMagOrientation();
                break;

            case Sensor.TYPE_GYROSCOPE:
                sensorFusion.gyroFunction(event);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                sensorFusion.setMagnet(event.values);
                break;
        }
        updateOrientationDisplay();
    }


    private float averageArray(float[] bob){
        float acc = 0;
        for (float f: bob){
            acc = f+acc;
        }
        return acc/bob.length;
    }

    private void updateGraphDisplays(){
        double azimuthValue = sensorFusion.getAzimuth();
        double rollValue =  sensorFusion.getRoll();
        double pitchValue =  sensorFusion.getPitch();

        // Text Update
        azimuthText.setText(String.valueOf(d.format(azimuthValue)));
        pithText.setText(String.valueOf(d.format(pitchValue)));
        rollText.setText(String.valueOf(d.format(rollValue)));

        //Graph Update

        for (int i=0; i< numHistory; i++){
            if (i< numHistory -1) {
                azimuthData[i] = new GraphViewData(i,azimuthData[i + 1].getY());
                pitchData[i] = new GraphViewData(i, pitchData[i+1].getY());
                rollData[i] = new GraphViewData(i, rollData[i+1].getY());
                micData[i] = new GraphViewData(i, micData[i+1].getY());
            }
            else{
                azimuthData[i] = new GraphViewData(numHistory, azimuthValue);
                pitchData[i] = new GraphViewData(numHistory, pitchValue);
                rollData[i] = new GraphViewData(numHistory, rollValue);
                micData[i] = new GraphViewData(numHistory, presentVal);
            }
        }

        azimuthSeries.resetData(azimuthData);
        pitchSeries.resetData(pitchData);
        rollSeries.resetData(rollData);
        micSeries.resetData(micData);
    }

    public void updateOrientationDisplay() {

        double azimuthValue = sensorFusion.getAzimuth();
        double rollValue =  sensorFusion.getRoll();
        double pitchValue =  sensorFusion.getPitch();

        //updateGraphDisplays(); ***** UNCOMMENT TO ENABLE GRAPH UPDATING

        //Data cycle store
        updateCount = (updateCount+1)%updateRepeat;
        if (updateCount == 0){
            audioRecordStopStart();
        }
        //counter for averaging
        azimuthAvg[thouCount] = (float)azimuthValue;
        pitchAvg[thouCount] = (float)pitchValue;
        rollAvg[thouCount] = (float)rollValue;

        // micAvg[thouCount] = presentVal;


        if (presentVals == null){
            micAvg[thouCount] = presentVal;
            //System.out.println("TEMPER");
        }
        else {
            micAvg = presentVals;
//            for (float f : presentVals) {
//                System.out.println(f);
//                System.out.println("LENGTH " + presentVals.length);
//            }
        }



        if ((thouCount % updateRepeat) == 0){
            azimuthFrame[minCount] = averageArray(azimuthAvg);
            pitchFrame[minCount] = averageArray(pitchAvg);
            rollFrame[minCount] = averageArray(rollAvg);
            micFrame[minCount] = averageArray(micAvg);
        }


        thouCount = (thouCount + 1) % updateRepeat;
        //System.out.println(thouCount);
        if (thouCount == 0){
            minCount = minCount+1;
        }
        //System.out.println("MIN COUNT " + minCount);
        if (minCount == 60){


            jsonThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        System.out.println("SENDING OUT");
                        SensorDataJSON present = new SensorDataJSON(azimuthFrame,pitchFrame,rollFrame,micFrame,emotion);
                        Gson gson = new Gson();
                        String lala = gson.toJson(present);
                        System.out.println(lala);
                        postData(lala);

                    }
                    catch(Exception e){
                        System.out.println("UH OH");
                    }

                }
            }, "JSON SENDING THREAD");
            jsonThread.start();

            /** reset emotion **/
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            emotion = "unknown";
            TextView emotionText = (TextView)findViewById(R.id.emotionState);
            emotionText.setText("Present Feels - unknown");

            minCount = 0;
        }


        //counter for minute (60 samples)


        //if counters both full, upload





    }

    public void postData(String dataToSend) throws URISyntaxException, IOException {

        DefaultHttpClient httpClient = new DefaultHttpClient();

        HttpPost post = new HttpPost("http://54.148.164.29/emotimon/phone/store");

        post.setEntity(new StringEntity(dataToSend));

        post.setHeader("Accept", "application/json");
        post.setHeader("Content-Type", "application/json; charset=utf-8");


        // Execute HTTP Post Request
        // HttpResponse response = httpClient.execute(post);

        HttpResponse httpResponse = httpClient.execute(post);
        if (httpResponse.getStatusLine().getStatusCode() == 200) {
            // If successful.
            System.out.println("SUCCESS");
        }

    }

    public void audioRecordStopStart(){
        stopRecording();
        startRecording();
    }

    public void micStopStart(){
        micStop();
        micStart();
        //mediaDecode(outputFile);
    }

    public void micStart(){
        try {
            myRecorder.prepare();
            myRecorder.start();
        } catch (IllegalStateException e) {
            // start:it is called before prepare()
            // prepare: it is called after start() or before setOutputFormat()
            e.printStackTrace();
        } catch (IOException e) {
            // prepare() fails
            e.printStackTrace();
        }

        //text.setText("Recording Point: Recording");

        Toast.makeText(getApplicationContext(), "Stopped old, Started new recording",
                Toast.LENGTH_SHORT).show();
    }

    public void micStop(){
        try {
            myRecorder.stop();
            myRecorder.release();
            //myRecorder  = null;


            //text.setText("Recording Point: Stop recording");

            Toast.makeText(getApplicationContext(), "Stop recording...",
                    Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException e) {
            //  it is called before start()
            e.printStackTrace();
        } catch (RuntimeException e) {
            // no valid audio/video data has been received
            e.printStackTrace();
        }
    }


    /** Graphing Details **/
    private void setupGraphs(){
        //azimuthText = (TextView) findViewById(R.id.azmuth);
        //pithText = (TextView) findViewById(R.id.pitch);
        //rollText = (TextView) findViewById(R.id.roll);

        //GraphView Stuff

        azimuthData = new GraphViewData[numHistory];
        pitchData = new GraphViewData[numHistory];
        rollData = new GraphViewData[numHistory];
        micData = new GraphViewData[numHistory];

        azimuthFrame = new float[60];
        pitchFrame = new float[60];
        rollFrame = new float[60];
        micFrame = new float[60];

        azimuthAvg = new float[updateRepeat];
        pitchAvg = new float[updateRepeat];
        rollAvg = new float[updateRepeat];
        micAvg = new float[updateRepeat];

        double v=0;
        for (int i=0; i<numHistory; i++) {
            v += 0.2;
            azimuthData[i] = new GraphViewData(i, 0);
            pitchData[i] = new GraphViewData(i, 0);
            rollData[i] = new GraphViewData(i, 0);
            micData[i] = new GraphViewData(i, 0);
        }
        azimuthSeries = new GraphViewSeries("Azimuth curve", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(200, 50, 00), 3), azimuthData);
        pitchSeries = new GraphViewSeries("Pitch curve", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(90, 250, 00), 3), pitchData);
        rollSeries = new GraphViewSeries("Roll curve", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(00, 60, 190), 3), rollData);
        micSeries = new GraphViewSeries("Mic curve", new GraphViewSeries.GraphViewSeriesStyle(Color.rgb(77,55,44),3), micData);

        sensorGraph = new LineGraphView(
                this // context
                , "Sensor Graph" // heading
        );
        sensorGraph.setManualYAxisBounds(359, -180);

        updateGraphView(sensorGraph, azimuthSeries);
        updateGraphView(sensorGraph, pitchSeries);
        updateGraphView(sensorGraph, rollSeries);

        LinearLayout layout = (LinearLayout) findViewById(R.id.graphBlock);
        layout.addView(sensorGraph);

        LineGraphView micGraph = new LineGraphView(this,"Mic Graph");
        updateGraphView(micGraph, micSeries);
        LinearLayout micLayout = (LinearLayout) findViewById(R.id.micGraph);
        micLayout.addView(micGraph);
        micGraph.setManualYAxisBounds(15, -15);

        // Button Stuff

        LinearLayout happyButtonLayout = (LinearLayout) findViewById(R.id.classify);

        Button happy = new Button(this);
        happy.setText("Happy! ^_^");
     /*   happy.setLayoutParams(new LinearLayout.LayoutParams(
               ViewGroup.LayoutParams.WRAP_CONTENT,
               ViewGroup.LayoutParams.WRAP_CONTENT));*/

        Button stressed = new Button(this);
        stressed.setText("Stressed... >_<");

        Button calm = new Button(this);
        calm.setText("Calm '_'");

        happyButtonLayout.addView(happy);
        happyButtonLayout.addView(calm);
        happyButtonLayout.addView(stressed);
    }

    private void setupImageGrid(){
        LinearLayout layout1 = (LinearLayout) findViewById(R.id.graphBlock);
        LinearLayout layout2 = (LinearLayout) findViewById(R.id.graphBlock2);
        LinearLayout layout3 = (LinearLayout) findViewById(R.id.graphBlock3);
        LinearLayout layout4 = (LinearLayout) findViewById(R.id.graphBlock4);

        String[] images = {"afraid","angry","calm","delighted","excited","frusterated","glad","happy"};
        int[] imageRefs = {R.drawable.afraid1,R.drawable.afraid2,R.drawable.afraid3,
                R.drawable.angry1, R.drawable.angry2,R.drawable.angry3,
                R.drawable.calm1, R.drawable.calm2, R.drawable.calm3,
                R.drawable.delighted1, R.drawable.delighted2, R.drawable.delighted3,
                R.drawable.excited1, R.drawable.excited2, R.drawable.excited3,
                R.drawable.frusterated1, R.drawable.frusterated2, R.drawable.frusterated3,
                R.drawable.glad1, R.drawable.glad2, R.drawable.glad3,
                R.drawable.gloomy1, R.drawable.gloomy2, R.drawable.gloomy3,
                R.drawable.happy1, R.drawable.happy2, R.drawable.happy3,
                R.drawable.miserable1, R.drawable.miserable2, R.drawable.miserable3,
                R.drawable.sad1, R.drawable.sad2, R.drawable.sad3,
                R.drawable.satisfied1, R.drawable.satisfied2, R.drawable.satisfied3,
                R.drawable.serene1, R.drawable.serene2, R.drawable.serene3,
                R.drawable.sleepy1, R.drawable.sleepy2, R.drawable.sleepy3,
                R.drawable.tense1, R.drawable.tense2, R.drawable.tense3,
                R.drawable.tired1, R.drawable.tired2, R.drawable.tired3};

        Context theRow;
        LinearLayout theLayout;
        Display display = getWindowManager().getDefaultDisplay();
        int rando = 0;
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int dim = Math.round((float)width/(float)4.4);
        int emoCount = 0;

        for (int i=0; i<16; i++){

            if (i >= 0 && i< 4){
                theLayout = layout1;
            }
            else if (i >= 4 && i< 8){
                theLayout = layout2;
            }
            else if (i >= 8 && i< 12){
                theLayout = layout3;
            }
            else{
                theLayout = layout4;
            }
            theRow = theLayout.getContext();

            ImageButton imageo = new ImageButton(theRow);
            imageo.setAlpha((float)1 - (float)(emoCount*.001));
            System.out.println("Alpha Set" + imageo.getAlpha());
            imageo.setOnClickListener(this);


            rando = (int)(i*3+Math.floor(Math.random()*3));
            imageo.setImageResource(imageRefs[rando]);
            System.out.println(rando);
            theLayout.addView(imageo);

            android.view.ViewGroup.LayoutParams params = imageo.getLayoutParams();
            params.height = dim;
            params.width = dim;
            imageo.setLayoutParams(params);

            emoCount ++;
        }
    }




    private void setCharacter(int emotion){

    }

    /** Standard Methods **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        /*
        /** The Tab Builder **/
        // Initilization
        /*
        viewPager = (ViewPager) findViewById(R.id.pager);
        actionBar = getActionBar();
        mAdapter = new TabsPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(mAdapter);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Adding Tabs
        for (String tab_name : tabs) {
            actionBar.addTab(actionBar.newTab().setText(tab_name)
                    .setTabListener(this));
        }

*/




        setupImageGrid();

        //SENSOR CODE
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        registerSensorManagerListeners();

        d.setMaximumFractionDigits(2);
        d.setMinimumFractionDigits(2);

        sensorFusion = new SensorFusion();
        // Use SensorFusion Mode always
        sensorFusion.setMode(SensorFusion.Mode.FUSION);

        //setupGraphs();
        azimuthFrame = new float[60];
        pitchFrame = new float[60];
        rollFrame = new float[60];
        micFrame = new float[60];

        azimuthAvg = new float[updateRepeat];
        pitchAvg = new float[updateRepeat];
        rollAvg = new float[updateRepeat];
        micAvg = new float[updateRepeat];


//        // Microphone Setup
//        // store it to sd card
//        outputFile = Environment.getExternalStorageDirectory().
//                getAbsolutePath() + "/trainRecording.3gpp";
//
//        myRecorder = new MediaRecorder();
//        myRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        myRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        myRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
//        myRecorder.setOutputFile(outputFile);

        // Audio Stuff

        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        // FOR LOCKING ORIENTATION
        lockOrientation();

    }

    int BufferElements2Rec = 500; // DEFAULT 1024 want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // DEFAULT 2  || 2 bytes in 16bit format

    private void startRecording() {

        recordingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                            RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

                    recorder.startRecording();
                    isRecording = true;


                    writeAudioDataToFile();
                }
                catch(Exception e){
                    System.out.println("UH OH");
                }

            }
        }, "AudioRecorder Thread");
        recordingThread.start();

        Toast.makeText(getApplicationContext(), "Stopped old, Started new recording",
                Toast.LENGTH_SHORT).show();
    }

    public float[] floatMe(short[] pcms) {
        float[] floaters = new float[pcms.length];
        for (int i = 0; i < pcms.length; i++) {
            floaters[i] = pcms[i];
        }
        return floaters;
    }

    public float[] floatFromByte(byte[] byters) {
        float[] floatList = new float[byters.length];
        for (int i = 0; i < byters.length; i++){
            floatList[i] = byters[i];
        }
        return floatList;
    }

    //convert short to byte
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte
        //String filePath = "/sdcard/voice8K16bitmono.pcm";

        String filePath = Environment.getExternalStorageDirectory().
                getAbsolutePath() + "/trainRecording.pcm";


        short sData[] = new short[BufferElements2Rec];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format

            recorder.read(sData, 0, BufferElements2Rec);
            //System.out.println("Short writing to file" + sData.toString());
            //System.out.println(filePath);
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);

                //float[] temp = floatMe(sData);


//                System.out.println(sData);
//                System.out.println(temp);
                // presentVal = temp[0];//temp[temp.length-1];
                // System.out.println(presentVal);

                os.write(bData, 0, BufferElements2Rec * BytesPerElement);

//                for (byte a:bData){
//                    System.out.println(a);
//                }
                presentVal = floatFromByte(bData)[bData.length-1];
                presentVals = floatFromByte(bData);


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    public void lockOrientation(){
        Display display = getWindowManager().getDefaultDisplay();
        int rotation = display.getRotation();

        Point size = new Point();
        display.getSize(size);

        int lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        if (rotation == Surface.ROTATION_0
                || rotation == Surface.ROTATION_180) {
            // if rotation is 0 or 180 and width is greater than height, we have
            // a tablet
            if (size.x > size.y) {
                if (rotation == Surface.ROTATION_0) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            } else {
                // we have a phone
                if (rotation == Surface.ROTATION_0) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                }
            }
        } else {
            // if rotation is 90 or 270 and width is greater than height, we
            // have a phone
            if (size.x > size.y) {
                if (rotation == Surface.ROTATION_90) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                }
            } else {
                // we have a tablet
                if (rotation == Surface.ROTATION_90) {
                    lock = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                } else {
                    lock = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                }
            }
        }

        setRequestedOrientation(lock);
    }

    public void updateGraphView(GraphView graphView, GraphViewSeries updatedSeries){
        graphView.addSeries(updatedSeries); // data
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_training, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        ImageButton booton = (ImageButton) view;
        //System.out.println(booton.getAlpha());
        float emoDecide = booton.getAlpha();
        TextView emotionText = (TextView)findViewById(R.id.emotionState);


        if (emoDecide == (float)1){
            emotion = "afraid";
        }
        else if (emoDecide == (float)0.999){
            emotion = "angry";
        }
        else if (emoDecide == (float)0.998){
            emotion = "calm";
        }
        else if (emoDecide == (float)0.997){
            emotion = "delighted";
        }
        else if (emoDecide == (float)0.996){
            emotion = "excited";
        }
        else if (emoDecide == (float)0.995){
            emotion = "frusterated";
        }
        else if (emoDecide == (float)0.994){
            emotion= "glad";
        }
        else if (emoDecide == (float)0.993){
            emotion = "gloomy";
        }
        else if (emoDecide == (float)0.992){
            emotion = "happy";
        }
        else if (emoDecide == (float)0.991){
            emotion = "miserable";
        }
        else if (emoDecide == (float)0.990){
            emotion = "sad";
        }
        else if (emoDecide == (float)0.989){
            emotion = "satisfied";
        }
        else if (emoDecide == (float)0.988){
            emotion = "serene";
        }
        else if (emoDecide == (float)0.987){
            emotion = "sleepy";
        }
        else if (emoDecide == (float)0.986){
            emotion = "tense";
        }
        else{
            emotion = "tired";
        }

        //System.out.println(emotion);
        emotionText.setText("Present Feels - " + emotion);

    }

//    @Override
//    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//
//    }
//
//    @Override
//    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//
//    }
//
//    @Override
//    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
//
//    }
}
