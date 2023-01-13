package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ScrollView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mLogField;
    private TextView mLogOperation;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private StringBuilder mlog = new StringBuilder();

    private boolean mConnected = false;
    private boolean mNotifyEnabled = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    public static final byte[] ENABLE_WRITE_VALUE = {0x01};

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private final int SERVICE_POSITION = 4;
    private final int NOTIFY_POSITION = 0;
    private final int READ_POSITION = 1;

    public final static UUID UUID_SERVICE = UUID.fromString(BleGattAttributes.SERVICE);

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                mNotifyEnabled = true;
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {

                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();

                        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(UUID_SERVICE,mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(UUID_SERVICE,characteristic, true);
                        }

                        return true;
                    }
                    return false;
                }
    };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
        mLogField.setText(R.string.no_data);
        mLogOperation.setText(R.string.no_data);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mLogField = (TextView) findViewById(R.id.log_field);
        mLogOperation = (TextView) findViewById(R.id.log_operation);

        TextView textView = findViewById(R.id.log_field);
        textView.setMovementMethod(new ScrollingMovementMethod());
        final Button mCalibrationButton = (Button)findViewById(R.id.calibration_button);

        mCalibrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(SERVICE_POSITION).get(NOTIFY_POSITION);
                final int charaProp = characteristic.getProperties();
                if (mNotifyEnabled) {
                    if (mGattCharacteristics != null) {

                        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(UUID_SERVICE, characteristic, true);
                        }
                        mBluetoothLeService.writeCharacteristic(characteristic, ENABLE_WRITE_VALUE);
                        mCalibrationButton.setText("Stop Calibration");
                        mNotifyEnabled = false;
                    }
                } else {
                    mNotifyCharacteristic = characteristic;
                    mBluetoothLeService.setCharacteristicNotification(UUID_SERVICE, characteristic, false);
                    mCalibrationButton.setText("Start Calibration");
                    mNotifyEnabled = true;
                }
            }
        });
        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        final Button mCalibrationButton = (Button)findViewById(R.id.calibration_button);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_read).setVisible(true);
            menu.findItem(R.id.menu_save).setVisible(true);
            mCalibrationButton.setEnabled(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_read).setVisible(false);
            menu.findItem(R.id.menu_save).setVisible(true);
            mCalibrationButton.setEnabled(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                if (mBluetoothLeService.connect(mDeviceAddress)){
                    mConnected = true;
                }
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case R.id.menu_read:
                if (mGattCharacteristics != null) {
                    final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(SERVICE_POSITION).get(READ_POSITION);
                    mBluetoothLeService.readCharacteristic(characteristic);
                }
                return true;
            case R.id.menu_save:
                saveLogText();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void updateLogState(final String data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String str_flip = "Flip";
                String str_detection = "Detection";
                String str_error = "Error";
                String str_success = "Finish";
                if (data.contains(str_flip)) {
                    Pattern p = Pattern.compile("(?!\\D)(\\d+)(?=\\D+)");
                    Matcher m = p.matcher(data);
                    if (m.find()) {
                        mLogOperation.setText("Flip to " + m.group());
                    }
                }
                else if(data.contains(str_detection)) {
                        mLogOperation.setText("Take your hand off cube.");
                }
                else if(data.contains(str_error)) {
                        mLogOperation.setText("Calibration Error.");
                }
                else if(data.contains(str_success)) {
                        mLogOperation.setText("Calibration Success.");
                }
                mlog.append(data.toString() + "\n");
                mLogField.setText(mlog);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            ((View) mLogField.getParent()).post(new ScrollDown());
            if (BluetoothLeService.UUID_CURRENT_CHARACTERISTIC.equals(BluetoothLeService.UUID_CALIBRATION_RATE_MEASUREMENT)){
                updateLogState(data);
            } else {
                mDataField.setText(data);
            }
        }
    }
    private class ScrollDown implements Runnable {
        public void run() {
            ((ScrollView) mLogField.getParent()).fullScroll(View.FOCUS_DOWN);
        }
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, BleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();

                currentCharaData.put(LIST_NAME, BleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

    private void saveLogText(){
        Context context = getApplicationContext();
        LocalDateTime ldt = LocalDateTime.now();
        String deviceName = mDeviceName.replaceAll(" ", "_");
        String fileName = deviceName + "_" + mDeviceAddress + "_"+ ldt;
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
        StringBuilder str = new StringBuilder();
        str.append(mDeviceName + mDeviceAddress + "\n\n");
        if (mDataField != null) {
            str.append("[Read_Calibration]" + "\n");
            str.append(mDataField.getText() + "\n\n");
        }

        if (mLogField != null) {
            str.append("[Notify_Calibration]" + "\n");
            str.append(mLogField.getText());
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(str.toString());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
