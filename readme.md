# Bluetooth Developer guide sample

This app is oriented to Google's developer guide example code and allows to connect and chat 
with another Android device.

These are the steps for a successful communication

- step 1: check if the device has a Bluetooth sender/receiver chip
- step 2: check that Bluetooth is enabled on the device
- step 3: grant the permissions that are needed to run the app
-step 4: find devices nearby and make your device discoverable by other devices
-step 5: run the server to listen for incomming connection requests
-step 6: list found devices
-step 7: connect to a found device
-step 8: if the two devices were not coupled before ask the user to allow a connection
-step 9: when the devices are coupled and connected allow both users to send messages

The main source of this app is the developer guide found here: https://developer.android.com/guide/topics/connectivity/bluetooth

A (a little bit more complicated but better) sample app is available on Github:

https://github.com/android/connectivity-samples/tree/master/BluetoothChat

Set up Bluetooth: https://developer.android.com/guide/topics/connectivity/bluetooth/setup#java

Find Bluetooth devices: https://developer.android.com/guide/topics/connectivity/bluetooth/find-bluetooth-devices

Connect Bluetooth devices: https://developer.android.com/guide/topics/connectivity/bluetooth/connect-bluetooth-devices

Transfer Bluetooth data: https://developer.android.com/guide/topics/connectivity/bluetooth/transfer-data

Bluetooth permissions: https://developer.android.com/guide/topics/connectivity/bluetooth/permissions

Bluetooth profiles: https://developer.android.com/guide/topics/connectivity/bluetooth/profiles

Companion device pairing: https://developer.android.com/guide/topics/connectivity/companion-device-pairing

