package com.versuscodice.smartladderapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.NumberPicker;

public class NumberPickerPreference extends DialogPreference {

    private NumberPicker numberPicker;
    public static int value;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        value = sharedPreferences.getInt("perf_appCalibration", 30);
    }

    @Override
    protected View onCreateDialogView() {
        return generateNumberPicker();
    }

    public NumberPicker generateNumberPicker() {
        numberPicker = new NumberPicker(getContext());
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(365);
        numberPicker.setValue(value);

        /*
         * Anything else you want to add to this.
         */

        return numberPicker;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            value = numberPicker.getValue();
            SharedPreferences.Editor editor = getEditor();
            editor.putInt("perf_appCalibration", value);
            editor.commit();
            Log.d("NumberPickerPreference", "NumberPickerValue : " + value);

        }
    }

}
