package com.versuscodice.smartladderapp;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Formatter;
import android.util.ArrayMap;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.SubMenu;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.obsez.android.lib.filechooser.ChooserDialog;
import com.rafakob.nsdhelper.NsdHelper;
import com.rafakob.nsdhelper.NsdListener;
import com.rafakob.nsdhelper.NsdService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MeterAdapter meterAdapter;
    public TextView txtAlarms;
    public GridView gridview;
    public boolean mBackground = true;
    public GridViewModel mModel;
    CommunicationService mService;

    SharedPreferences mPrefs;

    String mSSID;
    int mAlarmSetting;

    boolean mServiceBound = false;

    public List<Meter> meters = new ArrayList<>();
    public List<Meter> backgroundMeters = new ArrayList<>();

    boolean connectedToWifi = false;

    int mCalibrationReminder;

    int mPendingClearMeter = -1;
    int mPendingInsertionMeter = -1;

    public int mExternalStoragePermmisions = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, CommunicationService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        //mModel = ViewModelProviders.of(this).get(GridViewModel.class);
        //meters = mModel.getMeterList();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mSSID = sharedPref.getString("perf_appWifiSSID", "");
        mAlarmSetting = Integer.parseInt(sharedPref.getString("pref_appAlarm", "0"));

        //mNsdManager = (NsdManager) getApplicationContext().getSystemService(Context.NSD_SERVICE);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        registerReceiver(wifiReceiver, intentFilter);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CommunicationService.CommunicationBinder binder = (CommunicationService.CommunicationBinder) iBinder;
            mService = binder.getService();
            mService.mThat = MainActivity.this;
            meters = mService.meters;
            backgroundMeters = mService.backgroundMeters;

            setTheme(R.style.AppTheme);

            gridview = (GridView) findViewById(R.id.gridView);
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                gridview.setNumColumns(4);
            } else {
                gridview.setNumColumns(2);
            }
            ImageButton btnSilence = (ImageButton) findViewById(R.id.btnSilence);
            meterAdapter = new MeterAdapter(MainActivity.this, meters, btnSilence);
            gridview.setAdapter(meterAdapter);

            Meter.setContext(MainActivity.this, meterAdapter);

            txtAlarms = (TextView) findViewById(R.id.txtAlarms);

            gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {

                    final int finalSelectedMeterPosition = i;

                    PopupMenu popup = new PopupMenu(getApplicationContext(), view);
                    MenuInflater inflater = popup.getMenuInflater();
                    inflater.inflate(R.menu.menu_meter, popup.getMenu());

                    if (meters.get(i).mActive) {
                        popup.getMenu().add(Menu.NONE, 1, Menu.NONE, "Get Log");
                        popup.getMenu().add(Menu.NONE, 2, Menu.NONE, "Reset Insertion Count");
                        popup.getMenu().add(Menu.NONE, 3, Menu.NONE, "Clear Log");
                        popup.getMenu().add(Menu.NONE, 4, Menu.NONE, "Version " + meters.get(i).version).setEnabled(false);

                    }

                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {

                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                            switch (menuItem.getItemId()) {
                                case R.id.itemChooseMeter:

                                    SubMenu meterMenu = menuItem.getSubMenu();

                                    for (Meter testMeter : backgroundMeters) {
                                        meterMenu.add(Menu.NONE, Menu.NONE, Menu.NONE, testMeter.id);
                                    }
                                    return true;

                                case R.id.itemNone:
                                    Meter meterTest = meters.get(finalSelectedMeterPosition);
                                    if (meterTest.id != null) {
                                        backgroundMeters.add(meters.get(finalSelectedMeterPosition));
                                    }
                                    meters.set(finalSelectedMeterPosition, new Meter());
                                    meterAdapter.notifyDataSetChanged();
                                    return true;

                                case 3:
                                    builder.setMessage("Are you sure you want to clear the log for " + meters.get(i).id + "?").setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();
                                    mPendingClearMeter = i;
                                    break;

                                case 2:
                                    builder.setMessage("Are you sure you want to reset the insertion count for " + meters.get(i).id + "?").setPositiveButton("Yes", dialogClickListener).setNegativeButton("No", dialogClickListener).show();
                                    mPendingInsertionMeter = i;

                                    break;

                                case 1:
                                    meters.get(i).sendData("Log");

                                    break;

                                case 0:
                                    for (int i = 0; i < backgroundMeters.size(); i++) {
                                        if (menuItem.getTitle().equals(backgroundMeters.get(i).id)) {
                                            Meter meterTemp = meters.get(finalSelectedMeterPosition);
                                            Meter backgroundMeterTemp = backgroundMeters.get(i);

                                            meters.set(finalSelectedMeterPosition, backgroundMeterTemp);
                                            if (meterTemp.id != null) {
                                                backgroundMeters.set(i, meterTemp);
                                            } else {
                                                backgroundMeters.remove(i);
                                            }
                                            meterAdapter.notifyDataSetChanged();
                                            return true;
                                        }
                                    }
                                    break;
                            }
                            return false;
                        }
                    });

                    popup.show();
                }
            });

            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (networkInfo.isConnected()) {
                checkWifiState();
            }

            mServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            mServiceBound = false;
        }
    };

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    if (mPendingClearMeter != -1) {
                        meters.get(mPendingClearMeter).sendData("Clear");
                        mPendingClearMeter = -1;
                    } else if (mPendingInsertionMeter != -1) {
                        meters.get(mPendingInsertionMeter).sendData("Insertion");
                        mPendingInsertionMeter = -1;
                    }
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    mPendingClearMeter = -1;
                    mPendingInsertionMeter = -1;
                    break;
            }
        }
    };

    private final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals("android.net.wifi.STATE_CHANGE")) {
                checkWifiState();
            }
        }
    };

    public void checkWifiState() {
        if(mService != null) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifiManager.getConnectionInfo();
            String ssid = info.getSSID().replace("\"", "");
            Log.d("TEST", "SSID = " + ssid);
            if (ssid.equals(mSSID)) {
                connectedToWifi = true;
                meterAdapter.refresh();
            } else {
                connectedToWifi = false;
                txtAlarms.setText("ALARM-NETWORK-ERROR");
                txtAlarms.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    public void displayLog(ArrayMap<String, String> arrayMap, final String id) {

        final ArrayMap<String, String> finalArrayMap = arrayMap;

        final MainActivity that = this;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                final String fileName = "events.log";

                File defaultDir = Environment.getExternalStorageDirectory();
                final File logFile = new File(defaultDir, fileName);

                isStoragePermissionGranted();

                if (isStoragePermissionGranted()) {
                    try {
                        FileOutputStream outputStream = new FileOutputStream(logFile);
                        outputStream.write(finalArrayMap.get("log").getBytes());
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View logView = layoutInflater.inflate(R.layout.popup_log, null);

                TextView txtLog = logView.findViewById(R.id.txtLog);
                txtLog.append(finalArrayMap.get("log"));
                DisplayMetrics displayMetrics = new DisplayMetrics();
                WindowManager windowmanager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
                windowmanager.getDefaultDisplay().getMetrics(displayMetrics);

                final PopupWindow popupWindow = new PopupWindow(logView, displayMetrics.widthPixels - 100, displayMetrics.heightPixels - 80, true);
                popupWindow.showAtLocation(gridview, Gravity.CENTER, 0, 0);

                final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(that);
                String logDir = sharedPref.getString("logDir", "");
                if (logDir.equals("")) {
                    logDir = defaultDir.getPath();
                }

                final String finalLogDir = logDir;

                Button btnClose = logView.findViewById(R.id.btnClosePopup);
                btnClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });

                Button btnSave = logView.findViewById(R.id.btnSavePopup);
                btnSave.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        new ChooserDialog().with(that)
                                .enableOptions(true)
                                .withFilter(true, false)
                                .withStartFile(finalLogDir)
                                .withChosenListener(new ChooserDialog.Result() {
                                    @Override
                                    public void onChoosePath(String path, File pathFile) {
                                        Toast.makeText(MainActivity.this, "FOLDER: " + path, Toast.LENGTH_SHORT).show();

                                        SharedPreferences.Editor prefsEditor = sharedPref.edit();
                                        prefsEditor.putString("logDir", path);
                                        prefsEditor.commit();

                                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
                                        String date = dateFormat.format(Calendar.getInstance().getTime());


                                        final String fileName = date + " " + id + " events.log";

                                        final File finalLogFile = new File(path, fileName);
                                        Log.d("TEST", "logFile path: " + finalLogFile.getAbsolutePath());

                                        if (!isStoragePermissionGranted()) {
                                            while (mExternalStoragePermmisions == 0) ;
                                        }

                                        if (mExternalStoragePermmisions == 1) {
                                            try {
                                                FileOutputStream outputStream = new FileOutputStream(finalLogFile);
                                                outputStream.write(finalArrayMap.get("log").getBytes());
                                                outputStream.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                })
                                .build()
                                .show();
                    }
                });

                Button btnEmail = logView.findViewById(R.id.btnEmail);
                btnEmail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mExternalStoragePermmisions == 1) {
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("*/*");
                            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getApplicationContext(), "com.versuscodice.provider", logFile));
                            if (intent.resolveActivity(getPackageManager()) != null) {
                                startActivity(intent);
                            }
                        }
                    }
                });
            }
        });
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d("TEST", "Permission is granted");
                mExternalStoragePermmisions = 1;
                return true;
            }
            else {
                Log.d("TEST", "Permission is revoked");
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                mExternalStoragePermmisions = 0;
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.d("TEST", "Permission is granted");
            mExternalStoragePermmisions = 1;
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("TEST", "Permission: " + permissions[0] + "was " + grantResults[0]);
                //resume tasks needing this permission
                mExternalStoragePermmisions = 1;
            } else {
                mExternalStoragePermmisions = -1;
            }
        }
    }

    @Override
    protected void onDestroy() {
        //mNsdManager.unregisterService(mRegistrationListener);
        /*try {
            mServerSocketThread.close();
        } catch (Exception e) {
            Log.d("onDestroy", "Failed to close ServerSocket");
        }*/
        /*if(mService != null) {
            mService.unBind();
            unbindService(mConnection);
        }*/
        super.onDestroy();
    }

    @Override
    protected void onPause() {

        unregisterReceiver(wifiReceiver);
        if(mService != null) {
            mService.unBind();
        }

        if(mServiceBound) {
            try {
                unbindService(mConnection);
            }
            catch(IllegalArgumentException e) {
                Log.e("TEST", "Failed to unbind service");
            }
        }

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*if (mServerSocketThread != null) {
            if (!mServerSocketThread.isAlive()) {
                mServerSocketThread = new SocketListenThread();
                mServerSocketThread.start();
            }
        }*/

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mSSID = sharedPref.getString("perf_appWifiSSID", "");
        mAlarmSetting = Integer.parseInt(sharedPref.getString("perf_appAlarm", "0"));
        mCalibrationReminder = NumberPickerPreference.value;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        registerReceiver(wifiReceiver, intentFilter);
        checkWifiState();

        mBackground = true;
    }



    public void registerService(int port) {
        Log.d("TEST", "udp port: " + port);
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName("Smart Ladder");
        serviceInfo.setServiceType("_zvs._udp.");
        serviceInfo.setPort(port);

        //mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
        //mNsdManager.unregisterService(mRegistrationListener);
        //mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    private NsdManager.RegistrationListener mRegistrationListener = new NsdManager.RegistrationListener() {
        @Override
        public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {

        }

        @Override
        public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {

        }

        @Override
        public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
            //String mServiceName = NsdServiceInfo.getServiceName();
            Log.d("TEST", "Service Registered");
        }

        @Override
        public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
            Log.d("TEST", "Service unRegistered");
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.actParing:
                Intent pairingIntent = new Intent(this, PairingActivity.class);
                pairingIntent.putExtra("SSID", mSSID);
                startActivity(pairingIntent);
                return true;

            case R.id.actSetting:
                Intent settingIntents = new Intent(this, SettingsActivity.class);
                startActivity(settingIntents);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
