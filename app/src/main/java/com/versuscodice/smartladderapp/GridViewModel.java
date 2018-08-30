package com.versuscodice.smartladderapp;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class GridViewModel extends AndroidViewModel {

    //private MutableLiveData<List<Meter>> metersList;
    private List<Meter> metersList;

    public GridViewModel(@NonNull Application application) {
        super(application);
    }

    List<Meter> getMeterList() {
        if (metersList == null) {
            metersList = new ArrayList<>();
        }
        return metersList;
    }

    void setMeterList(List<Meter> meters) {
        metersList = meters;
    }
}
