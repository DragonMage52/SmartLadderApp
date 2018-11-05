package com.versuscodice.smartladderapp;

import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;
import android.util.MalformedJsonException;

import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Spencer Costello on 3/17/2018.
 */

public class Meter {

    String id;
    String temp;
    String meterBatteryLevel;
    String mBatteryLevel;
    String oxygenLevel;
    String carbondioxideLevel;
    String hydrogensulfideLevel;
    String combExLevel;
    String version;
    InetAddress mIpAddress;
    int mPort = 0;

    boolean mAlarmState = false;
    boolean mWarningState = true;

    boolean mManState = false;
    boolean mLadderState = false;
    boolean mBatteryState = false;
    boolean mBluetoothState = false;
    boolean mMeterState = false;
    boolean mEarlyState = false;
    boolean mMeterBatteryState = false;
    boolean mEarlyDoneState = false;
    boolean mIdleState = false;
    boolean mMeterBatteryDangerState = false;
    boolean mBatteryDangerState = false;

    boolean mAlarmOperator = false;
    boolean mAlarmMeterOff = false;
    boolean mAlarmMeterBattery = false;
    boolean mAlarmBattery = false;

    boolean mActive = false;

    Date lastUpdate;

    Date mLastCalibration;

    CountDownTimer mActiveTimer;

    Handler mActiveHandler = new Handler();

    int mAlarmSilenceState = 0;

    int mInsertionCount = 0;

    static MainActivity mThat;
    static MeterAdapter mMeterAdapter;

    Socket mSocket = null;

    ClientManageThread mManageThread;

    boolean mInitalized = false;

    public Meter() {

    }

    public Meter(String mID) {
        id = mID;
    }

    public Meter(ArrayMap<String, String> arrayMap) {
        update(arrayMap);
    }

    public static void setContext(MainActivity that, MeterAdapter meterAdapter) {
        mThat = that;
        mMeterAdapter = meterAdapter;
    }

