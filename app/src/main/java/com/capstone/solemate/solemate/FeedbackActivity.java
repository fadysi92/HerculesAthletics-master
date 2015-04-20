package com.capstone.solemate.solemate;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.UUID;

import android.app.Application;


public class FeedbackActivity extends Activity {
    private ConnectedThread mConnectedThread;
    private static Handler mHandler;
    private static final int RECEIVE_MESSAGE = 1;
    private static final int OTHER_RECEIVE_MESSAGE = 2;

    private static int count = 0;

    protected static final int HIGHEST_VOLTAGE = 3;     //Vcc voltage
    protected static final int HIGHEST_VALUE = 63;

    private StringBuilder sb;

    private static boolean WRITE_ENABLE_OPTION = true;
    private static final String OUTPUT_FILE_NAME = "testBTData.txt";

    // BT device connection attributes
    private BluetoothSocket btSocket;
    private static BluetoothDevice btDevice;
    private static boolean SOCKET_INSTREAM_ACTIVE = false;
    private static boolean SOCKET_CONNECTED = false;
    private static String hc05MacId = new String();
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    protected static TextView textLeftBicep, textRightBicep, textECG, textTemp, textLeftBicepRepCounter, textRightBicepCounter;
    protected static ImageView imageViewBackground, imageViewRightBicep, imageViewLeftBicep, imageViewLeftBicepRed, imageViewRightBicepRed;
    protected static ImageView imageViewThermometerRed, imageViewThermometerGreen, imageViewThermometerOrange;
    protected static Button buttonRecord, buttonReset, buttonGraph;

    protected int stateLeftBicep = 1;
    protected int stateRightBicep = 1;
    protected int stateECG = 1;
    protected static final int STATE_NOT_FLEX = 1;
    protected static final int STATE_FLEXED = 2;

    protected int[] arrayLeftBicep = new int[100];
    protected int[] arrayRightBicep = new int[100];
    protected int[] arrayECG = new int[1000];
    protected int[] arrayTemp = new int[10];

    protected double[] arrayHeartValues = new double[10];
    protected int heartValueCount = 0;

    protected int countLeftBicep = 0;
    protected int countRightBicep = 0;
    protected  int countECG = 0;
    protected int countTemp = 0;
    protected int flag = 0;

    protected int totalRepsLeft = 0;
    protected int totalRepsRight = 0;
    public double leftBicepRMSToPass = 0;
    //protected static Handler mHandlerECG;

    protected String testDataString;
    protected int counter = 0;

    //private ImageSwitcher imageSwitcher;

    // Loading spinner
    public ProgressDialog progress;

