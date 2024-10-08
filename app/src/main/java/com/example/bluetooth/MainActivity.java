package com.example.bluetooth;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'bluetooth' library on application startup.
    static {
        System.loadLibrary("bluetooth");
    }

    //request code
    //bluetooth
    public BluetoothAdapter mBluetoothAdapter;
    BluetoothManager mBluetoothManager;
    UsbManager mUsbManager;
    UsbDevice mUsbDevice;

    String TAG="error";

    public ActivityResultLauncher BluetoothLauncher;
    public ActivityResultLauncher USBLauncher;

    //list of bluetooth devices using recycle view
    RecyclerView BluetoothRecyclerView;

    //list of bluetooth devices using recycle view
    RecyclerView USBRecyclerView;

    @Override
    protected void onCreate(Bundle saveInstance){
        super.onCreate(saveInstance);
        setContentView(R.layout.activity_main);

        //check permission
        checkPermissions();
        //start camera activity,bluetooth
        BluetoothLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult o) {
                if(o!=null){
                    Toast.makeText(MainActivity.this, "back", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //start camera activity,USB
        USBLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult o) {
                if(o!=null){
                    Toast.makeText(MainActivity.this, "back", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //usb service
        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);
        if (!mUsbManager.getDeviceList().isEmpty()) {
            HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
            Iterator<UsbDevice> iterator = deviceList.values().iterator();
            if (iterator.hasNext()) {
                mUsbDevice = iterator.next();
                //check usb permission
                if (!mUsbManager.hasPermission(mUsbDevice)) {
                    Intent intent = new Intent(this, usbPermissionActivity.class);
                    startActivity(intent);
                }
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();

        //initial the Bluetooth recycleView
        initBluetoothRecycleView();
        //initial the USB recycleView
        initUSBRecycleView();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    //item animation
    private void initBluetoothRecycleView() {
        //bind recyclerView
        BluetoothRecyclerView=(RecyclerView) findViewById(R.id.BluetoothDeviceList);
        LinearLayoutManager manager=new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        BluetoothRecyclerView.setLayoutManager(manager);
        mBluetoothManager=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter=mBluetoothManager.getAdapter();
        Set<BluetoothDevice> pairedDevices=null;
        try{
            pairedDevices=mBluetoothAdapter.getBondedDevices();
            DeviceAdapter_Bluetooth adapter=new DeviceAdapter_Bluetooth(this,pairedDevices.toArray(new BluetoothDevice[pairedDevices.size()]),mBluetoothAdapter,this);
            DividerItemDecoration divider=new DividerItemDecoration(this,manager.getOrientation());
            BluetoothRecyclerView.addItemDecoration(divider);
            //set item animation
            Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.item_animate);
            LayoutAnimationController layoutAnimationController = new LayoutAnimationController(animation);
            layoutAnimationController.setOrder(LayoutAnimationController.ORDER_NORMAL);
            layoutAnimationController.setDelay(0.2f);
            BluetoothRecyclerView.setLayoutAnimation(layoutAnimationController);
            BluetoothRecyclerView.setAdapter(adapter);
        }catch (SecurityException s){
            Log.e(TAG, Objects.requireNonNull(s.getMessage()));
        }
    }

    private void initUSBRecycleView(){
        USBRecyclerView=(RecyclerView) findViewById(R.id.USBDeviceList);
        LinearLayoutManager manager=new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false);
        USBRecyclerView.setLayoutManager(manager);
        mUsbManager=(UsbManager) getSystemService(USB_SERVICE);
        DeviceAdapter_USB adapter=new DeviceAdapter_USB(mUsbManager,this,this);
        DividerItemDecoration divider=new DividerItemDecoration(this,manager.getOrientation());
        USBRecyclerView.addItemDecoration(divider);
        //set item animation
        Animation animation = AnimationUtils.loadAnimation(MainActivity.this, R.anim.item_animate);
        LayoutAnimationController layoutAnimationController = new LayoutAnimationController(animation);
        layoutAnimationController.setOrder(LayoutAnimationController.ORDER_NORMAL);
        layoutAnimationController.setDelay(0.2f);
        USBRecyclerView.setLayoutAnimation(layoutAnimationController);
        USBRecyclerView.setAdapter(adapter);
    }

    //check permissions
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
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            permissions.add(Manifest.permission.CAMERA);
        }
        if(!permissions.isEmpty()){
            ActivityCompat.requestPermissions(this,permissions.toArray(new String[permissions.size()]),Constants.PERMISSION_REQUEST_CODE);
        }
        else{
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==Constants.PERMISSION_REQUEST_CODE){
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