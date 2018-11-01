package com.versuscodice.smartladderapp;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.SubMenu;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.rafakob.nsdhelper.NsdHelper;
import com.rafakob.nsdhelper.NsdListener;
import com.rafakob.nsdhelper.NsdService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity  {

    private String SERVICE_NAME = "Smart Ladder";
    private String SERVICE_TYPE = "_http._udp";
    private NsdManager mNsdManager;
    private ClientListen udpConnect;
    public List<Meter> meters;

    public List<Meter> backgroundMeters = new ArrayList<>();
    private MeterAdapter meterAdapter;
    public TextView txtAlarms;
    public GridView gridview;
    public Handler mMulticastSendHandler = new Handler();
    public DatagramSocket udpSocket;
    public int mListenPort;
    public boolean mBackground = true;
    public GridViewModel mModel;

    SharedPreferences mPrefs;

    public int mExternalStoragePermmisions = 0;

    String mSSID;
    boolean connectedToWifi = false;

    int mCalibrationReminder;

    int mAlarmSetting;

    public ServerSocket mServerSocket = null;
    public SocketListenThread mServerSocketThread = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mSSID = sharedPref.getString("perf_appWifiSSID", "");
        mAlarmSetting = Integer.parseInt(sharedPref.getString("pref_appAlarm", "0"));
        mCalibrationReminder = NumberPickerPreference.value;

        mModel = ViewModelProviders.of(this).get(GridViewModel.class);
        meters = mModel.getMeterList();

        String metersID[] = new String[24];

        Gson gson = new Gson();
        mPrefs = getPreferences(MODE_PRIVATE);
        String storedIDs = mPrefs.getString("IDs", "");

        if(meters.size() < 1) {
            metersID = gson.fromJson(storedIDs, metersID.getClass());

            if (metersID != null) {
                for (int i = 0; i < 24 | i < metersID.length; i++) {
                    if (i < metersID.length && metersID[i] != null) {
                        meters.add(new Meter(metersID[i]));
                    } else {
                        meters.add(new Meter());
                    }
                }
            } else {
                for (int i = 0; i < 24; i++) {
                    meters.add(new Meter());
                }
            }
        }

        gridview = (GridView) findViewById(R.id.gridView);
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            gridview.setNumColumns(4);
        } else {
            gridview.setNumColumns(2);
        }
        ImageButton btnSilence = (ImageButton) findViewById(R.id.btnSilence);
        meterAdapter = new MeterAdapter(this, meters, btnSilence);
        gridview.setAdapter(meterAdapter);

        Meter.setContext(this, meterAdapter);

        txtAlarms = (TextView) findViewById(R.id.txtAlarms);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {

                final int finalSelectedMeterPosition = i;

                PopupMenu popup = new PopupMenu(getApplicationContext(), view);
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menu_meter, popup.getMenu());

                if (meters.get(i).mActive) {
                    popup.getMenu().add(Menu.NONE, 1, Menu.NONE, "Get Log");
                    popup.getMenu().add(Menu.NONE, 2, Menu.NONE, "Reset Insertion Count");
                    popup.getMenu().add(Menu.NONE, 3, Menu.NONE, "Clear Log");
                }

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {

                        DatagramPacket packet;
                        SendThread sendThread;

                        switch (menuItem.getItemId()) {
                            case R.id.itemChooseMeter:

                                SubMenu meterMenu = menuItem.getSubMenu();

                                for (Meter testMeter : backgroundMeters) {
                                    meterMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, testMeter.id);
                                }
                                return true;

                            case R.id.itemNone:
                                Meter meterTest = meters.get(finalSelectedMeterPosition);
                                if (meterTest.id != null) {
                                    backgroundMeters.add(meters.get(finalSelectedMeterPosition));
                                }
                                meters.set(finalSelectedMeterPosition, new Meter());
                                meterAdapter.notifyDataSetChanged();
                                return true;

                            case 3:
                                meters.get(i).sendData("Clear");
                                break;

                            case 2:
                                /*packet = new DatagramPacket("Insertion".getBytes(), "Insertion".length());
                                packet.setAddress(meters.get(i).mIpAddress);
                                packet.setPort(meters.get(i).mPort);
                                sendThread = new SendThread(packet);
                                sendThread.start();*/

                                meters.get(i).sendData("Insertion");

                                break;

                            case 1:
                                /*packet = new DatagramPacket("Log".getBytes(), "Log".length());
                                packet.setAddress(meters.get(i).mIpAddress);
                                packet.setPort(meters.get(i).mPort);
                                sendThread = new SendThread(packet);
                                sendThread.start();*/

                                meters.get(i).sendData("Log");

                                break;

                            case 0:
                                for (int i = 0; i < backgroundMeters.size(); i++) {
                                    if (menuItem.getTitle().equals(backgroundMeters.get(i).id)) {
                                        Meter meterTemp = meters.get(finalSelectedMeterPosition);
                                        Meter backgroundMeterTemp = backgroundMeters.get(i);

                                        meters.set(finalSelectedMeterPosition, backgroundMeterTemp);
                                        if (meterTemp.id != null) {
                                            backgroundMeters.set(i, meterTemp);
                                        } else {
                                            backgroundMeters.remove(i);
                                        }
                                        meterAdapter.notifyDataSetChanged();
                                        return true;
                                    }
                                }
                                break;
                        }
                        return false;
                    }
                });

                popup.show();
            }
        });

        //mNsdManager = (NsdManager) getApplicationContext().getSystemService(Context.NSD_SERVICE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        registerReceiver(wifiReceiver, intentFilter);

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            checkWifiState();
        }

        mMulticastSendHandler.post(multicastSendRunnable);
        /*udpConnect = new ClientListen();
        udpConnect.start();*/

        mServerSocketThread = new SocketListenThread();
        mServerSocketThread.start();;

    }

    public Runnable multicastSendRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                byte[] message = (ip + "," + mListenPort).getBytes();
                InetAddress group = InetAddress.getByName("239.52.8.234");
                DatagramPacket packet = new DatagramPacket(message, message.length, group, 52867);
                SendThread sendThread = new SendThread(packet);
                sendThread.start();
                if(mBackground) {
                    mMulticastSendHandler.postDelayed(multicastSendRunnable, 10000);
                }

            } catch (IOException e) {
                Log.e("multicastSend", "Failed to send");
            }
        }
    };

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action.equals("android.net.wifi.STATE_CHANGE")) {
                checkWifiState();
            }
        }
    };

    public void checkWifiState() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService (Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo ();
        String ssid = info.getSSID().replace("\"", "");
        Log.d("TEST", "SSID = " + ssid);
        if (ssid.equals(mSSID)) {
            connectedToWifi = true;
            meterAdapter.refresh();
        }
        else {
            connectedToWifi = false;
            txtAlarms.setText("ALARM-NETWORK-ERROR");
            txtAlarms.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    public void displayLog(ArrayMap<String, String> arrayMap) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
        String date = dateFormat.format(Calendar.getInstance().getTime());


        //final String fileName = date +  arrayMap.get("id") + " events.log";
        final String fileName = "events.log";

        //File logDir = getApplicationContext().getDir("logs", Context.MODE_PRIVATE);
        File logDir = Environment.getExternalStorageDirectory();
        final File logFile = new File(logDir, fileName);
        Log.d("TEST", "logFile path: " + logFile.getAbsolutePath());

        if(!isStoragePermissionGranted()) {
            while(mExternalStoragePermmisions == 0);
        }

        if(mExternalStoragePermmisions == 1) {
            try {
                FileOutputStream outputStream = new FileOutputStream(logFile);
                outputStream.write(arrayMap.get("log").getBytes());
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        final ArrayMap<String, String> finalArrayMap = arrayMap;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View logView = layoutInflater.inflate(R.layout.popup_log, null);

                TextView txtLog = logView.findViewById(R.id.txtLog);
                String test = finalArrayMap.get("log");
                txtLog.append(finalArrayMap.get("log"));
                DisplayMetrics displayMetrics = new DisplayMetrics();
                WindowManager windowmanager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
                windowmanager.getDefaultDisplay().getMetrics(displayMetrics);

                final PopupWindow popupWindow = new PopupWindow(logView, displayMetrics.widthPixels - 60, displayMetrics.heightPixels - 60, true);
                popupWindow.showAtLocation(gridview, Gravity.CENTER, 0, 0);

                Button btnClose = logView.findViewById(R.id.btnClosePopup);
                btnClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });

                Button btnEmail = logView.findViewById(R.id.btnEmail);
                btnEmail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mExternalStoragePermmisions == 1) {
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("*/*");
                            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getApplicationContext(), "com.versuscodice.provider", logFile));
                            if (intent.resolveActivity(getPackageManager()) != null) {
                                startActivity(intent);
                            }
                        }
                    }
                });
            }
        });
    }

    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d("TEST","Permission is granted");
                mExternalStoragePermmisions = 1;
                return true;
            } else {

                Log.d("TEST","Permission is revoked");
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                mExternalStoragePermmisions = 0;
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.d("TEST","Permission is granted");
            mExternalStoragePermmisions = 1;
            return true;
        }
    }

    @Override
    protected void onDestroy() {
        //mNsdManager.unregisterService(mRegistrationListener);
        try {
            mServerSocketThread.close();
        } catch (Exception e) {
            Log.d("onDestroy", "Failed to close ServerSocket");
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {

        try {
            mServerSocketThread.close();
            mBackground = false;
        } catch (Exception e) {
            Log.d("TEST", "Failed on Pause");
        }

        mMulticastSendHandler.removeCallbacks(multicastSendRunnable);

        String metersID[] = new String[24];
        int count = 0;

        for (Meter testMeter : meters) {
            if (testMeter.id != null) {
                metersID[count] = testMeter.id;
            }
            count++;

            if(testMeter.mSocket != null) {
                //testMeter.mManageThread.close();
                testMeter.sendData("close");
            }
        }

        for (Meter testMeter : backgroundMeters) {
            if (testMeter.mSocket != null) {
                //testMeter.mManageThread.close();
                testMeter.sendData("close");
            }
        }

        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(metersID);
        prefsEditor.putString("IDs", json);
        prefsEditor.commit();

        unregisterReceiver(wifiReceiver);

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mServerSocketThread != null) {
            if (!mServerSocketThread.isAlive()) {
                mServerSocketThread = new SocketListenThread();
                mServerSocketThread.start();
            }
        }

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            mSSID = sharedPref.getString("perf_appWifiSSID", "");
            mAlarmSetting = Integer.parseInt(sharedPref.getString("perf_appAlarm", "0"));
            mCalibrationReminder = NumberPickerPreference.value;

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.net.wifi.STATE_CHANGE");
            registerReceiver(wifiReceiver, intentFilter);
            checkWifiState();

        mBackground = true;
        mMulticastSendHandler.post(multicastSendRunnable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            Log.d("TEST","Permission: "+permissions[0]+ "was "+grantResults[0]);
            //resume tasks needing this permission
            mExternalStoragePermmisions = 1;
        }
        else {
            mExternalStoragePermmisions = -1;
        }
    }

    public void registerService(int port) {
        Log.d("TEST", "udp port: " + port);
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName("Smart Ladder");
        serviceInfo.setServiceType("_zvs._udp.");
        serviceInfo.setPort(port);

        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        //mNsdManager.unregisterService(mRegistrationListener);
        //mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
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
                Intent pairingIntent = new Intent(this, PairingActivity.class);
                startActivity(pairingIntent);
                return true;

            case R.id.actSetting:
                Intent settingIntents = new Intent(this, SettingsActivity.class);
                startActivity(settingIntents);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public class SocketListenThread extends Thread {
        private boolean run = true;

        @Override
        public void run() {
            try {
                mServerSocket = new ServerSocket(8975);
                mListenPort = mServerSocket.getLocalPort();
            }
            catch (IOException e) {
                Log.e("SocketListenThread", "Failed to open socket");
            }

            while(run) {

                Socket socket = null;
                try {
                    socket = mServerSocket.accept();
                    String ip = socket.getRemoteSocketAddress().toString();
                    Log.d("Test", "Found ip: " + ip);
                    final Socket finalSocket = socket;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Meter newMeter = new Meter();
                            newMeter.openConnection(finalSocket);
                        }
                    });
                } catch (IOException e1) {
                    Log.e("SocketListenThread", "Failed to accept client");
                }
            }
        }

        public void close() {
            run = false;
            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.d("close ServerSocketThread", "Failed to close server socket");
            }
        }
    }

    public class ClientListen extends Thread {
        private boolean run = true;

        @Override
        public void run() {
            try {
                run = true;


                udpSocket = new DatagramSocket();

                byte[] message = new byte[8000];
                DatagramPacket packet = new DatagramPacket(message, message.length);
                mListenPort = udpSocket.getLocalPort();
                //registerService(udpSocket.getLocalPort());
                while (run) {
                    udpSocket.receive(packet);
                    //socket.receive(packet);
                    String text = new String(message, 0, packet.getLength());
                    Log.d("Received data", text);
                    Gson gson = new Gson();
                    final ArrayMap<String, String> arrayMap = gson.fromJson(text, ArrayMap.class);

                    if (arrayMap.get("command").equals("log")) {

                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
                        String date = dateFormat.format(Calendar.getInstance().getTime());


                        //final String fileName = date +  arrayMap.get("id") + " events.log";
                        final String fileName = "events.log";

                        //File logDir = getApplicationContext().getDir("logs", Context.MODE_PRIVATE);
                        File logDir = Environment.getExternalStorageDirectory();
                        final File logFile = new File(logDir, fileName);
                        Log.d("TEST", "logFile path: " + logFile.getAbsolutePath());

                        if(!isStoragePermissionGranted()) {
                            while(mExternalStoragePermmisions == 0);
                        }

                        if(mExternalStoragePermmisions == 1) {
                            try {
                                FileOutputStream outputStream = new FileOutputStream(logFile);
                                outputStream.write(arrayMap.get("log").getBytes());
                                outputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                View logView = layoutInflater.inflate(R.layout.popup_log, null);

                                TextView txtLog = logView.findViewById(R.id.txtLog);
                                String test = arrayMap.get("log");
                                txtLog.append(arrayMap.get("log"));
                                DisplayMetrics displayMetrics = new DisplayMetrics();
                                WindowManager windowmanager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
                                windowmanager.getDefaultDisplay().getMetrics(displayMetrics);

                                final PopupWindow popupWindow = new PopupWindow(logView, displayMetrics.widthPixels - 60, displayMetrics.heightPixels - 60, true);
                                popupWindow.showAtLocation(gridview, Gravity.CENTER, 0, 0);

                                Button btnClose = logView.findViewById(R.id.btnClosePopup);
                                btnClose.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        popupWindow.dismiss();
                                    }
                                });

                                Button btnEmail = logView.findViewById(R.id.btnEmail);
                                btnEmail.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        if (mExternalStoragePermmisions == 1) {
                                            Intent intent = new Intent(Intent.ACTION_SEND);
                                            intent.setType("*/*");
                                            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getApplicationContext(), "com.versuscodice.provider", logFile));
                                            if (intent.resolveActivity(getPackageManager()) != null) {
                                                startActivity(intent);
                                            }
                                        }
                                    }
                                });
                            }
                        });
                    } else if (arrayMap.get("command").equals("update")) {

                        boolean found = false;

                        for (Meter testMeter : meters) {
                            if (testMeter.id != null) {
                                if (testMeter.id.equals(arrayMap.get("id"))) {
                                    testMeter.update(arrayMap);
                                    testMeter.mIpAddress = packet.getAddress();
                                    found = true;
                                }
                            }
                        }

                        if (found == false) {
                            for (Meter testMeter : backgroundMeters) {
                                if (testMeter.id.equals(arrayMap.get("id"))) {
                                    testMeter.update(arrayMap);
                                    testMeter.mIpAddress = packet.getAddress();
                                    found = true;
                                }
                            }

                        }

                        if (found == false) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    backgroundMeters.add(new Meter(arrayMap));
                                }
                            });
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                meterAdapter.notifyDataSetChanged();
                            }
                        });
                    }

                }
                udpSocket.close();
            } catch (IOException e) {
                Log.e("TEST", "error: ", e);
                udpSocket.close();
                run = false;
            }

        }

        public void close() {
            run = false;
        }



        public  boolean isStoragePermissionGranted() {
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.d("TEST","Permission is granted");
                    mExternalStoragePermmisions = 1;
                    return true;
                } else {

                    Log.d("TEST","Permission is revoked");
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    mExternalStoragePermmisions = 0;
                    return false;
                }
            }
            else { //permission is automatically granted on sdk<23 upon installation
                Log.d("TEST","Permission is granted");
                mExternalStoragePermmisions = 1;
                return true;
            }
        }
    }

    class SendThread extends Thread {

        private DatagramPacket mPacket;

        public SendThread(DatagramPacket packet) {
            mPacket = packet;
        }

        public void run() {
            try {
                DatagramSocket udpSocket = new DatagramSocket();
                udpSocket.send(mPacket);
                udpSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
