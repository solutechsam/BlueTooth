package com.example.samop.dnasensorapp;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/**
 * Created by samop on 8/4/2017.
 */

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";

    private static final String appName ="MYAPP";

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("677d1698-c360-4cc9-8509-d3c7f8464ccf");

    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mprogressDialog;

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private ConnectedThread mConnectedThread;


    public BluetoothConnectionService( Context context) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mContext = context;
        start();
    }


    private class ConnectThread extends Thread{
    private BluetoothSocket mmSocket;
        public ConnectThread(BluetoothDevice device, UUID uuid){
            Log.d(TAG,"ConnectThread: started");
            mmDevice = device;
            deviceUUID = uuid;

        }
        public void run(){
            BluetoothSocket tmp = null;
            Log.i(TAG,"RUN mConnectThread");


            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID");
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            }catch( IOException e ){
                e.printStackTrace();
                Log.d(TAG, "ConnectThread: Fauiled to create InsectureRfcommSocket using UUID" + e.getMessage());
            }
            mmSocket = tmp;

            mBluetoothAdapter.cancelDiscovery();
            try{
                mmSocket.connect();
                Log.d(TAG, "RUN: Connection Successful");
            }catch(IOException e){
                try{
                    mmSocket.close();
                    Log.d(TAG, "RUN: closed socket ");
                }
                catch(IOException el){
                    Log.d(TAG, "RUN: unable to close socket" + el.getMessage());
                }
                Log.d(TAG, "RUN: could not connect to  UIUD" + MY_UUID_INSECURE);
            }

            connected(mmSocket, mmDevice);

        }

        public void cancel(){
            try{
                Log.d(TAG, "Cancel: closing socket.");
                mmSocket.close();

            }catch(IOException e){
                Log.d(TAG, "Cancel: Failed closing socket." + e.getMessage());
            }
        }


        public synchronized void start(){
            Log.d(TAG, "START");

            if(mConnectThread != null){
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        public void startClient(BluetoothDevice device, UUID uuid){
            Log.d(TAG, "startClient: started");

            //initprogressdialog

            mprogressDialog = ProgressDialog.show(mContext, " Connecting Bluetooth" , "Please Wait...", true);
            mConnectThread = new ConnectThread(device, uuid);
            mConnectThread.start();

        }

    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            Log.d(TAG, "ConnectedThread: starting");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;


            mprogressDialog.dismiss();

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            }catch(IOException e){
                Log.d(TAG, "ConnectThread: failed to setup input/output streams " + e.getMessage());
            }


            mmInStream = tmpIn;
            mmOutStream = tmpOut;

        }

        public void run(){
            byte[] buffer = new byte[1024]; //buffer store for the stream

            int bytes; // bytes returned from read()

            while(true){
                //read from inputstream
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingmessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingmessage);
                }catch(IOException e){
                    Log.d(TAG, "INPUTSTREAM ERROR: "+ e.getMessage());
                    break;
                }
            }
        }

        public void cancel(){
            try{
                mmSocket.close();
            }catch(IOException e){
                Log.d(TAG, "ConnectedTread Failed to close socket: " + e.getMessage());
            }
        }

        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "writing to outputstream: "+ text);

            try{
                mmOutStream.write(bytes);
            }catch(IOException e ){
                Log.d(TAG, "OUTPUTSTREAM ERROR:"+ e.getMessage());

            }
        }
    }
    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice){
        Log.d(TAG, "connected: starting");

        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectThread.start();

    }

    public void write(byte[] out){
        ConnectedThread r;

        Log.d(TAG,"write: Write Called");

        mConnectedThread.write(out);



    }
}
