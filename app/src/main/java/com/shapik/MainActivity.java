package com.shapik;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_ENABLE_BLUETOOTH = 1001;
    public final String TAG = getClass().getSimpleName();

    private ImageButton buttonLeftLight;
    private ImageButton buttonRightLight;
    private ImageButton buttonFrontLight;
    private ImageButton buttonCrashSignal;
    private ImageButton buttonStartStop;
    private ImageButton buttonSignaling;

    private boolean isLeft = false;
    private boolean isRight = false;
    private boolean isFront = false;
    private boolean isCrash = false;
    private boolean isStartStop = false;
    private boolean isSignaling = false;

    private BluetoothAdapter bluetoothAdapter;
    private ProgressDialog progressDialog;
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private ListView listDevices;
    private DeviceListAdapter deviceListAdapter;

    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String active = intent.getAction();
            if (active.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                progressDialog = ProgressDialog.show(MainActivity.this, "Поиск устройства", "Пожалуйста подождите...");
                Log.d(TAG, "onReceive : ACTION_DISCOVERY_STARTED");
                showToastMessage("Начат поиск устройств.");

            } else if (active.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Log.d(TAG, "onReceive : ACTION_DISCOVERY_FINISHED");
                progressDialog.dismiss();
                showToastMessage("Поиск устройств завершон.");

                showListDevices();

            } else if (active.equals(BluetoothDevice.ACTION_FOUND)) {
                Log.d(TAG, "onReceive : ACTION_FOUND");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    if (!devices.contains(device)) {
                        deviceListAdapter.add(device);
                    }
                }
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "OneCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonLeftLight = findViewById(R.id.leftLight);
        buttonRightLight = findViewById(R.id.rightLight);
        buttonFrontLight = findViewById(R.id.frontLight);
        buttonCrashSignal = findViewById(R.id.crashSignal);
        buttonStartStop = findViewById(R.id.startStop);
        buttonSignaling = findViewById(R.id.signaling);

        buttonLeftLight.setOnClickListener(clickListener);
        buttonRightLight.setOnClickListener(clickListener);
        buttonFrontLight.setOnClickListener(clickListener);
        buttonCrashSignal.setOnClickListener(clickListener);
        buttonStartStop.setOnClickListener(clickListener);
        buttonSignaling.setOnClickListener(clickListener);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (buttonRightLight == null) {
            Log.d(TAG , "Устройство не потдержует блютус");
            finish();
        }

        deviceListAdapter = new DeviceListAdapter(this, R.layout.device_item, devices);
        enableBluetooth();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.item_finde:
                searchDevices();
                break;
            case R.id.item_exit:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void searchDevices() {
        Log.d(TAG, "searchDevices()");
        enableBluetooth();
        checkPermissionLocation();

        if (!bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.startDiscovery();
            Log.d(TAG,"searchDevices: start device search");
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
            bluetoothAdapter.startDiscovery();
            Log.d(TAG,"searchDevices: search was started earlier, restart");
        }

        IntentFilter filter=new IntentFilter((BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(myReceiver, filter);
    }

    private void showListDevices() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("найденые устройства");

        View view = getLayoutInflater().inflate(R.layout.list_devices_vaew, null);
        listDevices = view.findViewById(R.id.list_devices);
        listDevices.setAdapter(deviceListAdapter);
        listDevices.setOnItemClickListener(itemClickListener);


        builder.setView(view);
        builder.setNegativeButton("OK", null);
        builder.create();
        builder.show();
    }

    private void checkPermissionLocation() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int check = checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            check += checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");

            if (check != 0) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1002);
            }

        }

    }

    private void enableBluetooth() {
        Log.d(TAG, "enableBluetooth");
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableBluetooth: Bluetooth turn off, turn on ?");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQ_ENABLE_BLUETOOTH);

        }
    }

    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ENABLE_BLUETOOTH) {
            if (!bluetoothAdapter.isEnabled()) {
                Log.d(TAG, "Try again turn on Bluetooth");
                enableBluetooth();
            }
        }
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String command = "";
            if (v.equals(buttonLeftLight)) {
                isLeft = !isLeft;
                command = isLeft ? "1" : "2";
                Log.d(TAG, "Left light turn on " + isLeft);
            }

            if (v.equals(buttonRightLight)) {
                isRight = !isRight;
                command = isRight ? "3" : "4";
                Log.d(TAG, "Right light turn on " + isRight);
            }

            if (v.equals(buttonFrontLight)) {
                isFront = !isFront;
                command = isFront ? "5" : "6";;
                Log.d(TAG, "Front light turn on " + isFront);
            }
            if (v.equals(buttonCrashSignal)) {
                isCrash = !isCrash;
                command = isCrash ? "7" : "8";;
                Log.d(TAG, "Crash light turn on " + isCrash);
            }
            if (v.equals(buttonStartStop)) {
                isStartStop = !isStartStop;
                command = isStartStop ? "a" : "b";;
                Log.d(TAG, "Work motor turn on " + isStartStop);
            }
            if (v.equals(buttonSignaling)) {
                isSignaling = !isSignaling;
                command = isSignaling ? "c" : "d";;
                Log.d(TAG, "Work signaling turn on " + isSignaling);
            }
            setMessage(command);
        }
    };

    private void setMessage(String command) {
        byte[] buffer = command.getBytes();

        if (outputStream  != null) {
            try {
                outputStream.write(buffer);
                outputStream.flush();
            } catch (Exception e) {
                showToastMessage("Серег не получилось отправить команду СОРЯМБА");
                e.printStackTrace();
            }
        }
    }

    private void startConnection(BluetoothDevice device) {
        if (device != null) {
            Method method = null;
            try {
                method = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
                bluetoothSocket = (BluetoothSocket) method.invoke(device, 1);
                bluetoothSocket.connect();

                outputStream = bluetoothSocket.getOutputStream();

                showToastMessage("Подключеник успешое. Серег ты КРУТ ");
            } catch (Exception e) {
                showToastMessage("Ошибка подключения . Серег ЭЭЭЭЭЭЭЭ");
                e.printStackTrace();
            }

        }
    }

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice device = devices.get(position);

            startConnection(device);
        }
    };


}