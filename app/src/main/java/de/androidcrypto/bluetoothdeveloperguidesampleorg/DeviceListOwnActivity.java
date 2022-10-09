package de.androidcrypto.bluetoothdeveloperguidesampleorg;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class DeviceListOwnActivity extends AppCompatActivity {

    ListView listView;
    Button scan;
    ProgressBar progressBar;

    ArrayAdapter<String> scannedDevicesArrayAdapter;
    private BluetoothAdapter mBtAdapter;

    /**
     * Return Intent extra
     */
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list_own);
        // inflate option menu
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        progressBar = findViewById(R.id.pbList);
        listView = findViewById(R.id.lvListListView);
        scan = findViewById(R.id.btnListScan);

        // populate the data
        scannedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        listView.setAdapter(scannedDevicesArrayAdapter);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("scan clicked");
                progressBar.setIndeterminate(false);
                progressBar.setVisibility(View.VISIBLE);
                doDiscovery();
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                progressBar.setIndeterminate(true);
                progressBar.setVisibility(View.GONE);
                System.out.println("This item was clicked: " + i);
                // Cancel discovery because it's costly and we're about to connect
                mBtAdapter.cancelDiscovery();
                // Get the device MAC address, which is the last 17 chars in the View
                String info = ((TextView) view).getText().toString();
                String address = info.substring(info.length() - 17);
                // check for ther text "scanning complete"
                if (address.equalsIgnoreCase("scanning complete")) {
                    System.out.println("do not use this data");
                    address = "";
                }
                System.out.println("*** MAC: " + address);
                // Create the Intent and include the MAC address
                Intent intent = new Intent(DeviceListOwnActivity.this, MainActivity.class);
                intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
                startActivity(intent);
                finish();
            }
        });
    }

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                System.out.println("device found");
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                if (device != null) {
                    scannedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                System.out.println("scanning finished");
                progressBar.setIndeterminate(true);
                progressBar.setVisibility(View.GONE);
                scannedDevicesArrayAdapter.add("scanning complete");
                /*
                if (scannedDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = "no Bluetooth devices found";
                    scannedDevicesArrayAdapter.add(noDevices);
                } else {
                    scannedDevicesArrayAdapter.add("scanning complete");
                }

                 */
            }
        }
    };

    /**
     * Start device discover with the BluetoothAdapter
     */
    @SuppressLint("MissingPermission")
    private void doDiscovery() {
        //Log.d(TAG, "doDiscovery()");

        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBtAdapter != null) {
            // Make sure we're not doing discovery anymore
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        try {
            this.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            // the receiver is not registered any more
        }
    }
}