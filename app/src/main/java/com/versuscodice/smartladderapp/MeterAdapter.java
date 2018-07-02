package com.versuscodice.smartladderapp;

import android.content.Context;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Spencer Costello on 3/17/2018.
 */

public class MeterAdapter extends BaseAdapter {
    private Context mContext;
    private List<Meter> meters = new ArrayList<Meter>();
    private MediaPlayer mRingtone;
    private ImageButton mBtnSilence;

    TextView txtID;
    //TextView txtTemp;
    TextView txtStatus;
    TextView txtLED;
    TextView txtRemoteBatteryLevel;
    TextView txtLocalBatteryLevel;
    TextView txtLastUpdate;
    TextView txtOxygenLevel;
    TextView txtHydrogenSulfideLevel;
    TextView txtCarbonDioxideLevel;
    TextView txtCombExLevel;
    ConstraintLayout container;

    //TextView txtStaticTemp;
    TextView txtStaticLEL;
    TextView txtStaticO2;
    TextView txtStaticH2SO4;
    TextView txtStaticCO;
    TextView txtStaticMeterBattery;
    TextView txtStaticLocalBattery;
    TextView txtStaticLastUpdated;


    public MeterAdapter(Context c, List<Meter> m, ImageButton btnSilence) {
        mContext = c;
        meters = m;
        mBtnSilence = btnSilence;

        mBtnSilence.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                for (Meter testMeter : meters) {
                    if(testMeter.mAlarmSilenceState == 1) {
                        testMeter.mAlarmSilenceState = -1;
                        notifyDataSetChanged();
                    }
                }
            }
        });
    }

    public int getCount() {
        return meters.size();
    }

    public Object getItem(int position) {
        return null;
    }

    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {

        Meter thisMeter = meters.get(i);

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view;
        if(convertView == null) {
            view = new View(mContext);
            view = inflater.inflate(R.layout.meter_layout, null);
        }
        else {
            view = (View) convertView;
        }

            txtID = (TextView) view.findViewById(R.id.txtID);
            //txtTemp = (TextView) view.findViewById(R.id.txtTemp);
            txtStatus = (TextView) view.findViewById(R.id.txtStatus);
            txtLED = (TextView) view.findViewById(R.id.txtLED);
            txtRemoteBatteryLevel = (TextView) view.findViewById(R.id.txtRemoteBatteryLevel);
            txtLocalBatteryLevel = (TextView) view.findViewById(R.id.txtLocalBattery);
            txtLastUpdate = (TextView) view.findViewById(R.id.txtLastUpdate);
            txtOxygenLevel = (TextView) view.findViewById(R.id.txtOxygenLevel);
            txtHydrogenSulfideLevel = (TextView) view.findViewById(R.id.txtHydrogenSulfideLevel);
            txtCarbonDioxideLevel = (TextView) view.findViewById(R.id.txtCarbonDioxideLevel);
            txtCombExLevel = (TextView) view.findViewById(R.id.txtCombExLevel);
            container = (ConstraintLayout) view.findViewById(R.id.meter_container);

            //txtStaticTemp = (TextView) view.findViewById(R.id.txtStaticTemp);
            txtStaticLEL = (TextView) view.findViewById(R.id.txtStaticLEL);
            txtStaticO2 = (TextView) view.findViewById(R.id.txtStaticO2);
            txtStaticH2SO4 = (TextView) view.findViewById(R.id.txtStaticH2SO4);
            txtStaticCO = (TextView) view.findViewById(R.id.txtStaticCO);
            txtStaticMeterBattery = (TextView) view.findViewById(R.id.txtStaticMeterBattery);
            txtStaticLocalBattery = (TextView) view.findViewById(R.id.txtStaticLocalBattery);
            txtStaticLastUpdated = (TextView) view.findViewById(R.id.txtStaticLastUpdate);



            if(thisMeter.id == null) {
                setAllInvisible();
                txtID.setVisibility(View.INVISIBLE);
                txtLED.setVisibility(View.INVISIBLE);
            }
            else if(!thisMeter.mActive) {
                txtLED.setText("LADDER OFF");
                txtID.setVisibility(View.VISIBLE);
                txtLED.setVisibility(View.VISIBLE);
                setAllInvisible();
            }
            else {
                setAllVisible();
            }

            if(thisMeter.id != null) {
                txtID.setText(thisMeter.id);
            }
            else {
                container.setBackgroundColor(mContext.getResources().getColor(R.color.colorDisabled));
                refresh();
                return view;
            }

            txtLocalBatteryLevel.setText(thisMeter.mBatteryLevel + "%");
            if(thisMeter.mBluetoothState) {
                //txtTemp.setText(thisMeter.temp + (char) 0x00B0 + "F");
                txtRemoteBatteryLevel.setText(thisMeter.meterBatteryLevel + "%");
                txtOxygenLevel.setText(thisMeter.oxygenLevel + "%");
                txtHydrogenSulfideLevel.setText(thisMeter.hydrogensulfideLevel + "%");
                txtCarbonDioxideLevel.setText(thisMeter.carbondioxideLevel + "%");
                txtCombExLevel.setText(thisMeter.combExLevel + "%");
            }
            else {
                //txtTemp.setText("?");
                txtRemoteBatteryLevel.setText("?");
                txtOxygenLevel.setText("?");
                txtHydrogenSulfideLevel.setText("?");
                txtCarbonDioxideLevel.setText("?");
                txtCombExLevel.setText("?");
            }
            if(thisMeter.lastUpdate != null) {
                txtLastUpdate.setText(thisMeter.lastUpdate.toString());
            }

            if(!thisMeter.mActive) {
                container.setBackgroundColor(mContext.getResources().getColor(R.color.colorOff));
            }
            else if(thisMeter.mAlarmState) {
                container.setBackgroundColor(mContext.getResources().getColor(R.color.colorAlarm));
            }
            else if(thisMeter.mWarningState) {
                container.setBackgroundColor(mContext.getResources().getColor(R.color.colorWarning));
            }
            else if(!thisMeter.mLadderState && !thisMeter.mManState) {
                container.setBackgroundColor(mContext.getResources().getColor(R.color.colorIdle));
            }
            else {
                container.setBackgroundColor(mContext.getResources().getColor(R.color.colorGood));
            }

            if(thisMeter.mLadderState || thisMeter.mManState) {
                txtLED.setText("LADDER ACTIVE");
            }
            else if(!thisMeter.mLadderState && !thisMeter.mManState && thisMeter.mActive){
                txtLED.setText("LADDER IDLE");
            }

            if(thisMeter.mMeterState) {
                txtStatus.setText("ALARM-GAS");
            }
            else if(thisMeter.mAlarmOperator) {
                txtStatus.setText("ALARM-OPERATOR");
            }
            else if(thisMeter.mAlarmMeterOff) {
                txtStatus.setText("ALARM-METER-OFF");
            }
            else if(thisMeter.mAlarmBattery) {
                txtStatus.setText("ALARM-BATTERY");
            }
            else if(thisMeter.mAlarmMeterBattery) {
                txtStatus.setText("ALARM-METER BATTERY");
            }
            else if(thisMeter.mEarlyState) {
                txtStatus.setText("SAMPLING");
            }
            else if(!thisMeter.mBluetoothState) {
                txtStatus.setText("METER OFF");
            }
            else if(thisMeter.mEarlyDoneState && !thisMeter.mAlarmState && !thisMeter.mWarningState) {
                txtStatus.setText("ENTER");
            }
            else if(!thisMeter.mLadderState && !thisMeter.mManState){
                txtStatus.setText("");
            }

            refresh();

        return view;
    }

    public void refresh() {
        int totalAlarms = 0;
        boolean playRingtone = false;

        for (Meter testMeter : meters) {
            if (testMeter.mAlarmState && testMeter.mActive) {
                totalAlarms++;
            }
            if(testMeter.mAlarmSilenceState == 1 && testMeter.mActive) {
                playRingtone = true;
            }
        }

        if(mRingtone == null) {
            mRingtone = MediaPlayer.create((MainActivity) mContext, Settings.System.DEFAULT_RINGTONE_URI);
        }

        if(playRingtone) {
            if(!mRingtone.isPlaying()) {
                mRingtone = MediaPlayer.create((MainActivity) mContext, Settings.System.DEFAULT_RINGTONE_URI);
                mRingtone.start();
            }
            mBtnSilence.setVisibility(View.VISIBLE);
        }
        else {
            if(mRingtone.isPlaying()) {
                mRingtone.stop();
            }
            mBtnSilence.setVisibility(View.INVISIBLE);
        }

        final MainActivity finalThat = (MainActivity) mContext;

        final int finalTotalAlarm = totalAlarms;

        finalThat.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(finalTotalAlarm > 0) {
                    finalThat.txtAlarms.setText(finalTotalAlarm + " Alarm(s)");
                    finalThat.txtAlarms.setVisibility(View.VISIBLE);
                }
                else {
                    finalThat.txtAlarms.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    public void setAllInvisible() {
        //txtTemp.setVisibility(View.INVISIBLE);
        txtStatus.setVisibility(View.INVISIBLE);
        txtRemoteBatteryLevel.setVisibility(View.INVISIBLE);
        txtLocalBatteryLevel.setVisibility(View.INVISIBLE);
        txtLastUpdate.setVisibility(View.INVISIBLE);
        txtOxygenLevel.setVisibility(View.INVISIBLE);
        txtHydrogenSulfideLevel.setVisibility(View.INVISIBLE);
        txtCarbonDioxideLevel.setVisibility(View.INVISIBLE);
        txtCombExLevel.setVisibility(View.INVISIBLE);

        //txtStaticTemp.setVisibility(View.INVISIBLE);
        txtStaticLEL.setVisibility(View.INVISIBLE);
        txtStaticO2.setVisibility(View.INVISIBLE);
        txtStaticH2SO4.setVisibility(View.INVISIBLE);
        txtStaticCO.setVisibility(View.INVISIBLE);
        txtStaticMeterBattery.setVisibility(View.INVISIBLE);
        txtStaticLocalBattery.setVisibility(View.INVISIBLE);
        txtStaticLastUpdated.setVisibility(View.INVISIBLE);
    }

    public void setAllVisible() {
        txtID.setVisibility(View.VISIBLE);
        txtLED.setVisibility(View.VISIBLE);
        //txtTemp.setVisibility(View.VISIBLE);
        txtStatus.setVisibility(View.VISIBLE);
        txtRemoteBatteryLevel.setVisibility(View.VISIBLE);
        txtLocalBatteryLevel.setVisibility(View.VISIBLE);
        txtLastUpdate.setVisibility(View.VISIBLE);
        txtOxygenLevel.setVisibility(View.VISIBLE);
        txtHydrogenSulfideLevel.setVisibility(View.VISIBLE);
        txtCarbonDioxideLevel.setVisibility(View.VISIBLE);
        txtCombExLevel.setVisibility(View.VISIBLE);

        //txtStaticTemp.setVisibility(View.VISIBLE);
        txtStaticLEL.setVisibility(View.VISIBLE);
        txtStaticO2.setVisibility(View.VISIBLE);
        txtStaticH2SO4.setVisibility(View.VISIBLE);
        txtStaticCO.setVisibility(View.VISIBLE);
        txtStaticMeterBattery.setVisibility(View.VISIBLE);
        txtStaticLocalBattery.setVisibility(View.VISIBLE);
        txtStaticLastUpdated.setVisibility(View.VISIBLE);
    }
}
