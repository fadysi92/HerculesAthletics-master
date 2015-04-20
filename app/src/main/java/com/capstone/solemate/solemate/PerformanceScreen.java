package com.capstone.solemate.solemate;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


public class PerformanceScreen extends Activity {

    GraphView graphLeft, graphRight;
    private double graphLastXValue = 4d, graphLastXValueRight = 4d;
    private LineGraphSeries<DataPoint> series2, series3;
    private double RMSValueToGraph;

    private static final int RECEIVE_MESSAGE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_performance_screen);
        getActionBar().setDisplayHomeAsUpEnabled(false);
        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            RMSValueToGraph = extras.getDouble("rmsValue",0);
        }

        graphLeft = (GraphView) findViewById(R.id.graphLeft);
        graphLeft.getViewport().setXAxisBoundsManual(true);
        graphLeft.getViewport().setYAxisBoundsManual(true);
        graphLeft.getViewport().setMinX(0);
        graphLeft.getViewport().setMaxX(30);
        graphLeft.getViewport().setMinY(0);
        graphLeft.getViewport().setMaxY(110);
        graphLeft.setTitle("Left Bicep");
        graphLeft.getGridLabelRenderer().setVerticalAxisTitle("% Efficiency");


        series2 = new LineGraphSeries<DataPoint>(new DataPoint[] {
                new DataPoint(0, 3),
                new DataPoint(1, 3),
                new DataPoint(2, 6),
                new DataPoint(3, 2),
                new DataPoint(4, 5)
        });
        graphLeft.addSeries(series2);

        UpdateGraphLeft updateGraphLeft = new UpdateGraphLeft();
        updateGraphLeft.start();


        graphRight = (GraphView) findViewById(R.id.graphRight);
        graphRight.getViewport().setXAxisBoundsManual(true);
        graphRight.getViewport().setYAxisBoundsManual(true);
        graphRight.getViewport().setMinX(0);
        graphRight.getViewport().setMaxX(30);
        graphRight.getViewport().setMinY(0);
        graphRight.getViewport().setMaxY(110);
        graphRight.setTitle("Right Bicep");
        graphRight.getGridLabelRenderer().setVerticalAxisTitle("% Efficiency");


        series3 = new LineGraphSeries<DataPoint>(new DataPoint[] {
                new DataPoint(0, 3),
                new DataPoint(1, 3),
                new DataPoint(2, 6),
                new DataPoint(3, 2),
                new DataPoint(4, 5)
        });
        graphRight.addSeries(series3);

        UpdateGraphRight updateGraphRight = new UpdateGraphRight();
        updateGraphRight.start();

    }


    private double getValueRMSLeft()
    {
        return ((MyApplication) this.getApplication()).getRMSValueLeftBicep();
    }

    private double getValueRMSRight()
    {
        return ((MyApplication) this.getApplication()).getRMSValueRightBicep();
    }

    final Handler mHandlerGraphLeft = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            DataPoint value = (DataPoint) msg.obj;
            if(msg.what==RECEIVE_MESSAGE){
                series2.appendData(value,true,30);
            }
            super.handleMessage(msg);
        }
    };
    private class UpdateGraphLeft extends Thread {
        public UpdateGraphLeft() {

        }
        public void run() {
            int size = 1;
            double valueReceived;
            while (true) {
                graphLastXValue += 1d;
                //series2.appendData(new DataPoint(graphLastXValue, Math.random()%5),true,10);
                valueReceived = getValueRMSLeft();
                DataPoint dataPoint = new DataPoint(graphLastXValue, valueReceived);
                //DataPoint dataPoint = new DataPoint(graphLastXValue, RMSValueToGraph);
                mHandlerGraphLeft.obtainMessage(RECEIVE_MESSAGE, size, -1, dataPoint).sendToTarget();
                try {
                    Thread.sleep(300);
                }
                catch (InterruptedException e){}
            }
        }
    }


    final Handler mHandlerGraphRight = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            DataPoint value = (DataPoint) msg.obj;
            if(msg.what==RECEIVE_MESSAGE){
                series3.appendData(value,true,30);
            }
            super.handleMessage(msg);
        }
    };
    private class UpdateGraphRight extends Thread {
        public UpdateGraphRight() {

        }
        public void run() {
            int size = 1;
            double valueReceived;
            while (true) {
                graphLastXValueRight += 1d;
                //series2.appendData(new DataPoint(graphLastXValue, Math.random()%5),true,10);
                valueReceived = getValueRMSRight();
                DataPoint dataPoint = new DataPoint(graphLastXValueRight, valueReceived);
                //DataPoint dataPoint = new DataPoint(graphLastXValue, RMSValueToGraph);
                mHandlerGraphRight.obtainMessage(RECEIVE_MESSAGE, size, -1, dataPoint).sendToTarget();
                try {
                    Thread.sleep(300);
                }
                catch (InterruptedException e){}
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_performance_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id)
        {
            case R.id.action_settings:
                return true;
            case R.id.home:
                finish();
        }

        //noinspection SimplifiableIfStatement


        return super.onOptionsItemSelected(item);
    }
}
