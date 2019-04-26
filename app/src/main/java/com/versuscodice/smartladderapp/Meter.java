package com.versuscodice.smartladderapp;

import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.JsonReader;
import android.util.Log;
import android.util.MalformedJsonException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import oscP5.OscArgument;
import oscP5.OscMessage;

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

    int mCalDueInternal = 0;

    public static CommunicationService mService;

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

    public void update(OscMessage message) {

        String caldueinterval;
        String dateString;

        id = message.get(0).stringValue();
        temp = message.get(1).stringValue();
        meterBatteryLevel = message.get(2).stringValue();
        mBatteryLevel = message.get(3).stringValue();
        oxygenLevel = message.get(4).stringValue();
        carbondioxideLevel = message.get(5).stringValue();
        hydrogensulfideLevel = message.get(6).stringValue();
        combExLevel = message.get(7).stringValue();
        mAlarmState = message.get(8).booleanValue();
        mWarningState = message.get(9).booleanValue();
        mManState = message.get(10).booleanValue();
        mLadderState = message.get(11).booleanValue();
        mBatteryState = message.get(12).booleanValue();
        mBluetoothState = message.get(13).booleanValue();
        mMeterState = message.get(14).booleanValue();
        mEarlyState = message.get(15).booleanValue();
        mMeterBatteryState = message.get(16).booleanValue();
        mEarlyDoneState = message.get(17).booleanValue();
        mIdleState = message.get(18).booleanValue();
        mMeterBatteryDangerState = message.get(19).booleanValue();
        mBatteryDangerState = message.get(20).booleanValue();
        mAlarmOperator = message.get(21).booleanValue();
        mAlarmMeterOff = message.get(22).booleanValue();
        mPort = message.get(23).intValue();
        mInsertionCount = message.get(24).intValue();
        version = message.get(25).stringValue();
        caldueinterval = message.get(26).stringValue();
        dateString = message.get(27).stringValue();

        if(!dateString.equals("")) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            try {
                mLastCalibration = dateFormat.parse(dateString);
            } catch (ParseException e) {
                Log.d("TEST", "Failed to convert last calibration date");
            }
        }
        else {
            mLastCalibration = null;
        }

        lastUpdate = Calendar.getInstance().getTime();

        if(!caldueinterval.equals("")) {
            mCalDueInternal = Integer.parseInt(caldueinterval);
        }
        else {
            mCalDueInternal = 0;
        }

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
                mService.refresh();

            }
        });

        mActiveHandler.removeCallbacks(activeRunnable);
        mActiveHandler.postDelayed(activeRunnable, 10000);

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
        String caldueinterval = arrayMap.get("caldueinterval");

        String dateString = arrayMap.get("lastcalibration");

        if(dateString != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            try {
                mLastCalibration = dateFormat.parse(dateString);
            } catch (ParseException e) {
                Log.d("TEST", "Failed to convert last calibration date");
            }
        }
        else {
            mLastCalibration = null;
        }
        lastUpdate = Calendar.getInstance().getTime();

        if(caldueinterval != null) {
            mCalDueInternal = Integer.parseInt(caldueinterval);
        }
        else {
            mCalDueInternal = 0;
        }

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
                mService.refresh();

            }
        });
    }

    public Runnable activeRunnable = new Runnable() {
        @Override
        public void run() {

            Date now = Calendar.getInstance().getTime();

            if (lastUpdate != null) {
                if (now.getTime() - lastUpdate.getTime() > 10000) {
                    mActive = false;
                    mThat.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMeterAdapter.notifyDataSetChanged();
                            mService.refresh();
                        }
                    });
                } else {
                    mActiveHandler.postDelayed(activeRunnable, 10000);
                }
            }
        }    };

    public int getDaysToCal() {
        if(mLastCalibration != null && mCalDueInternal != 0) {
            Date today = new Date();
            long daysInMilli = today.getTime() - mLastCalibration.getTime();
            int days = (int) (daysInMilli / 86400000);
            return (mCalDueInternal - days < 0) ? (0) : (mCalDueInternal - days);
        }
        else {
            return 0;
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
            String strBuffer = "";
            int test;

            try {
                dataIn = new DataInputStream(mSocket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }

            run = true;
            while(run) {
                try {
                    do {
                        if((test = dataIn.read(buffer)) > 0) {
                            String text = new String(buffer, 0, test).trim();
                            //Log.d("Received data", text);

                            strBuffer = strBuffer + text;

                            Log.d("TEST", "strBuffer = " + strBuffer);

                            mActiveHandler.removeCallbacks(activeRunnable);
                            mActiveHandler.postDelayed(activeRunnable, 10000);
                        }
                    }while(strBuffer.indexOf('}') == -1);

                    String message = "";
                    try {
                        message = strBuffer.substring(strBuffer.indexOf('{'), strBuffer.indexOf('}') + 1);
                        strBuffer = "";
                    }
                    catch (StringIndexOutOfBoundsException e) {
                        Log.d("TEST", "out of bounds");
                        return;
                    }

                    try {
                        Gson gson = new Gson();
                        ArrayMap<String, String> arrayMap = gson.fromJson(message, ArrayMap.class);
                        if(arrayMap.get("command").equals("update")) {
                            update(arrayMap);
                        }
                        else if(arrayMap.get("command").equals("log")) {
                            mThat.displayLog(arrayMap, id);
                        }
                        else if(arrayMap.get("command").equals("date")) {
                            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                            sendData("Date," + format.format(Calendar.getInstance().getTime()));
                        }
                    } catch(Exception e) {
                        Log.d("Test", "Failed convert");
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