    public void update(ArrayMap<String, String> arrayMap) {
        id = arrayMap.get("id");
        temp = arrayMap.get("temp");
        meterBatteryLevel = arrayMap.get("meterBatteryLevel");
        mBatteryLevel = arrayMap.get("batteryLevel");
        oxygenLevel = arrayMap.get("oxygenLevel");
        carbondioxideLevel = arrayMap.get("carbondioxideLevel");
        hydrogensulfideLevel = arrayMap.get("hydrogensulfideLevel");
        combExLevel = arrayMap.get("combExLevel");
        mAlarmState = Boolean.valueOf(arrayMap.get("alarmState"));
        mWarningState = Boolean.valueOf(arrayMap.get("warningState"));
        mManState = Boolean.valueOf(arrayMap.get("manState"));
        mLadderState = Boolean.valueOf(arrayMap.get("ladderState"));
        mBatteryState = Boolean.valueOf(arrayMap.get("batteryState"));
        mBluetoothState = Boolean.valueOf(arrayMap.get("bluetoothState"));
        mMeterState = Boolean.valueOf(arrayMap.get("meterState"));
        mEarlyState = Boolean.valueOf(arrayMap.get("earlyState"));
        mMeterBatteryState = Boolean.valueOf(arrayMap.get("meterbatteryState"));
        mEarlyDoneState = Boolean.valueOf(arrayMap.get("earlydoneState"));
        mIdleState = Boolean.valueOf(arrayMap.get("idleState"));
        mMeterBatteryDangerState = Boolean.valueOf(arrayMap.get("meterbatterydangerState"));
        mBatteryDangerState = Boolean.valueOf(arrayMap.get("batterydangerState"));
        mAlarmOperator = Boolean.valueOf(arrayMap.get("alarmOperator"));
        mAlarmMeterOff = Boolean.valueOf(arrayMap.get("alarmmeterOff"));
        mAlarmMeterBattery = Boolean.valueOf(arrayMap.get("alarmmeterBattery"));
        mAlarmBattery = Boolean.valueOf(arrayMap.get("alarmBattery"));
        mPort = Integer.parseInt(arrayMap.get("port"));
        mInsertionCount = Integer.parseInt(arrayMap.get("insertion"));
        version = arrayMap.get("version");

        String dateString = arrayMap.get("lastcalibration");

        if(dateString != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            try {
                mLastCalibration = dateFormat.parse(dateString);
            } catch (ParseException e) {
                Log.d("TEST", "Failed to convert last calibration date");
            }
        }
        lastUpdate = Calendar.getInstance().getTime();

        mActive = true;

        if(mAlarmState && mAlarmSilenceState == 0) {
            mAlarmSilenceState = 1;
        }
        else if(!mAlarmState && (mAlarmSilenceState == 1 || mAlarmSilenceState == -1)) {
            mAlarmSilenceState = 0;
        }

        if(!mInitalized) {
            mInitalized = true;

            boolean found = false;
            for(int i = 0; i < mThat.meters.size(); i++) {
                if(mThat.meters.get(i).id != null) {
                    if(mThat.meters.get(i).id.equals(id)) {
                        mThat.meters.set(i, this);
                        found = true;
                    }
                }
            }

            if(!found) {
                for(int i = 0; i < mThat.backgroundMeters.size(); i++) {
                    if(mThat.backgroundMeters.get(i).id != null) {
                        if(mThat.backgroundMeters.get(i).id.equals(id)) {
                            mThat.backgroundMeters.set(i, this);
                            found = true;
                        }
                    }
                }
            }

            if (!found) {

                final Meter finalThisMeter = this;

                mThat.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mThat.backgroundMeters.add(finalThisMeter);
                    }
                });
            }
        }

        mThat.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mMeterAdapter.notifyDataSetChanged();
            }
        });
    }

    public Runnable activeRunnable = new Runnable() {
        @Override
        public void run() {

            Date now = Calendar.getInstance().getTime();

            if(now.getTime() - lastUpdate.getTime() > 10000) {
                mActive = false;
                mThat.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMeterAdapter.notifyDataSetChanged();
                    }
                });
            }
            else {
                mActiveHandler.postDelayed(activeRunnable, 10000);
            }
        }
    };

    public boolean isCalibrated() {
        if(mLastCalibration != null && mThat.mCalibrationReminder != 0) {
            if ((new Date().getTime() - mLastCalibration.getTime()) > (mThat.mCalibrationReminder * 86400000)) {
                return false;
            } else {
                return true;
            }
        }
        else {
            return true;
        }
    }

    public void openConnection(Socket socket) {
        mSocket = socket;

        mManageThread = new ClientManageThread();
        mManageThread.start();

    }

    public void sendData(String message) {
        SendThread sendThread = new SendThread(message);
        sendThread.start();
    }

    public class ClientManageThread extends Thread {

        DataInputStream dataIn;
        boolean run = false;

        @Override
        public void run() {

            byte buffer [] = new byte[100000];
            int test;

            try {
                dataIn = new DataInputStream(mSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }


            run = true;
            while(run) {
                try {
                    if((test = dataIn.read(buffer)) > 0) {
                        String text = new String(buffer, 0, test);
                        Log.d("Received data", text);

                        mActiveHandler.removeCallbacks(activeRunnable);
                        mActiveHandler.postDelayed(activeRunnable, 10000);

                        try {
                            Gson gson = new Gson();
                            ArrayMap<String, String> arrayMap = gson.fromJson(text, ArrayMap.class);
                            if(arrayMap.get("command").equals("update")) {
                                update(arrayMap);
                            }
                            else if(arrayMap.get("command").equals("log")) {
                                mThat.displayLog(arrayMap);
                            }
                        } catch(Exception e) {
                            Log.d("Test", "Failed convert");
                        }

                    }
                    else {
                        Log.d("TEST", "break out of recieve loop");
                        break;
                    }
                } catch (IOException e) {
                    Log.e("ClientManageThread", "Failed to open input stream");
                    close();
                }
            }
            try {
                mSocket.close();

            } catch (IOException e) {
                Log.e("ClientMangeThread", "Failed to close socket");
            }
        }

        public void close() {
            run = false;
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e("close ClientMangeThread", "Failed to close socket");
            }
        }
    }

    public class SendThread extends Thread {

        DataOutputStream dataOut;
        byte [] mMessage;
        String mStrMessage;

        public SendThread(String message) {
            mMessage = message.getBytes();
            mStrMessage = message;
        }

        @Override
        public void run() {
            if (mSocket != null) {
                if (!mSocket.isClosed()) {

                    try {
                        dataOut = new DataOutputStream(mSocket.getOutputStream());
                    } catch (IOException e) {
                        Log.e("ServerMangeThread", "Failed to open socket");
                    }

                    try {
                        dataOut.write(mMessage);
                        Log.d("SendThread", "sent");
                    } catch (Exception e) {
                        Log.e("SendThread", "Failed to open output stream");
                    }

                    if(mStrMessage.equals("close")) {
                        close();
                    }
                }
            }
        }

        public void close() {
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.d("close SendThread", "Failed to close socket");
            }
        }
    }

}
