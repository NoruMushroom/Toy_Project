package com.example.smart_fan;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity implements Listener {
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private Switch bluetooth_switch;
    private Switch auto_switch;
    private SeekBar angle_seekbar;
    private TextView angle_text;
    private TextView connect_device;
    private View auto_view;
    private View angle_view;
    private View Fan_view;
    private BluetoothDevice bluetoothDevice;
    private BluetoothGatt gatt_device;
    private Dialog Progress_Connect;
    private RadioGroup Fan_Speed;
    private final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
    private BluetoothGattCharacteristic send_characteristic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//Title bar ?????????
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        angle_seekbar = findViewById(R.id.angle_bar);
        connect_device = findViewById(R.id.connect_device);
        angle_text = findViewById(R.id.angle);
        bluetooth_switch = findViewById(R.id.bluetooth_btn);
        auto_switch = findViewById(R.id.auto_btn);
        Fan_Speed = findViewById(R.id.Fan_Speed);
        auto_view = findViewById(R.id.Auto_view);
        angle_view = findViewById(R.id.Angle_view);
        Fan_view = findViewById(R.id.Fan_view);
        auto_view.setVisibility(View.INVISIBLE);//????????????
        angle_view.setVisibility(View.INVISIBLE);//
        Fan_view.setVisibility(View.INVISIBLE);
        bluetooth_switch.setChecked(false);//???????????? off
        auto_switch.setChecked(true);//???????????? ON
        ActivityResultLauncher<Intent> startActivityResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Device_List Dialog = new Device_List(this,this);
                        auto_view.setVisibility(View.VISIBLE);
                        Dialog.show();
                        Dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                Progress_Connect = new Dialog(MainActivity.this);
                                Progress_Connect.setContentView(R.layout.progressbar_connect);
                                if(bluetoothDevice != null){
                                    gatt_device = bluetoothDevice.connectGatt(getApplicationContext(),false,gattCallback);
                                    Progress_Connect.show();
                                }
                                else{
                                    bluetooth_switch.setChecked(false);
                                    auto_view.setVisibility(View.INVISIBLE);
                                    angle_view.setVisibility(View.INVISIBLE);
                                    Fan_view.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                    else{
                        bluetooth_switch.setChecked(false);
                    }
                });
        bluetooth_switch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked) {
                Device_List Dialog = new Device_List(this,this);
                if(bluetoothAdapter == null){//???????????? ????????? ????????? ??????
                    bluetooth_switch.setChecked(false);
                    Toast.makeText(this,"??????????????? ???????????? ?????? ???????????????.",Toast.LENGTH_SHORT).show();
                }
                else{
                    if (!bluetoothAdapter.isEnabled()) {//?????? ????????? ??????????????????
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityResult.launch(enableBtIntent);
                    }
                    else {//????????? ????????????
                        auto_view.setVisibility(View.VISIBLE);
                        Dialog.show();
                        Dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                Progress_Connect = new Dialog(MainActivity.this);
                                Progress_Connect.setContentView(R.layout.progressbar_connect);
                                if(bluetoothDevice != null){
                                    gatt_device = bluetoothDevice.connectGatt(getApplicationContext(),false,gattCallback);
                                    Progress_Connect.show();
                                }
                                else{
                                    bluetooth_switch.setChecked(false);
                                    auto_view.setVisibility(View.INVISIBLE);
                                    angle_view.setVisibility(View.INVISIBLE);
                                    Fan_view.setVisibility(View.INVISIBLE);
                                }
                            }
                        });
                    }
                }//?????? ?????? ?????? ??? ?????? ??????
            }//??????????????? ?????????
            else{
                if(gatt_device != null) {
                    gatt_device.disconnect();
                }
                bluetooth_switch.setChecked(false);
                auto_view.setVisibility(View.INVISIBLE);
                angle_view.setVisibility(View.INVISIBLE);
                Fan_view.setVisibility(View.INVISIBLE);
            }//???????????? ????????? ???????????? ????????? ???????????? ???????????? ?????????.
        });
        auto_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    send_characteristic.setValue("Auto");
                    gatt_device.writeCharacteristic(send_characteristic);
                    angle_view.setVisibility(View.INVISIBLE);
                    Fan_view.setVisibility(View.INVISIBLE);
                }//????????????
                else{
                    send_characteristic.setValue("Handle");
                    gatt_device.writeCharacteristic(send_characteristic);
                    angle_seekbar.setProgress(90);
                    angle_view.setVisibility(View.VISIBLE);
                    Fan_view.setVisibility(View.VISIBLE);
                }//????????????
            }
        });
        angle_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                send_characteristic.setValue(String.valueOf(progress));
                gatt_device.writeCharacteristic(send_characteristic);
                angle_text.setText(progress+"??");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });//???????????? ????????? ????????? ????????? ?????? ???????????????.
        Fan_Speed.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.High:
                        send_characteristic.setValue("High");
                        gatt_device.writeCharacteristic(send_characteristic);
                        break;
                    case R.id.Middle:
                        send_characteristic.setValue("Middle");
                        gatt_device.writeCharacteristic(send_characteristic);
                        break;
                    case R.id.Low:
                        send_characteristic.setValue("Low");
                        gatt_device.writeCharacteristic(send_characteristic);
                        break;
                    case R.id.Stop:
                        send_characteristic.setValue("Stop");
                        gatt_device.writeCharacteristic(send_characteristic);
                        break;
                }
            }
        });
    }
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("??????", "???????????? ?????? ??????, ????????? ??????");
                connect_device.setText("????????? ?????? : "+gatt.getDevice().getName());
                gatt_device.discoverServices(); // onServicesDiscovered() ?????? (????????? ?????? ?????? ??? ??????)
                Progress_Connect.dismiss();
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                auto_view.setVisibility(View.INVISIBLE);
                connect_device.setText("????????? ?????? : ??????");
                gatt_device = null;
            }
        }
        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if( status == BluetoothGatt.GATT_SUCCESS)
            {
                List<BluetoothGattService> services = gatt.getServices();
                for (BluetoothGattService service : services) {
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        //"Found characteristic : " + characteristic.getUuid()
                        if( hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE))
                        {
                            gatt.writeCharacteristic(characteristic);
                            send_characteristic = characteristic;
                            Log.d("??????", "??????" + characteristic.getUuid());
                        }

                        if( hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY))
                        {
                            // "Register notification for characteristic : " + characteristic.getUuid());
                            gatt.setCharacteristicNotification(characteristic, true);
                        }
                    }
                }
            }
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d("??????", "Characteristic " + characteristic.getUuid() + " written");
        }

    };
    public boolean hasProperty(BluetoothGattCharacteristic characteristic, int property)
    {
        int prop = characteristic.getProperties() & property;
        return prop == property;
    }
    @Override
    public void Device_info(BluetoothDevice ble_device) {
        this.bluetoothDevice = ble_device;
    }
}