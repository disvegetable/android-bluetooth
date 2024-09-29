package com.example.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;


public class CameraActivity extends AppCompatActivity {

    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;

    String TAG="camera error";

    ActionBar actionBar;

    Thread connectThread=new Thread(){
        @Override
        public void run(){
            connectToDevice();
        }
    };

    @Override
    protected void onCreate(Bundle saveInstance){
        super.onCreate(saveInstance);
        setContentView(R.layout.camera_layout);
        //get device and adapter
        mBluetoothDevice=getIntent().getParcelableExtra("device", BluetoothDevice.class);
        try{
            Toast.makeText(this, mBluetoothDevice.getName(), Toast.LENGTH_SHORT).show();
        }catch(SecurityException s){
            Log.e(TAG, Objects.requireNonNull(s.getMessage()));
        }

        //arrow to get back to MainActivity
        actionBar=getSupportActionBar();
        if(actionBar!=null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        connectThread.start();


    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d("camera finish","finish");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        if(item.getItemId()==android.R.id.home){
            if(mBluetoothSocket!=null){
                try {
                    mBluetoothSocket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            finish();
            return true;
        }
        else{
            return super.onOptionsItemSelected(item);
        }
    }


    void connectToDevice(){
        try {
            UUID uuid = mBluetoothDevice.getUuids()[1].getUuid();
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            mBluetoothSocket.connect();
            if(mBluetoothSocket.isConnected()){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "successfully", Toast.LENGTH_SHORT).show();
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
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
                try {
                    mBluetoothSocket.close();
                }catch (IOException i){
                    Log.e(TAG,e.getMessage());
                }
            }
        }
    }
}
