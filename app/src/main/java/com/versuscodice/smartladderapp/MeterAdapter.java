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

            TextView txtID = (TextView) view.findViewById(R.id.txtID);
            TextView txtTemp = (TextView) view.findViewById(R.id.txtTemp);
            TextView txtStatus = (TextView) view.findViewById(R.id.txtStatus);
            TextView txtLED = (TextView) view.findViewById(R.id.txtLED);
            TextView txtRemoteBatteryLevel = (TextView) view.findViewById(R.id.txtRemoteBatteryLevel);
            TextView txtLocalBatteryLevel = (TextView) view.findViewById(R.id.txtLocalBattery);
            TextView txtLastUpdate = (TextView) view.findViewById(R.id.txtLastUpdate);
            TextView txtOxygenLevel = (TextView) view.findViewById(R.id.txtOxygenLevel);
            TextView txtHydrogenSulfideLevel = (TextView) view.findViewById(R.id.txtHydrogenSulfideLevel);
            TextView txtCarbonDioxideLevel = (TextView) view.findViewById(R.id.txtCarbonDioxideLevel);
            TextView txtCombExLevel = (TextView) view.findViewById(R.id.txtCombExLevel);
            ConstraintLayout container = (ConstraintLayout) view.findViewById(R.id.meter_container);

            TextView txtStaticTemp = (TextView) view.findViewById(R.id.txtStaticTemp);
            TextView txtStaticLEL = (TextView) view.findViewById(R.id.txtStaticLEL);
            TextView txtStaticO2 = (TextView) view.findViewById(R.id.txtStaticO2);
            TextView txtStaticH2SO4 = (TextView) view.findViewById(R.id.txtStaticH2SO4);
            TextView txtStaticCO = (TextView) view.findViewById(R.id.txtStaticCO);
            TextView txtStaticMeterBattery = (TextView) view.findViewById(R.id.txtStaticMeterBattery);
            TextView txtStaticLocalBattery = (TextView) view.findViewById(R.id.txtStaticLocalBattery);
            TextView txtStaticLastUpdated = (TextView) view.findViewById(R.id.txtStaticLastUpdate);


            txtID.setText(thisMeter.id);

            if(!thisMeter.mActive) {
                txtLED.setText("LADDER OFF");
                txtTemp.setVisibility(View.INVISIBLE);
                txtStatus.setVisibility(View.INVISIBLE);
                txtRemoteBatteryLevel.setVisibility(View.INVISIBLE);
                txtLocalBatteryLevel.setVisibility(View.INVISIBLE);
                txtLastUpdate.setVisibility(View.INVISIBLE);
                txtOxygenLevel.setVisibility(View.INVISIBLE);
                txtHydrogenSulfideLevel.setVisibility(View.INVISIBLE);
                txtCarbonDioxideLevel.setVisibility(View.INVISIBLE);
                txtCombExLevel.setVisibility(View.INVISIBLE);

                txtStaticTemp.setVisibility(View.INVISIBLE);
                txtStaticLEL.setVisibility(View.INVISIBLE);
                txtStaticO2.setVisibility(View.INVISIBLE);
                txtStaticH2SO4.setVisibility(View.INVISIBLE);
                txtStaticCO.setVisibility(View.INVISIBLE);
                txtStaticMeterBattery.setVisibility(View.INVISIBLE);
                txtStaticLocalBattery.setVisibility(View.INVISIBLE);
                txtStaticLastUpdated.setVisibility(View.INVISIBLE);

                //return view;
            }
            else {
                txtTemp.setVisibility(View.VISIBLE);
                txtStatus.setVisibility(View.VISIBLE);
                txtRemoteBatteryLevel.setVisibility(View.VISIBLE);
                txtLocalBatteryLevel.setVisibility(View.VISIBLE);
                txtLastUpdate.setVisibility(View.VISIBLE);
                txtOxygenLevel.setVisibility(View.VISIBLE);
                txtHydrogenSulfideLevel.setVisibility(View.VISIBLE);
                txtCarbonDioxideLevel.setVisibility(View.VISIBLE);
                txtCombExLevel.setVisibility(View.VISIBLE);

                txtStaticTemp.setVisibility(View.VISIBLE);
                txtStaticLEL.setVisibility(View.VISIBLE);
                txtStaticO2.setVisibility(View.VISIBLE);
                txtStaticH2SO4.setVisibility(View.VISIBLE);
                txtStaticCO.setVisibility(View.VISIBLE);
                txtStaticMeterBattery.setVisibility(View.VISIBLE);
                txtStaticLocalBattery.setVisibility(View.VISIBLE);
                txtStaticLastUpdated.setVisibility(View.VISIBLE);
            }

            txtLocalBatteryLevel.setText(thisMeter.mBatteryLevel + "%");
            if(thisMeter.mBluetoothState) {
                txtTemp.setText(thisMeter.temp + (char) 0x00B0 + "F");
                txtRemoteBatteryLevel.setText(thisMeter.meterBatteryLevel + "%");
                txtOxygenLevel.setText(thisMeter.oxygenLevel + "%");
                txtHydrogenSulfideLevel.setText(thisMeter.hydrogensulfideLevel + "%");
                txtCarbonDioxideLevel.setText(thisMeter.carbondioxideLevel + "%");
                txtCombExLevel.setText(thisMeter.combExLevel + "%");
            }
            else {
                txtTemp.setText("?");
                txtRemoteBatteryLevel.setText("?");
                txtOxygenLevel.setText("?");
                txtHydrogenSulfideLevel.setText("?");
                txtCarbonDioxideLevel.setText("?");
                txtCombExLevel.setText("?");
            }
            txtLastUpdate.setText(thisMeter.lastUpdate.toString());

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
            else if(!thisMeter.mLadderState && !thisMeter.mManState){
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

        return view;
    }
}
