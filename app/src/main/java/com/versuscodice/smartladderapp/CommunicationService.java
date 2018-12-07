package com.versuscodice.smartladderapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CommunicationService extends Service {

    private final IBinder mBinder = new CommunicationBinder();

    public ServerSocket mServerSocket = null;
    public SocketListenThread mServerSocketThread = null;

    public int mExternalStoragePermmisions = 0;

    public Handler mMulticastSendHandler = new Handler();
    public int mListenPort;

    public List<Meter> meters = new ArrayList<>();
    public List<Meter> backgroundMeters = new ArrayList<>();

    MainActivity mThat;

    String mSSID;

    Notification mNotification;

    public class CommunicationBinder extends Binder {
        CommunicationService getService() {
            return CommunicationService.this;
        }
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "service creating", Toast.LENGTH_SHORT).show();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelID = "smartladder_channel_01";
            String channelName = "SafeAir Ladder";
            String channelDescription = "Notification for Safe Air Ladder Communication";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(channelID, channelName, importance);
            channel.setDescription(channelDescription);

            notificationManager.createNotificationChannel(channel);

            mNotification = new Notification.Builder(this, channelID)
                    .setContentTitle("SafeAir Ladder")
                    .setContentText("TEST")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setChannelId(channelID)
                    .build();
        }
        else {
            mNotification = new Notification.Builder(this)
                    .setContentTitle("SafeAir Ladder")
                    .setContentText("TEST")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .build();
        }

        startForeground(52, mNotification);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mSSID = sharedPref.getString("perf_appWifiSSID", "");

        String metersID[] = new String[24];

        Gson gson = new Gson();
        String storedIDs = sharedPref.getString("IDs", "");

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

        mMulticastSendHandler.post(multicastSendRunnable);

        mServerSocketThread = new SocketListenThread();
        mServerSocketThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();

        try {
            mServerSocketThread.close();
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

        SharedPreferences.Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        Gson gson = new Gson();
        String json = gson.toJson(metersID);
        sharedPrefEditor.putString("IDs", json);
        sharedPrefEditor.commit();
    }

    public void unBind() {

        String metersID[] = new String[24];
        int count = 0;

        for (Meter testMeter : meters) {
            if (testMeter.id != null) {
                metersID[count] = testMeter.id;
            }
            count++;
        }

        SharedPreferences.Editor sharedPrefEditor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        Gson gson = new Gson();
        String json = gson.toJson(metersID);
        sharedPrefEditor.putString("IDs", json);
        sharedPrefEditor.commit();
    }

    public class SocketListenThread extends Thread {
        private boolean run = true;

        @Override
        public void run() {
            try {
                mServerSocket = new ServerSocket(8975);
                mListenPort = mServerSocket.getLocalPort();
            } catch (IOException e) {
                Log.e("SocketListenThread", "Failed to open socket");
            }

            while (run) {

                Socket socket = null;
                try {
                    socket = mServerSocket.accept();
                    String ip = socket.getRemoteSocketAddress().toString();
                    Log.d("Test", "Found ip: " + ip);
                    final Socket finalSocket = socket;

                    mThat.runOnUiThread(new Runnable() {
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
                mMulticastSendHandler.postDelayed(multicastSendRunnable, 10000);

            } catch (IOException e) {
                Log.e("multicastSend", "Failed to send");
            }
        }
    };

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


