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
    String mIdentifier;
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

        mActiveHandler.removeCallbacks(activeRunnable);
        mActiveHandler.postDelayed(activeRunnable, 30000);

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

            if(now.getTime() - lastUpdate.getTime() > 30000) {
                mActive = false;
                mThat.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMeterAdapter.notifyDataSetChanged();
                    }
                });
            }
            else {
                mActiveHandler.postDelayed(activeRunnable, 30000);
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

    public class ClientManageThread extends Thread {

        DataInputStream dataIn;

        @Override
        public void run() {

            byte buffer [] = new byte[5000];
            int test = 0;

            try {
                dataIn = new DataInputStream(mSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            while(mSocket.isConnected()) {
                try {
                    if((test = dataIn.read(buffer)) > 0) {
                        String text = new String(buffer, 0, test);
                        Log.d("Received data", text);

                        try {
                            Gson gson = new Gson();
                            final ArrayMap<String, String> arrayMap = gson.fromJson(text, ArrayMap.class);
                            update(arrayMap);
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
                }
            }
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