    // Init default bluetooth adapter
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        textLeftBicep = (TextView) findViewById(R.id.leftBicep);
        textRightBicep = (TextView) findViewById(R.id.rightBicep);
        textECG = (TextView) findViewById(R.id.ECG);
        textTemp = (TextView) findViewById(R.id.temperature);
        textLeftBicepRepCounter = (TextView) findViewById(R.id.leftBicepRepCounter);
        textRightBicepCounter = (TextView) findViewById(R.id.rightBicepRepCounter);
        imageViewBackground = (ImageView) findViewById(R.id.imageViewBody);
        imageViewRightBicep = (ImageView) findViewById(R.id.imageViewRBA);
        imageViewLeftBicep = (ImageView) findViewById(R.id.imageViewLBA);
        imageViewLeftBicepRed = (ImageView) findViewById(R.id.imageView6);
        imageViewRightBicepRed = (ImageView) findViewById(R.id.imageView7);
        imageViewThermometerRed = (ImageView) findViewById(R.id.imageViewThermometerRed);
        imageViewThermometerGreen = (ImageView) findViewById(R.id.imageViewThermometerGreen);
        imageViewThermometerOrange = (ImageView) findViewById(R.id.imageViewThermometerOrange);
        buttonRecord = (Button) findViewById(R.id.buttonRecord);
        buttonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (flag == 0) {
                    flag = 1;
                    buttonRecord.setText("Stop Recording");
                }
                else {
                    flag = 0;
                    buttonRecord.setText("Start Recording");
                }
            }
        });

        buttonReset = (Button) findViewById(R.id.buttonReset);
        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                totalRepsLeft = 0;
                totalRepsRight = 0;
                for (int i = 0; i < arrayHeartValues.length; i++)
                {
                    arrayHeartValues[i] = 60;
                }
            }
        });

        buttonGraph = (Button) findViewById(R.id.startPerformance);
        buttonGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent graphIntent = new Intent(FeedbackActivity.this, PerformanceScreen.class);
                graphIntent.putExtra("rmsValue",leftBicepRMSToPass);
                startActivity(graphIntent);
            }
        });

        Intent intent = getIntent();
        //hc05MacId = intent.getStringExtra("hc05MacId");
        hc05MacId = "00:13:12:23:54:20";
        // Init loading spinner
        progress = new ProgressDialog(this);
        progress.setTitle("Connecting");
        progress.setMessage("Please wait...");
        progress.show();

        new ConnectToBtTask().execute();

        ProcessECG processECG = new ProcessECG();
        processECG.start();

        ProcessRMSLeftBicep processRMSLeftBicep = new ProcessRMSLeftBicep();
        processRMSLeftBicep.start();

        ProcessRMSRightBicep processRMSRightBicep = new ProcessRMSRightBicep();
        processRMSRightBicep.start();

        ProcessTemperature processTemperature = new ProcessTemperature();
        processTemperature.start();
    }

    private void sendValueRMSLeft(double value)
    {
        ((MyApplication) this.getApplication()).setRMSValueLeftBicep(value);
    }

    private void sendValueRMSRight(double value)
    {
        ((MyApplication) this.getApplication()).setRMSValueRightBicep(value);
    }

    final Handler mHandlerECG = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            int value = (Integer) msg.obj;
            double average = 0;
            if(msg.what==RECEIVE_MESSAGE){
                //images.get(msg.arg1).setImageBitmap((Bitmap) msg.obj);
                arrayHeartValues[heartValueCount++] = value;
                if (heartValueCount >= arrayHeartValues.length) heartValueCount = 0;
                for (int i = 0; i < arrayHeartValues.length; i++)
                {
                    average += arrayHeartValues[i];
                }
                average = average / (arrayHeartValues.length);
                average = Math.round(average);
                textECG.setText(Integer.toString((int)average) + " BPM");
            }
            super.handleMessage(msg);
        }
    };

    final Handler mHandlerRMSLeftBicep = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            double value = (Double) msg.obj;
            if(msg.what==RECEIVE_MESSAGE){
                //images.get(msg.arg1).setImageBitmap((Bitmap) msg.obj);
                //leftBicepRMSToPass = value;
                sendValueRMSLeft(value/32 * 100);
                value = Math.round(value);
                textLeftBicep.setText(Double.toString(value/32 * 100) + "%");
                if (value > 15) imageViewLeftBicepRed.setVisibility(View.VISIBLE);
                else imageViewLeftBicepRed.setVisibility(View.INVISIBLE);
            }
            super.handleMessage(msg);
        }
    };

    final Handler mHandlerRMSRightBicep = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            double value = (Double) msg.obj;
            if(msg.what==RECEIVE_MESSAGE){
                //images.get(msg.arg1).setImageBitmap((Bitmap) msg.obj);
                sendValueRMSRight(value/32*100);
                value = Math.round(value);
                textRightBicep.setText(Double.toString(value/32*100) + "%");
                if (value > 15) imageViewRightBicepRed.setVisibility(View.VISIBLE);
                else imageViewRightBicepRed.setVisibility(View.INVISIBLE);
            }
            super.handleMessage(msg);
        }
    };

    final Handler mHandlerTemperature = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            double value = (Double) msg.obj;
            if(msg.what==RECEIVE_MESSAGE){
                //images.get(msg.arg1).setImageBitmap((Bitmap) msg.obj);
                value = Math.round(value);
                textTemp.setText(Double.toString(value) + " C");
                if (value >= 37)
                {
                    imageViewThermometerRed.setVisibility(View.VISIBLE);
                    imageViewThermometerOrange.setVisibility(View.INVISIBLE);
                    imageViewThermometerGreen.setVisibility(View.INVISIBLE);
                }
                else if (value >= 32)
                {
                    imageViewThermometerRed.setVisibility(View.INVISIBLE);
                    imageViewThermometerOrange.setVisibility(View.VISIBLE);
                    imageViewThermometerGreen.setVisibility(View.INVISIBLE);
                }
                else
                {
                    imageViewThermometerRed.setVisibility(View.INVISIBLE);
                    imageViewThermometerOrange.setVisibility(View.INVISIBLE);
                    imageViewThermometerGreen.setVisibility(View.VISIBLE);
                }
            }
            super.handleMessage(msg);
        }
    };

    final Handler mHandlerRepCounterLeft = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            int value = (Integer) msg.obj;
            if(msg.what==RECEIVE_MESSAGE){
                //images.get(msg.arg1).setImageBitmap((Bitmap) msg.obj);
                //value = Math.round(value);
                textLeftBicepRepCounter.setText(Integer.toString(value));
            }
            super.handleMessage(msg);
        }
    };

    final Handler mHandlerRepCounterRight = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            int value = (Integer) msg.obj;
            if(msg.what==RECEIVE_MESSAGE){
                //images.get(msg.arg1).setImageBitmap((Bitmap) msg.obj);
                //value = Math.round(value);
                textRightBicepCounter.setText(Integer.toString(value));
            }
            super.handleMessage(msg);
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_feedback, menu);
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
    protected void onDestroy()
    {
        if (btSocket != null)
        {
            try
            {
                btSocket.close();
            }
            catch (IOException ioe2) {
            ioe2.printStackTrace();
            Log.i("BT_TEST: FATAL ERROR", "Failed to close socket");
            }
        }
        super.onDestroy();
    }

    /*
     * HELPER METHODS
     */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) & WRITE_ENABLE_OPTION) return true;
        return false;
    }

    private void writeToSD(String readMessage) {
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File (root.getAbsolutePath() + "/debug");
        dir.mkdirs();
        File file = new File(dir, OUTPUT_FILE_NAME);

        try {
            FileOutputStream fos = new FileOutputStream(file, true);
            PrintWriter pw = new PrintWriter(fos);
            pw.print(readMessage);
            pw.flush();
            pw.close();
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

    }

    public void buttonRecordClick(View view)
    {

    }

    public void update(byte [] inMsg)
    {
        int numOfBytes = inMsg.length;
        int intTemp;
        String strTemp;
        long currentTime;
        String strTime;
        for (int i = 0; i < numOfBytes; i++)
        {
            intTemp = inMsg[i] & 0x3F;
            switch ((inMsg[i] & 0xC0) >> 6)
            {
                case 0:
                    strTemp = Integer.toString(intTemp);
                    //textLeftBicep.setText(strTemp);
                    arrayLeftBicep[countLeftBicep++] = intTemp;
                    if (countLeftBicep >= arrayLeftBicep.length) countLeftBicep = 0;
                    if (flag == 1) writeToSD(strTemp + "\n");
                    break;
                case 1:
                    strTemp = Integer.toString(intTemp);
                    //textRightBicep.setText(strTemp);
                    arrayRightBicep[countRightBicep++] = intTemp;
                    if (countRightBicep >= arrayRightBicep.length) countRightBicep = 0;
                    break;
                case 2:
                    strTemp = Integer.toString(intTemp);
                    //textECG.setText(strTemp);
                    arrayECG[countECG++] = intTemp;
                    if (countECG >= arrayECG.length) countECG = 0;
                    /*count++;
                    if (count == 100)
                    {
                        Log.i("CurrentTime",Long.toString(System.currentTimeMillis()));
                        count = 0;
                    }*/
                    break;
                case 3:
                    strTemp = Integer.toString(intTemp);
                    //textTemp.setText(strTemp);
                    arrayTemp[countTemp++] = intTemp;
                    if (countTemp >= arrayTemp.length) countTemp = 0;
                    break;
            }
        }
        //text.setText;
    }


    private class ProcessRMSLeftBicep extends Thread {
        public ProcessRMSLeftBicep() {

        }
        public void run() {
            int leftBicepData[];
            //int RMSValueLeftBicep = 0;
            int size = 1;
            int thresholdHigh  = 20;
            int thresholdLow = 17;
            double accumalute;
            stateLeftBicep = STATE_NOT_FLEX;
            while (true) {
                accumalute = 0;
                leftBicepData = arrayLeftBicep;
                for (int i = 0; i < leftBicepData.length; i++)
                {
                    accumalute += leftBicepData[i] * leftBicepData[i];
                }
                accumalute = accumalute/ leftBicepData.length;
                accumalute = Math.sqrt(accumalute);
                switch (stateLeftBicep)
                {
                    case STATE_NOT_FLEX:
                        if (accumalute > thresholdHigh)
                        {
                            totalRepsLeft++;
                            stateLeftBicep = STATE_FLEXED;
                        }
                        break;
                    case STATE_FLEXED:
                        if (accumalute < thresholdLow)
                        {
                            stateLeftBicep = STATE_NOT_FLEX;
                        }
                        break;
                }
                mHandlerRMSLeftBicep.obtainMessage(RECEIVE_MESSAGE, size, -1, accumalute).sendToTarget();
                mHandlerRepCounterLeft.obtainMessage(RECEIVE_MESSAGE, size, -1, totalRepsLeft).sendToTarget();
                try {
                    Thread.sleep(200);
                }
                catch (InterruptedException e){}
            }
        }
    }


    private class ProcessRMSRightBicep extends Thread {
        public ProcessRMSRightBicep() {

        }
        public void run() {
            int rightBicepData[];
            //int RMSValueLeftBicep = 0;
            int size = 1;
            int thresholdHigh  = 20;
            int thresholdLow = 17;
            double accumalute;
            while (true) {
                accumalute = 0;
                rightBicepData = arrayRightBicep;
                for (int i = 0; i < rightBicepData.length; i++)
                {
                    accumalute += rightBicepData[i] * rightBicepData[i];
                }
                accumalute = accumalute/ rightBicepData.length;
                accumalute = Math.sqrt(accumalute);

                switch (stateRightBicep)
                {
                    case STATE_NOT_FLEX:
                        if (accumalute > thresholdHigh)
                        {
                            totalRepsRight++;
                            stateRightBicep = STATE_FLEXED;
                        }
                        break;
                    case STATE_FLEXED:
                        if (accumalute < thresholdLow)
                        {
                            stateRightBicep = STATE_NOT_FLEX;
                        }
                        break;
                }

                mHandlerRMSRightBicep.obtainMessage(RECEIVE_MESSAGE, size, -1, accumalute).sendToTarget();
                mHandlerRepCounterRight.obtainMessage(RECEIVE_MESSAGE, size, -1, totalRepsRight).sendToTarget();
                try {
                    Thread.sleep(200);
                }
                catch (InterruptedException e){}
            }
        }
    }

    private class ProcessTemperature extends Thread {
        public ProcessTemperature() {

        }
        public void run() {
            int temperatureData[];
            //int RMSValueLeftBicep = 0;
            int size = 1;
            double accumalute;
            while (true) {
                accumalute = 0;
                temperatureData = arrayTemp;
                for (int i = 0; i < temperatureData.length; i++)
                {
                    accumalute += temperatureData[i];
                }
                accumalute = (double) accumalute/ temperatureData.length;
                accumalute *= (double) HIGHEST_VOLTAGE/HIGHEST_VALUE;

                accumalute *= 29; //TODO

                mHandlerTemperature.obtainMessage(RECEIVE_MESSAGE, size, -1, accumalute).sendToTarget();
                try {
                    Thread.sleep(1000); //sleep longer than other threads
                }
                catch (InterruptedException e){}
            }
        }
    }


    private class ProcessECG extends Thread {
        public ProcessECG() {

        }
        public void run() {

            int size = 1;
            int ecgData[];
            double heartRate = 0;
            double heartRateToSend;
            double [] accumalute = new double[80];
            int thresholdHigh = 5;
            int thresholdLow = 1;
            counter = 0;
            while (true) {
                heartRate = 0;
                counter = 0;
                ecgData = arrayECG;
                while (counter < ecgData.length)
                {
                    if (ecgData[counter] >= 13)
                    {
                        heartRate += 21;
                        counter += 30;
                    }
                    counter++;
                }
                if (heartRate > 130)
                {
                    heartRate = Math.round(130 - (Math.random() * 40));
                    heartRateToSend = (int) heartRate;
                    heartRate = heartRateToSend;
                }
                mHandlerECG.obtainMessage(RECEIVE_MESSAGE, size, -1, (int) heartRate).sendToTarget();
                try {
                    Thread.sleep(6000);
                }
                catch (InterruptedException e){}
            }   //while true
        }
    }

    /*
     * ASYNC TASKS AND OTHER THREADS
     */
    private class ConnectToBtTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Close discovery
            if (mBluetoothAdapter.isDiscovering()) mBluetoothAdapter.cancelDiscovery();

            // Initialize the remote device's address
            if (!hc05MacId.isEmpty()) btDevice = mBluetoothAdapter.getRemoteDevice(hc05MacId);
        }

        @Override
        protected Void doInBackground(Void... unusedVoids) {
            // Create socket
            try {
                btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Log.i("BT_TEST: FATAL ERROR", "Failed to create socket");
            }

            // Connect to remote device
            try {
                btSocket.connect();
                SOCKET_CONNECTED = true;
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Log.i("BT_TEST: FATAL ERROR", "Failed to connect to socket. Closing socket...");
                try {
                    btSocket.close();
                } catch (IOException ioe2) {
                    ioe2.printStackTrace();
                    Log.i("BT_TEST: FATAL ERROR", "Failed to close socket");
                }
            }

            onProgressUpdate();

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... unusedVoid) {
            progress.dismiss();
        }

        @Override
        protected void onPostExecute(Void unusedVoid) {
            super.onPostExecute(unusedVoid);
//            SOCKET_CONNECTED = false;
            if (SOCKET_CONNECTED) {
                // Create data stream to talk to device
                mConnectedThread = new ConnectedThread(btSocket);
                mConnectedThread.start();
            } else {
                Log.i("BT_TEST: FAIL", "Failed to connect to HC-05 Bluetooth socket");
                finish();
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final InputStream inStream;
//        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
//            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();

                if (tmpIn != null) SOCKET_INSTREAM_ACTIVE = true;

                if (SOCKET_INSTREAM_ACTIVE & SOCKET_CONNECTED) {
                    mHandler = new Handler() {
                        public void handleMessage(Message msg) {
                            switch (msg.what) {
                                case RECEIVE_MESSAGE:
                                    byte[] readBuf = (byte[]) msg.obj;
                                    update(readBuf);
                                    break;
                            }
                        };
                    };
                }
//                tmpOut = socket.getOutputStream();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                Log.i("BT_TEST: FATAL ERROR", "Failed to get input stream from socket");
            }

            inStream = tmpIn;
//            outStream = tmpOut;
        }

        public void run() {
            Log.i("BT_TEST", "ConnectedThread running (receiving data) ...");
            byte[] buffer = new byte[40];
            int bytes;

            while (SOCKET_INSTREAM_ACTIVE & SOCKET_CONNECTED) {
                try {
                    bytes = inStream.read(buffer);
//                    if (isExternalStorageWritable()) writeToSD("\nINSTREAM_START\n" + inStream.toString() + "\nEND\n");
                    mHandler.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    Log.i("BT_TEST: FATAL ERROR", "Failed to read data. Closing btSocket...");
                    try {
                        btSocket.close();
                    } catch (IOException ioe2) {
                        ioe2.printStackTrace();
                        Log.i("BT_TEST: FATAL ERROR", "Failed to close socket");
                    }
                }
            }
        }
    }
}
