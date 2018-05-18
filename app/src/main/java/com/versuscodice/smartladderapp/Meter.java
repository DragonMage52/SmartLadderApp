package com.versuscodice.smartladderapp;

import java.util.Date;

/**
 * Created by Spencer Costello on 3/17/2018.
 */

public class Meter {
    String id = "";
    boolean wariningState;
    boolean alarmState;
    boolean ladderState;
    boolean manState;
    String temp = "";
    Date lastUpdate;
    String meterBattery = "";
    String oxygenLevel;
    String hydrogensulfideLevel;
    String carbondioxideLevel;
    String combExLevel;

    public Meter(String mID, boolean mWariningState, boolean mAlarmState, boolean mLadderState, boolean mManState, String mTemp, String mMeterBattery, String mOxygenLevel, String mHydrogensulfideLevel, String mCarbondioxideLevel, String mCombExLevel ,Date mLastUpdate) {
        id = mID;
        wariningState = mWariningState;
        alarmState = mAlarmState;
        ladderState = mLadderState;
        manState = mManState;
        temp = mTemp;
        lastUpdate = mLastUpdate;
        meterBattery = mMeterBattery;
        oxygenLevel = mOxygenLevel;
        hydrogensulfideLevel = mHydrogensulfideLevel;
        carbondioxideLevel = mCarbondioxideLevel;
        combExLevel = mCombExLevel;
    }
}
