package com.versuscodice.smartladderapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TimeZone;
import java.util.UUID;

import co.lujun.lmbluetoothsdk.BluetoothController;

public class PairingActivity extends AppCompatActivity {

    BluetoothAdapter mBluetoothAdapter;

    TextView txtStatus;

    PairingActivity mActivity;

    String mBluetoothName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        TextView txtSSID = (TextView) findViewById(R.id.edittxtSSID);
        txtSSID.setText(getIntent().getStringExtra("SSID"));

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mBluetoothName = mBluetoothAdapter.getName();

        mBluetoothAdapter.setName("ZistosSafeAirLadderApp");

        txtStatus = (TextView) findViewById(R.id.txtStatus);

        mActivity = this;

        Button btnPair = findViewById(R.id.btnPair);
        btnPair.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
                startActivityForResult(discoverableIntent, 1);

                TextView txtSSID = (TextView) findViewById(R.id.edittxtSSID);
                String SSID = txtSSID.getText().toString();

                TextView txtPassword = (TextView) findViewById(R.id.edittxtPassword);
                String Password = txtPassword.getText().toString();

                TextView txtName = (TextView) findViewById(R.id.edittxtName);
                String Name = txtName.getText().toString();

                Log.d("TEST", "setting: " + SSID + " " + Password + " " + Name + "\n");

                setStatusText("Searching...");

                AcceptThread acceptThread = new AcceptThread(mBluetoothAdapter, SSID, Password, Name, mActivity);
                acceptThread.start();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                int i = 0;
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.

                // Do something with the contact here (bigger example below)
            }
        }
    }



    @Override
    public void onPause() {
        super.onPause();
        //mBluetoothAdapter.setName(mBluetoothName);
    }

    public void setStatusText(String text) {
        txtStatus.setText(text);
        if(text.equals("Connected")) {
            finish();
        }
    }
}


class AcceptThread extends Thread {
    private final BluetoothServerSocket mmServerSocket;
    private BluetoothAdapter mBluetoothAdapter;
    private String SSID;
    private String Password;
    private String Name;
    private PairingActivity Activity;

    public AcceptThread(BluetoothAdapter bluetoothAdapter, String ssid, String password, String name, PairingActivity activity) {
        // Use a temporary object that is later assigned to mmServerSocket
        // because mmServerSocket is final.

        SSID = ssid;
        Password = password;
        Name = name;
        Activity = activity;

        mBluetoothAdapter = bluetoothAdapter;
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code.
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("ZistosSmartLadderApp", UUID.fromString("3a44c900-519a-11e8-b566-0800200c9a66"));
        } catch (IOException e) {
            Log.e("TEST", "Socket's listen() method failed", e);
        }
        mmServerSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned.
        while (true) {
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.e("TEST", "Socket's accept() method failed", e);
                break;
            }
            if (socket != null) {
                // A connection was accepted. Perform work associated with
                // the connection in a separate thread.
                //cancel();

                try {
                    //String message = Name + "," + SSID + "," + Password + ",";

                    ArrayMap<String, String> arrayMap = new ArrayMap<>();
                    arrayMap.put("name", Name);
                    arrayMap.put("ssid", SSID);
                    arrayMap.put("password", Password);

                    Gson gson = new Gson();
                    String json = gson.toJson(arrayMap);

                    byte [] outBuffer = json.getBytes();
                    OutputStream out = socket.getOutputStream();
                    out.write(outBuffer);
                    Log.d("TEST", "Sent: " + json);


                } catch (IOException e) {
                    Log.d("TEST", "Failed to get send bluetooth message");
                }

                Activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Activity.setStatusText("Connected");
                    }
                });

                break;
            }
        }
        cancel();
    }


    // Closes the connect socket and causes the thread to finish.
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) {
            Log.e("TEST", "Could not close the connect socket", e);
        }

    }
}