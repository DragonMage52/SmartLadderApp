package com.versuscodice.smartladderapp;

import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;

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

    CountDownTimer mActiveTimer;

    int mAlarmSilenceState = 0;

    static MainActivity mThat;
    static MeterAdapter mMeterAdapter;

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

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
        lastUpdate = Calendar.getInstance().getTime();

        mActive = true;

        if(mAlarmState && mAlarmSilenceState == 0) {
            mAlarmSilenceState = 1;
        }
        else if(!mAlarmState && (mAlarmSilenceState == 1 || mAlarmSilenceState == -1)) {
            mAlarmSilenceState = 0;
        }

        if(mActiveTimer != null) {
            mActiveTimer.cancel();
        }

        mThat.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateTimer();
            }
        });
    }

    public void updateTimer() {
        mActiveTimer =  new CountDownTimer(10000, 5000) {

            public void onTick(long millisUntilFinished) {
                Log.d("TEST", "seconds remaining: " + millisUntilFinished/1000);

            }

            public void onFinish() {
                Log.d("TEST", "Countdown Timer finished");
                mActive = false;
                mThat.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMeterAdapter.notifyDataSetChanged();
                    }
                });
            }
        }.start();
    }

    public void clearMeter() {
        id = null;
        mActive = false;
    }
}
