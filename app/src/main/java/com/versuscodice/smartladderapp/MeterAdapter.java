package com.versuscodice.smartladderapp;

import android.content.Context;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Spencer Costello on 3/17/2018.
 */

public class MeterAdapter extends BaseAdapter {
    private Context mContext;
    private List<Meter> meters = new ArrayList<Meter>();
    private MediaPlayer mRingtone;
    public ImageButton mBtnSilence;
    private Vibrator mVibrate;
    private TextToSpeech mTextToSpeech;
    private UtteranceProgressListener mUtteranceProgressListener;

    long [] mVibratePattern = {0, 500, 500};

    TextView txtID;
    //TextView txtTemp;
    TextView txtStatus;
    TextView txtLED;
    TextView txtRemoteBatteryLevel;
    TextView txtLocalBatteryLevel;
    //TextView txtLastUpdate;
    TextView txtOxygenLevel;
    TextView txtHydrogenSulfideLevel;
    TextView txtCarbonDioxideLevel;
    TextView txtCombExLevel;
    TextView txtInsertionCount;
    TextView txtDaysToCal;
    ConstraintLayout container;

    //TextView txtStaticTemp;
    TextView txtStaticLEL;
    TextView txtStaticO2;
    TextView txtStaticH2SO4;
    TextView txtStaticCO;
    TextView txtStaticMeterBattery;
    TextView txtStaticLocalBattery;
    //TextView txtStaticLastUpdated;
    TextView txtStaticInsertionCount;
    ImageView imgSync;


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
            view = inflater.inflate(R.layout.meter_layout, null);
        }
        else {
            view = convertView;
            imgSync = view.findViewById(R.id.imgSync);
            imgSync.clearAnimation();
        }

            txtID = view.findViewById(R.id.txtID);
            //txtTemp = view.findViewById(R.id.txtTemp);
            txtStatus = view.findViewById(R.id.txtStatus);
            txtLED = view.findViewById(R.id.txtLED);
            txtRemoteBatteryLevel = view.findViewById(R.id.txtRemoteBatteryLevel);
            txtLocalBatteryLevel = view.findViewById(R.id.txtLocalBattery);
            //txtLastUpdate = view.findViewById(R.id.txtLastUpdate);
            txtOxygenLevel = view.findViewById(R.id.txtOxygenLevel);
            txtHydrogenSulfideLevel = view.findViewById(R.id.txtHydrogenSulfideLevel);
            txtCarbonDioxideLevel = view.findViewById(R.id.txtCarbonDioxideLevel);
            txtCombExLevel = view.findViewById(R.id.txtCombExLevel);
            txtInsertionCount = view.findViewById(R.id.txtInsertionCount);
            txtDaysToCal = view.findViewById(R.id.txtDaysToCal);
            container = view.findViewById(R.id.meter_container);

            //txtStaticTemp = view.findViewById(R.id.txtStaticTemp);
            txtStaticLEL = view.findViewById(R.id.txtStaticLEL);
            txtStaticO2 = view.findViewById(R.id.txtStaticO2);
            txtStaticH2SO4 = view.findViewById(R.id.txtStaticH2SO4);
            txtStaticCO = view.findViewById(R.id.txtStaticCO);
            txtStaticMeterBattery = view.findViewById(R.id.txtStaticMeterBattery);
            txtStaticLocalBattery = view.findViewById(R.id.txtStaticLocalBattery);
            //txtStaticLastUpdated = view.findViewById(R.id.txtStaticLastUpdate);
            txtStaticInsertionCount = view.findViewById(R.id.txtStaticInsertionCount);


            imgSync = view.findViewById(R.id.imgSync);

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
                //refresh();
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
                txtDaysToCal.setText("Days to Calibration Due: " + thisMeter.getDaysToCal());

            }
            else {
                //txtTemp.setText("?");
                txtRemoteBatteryLevel.setText("?");
                txtOxygenLevel.setText("?");
                txtHydrogenSulfideLevel.setText("?");
                txtCarbonDioxideLevel.setText("?");
                txtCombExLevel.setText("?");
                txtDaysToCal.setText("Days to Calibration Due: ?");
            }
            if(thisMeter.lastUpdate != null) {
                //txtLastUpdate.setText(thisMeter.lastUpdate.toString());
            }

            txtInsertionCount.setText(thisMeter.mInsertionCount + "");

            if(thisMeter.mActive) {
                AnimationSet animationSet = new AnimationSet(false);
                Animation alphaAnimation = new AlphaAnimation(0, 1f);
                alphaAnimation.setDuration(750);
                alphaAnimation.setInterpolator(new LinearInterpolator());
                alphaAnimation.setRepeatCount(1);
                alphaAnimation.setRepeatMode(Animation.REVERSE);
                alphaAnimation.setFillAfter(true);

                Animation rotateAnimation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF,  .5f);
                rotateAnimation.setDuration(1000);
                rotateAnimation.setInterpolator(new LinearInterpolator());

                animationSet.setFillAfter(true);

                animationSet.addAnimation(alphaAnimation);
                animationSet.addAnimation(rotateAnimation);
                imgSync.startAnimation(animationSet);
            }



            if(!thisMeter.mActive) {
                container.setBackgroundColor(mContext.getResources().getColor(R.color.colorOff));
            }
            else if(thisMeter.mAlarmState) {
                container.setBackgroundColor(mContext.getResources().getColor(R.color.colorAlarm));
            }
            else if(thisMeter.mWarningState || (thisMeter.getDaysToCal() < 1 && thisMeter.mBluetoothState)) {
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
            else if(thisMeter.getDaysToCal() < 1 && thisMeter.mBluetoothState) {
                txtStatus.setText("CALIBRATION NEEDED");
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

            //refresh();

        return view;
    }

    public void setAllInvisible() {
        //txtTemp.setVisibility(View.INVISIBLE);
        txtStatus.setVisibility(View.INVISIBLE);
        txtRemoteBatteryLevel.setVisibility(View.INVISIBLE);
        txtLocalBatteryLevel.setVisibility(View.INVISIBLE);
        //txtLastUpdate.setVisibility(View.INVISIBLE);
        txtOxygenLevel.setVisibility(View.INVISIBLE);
        txtHydrogenSulfideLevel.setVisibility(View.INVISIBLE);
        txtCarbonDioxideLevel.setVisibility(View.INVISIBLE);
        txtCombExLevel.setVisibility(View.INVISIBLE);
        txtInsertionCount.setVisibility(View.INVISIBLE);
        txtDaysToCal.setVisibility(View.INVISIBLE);

        //txtStaticTemp.setVisibility(View.INVISIBLE);
        txtStaticLEL.setVisibility(View.INVISIBLE);
        txtStaticO2.setVisibility(View.INVISIBLE);
        txtStaticH2SO4.setVisibility(View.INVISIBLE);
        txtStaticCO.setVisibility(View.INVISIBLE);
        txtStaticMeterBattery.setVisibility(View.INVISIBLE);
        txtStaticLocalBattery.setVisibility(View.INVISIBLE);
        //txtStaticLastUpdated.setVisibility(View.INVISIBLE);
        txtStaticInsertionCount.setVisibility(View.INVISIBLE);
        imgSync.setVisibility(View.INVISIBLE);
    }

    public void setAllVisible() {
        txtID.setVisibility(View.VISIBLE);
        txtLED.setVisibility(View.VISIBLE);
        //txtTemp.setVisibility(View.VISIBLE);
        txtStatus.setVisibility(View.VISIBLE);
        txtRemoteBatteryLevel.setVisibility(View.VISIBLE);
        txtLocalBatteryLevel.setVisibility(View.VISIBLE);
        //txtLastUpdate.setVisibility(View.VISIBLE);
        txtOxygenLevel.setVisibility(View.VISIBLE);
        txtHydrogenSulfideLevel.setVisibility(View.VISIBLE);
        txtCarbonDioxideLevel.setVisibility(View.VISIBLE);
        txtCombExLevel.setVisibility(View.VISIBLE);
        txtInsertionCount.setVisibility(View.VISIBLE);
        txtDaysToCal.setVisibility(View.VISIBLE);

        //txtStaticTemp.setVisibility(View.VISIBLE);
        txtStaticLEL.setVisibility(View.VISIBLE);
        txtStaticO2.setVisibility(View.VISIBLE);
        txtStaticH2SO4.setVisibility(View.VISIBLE);
        txtStaticCO.setVisibility(View.VISIBLE);
        txtStaticMeterBattery.setVisibility(View.VISIBLE);
        txtStaticLocalBattery.setVisibility(View.VISIBLE);
        //txtStaticLastUpdated.setVisibility(View.VISIBLE);
        txtStaticInsertionCount.setVisibility(View.VISIBLE);
        imgSync.setVisibility(View.VISIBLE);
    }
}
