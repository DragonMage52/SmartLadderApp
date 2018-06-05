package com.versuscodice.smartladderapp;

import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.ArrayMap;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;

import com.google.gson.Gson;
import com.rafakob.nsdhelper.NsdHelper;
import com.rafakob.nsdhelper.NsdListener;
import com.rafakob.nsdhelper.NsdService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String SERVICE_NAME = "Smart Ladder";
    private String SERVICE_TYPE = "_http._udp";
    private NsdManager mNsdManager;
    private ClientListen udpConnect;
    private List<Meter> meters = new ArrayList<Meter>();
    private MeterAdapter meterAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GridView gridview = (GridView) findViewById(R.id.gridView);
        meterAdapter = new MeterAdapter(this, meters);
        gridview.setAdapter(meterAdapter);

        Meter.setContext(this, meterAdapter);

        mNsdManager = (NsdManager) getApplicationContext().getSystemService(Context.NSD_SERVICE);

        udpConnect = new ClientListen();
        udpConnect.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        //mNsdManager.unregisterService(mRegistrationListener);
        udpConnect.close();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        try {
            mNsdManager.unregisterService(mRegistrationListener);
            udpConnect.close();
        } catch (Exception e) {
            Log.d("TEST", "Failed on Pause");
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (udpConnect != null) {
            if (!udpConnect.isAlive()) {
                udpConnect = new ClientListen();
                udpConnect.start();
            }
        }
    }

    public void registerService(int port) {
        Log.d("TEST", "udp port: " + port);
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName("Smart Ladder");
        serviceInfo.setServiceType("_zvs._udp.");
        serviceInfo.setPort(port);

        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    private NsdManager.RegistrationListener mRegistrationListener = new NsdManager.RegistrationListener() {
        @Override
        public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {

        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {

        }

        @Override
        public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
            //String mServiceName = NsdServiceInfo.getServiceName();
            Log.d("TEST", "Service Registered");
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
            Log.d("TEST", "Service unRegistered");
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.actParing:
                Intent intent = new Intent(this, PairingActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class ClientListen extends Thread {
        private boolean run = true;

        @Override
        public void run() {
            try {
                run = true;
                DatagramSocket udpSocket = new DatagramSocket();

                byte[] message = new byte[8000];
                DatagramPacket packet = new DatagramPacket(message, message.length);
                registerService(udpSocket.getLocalPort());
                while (run) {
                    udpSocket.receive(packet);
                    String text = new String(message, 0, packet.getLength());
                    Log.d("Received data", text);
                    Gson gson = new Gson();
                    ArrayMap<String, String> arrayMap = gson.fromJson(text, ArrayMap.class);

                    boolean found = false;

                    for (Meter testMeter : meters) {
                        if (testMeter.id.equals(arrayMap.get("id"))) {
                            testMeter.update(arrayMap);
                            found = true;
                        }
                    }

                    if (found == false) {
                        meters.add(new Meter(arrayMap));
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            meterAdapter.notifyDataSetChanged();
                        }
                    });
                }
                udpSocket.close();
            } catch (IOException e) {
                Log.e("TEST", "error: ", e);
                run = false;
            }

        }

        public void close() {
            run = false;
        }
    }
}
