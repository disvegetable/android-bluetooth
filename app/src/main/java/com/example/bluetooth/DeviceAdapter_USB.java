package com.example.bluetooth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DeviceAdapter_USB extends RecyclerView.Adapter<DeviceAdapter_USB.MyHolder> {

    UsbManager mUsbManager;
    List<UsbDevice> usbDeviceList=new ArrayList<>();
    MainActivity activity;
    Context context;
    LayoutInflater layoutInflater;

    public DeviceAdapter_USB(UsbManager usbManager, MainActivity activity, Context context){
        this.mUsbManager=usbManager;
        HashMap<String,UsbDevice> deviceMap=usbManager.getDeviceList();
        this.usbDeviceList.addAll(deviceMap.values());
        this.activity=activity;
        this.context=context;
        this.layoutInflater=LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view=this.layoutInflater.inflate(R.layout.usb_devict_item,parent,false);
        return new MyHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        holder.devicePath.setText("Path: "+usbDeviceList.get(position).getDeviceName());
        holder.ID.setText("Vendor ID: "+usbDeviceList.get(position).getVendorId()+" "+"Product ID: "+usbDeviceList.get(position).getProductId());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UsbDevice usbDevice=usbDeviceList.get(position);
                Intent intent=new Intent(activity,CameraActivity_USB.class);
                intent.putExtra("usbDevice",usbDevice);
                activity.USBLauncher.launch(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.usbDeviceList.size();
    }

    public static class MyHolder extends RecyclerView.ViewHolder{

        TextView devicePath;
        TextView ID;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            devicePath=itemView.findViewById(R.id.usbDevicePath);
            ID=itemView.findViewById(R.id.usbDeviceID);
        }
    }
}
