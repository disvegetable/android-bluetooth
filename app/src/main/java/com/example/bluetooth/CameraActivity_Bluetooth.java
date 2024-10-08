package com.example.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CameraActivity_Bluetooth extends AppCompatActivity {

    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;
    String TAG="camera error";
    ActionBar actionBar;
    ExecutorService cameraExecutor;//camerax
    Button mButton;
    ScrollView scrollView;

    //thread for bluetooth connection
    Thread connectThread=new Thread(){
        @Override
        public void run(){
            connectToDevice();
        }
    };

    Thread sendThread=new Thread(){
        @Override
        public void run(){
            while(true){
                try {
                    Thread.sleep(1000);
                    sendMessage();
                    //testSend();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    };

    Thread receiveThread=new Thread() {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(1000);
                    receiveMessage();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
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

        mButton=(Button)findViewById(R.id.sendButton);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText editText=(EditText)findViewById(R.id.commendLine);
                if(editText.getText()==null){
                    return;
                }
                testSend(editText.getText().toString());
            }
        });

        scrollView=(ScrollView)findViewById(R.id.terminal);

        //camera executor thread
        cameraExecutor= Executors.newSingleThreadExecutor();
        startCamera();
    }

    @Override
    public void onResume(){
        super.onResume();
        connectThread.start();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //close camera
        cameraExecutor.shutdown();
        Log.d("camera finish","finish");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        //get back to mainActivity
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

    //connect to device with bluetooth
    void connectToDevice(){
        try {
            UUID uuid = mBluetoothDevice.getUuids()[1].getUuid();
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            mBluetoothSocket.connect();
            if(mBluetoothSocket.isConnected()){
                sendThread.start();
                receiveThread.start();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addCommendHistory("connected!",Color.GREEN);
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

    //open camera
    private void startCamera(){
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture=ProcessCameraProvider.getInstance(this);
        PreviewView cameraPreview=(PreviewView) findViewById(R.id.preview);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    //get surface provider
                    ProcessCameraProvider cameraProvider=cameraProviderFuture.get();
                    Preview preview=new Preview.Builder().build();
                    preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                    CameraSelector cameraSelector=CameraSelector.DEFAULT_BACK_CAMERA;
                    //unbind all camera
                    cameraProvider.unbindAll();
                    cameraProvider.bindToLifecycle(CameraActivity_Bluetooth.this,cameraSelector,preview);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    //send message function
    void sendMessage(){
        if(!mBluetoothSocket.isConnected()){
            return;
        }
        try{
            OutputStream outputStream=mBluetoothSocket.getOutputStream();
            int m=0x32;
            outputStream.write(m);
            //show sending text
            runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    addCommendHistory("[O] "+"0x32",Color.YELLOW);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            });
        }catch(IOException i){
            i.printStackTrace();
        }
    }

    //receive message function
    void receiveMessage(){
        if(!mBluetoothSocket.isConnected()){
            return;
        }
        try{
            InputStream inputStream=mBluetoothSocket.getInputStream();
            byte[] message = new byte[2];
            int result=inputStream.read(message);
            if(result==1){
                //show receiving text
                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        addCommendHistory("[I] "+Arrays.toString(message),Color.CYAN);
                    }
                });
            }
            else{
                //show error receiving
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addCommendHistory("receiving error!",Color.RED);
                    }
                });
            }
        }catch(IOException i){
            i.printStackTrace();
        }
    }

    void testSend(String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                byte[] msg=message.getBytes();
                addCommendHistory("[O] "+ Arrays.toString(msg),Color.GREEN);
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void addCommendHistory(String commend, int color){
        LinearLayout linearLayout=(LinearLayout) findViewById(R.id.cmdHistory);
        TextView textView=new TextView(CameraActivity_Bluetooth.this);
        textView.setText(commend);
        textView.setTextColor(color);
        linearLayout.addView(textView);
    }
}
