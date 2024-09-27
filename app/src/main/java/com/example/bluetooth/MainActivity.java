package com.example.bluetooth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'bluetooth' library on application startup.
    static {
        System.loadLibrary("bluetooth");
    }

    private final int REQUEST_CODE=100;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothManager mBluetoothManager;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;

    String TAG="error";

    Button mButton;

    Thread ConnectThread=new Thread(){
        @Override
        public void run(){
            connectToDevice();
        }
    };

    @Override
    protected void onCreate(Bundle saveInstance){
        super.onCreate(saveInstance);
        setContentView(R.layout.activity_main);

        checkPermissions();

        mButton=(Button)findViewById(R.id.sendButton);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int cmd=0x32;
                send(cmd);
            }
        });

    }

    @Override
    public void onResume(){
        super.onResume();
        mBluetoothManager=(BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter=mBluetoothManager.getAdapter();
        Set<BluetoothDevice> pairedDevices=null;
        try {
            pairedDevices =mBluetoothAdapter.getBondedDevices();
            Toast.makeText(this,String.valueOf(pairedDevices.size()), Toast.LENGTH_SHORT).show();
            TextView textView=(TextView) findViewById(R.id.deviseList);
            StringBuilder stringBuilder=new StringBuilder();
            for(BluetoothDevice device:pairedDevices){
                mBluetoothDevice=device;
                stringBuilder.append(textView.getText()).append(device.getName()).append(" ").append(Arrays.toString(device.getUuids()));
                break;
            }
            textView.setText(stringBuilder.toString());
            ConnectThread.start();

        }catch (SecurityException s){
            s.printStackTrace();
        }
    }

    @Override
    public void onDestroy(){
        try{
            mBluetoothSocket.close();
        }catch (IOException i){
            Log.e(TAG,i.getMessage());
        }
        super.onDestroy();
    }

    void connectToDevice(){
        try {
            UUID uuid = mBluetoothDevice.getUuids()[1].getUuid();
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            mBluetoothSocket.connect();
            if (mBluetoothSocket.isConnected()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = (TextView) findViewById(R.id.deviceStatus);
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(textView.getText()).append("successful");
                        textView.setText(stringBuilder.toString());
                    }
                });
            }
            else{
                mBluetoothSocket.close();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = (TextView) findViewById(R.id.deviceStatus);
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(textView.getText()).append("failed");
                        textView.setText(stringBuilder.toString());
                    }
                });

            }
        }
        catch(SecurityException | IOException s){
            try{
                Method method=mBluetoothDevice.getClass().getMethod("createRfcommSocketToServiceRecord", UUID.class);
                mBluetoothSocket=(BluetoothSocket) method.invoke(mBluetoothDevice,mBluetoothDevice.getUuids()[0].getUuid());
                mBluetoothSocket.connect();
            }catch (Exception e){
                Log.e(TAG,e.getMessage());
                try {
                    mBluetoothSocket.close();
                }catch (IOException i){
                    Log.e(TAG,e.getMessage());
                }
            }
        }
    }


    void send(int commend){
        try {
            OutputStream outputStream = mBluetoothSocket.getOutputStream();
            outputStream.write(commend);
            outputStream.flush();
        }catch (IOException i){
            Log.e(TAG,i.getMessage());
        }
    }

    void checkPermissions(){
        List<String> permissions=new ArrayList<>();
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)!=PackageManager.PERMISSION_GRANTED){
            permissions.add(Manifest.permission.BLUETOOTH);
        }
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED){
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_ADMIN)!=PackageManager.PERMISSION_GRANTED){
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(!permissions.isEmpty()){
            ActivityCompat.requestPermissions(this,permissions.toArray(new String[permissions.size()]),REQUEST_CODE);
        }
        else{
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQUEST_CODE){
            for(int grant:grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "error granted", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        else{
            Toast.makeText(this, "no permissions", Toast.LENGTH_SHORT).show();
        }
    }
}