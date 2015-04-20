package com.capstone.solemate.solemate;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements OnClickListener {
    private final static int REQUEST_ENABLE_BT = 1;

    // List to pass to list view array adapter
    private static List<String> discoveredDevices = new ArrayList<String>();
    private static ArrayAdapter arrayAdapter;

    // BT device connection attributes
    private static String hc05MacId = new String();
    private static Point size = new Point();
    private static Display display;

    // Loading spinner
    public ProgressDialog progress;

    // Init default bluetooth adapter
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    public Button startDiscoveryButton;
    public ListPopupWindow listPopupWindow;

    // Register the BroadcastReceiver
    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                Log.i ("BT_TEST: DEVICE FOUND", "DEVICE NAME: " + device.getName());
                Log.i ("BT_TEST: DEVICE FOUND", "DEVICE ADDRESS: " + device.getAddress());
                if (!discoveredDevices.contains(device.getName() + "\n" + device.getAddress())) {
                    discoveredDevices.add(device.getName() + "\n" + device.getAddress());
                    arrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Cancel bluetooth discovery
        mBluetoothAdapter.cancelDiscovery();

        progress.dismiss();
        listPopupWindow.dismiss();

        // Clear list and adapter
        discoveredDevices.clear();
        arrayAdapter.clear();

    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        progress.dismiss();
        listPopupWindow.dismiss();

        // Make sure bluetooth is not discovering
        mBluetoothAdapter.cancelDiscovery();

        // Unregister receiver
        unregisterReceiver(mReceiver);

        // Clear list and adapter
        discoveredDevices.clear();
        arrayAdapter.clear();


    }

    protected void init() {
        // Init discovery button
        startDiscoveryButton = (Button) findViewById(R.id.startDiscovery);
        startDiscoveryButton.setOnClickListener(this);
//        startDiscoveryButton.setBackgroundColor(0xFF211a72);

        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        // Init array adapter
        arrayAdapter = new ArrayAdapter(this, R.layout.device_list_item);

        // Init listpopupwindow
        listPopupWindow = new ListPopupWindow(this);
        listPopupWindow.setAdapter(arrayAdapter);
        arrayAdapter.setNotifyOnChange(true);
        listPopupWindow.setAnchorView(findViewById(R.id.frameLayout));
        display = getWindowManager().getDefaultDisplay();
        display.getSize(size);
        listPopupWindow.setHeight(size.y-400);
        listPopupWindow.setWidth(ListPopupWindow.WRAP_CONTENT);
        listPopupWindow.setVerticalOffset(-80);
        listPopupWindow.setModal(true);
        listPopupWindow.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                TextView selectedItemView = (TextView) arg1;
                String selectedItemStr = selectedItemView.getText().toString();

                // MAC ID for HC-05 module: 98:D3:31:40:20:D9
                hc05MacId = selectedItemStr.substring(selectedItemStr.indexOf("\n") + "\n".length());
//                hc05MacId = "98:D3:31:40:20:D9";

                if (mBluetoothAdapter == null) {
                    Log.i("BT_TEST: FATAL ERROR", "Bluetooth adapter is null!");
                } else {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Log.i("BT_TEST", "Bluetooth is not enabled!");
                    } else {
                        listPopupWindow.dismiss();

                        // Launch new activity to connect to bluetooth and provide real-time user feedback
                        Intent myIntent = new Intent(MainActivity.this, FeedbackActivity.class);
                        myIntent.putExtra("hc05MacId", hc05MacId);
                        MainActivity.this.startActivity(myIntent);
                    }
                }
            }
        });

        // Init loading spinner
        progress = new ProgressDialog(this);
        progress.setTitle("Finding Devices");
        progress.setMessage("Please wait...");

    }

    public void onClick(View v) {
        if (v.getId() == R.id.startDiscovery) {
            /*
             * ENABLE BLUETOOTH ADAPTER
             */

            // Perform action on click
            if (mBluetoothAdapter == null) {
                Log.i ("BT_TEST_DEBUG", "Device does not support Bluetooth");
            } else if (!mBluetoothAdapter.isEnabled()) {
                /*
                 * Begin AsyncTask to enable BT
                 */
                Log.i("BT_TEST", "Bluetooth is not enabled!");
                new EnableBtTask().execute();
            } else if (!mBluetoothAdapter.isDiscovering()) {
                /*
                 * Begin AsyncTask to start discovery
                 */
                if (arrayAdapter.isEmpty()) {
                    new DiscoveryTask().execute();
                } else {
                    listPopupWindow.show();
                }

            } else if (mBluetoothAdapter.isDiscovering()) {
                listPopupWindow.show();
                progress.show();
                Log.i("BT_TEST", "Bluetooth is already discovering!");
//                    } else if (!mBluetoothAdapter.isEnabled()) {
//                        Log.i("BT_TEST", "Bluetooth is not enabled!");
            }

        } else {
            progress.dismiss();
            listPopupWindow.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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


    /*
     * ASYNC TASKS AND OTHER THREADS
     */
    private class EnableBtTask extends AsyncTask<Void, Void, Void> {
        protected boolean START_DISCOVERY = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... unusedVoids) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            int count = 0;
            while (!START_DISCOVERY && count < 4) {
                if (mBluetoothAdapter.isEnabled()) {
                    START_DISCOVERY = true;
                } else {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Log.i("BT_TEST", "Bluetooth is not yet enabled...");
                    }

                    // Put this thread to sleep for 0.5s
                    try {
                        Thread.sleep(2000);
                        Log.i("BT_TEST", "EnableBtTask sleeping for 3000ms...");
                        count++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void unusedVoid) {
            super.onPostExecute(unusedVoid);

            if (START_DISCOVERY && !mBluetoothAdapter.isDiscovering()) {
                new DiscoveryTask().execute();
            }

        }
    }

    private class DiscoveryTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            listPopupWindow.show();
            progress.show();

        }

        @Override
        protected Void doInBackground(Void... unusedVoids) {
            if (mBluetoothAdapter.isEnabled()) {
                boolean isDiscovering = mBluetoothAdapter.startDiscovery();
                int count = 0;
                while (isDiscovering && count < 3) {
                    isDiscovering = mBluetoothAdapter.isDiscovering();
                    count++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }

                }

                // Cancel discovery
                mBluetoothAdapter.cancelDiscovery();
            }

            return null;
        }


        @Override
        protected void onPostExecute(Void unusedVoid) {
            super.onPostExecute(unusedVoid);

            progress.dismiss();
        }
    }

}
