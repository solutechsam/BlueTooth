package com.example.samop.dnasensorapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    private static final String TAG = "MainActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter mBluetoothAdapter;
    ArrayList<BluetoothDevice> devicelist = new ArrayList<>();
    ArrayAdapter adapter;
    Button btnStartConnection;
    Button btnGetResults;
    TextView dataview;


    BluetoothConnectionService mBluetoothConnection;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("677d1698-c360-4cc9-8509-d3c7f8464ccf");
    BluetoothDevice mBTDevice;

    ListView devicelistview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        devicelist = new ArrayList<>();
        devicelistview = (ListView) findViewById(R.id.devicelistview);
        Button finddevicesButton = (Button) findViewById(R.id.finddevicesbutton);
        finddevicesButton.setOnClickListener(new View.OnClickListener(){
           @Override
            public void onClick(View view){
            enableBluetooth();
               Log.d(TAG, "enabled BT");
            discoverdevices();
               Log.d(TAG, "Discovered All Devices");

           }
        });

        btnStartConnection = (Button) findViewById(R.id.Connectbutton);
        btnStartConnection.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                startConnection();

            }
        });

        btnGetResults = (Button) findViewById(R.id.Getresultsbutton);
        btnGetResults.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                byte[]bytes = ("sendr").getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(bytes);
            }
        });

        dataview = (TextView) findViewById(R.id.resultstextView);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this,"Bluetooth not supported on this device. Application will not work.", Toast.LENGTH_LONG).show();
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(pReceiver, filter);
        devicelistview.setOnItemClickListener(MainActivity.this);

    }


    public void startConnection(){
        startBTConnection(mBTDevice, MY_UUID_INSECURE);
    }
    public void startBTConnection(BluetoothDevice device , UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing Bluetooth Connection.");
        mBluetoothConnection.startClient(device, uuid);


    }
    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                devicelist.add(device);
                Log.d(TAG, "found device:" + deviceName);
                ArrayAdapter adapter = new ArrayAdapter(context, android.R.layout.simple_list_item_1, devicelist);
                devicelistview.setAdapter(adapter);

                //adapter.notifyDataSetChanged();
            }
        }
    };
    private final BroadcastReceiver pReceiver = new BroadcastReceiver(){
      @Override
        public void onReceive(Context context, Intent intent){
          final String action = intent.getAction();

          if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
              BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
              //3 cases

              //case1: already bound
              if(   mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BONDED");
                    mBTDevice = mDevice;
              }
              if( mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                  Log.d(TAG, "BONDING");
              }
              if(mDevice.getBondState() == BluetoothDevice.BOND_NONE){
                  Log.d(TAG, "BOND_NONE");
              }
          }

      }
    };

    public void enableBluetooth(){
        // Register for broadcasts when a device is discovered.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

    }

    public void discoverdevices(){
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "Cancel Discovery");
            Toast.makeText(this,"stopping discovery.",Toast.LENGTH_SHORT).show();

            checkBTpermissions();

            mBluetoothAdapter.startDiscovery();
            Log.d(TAG, "Start Discovery");
            Toast.makeText(this,"starting discovery.",Toast.LENGTH_SHORT).show();
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);

        }
        else{
            checkBTpermissions();
            mBluetoothAdapter.startDiscovery();
            Toast.makeText(this,"starting discovery.",Toast.LENGTH_SHORT).show();
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);
        }
    }

    private void checkBTpermissions(){

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissioncheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissioncheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if(permissioncheck != 0){
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);

            }
        }else{
            Log.d(TAG, "CheckBTPermissions: no need to check permissions. SDK < LOLLIPOP");
        }
    }
    @Override
    protected void onDestroy() {
        Log.d(TAG, "DESTROY CALLED");
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
        unregisterReceiver(pReceiver);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "CLICKED ON DEVICE");
        String deviceName = devicelist.get(position).getName();
        String deviceAddress = devicelist.get(position).getAddress();

        //create bond
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "trying to pair with" + deviceName);
            devicelist.get(position).createBond();

            mBTDevice = devicelist.get(position);
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
        }


    }
}
