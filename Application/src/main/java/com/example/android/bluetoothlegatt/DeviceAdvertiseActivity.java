package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class DeviceAdvertiseActivity extends Activity  {
    BluetoothManager mBleManager;
    BluetoothAdapter mBleAdapter;
    BluetoothLeAdvertiser mBtAdvertiser;
    BluetoothGattCharacteristic mPsdiCharacteristic;
    BluetoothGattCharacteristic mBtCharacteristic2;
    BluetoothGattCharacteristic mNotifyCharacteristic;
    BluetoothGattService btPsdiService;
    BluetoothGattService btGattService;
    BluetoothGattServer mBtGattServer;
    BluetoothDevice mConnectedDevice;
    private TextView mConnectionState;
    private TextView mDataField;

    private static final UUID UUID_PSDI_SERVICE = UUID.fromString("e625601e-9e55-4597-a598-76018a0d293d");
    private static final UUID UUID_PSDI = UUID.fromString("26e2b12b-85f0-4f3f-9fdd-91d114270e6e");

    private static final UUID UUID_SERVICE = UUID.fromString(BleGattAttributes.SERVICE);
    private static final UUID UUID_NOTIFY = UUID.fromString(BleGattAttributes.CALIBRATION_RATE_MEASUREMENT);
    private static final UUID UUID_READ = UUID.fromString(BleGattAttributes.CALIBRATION_READ_MEASUREMENT);
    private static final UUID UUID_DESC = UUID.fromString(BleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG);
    private static final int UUID_VALUE_SIZE = 500;

    boolean mIsConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_peripheral);

        mBleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBleAdapter = mBleManager.getAdapter();
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);

        if (mBleAdapter != null) {
            peripheralBle();
        }
        Button button1 = (Button)findViewById(R.id.test1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = "Flip to 3 in 5 seconds. 5";
                byte[] notifyValue = str.getBytes(StandardCharsets.UTF_8);

                mNotifyCharacteristic.setValue(notifyValue);
                mBtGattServer.notifyCharacteristicChanged(mConnectedDevice, mNotifyCharacteristic, false);
            }

        });
        Button button2 = (Button)findViewById(R.id.test2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = "Finish. calibration." +
                        "Device: <DeviceID>\n" +
                        "Date: <DateTime>\n" +
                        "Result: <Error message>\n" +
                        "Calibration values: <Calibration values> \n";
                byte[] notifyValue = str.getBytes(StandardCharsets.UTF_8);
                mNotifyCharacteristic.setValue(notifyValue);
                mBtGattServer.notifyCharacteristicChanged(mConnectedDevice, mNotifyCharacteristic, false);
            }

        });
        Button button3 = (Button)findViewById(R.id.test3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String str = "Detection stable posture.Take your hand off cube.";
                byte[] notifyValue = str.getBytes(StandardCharsets.UTF_8);

                mNotifyCharacteristic.setValue(notifyValue);
                mBtGattServer.notifyCharacteristicChanged(mConnectedDevice, mNotifyCharacteristic, false);
            }

        });
        Button button4 = (Button)findViewById(R.id.test4);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String str = "Calibration Error.\n" +
                        "Finish. calibration error.\n";
                byte[] notifyValue = str.getBytes(StandardCharsets.UTF_8);

                mNotifyCharacteristic.setValue(notifyValue);
                mBtGattServer.notifyCharacteristicChanged(mConnectedDevice, mNotifyCharacteristic, false);
            }

        });
    }

    private void peripheralBle() {
        mBtAdvertiser = mBleAdapter.getBluetoothLeAdvertiser();
        mConnectionState.setText("Advertising");
        mDataField.setText("peripheral_mood");
        if ( mBtAdvertiser == null ) {
            Toast.makeText(this, "BLE Peripheralモードが使用できません。", Toast.LENGTH_SHORT).show();
            return;
        }

        mBtGattServer = mBleManager.openGattServer(this, mGattServerCallback);

        btPsdiService = new BluetoothGattService(UUID_PSDI_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mPsdiCharacteristic = new BluetoothGattCharacteristic(UUID_PSDI,
                                BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        btPsdiService.addCharacteristic(mPsdiCharacteristic);

        mBtGattServer.addService(btPsdiService);

        try { Thread.sleep(200); }catch(Exception ex){}

        btGattService = new BluetoothGattService(UUID_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        mNotifyCharacteristic = new BluetoothGattCharacteristic(UUID_NOTIFY,
                                BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);
        btGattService.addCharacteristic(mNotifyCharacteristic);

        mBtCharacteristic2 = new BluetoothGattCharacteristic(UUID_READ,
                                BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        btGattService.addCharacteristic(mBtCharacteristic2);

        BluetoothGattDescriptor dataDescriptor = new BluetoothGattDescriptor(UUID_DESC,
                    BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ);
        mNotifyCharacteristic.addDescriptor(dataDescriptor);

        mBtGattServer.addService(btGattService);

        try { Thread.sleep(200); }catch(Exception ex){}

        startBleAdvertising();
    }

    private void startBleAdvertising() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeTxPowerLevel(true);
        dataBuilder.addServiceUuid(ParcelUuid.fromString(BleGattAttributes.SERVICE));

        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
        settingsBuilder.setTimeout(0);
        settingsBuilder.setConnectable(true);

        AdvertiseData.Builder respBuilder = new AdvertiseData.Builder();
        respBuilder.setIncludeDeviceName(true);

        mBtAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), respBuilder.build(), new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.d("bleperi", "onStartSuccess");
            }
            @Override
            public void onStartFailure(int errorCode) {
                Log.d("bleperi", "onStartFailure");
            }
        });
    }

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        private byte[] psdiValue = new byte[8];
        private byte[] notifyDescValue = new byte[2];
        private byte[] uuid_value = new byte[UUID_VALUE_SIZE]; /* max 512 */

        @Override
        public void onMtuChanged (BluetoothDevice device, int mtu) {
            Log.d("bleperi", "onMtuChanged(" + mtu + ")");
        }

        @Override
        public void onConnectionStateChange(android.bluetooth.BluetoothDevice device, int status, int newState) {
            Log.d("bleperi", "onConnectionStateChange");

            if ( newState == BluetoothProfile.STATE_CONNECTED ) {
                mConnectedDevice = device;
                mIsConnected = true;
                Log.d("bleperi", "STATE_CONNECTED:" + device.toString());
            }
            else {
                mIsConnected = false;
                Log.d("bleperi", "Unknown STATE:" + newState);
            }
        }

        public void onCharacteristicReadRequest(android.bluetooth.BluetoothDevice device, int requestId,
                                                int offset, BluetoothGattCharacteristic characteristic) {
            Log.d("bleperi", "onCharacteristicReadRequest");

            if ( characteristic.getUuid().compareTo(UUID_PSDI) == 0 ) {
                mBtGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, psdiValue);
            } else if ( characteristic.getUuid().compareTo(UUID_READ) == 0 ) {

                if ( offset >= uuid_value.length ) {
                    mBtGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
                } else {
                    byte[] value = new byte[this.uuid_value.length - offset];
                    System.arraycopy(this.uuid_value, offset, value, 0, value.length);
                    mBtGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                }

            } else if (characteristic.getUuid().compareTo(UUID_NOTIFY) == 0 ) {
                byte[] value = new byte[this.uuid_value.length - offset];
                System.arraycopy(this.uuid_value, offset, value, 0, value.length);
                mBtGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);

            } else {
                mBtGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null );
            }
        }

        public void onCharacteristicWriteRequest(android.bluetooth.BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d("bleperi", "onCharacteristicWriteRequest");

            if ( characteristic.getUuid().compareTo(UUID_NOTIFY) == 0 ) {

                if ( offset < this.uuid_value.length ) {
                    int len = value.length;
                    if ( (offset + len ) > this.uuid_value.length)
                        len = this.uuid_value.length - offset;
                    System.arraycopy(value, 0, this.uuid_value, offset, len);
                    mBtGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
                } else {
                    mBtGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
                }

                if ( (notifyDescValue[0] & 0x01) == 0x01 ) {
                    if (offset == 0 && value[0] == (byte) 0xff) {
                        mNotifyCharacteristic.setValue(this.uuid_value);
                        mBtGattServer.notifyCharacteristicChanged(mConnectedDevice, mNotifyCharacteristic, false);
                    }
                }
            } else {
                mBtGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
            }
        }

        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Log.d("bleperi", "onDescriptorReadRequest");

            if ( descriptor.getUuid().compareTo(UUID_DESC) == 0 ) {
                mBtGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, notifyDescValue);
            }
        }

        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.d("bleperi", "onDescriptorWriteRequest");

            if ( descriptor.getUuid().compareTo(UUID_DESC) == 0 ) {
                notifyDescValue[0] = value[0];
                notifyDescValue[1] = value[1];

                mBtGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
            }
        }
    };
}
