package de.androidcrypto.bluetoothdeveloperguidesampleorg;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;

    AcceptThread mAcceptThread;
    ConnectThread mConnectThread;
    ConnectedThread mConnectedThread;

    Button discoverable, scan, list, sendText;
    TextView textViewLog, textViewChat;
    EditText textToSend;
    TextInputLayout textToSendDecoration;

    /**
     * Return Intent extra
     */
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    String macAddressFromScan = ""; // will get filled by Intent from DeviceListOwnActivity

    private static final int REQUEST_ENABLE_BT = 201;
    private static final int REQUEST_DISCOVERABLE_BT = 202;
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 203;
    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("2f5624ab-a8e8-4dd3-801a-262ad46dcee7");
    // this is UUID from the sample app, not used here
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    /**
     * This block is for requesting permissions up to Android 12+
     *
     */

    private static final int PERMISSIONS_REQUEST_CODE = 191;
    private static final String[] BLE_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };
    @SuppressLint("InlinedApi")
    private static final String[] ANDROID_12_BLE_PERMISSIONS = new String[]{
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public static void requestBlePermissions(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ActivityCompat.requestPermissions(activity, ANDROID_12_BLE_PERMISSIONS, requestCode);
        else
            ActivityCompat.requestPermissions(activity, BLE_PERMISSIONS, requestCode);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // inflate option menu
        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        discoverable = findViewById(R.id.btnMainDiscoverable);
        scan = findViewById(R.id.btnMainScan);
        list = findViewById(R.id.btnMainList);
        textViewLog = findViewById(R.id.tvMainLog);
        textViewChat = findViewById(R.id.tvMainChat);
        textToSend = findViewById(R.id.etMainTextToSend);
        textToSendDecoration = findViewById(R.id.etMainTextToSendDecoration);
        sendText = findViewById(R.id.btnMainSendText);

        // receive the address from DeviceListOwnActivity, if we receive data run the connection part
        Intent incommingIntent = getIntent();
        Bundle extras = incommingIntent.getExtras();
        if (extras != null) {
            macAddressFromScan = extras.getString(EXTRA_DEVICE_ADDRESS); // retrieve the data using keyName
            System.out.println("Main received data: " + macAddressFromScan);
            try {
                if (!macAddressFromScan.equals("")) {
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    System.out.println("MAC address received: " + macAddressFromScan + " ... try to connect with...");
                    appendLog("MAC address received: " + macAddressFromScan + " ... try to connect with...");
                    // Get the BluetoothDevice object
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddressFromScan);
                    ConnectThread connect = new ConnectThread(device, MY_UUID);
                    connect.start();
                }
            } catch (NullPointerException e) {
                // do nothing, there are just no data
            }
        }



        // step 1 check if the device has a Bluetooth sender/receiver chip
        if (isBluetoothAvailableOnDevice()) {
            appendLog("Bluetooth is available on the device");
        } else {
            appendLog("Bluetooth is NOT available on the device");
            showQuitDialog("As Bluetooth is not available on your device this app is quitting now");
        }

        // step 2 grant the permissions that are needed to run the app
        appendLog("checking if the app has all permissions granted");
        requestBlePermissions(this, PERMISSIONS_REQUEST_CODE);

        // step 3: check that Bluetooth is enabled on the device
        if (!bluetoothAdapter.isEnabled()) {
            appendLog("Bluetooth is not enabled on the device, try to enable it");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            appendLog("Bluetooth is enabled on the device");
        }

        // now the app is ready to run, the button startAppActivity is enabled
        discoverable.setVisibility(View.VISIBLE);
        discoverable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // now it is time for step 4 and start the (listening) server
                startServer();

                // first make the device visible to other devices
                Intent discoverableIntent =
                        new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE_BT);

                // step 5 is located in onActivityResult so commented out here
                /*
                // step 5 find devices nearby and make your device discoverable by other devices
                // Launch the DeviceListActivity to see devices and do scan
                appendLog("check for already paired and new devices");
                Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                */

            }
        });

        scan.setVisibility(View.VISIBLE);
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // now it is time for step 4 and start the (listening) server
                startServer();
                // if the user does not make the devices discoverable then he can connect to
                // previously connected devices only

                // step 5 find devices nearby and make your device discoverable by other devices
                // Launch the DeviceListActivity to see devices and do scan
                appendLog("check for already paired and new devices");
                Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            }
        });

        list.setVisibility(View.VISIBLE);
        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MainActivity.this, DeviceListOwnActivity.class);
                startActivity(intent);
            }
        });

        // this is the separate send button
        sendText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        // this is the send button in keyboard
        textToSend.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    handled = true;
                }
                return handled;
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    Bundle extras = data.getExtras();
                    if (extras == null) {
                        return;
                    }
                    pairDevice(data);
                }
                break;
            case REQUEST_DISCOVERABLE_BT:
                // When the request to discoverable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // device is ready to get found by other devices
                    appendLog("Bluetooth is discoverable by other devices");
                } else {
                    // User did not enable Bluetooth or an error occurred
                    appendLog("Bluetooth is NOT discoverable by other devices");
                }

                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    appendLog("Bluetooth was enabled successfully");
                } else {
                    // User did not enable Bluetooth or an error occurred
                    showQuitDialog("As you did not enable Bluetooth this app is quitting now");
                }
        }
    }

    private boolean isBluetoothAvailableOnDevice() {
        // minimum Android 23
        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            return false;
        } else {
            return true;
        }
    }

    private void showQuitDialog(String message) {
        AlertDialog mBuilder = new AlertDialog.Builder(this)
                .setTitle("Confirm")
                .setMessage(message)
                .setPositiveButton("Yes", null)
                //.setNegativeButton("No", null)
                .show();
        // Function for the positive button
        // is programmed to exit the application
        Button mPositiveButton = mBuilder.getButton(AlertDialog.BUTTON_POSITIVE);
        mPositiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishAffinity();
                System.exit(0);
            }
        });
    }

    private void enableChatInput() {
        runOnUiThread(() -> {
            textViewChat.setVisibility(View.VISIBLE);
            sendText.setVisibility(View.VISIBLE);
            textToSendDecoration.setVisibility(View.VISIBLE);
            // this is cancelling the DeviceList activity
            //startActivity(new Intent(MainActivity.this, DeviceListActivity.class));
        });
    }

    private void disableChatInput() {
        runOnUiThread(() -> {
            textViewChat.setVisibility(View.GONE);
            sendText.setVisibility(View.GONE);
            textToSendDecoration.setVisibility(View.GONE);
            // this is cancelling the DeviceList activity
            //startActivity(new Intent(MainActivity.this, DeviceListActivity.class));
        });
    }

    private void appendLog(String message) {
        runOnUiThread(() -> {
            String newMessages = textViewLog.getText().toString() + "\n" + message;
            textViewLog.setText(newMessages);
        });
    }

    private void appendChat(String message) {
        runOnUiThread(() -> {
            String newMessages = textViewChat.getText().toString() + "\n" + message;
            textViewChat.setText(newMessages);
        });
    }

    /**
     * we are pairing and connecting the two devices
     */

    private void pairDevice(Intent data) {
        appendLog("starting pairDevice");
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Bundle extras = data.getExtras();
        if (extras == null) {
            return;
        }
        String address = extras.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        appendLog("MAC address received: " + address + ", trying to connect...");
        // Get the BluetoothDevice object
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        mConnectThread = new ConnectThread(device, MY_UUID);
        mConnectThread.start();
    }

    /**
     * The ConnectThread is actively starting a connection to a running server on the paired device
     * If the device does not have a server running OR the UUID is not matching there will be no
     * successful connection
     */

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;
        private UUID deviceUUID;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            appendLog("ConnectThread: started.");
            //Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        @SuppressLint("MissingPermission")
        public void run(){
            BluetoothSocket tmp = null;
            appendLog("RUN mConnectThread ");
            //Log.i(TAG, "RUN mConnectThread ");
            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                appendLog("ConnectThread: Trying to create SecureRfcommSocket using UUID: " + MY_UUID);
                //Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: " +MY_UUID_INSECURE );
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                appendLog("ConnectThread: Could not create SecureRfcommSocket " + e.getMessage());
                //Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
                return;
            }

            mmSocket = tmp;
            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                    appendLog("ConnectThread run: Closed Socket.");
                    //Log.d(TAG, "run: Closed Socket.");
                } catch (IOException e1) {
                    appendLog("mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                    //Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }
                appendLog("ConnectThread run: Could not connect to UUID: " + MY_UUID);
                //Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE );
                return;
            }
            // here we are ready to chat
            connected(mmSocket);
        }
        public void cancel() {
            try {
                appendLog("ConnectThread cancel: Closing Client Socket.");
                //Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e) {
                appendLog("ConnectThread cancel: close() of mmSocket in Connectthread failed. " + e.getMessage());
                //Log.e(TAG, "cancel: close() of mmSocket in Connectthread failed. " + e.getMessage());
            }
        }
    }

    /**
     * The AcceptThread is the server that needs to run and listen for any incomming connection
     * requests from another device ("client"). If the UUID is not matching there will be no
     * successful connection. The server needs to be up when Bluetooth is enabled and the
     * permissions are granted.
     */

    public void startServer() {
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
    }

    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        @SuppressLint("MissingPermission")
        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                //tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("appname", MY_UUID_INSECURE);
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("appname", MY_UUID);
                appendLog("\"AcceptThread: Setting up Server using: " + MY_UUID);
                //Log.d(TAG, "AcceptThread: Setting up Server using: " + MY_UUID_INSECURE);
            } catch (IOException e) {
                appendLog("AcceptThread: IOException: " + e.getMessage());
                //Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }
            mmServerSocket = tmp;
        }

        public void run() {
            appendLog("AcceptThread run: Running.");
            //Log.d(TAG, "run: AcceptThread Running.");

            BluetoothSocket socket = null;

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                appendLog("AcceptThread run: RFCOM server socket start.....");
                //Log.d(TAG, "run: RFCOM server socket start.....");

                socket = mmServerSocket.accept();
                appendLog("AcceptThread run: RFCOM server socket accepted connection.");
                //Log.d(TAG, "run: RFCOM server socket accepted connection.");

            } catch (IOException e) {
                appendLog("AcceptThread: IOException: " + e.getMessage());
                //Log.e(TAG, "AcceptThread: IOException: " + e.getMessage());
            }

            //talk about this is in the 3rd
            if (socket != null) {
                connected(socket);
            }
            appendLog("AcceptThread END");
            //Log.i(TAG, "END mAcceptThread ");
        }

        public void cancel() {
            appendLog("AcceptThread: cancel - Canceling");
            //Log.d(TAG, "cancel: Canceling AcceptThread.");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                appendLog("AcceptThread cancel: Close of ServerSocket failed: " + e.getMessage());
                //Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage());
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void connected(BluetoothSocket mmSocket) {
        appendLog("devices are connected, starting the chat");
        String chatName = mmSocket.getRemoteDevice().getName();
        appendLog("connected to " + chatName);
        appendChat("connected to " + chatName);
        enableChatInput();
        //Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    /**
     * The ConnectedThread takes the socket after connection eather from AcceptThread or
     * ConnectTread as is doing all of the communication
     */

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            appendLog("ConnectedThread: Starting.");
            //Log.d(TAG, "ConnectedThread: Starting.");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];  // buffer store for the stream

            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    appendLog("ConnectedThread InputStream: " + incomingMessage);
                    //Log.d(TAG, "InputStream: " + incomingMessage);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            appendChat("3rd: " + incomingMessage);
                            //view_data.setText(incomingMessage);
                        }
                    });
                } catch (IOException e) {
                    appendLog("ConnectedThread write: Error reading Input Stream. " + e.getMessage());
                    //Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage() );
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            appendLog("ConnectedThread write Writing to outputstream : " + text);
            //Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                appendLog("ConnectedThread write: Error writing to output stream. " + e.getMessage());
                //Log.e(TAG, "write: Error writing to output stream. " + e.getMessage() );
            }
        }

        // Call this from the main activity to shutdown the connection
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    public void sendMessage() {
        String message = textToSend.getText().toString();
        byte[] bytes = message.getBytes(Charset.defaultCharset());
        mConnectedThread.write(bytes);
        appendChat(("own: " + message));
        textToSend.setText("");
    }

    /**
     * tasks for the menu
     * @param menu
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);

        /**
         * resets the connection, log and chat
         */
        MenuItem mResetApp = menu.findItem(R.id.action_reset);
        mResetApp.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //Intent i = new Intent(MainActivity.this, AddEntryActivity.class);
                //startActivity(i);
                //exportDumpMail();

                textViewLog.setText("app reset");
                textViewChat.setText("");
                disableChatInput();
                if (mConnectThread != null) mConnectThread.cancel();
                if (mAcceptThread != null) mAcceptThread.cancel();
                startServer();
                return false;
            }
        });

        MenuItem mExportFile = menu.findItem(R.id.action_export_file);
        mExportFile.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //Intent i = new Intent(MainActivity.this, AddEntryActivity.class);
                //startActivity(i);
                //exportDumpFile();
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

}