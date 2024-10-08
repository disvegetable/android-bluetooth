package com.example.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


public class DeviceAdapter_Bluetooth extends RecyclerView.Adapter<DeviceAdapter_Bluetooth.MyHolder> {

    Context context;
    BluetoothDevice[] devices;
    LayoutInflater layoutInflater;
    BluetoothDevice mBluetoothDevice;
    MainActivity activity;

    String TAG="ERROR";

    public DeviceAdapter_Bluetooth(Context context, BluetoothDevice[] devices, BluetoothAdapter adapter, MainActivity activity){
        this.context=context;
        this.devices=devices;
        this.layoutInflater=LayoutInflater.from(context);
        this.activity=activity;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view=layoutInflater.inflate(R.layout.bluetooth_device_item,parent,false);
        return new MyHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, @SuppressLint("RecyclerView") int position) {
        try{
            holder.nameText.setText("Name: "+devices[position].getName());
            holder.macText.setText("MAC: "+devices[position].getAddress());
        }catch (SecurityException s){
            s.printStackTrace();
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //start camera activity
                mBluetoothDevice=devices[position];
                Intent intent=new Intent(activity, CameraActivity_Bluetooth.class);
                intent.putExtra("device",mBluetoothDevice);
                activity.BluetoothLauncher.launch(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.length;
    }

    public static class MyHolder extends RecyclerView.ViewHolder{

        TextView nameText;
        TextView macText;

        public MyHolder(View view){
            super(view);
            nameText=view.findViewById(R.id.deviceName);
            macText=view.findViewById(R.id.macAddress);
        }
    }
}
