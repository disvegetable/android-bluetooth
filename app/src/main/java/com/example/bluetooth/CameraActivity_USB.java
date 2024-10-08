package com.example.bluetooth;


import android.annotation.SuppressLint;
import android.graphics.Color;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
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
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity_USB extends AppCompatActivity {

    ActionBar actionBar;
    ExecutorService cameraExecutor;//camerax
    Button mButton;
    ScrollView scrollView;
    UsbManager mUsbManager;
    UsbDevice mUsbDevice;
    UsbInterface mInterface;
    UsbDeviceConnection mConnection;
    UsbEndpoint EndpointIn;
    UsbEndpoint EndpointOut;

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
                    byte message=0x31;
                    sendByte(message);
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
                    receiveByte();
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
        mUsbManager=(UsbManager) getSystemService(USB_SERVICE);
        mUsbDevice=getIntent().getParcelableExtra("usbDevice",UsbDevice.class);

        //arrow to get back to MainActivity
        actionBar=getSupportActionBar();
        if(actionBar!=null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

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
        cameraExecutor.shutdown();
    }

    //back to main activity
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        //get back to mainActivity
        if(item.getItemId()==android.R.id.home){
            finish();
            return true;
        }
        else{
            return super.onOptionsItemSelected(item);
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
                    cameraProvider.bindToLifecycle(CameraActivity_USB.this,cameraSelector,preview);
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    //connect to device
    @SuppressLint("SetTextI18n")
    public void connectToDevice() {
        mConnection = mUsbManager.openDevice(mUsbDevice);
        if (mConnection != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addCommendHistory("connected!", Color.GREEN);
                }
            });
            //configure the endpoints
            mInterface = mUsbDevice.getInterface(0);
            int SumOfEndpoint = mInterface.getEndpointCount();
            //make endpoint for sending or receiving message
            for (int i = 0; i < SumOfEndpoint; i++) {
                if (mInterface.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (mInterface.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN) {
                        EndpointIn = mInterface.getEndpoint(i);
                    } else {
                        EndpointOut = mInterface.getEndpoint(i);
                    }
                }
            }
            mConnection.claimInterface(mInterface, true);
            //config baud rate
            configBaudRate(9600);
            //start thread to send and receive message
            sendThread.start();
            receiveThread.start();
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addCommendHistory("no permissions!",Color.GREEN);
                }
            });
        }
    }


    @SuppressLint("SetTextI18n")
    public void sendByte(byte commend) {
        byte[] mes=new byte[1];
        mes[0]=commend;
        int result = mConnection.bulkTransfer(EndpointOut, mes, mes.length, 100);
        if (result > 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addCommendHistory("[I] "+"0x"+Integer.toHexString(commend),Color.GREEN);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addCommendHistory("[I] "+"send failed",Color.RED);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            });
        }
    }

    @SuppressLint("SetTextI18n")
    public void receiveByte() {
        byte[] mes = new byte[1];
        int result = mConnection.bulkTransfer(EndpointIn, mes, mes.length, 200);
        if (result > 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addCommendHistory("[O] "+"0x"+Integer.toHexString(mes[0]),Color.GREEN);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addCommendHistory("[O] "+"receive failed",Color.RED);
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            });
        }
    }

    //add command history
    @SuppressLint("SetTextI18n")
    public void addCommendHistory(String commend, int color) {
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.cmdHistory);
        TextView textView = new TextView(CameraActivity_USB.this);
        textView.setText(commend);
        textView.setTextColor(color);
        linearLayout.addView(textView);
    }

    //config baud rate for usb
    public void configBaudRate(int BaudRate) {
        byte[] arrayOfByte = new byte[8];
        mConnection.controlTransfer(192, 95, 0, 0, arrayOfByte, 8, 1000);
        mConnection.controlTransfer(64, 161, 0, 0, null, 0, 1000);
        long l1 = 1532620800 / BaudRate;
        for (int i = 3; ; i--) {
            if ((l1 <= 65520L) || (i <= 0)) {
                long l2 = 65536L - l1;
                int j = (short) (int) (0xFF00 & l2 | i);
                int k = (short) (int) (0xFF & l2);
                mConnection.controlTransfer(64, 154, 4882, j, null, 0, 1000);
                mConnection.controlTransfer(64, 154, 3884, k, null, 0, 1000);
                mConnection.controlTransfer(192, 149, 9496, 0, arrayOfByte, 8, 1000);
                mConnection.controlTransfer(64, 154, 1304, 80, null, 0, 1000);
                mConnection.controlTransfer(64, 161, 20511, 55562, null, 0, 1000);
                mConnection.controlTransfer(64, 154, 4882, j, null, 0, 1000);
                mConnection.controlTransfer(64, 154, 3884, k, null, 0, 1000);
                mConnection.controlTransfer(64, 164, 0, 0, null, 0, 1000);
                return;
            }
            l1 >>= 3;
        }
    }

}
