package com.versuscodice.smartladderapp;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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

    public MeterAdapter(Context c, List<Meter> m) {
        mContext = c;
        meters = m;
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
            TextView txtRemoteBatteryLevel = (TextView) view.findViewById(R.id.txtRemoteBatteryLevel);
            TextView txtLastUpdate = (TextView) view.findViewById(R.id.txtLastUpdate);
            TextView txtOxygenLevel = (TextView) view.findViewById(R.id.txtOxygenLevel);
            TextView txtHydrogenSulfideLevel = (TextView) view.findViewById(R.id.txtHydrogenSulfideLevel);
            TextView txtCarbonDioxideLevel = (TextView) view.findViewById(R.id.txtCarbonDioxideLevel);
            TextView txtCombExLevel = (TextView) view.findViewById(R.id.txtCombExLevel);
            ConstraintLayout container = (ConstraintLayout) view.findViewById(R.id.meter_container);

            txtID.setText(thisMeter.id);
            txtTemp.setText(thisMeter.temp);
            txtRemoteBatteryLevel.setText(thisMeter.meterBattery);
            txtOxygenLevel.setText(thisMeter.oxygenLevel);
            txtHydrogenSulfideLevel.setText(thisMeter.hydrogensulfideLevel);
            txtCarbonDioxideLevel.setText(thisMeter.carbondioxideLevel);
            txtCombExLevel.setText(thisMeter.combExLevel);
            txtLastUpdate.setText(thisMeter.lastUpdate.toString());

            if(thisMeter.alarmState) {
                txtStatus.setText("Alarm");
                container.setBackgroundColor(mContext.getResources().getColor(R.color.colorAlarm));
            }
            else if(thisMeter.wariningState) {
                txtStatus.setText("Warning");
                container.setBackgroundColor(mContext.getResources().getColor(R.color.colorWarning));
            }
            else {
                txtStatus.setText("Enter");
                container.setBackgroundColor(mContext.getResources().getColor(R.color.colorGood));
            }
        return view;
    }

    // create a new ImageView for each item referenced by the Adapter
    /*public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(85, 85));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        imageView.setImageResource(mThumbIds[position]);
        return imageView;
    }*/
}
