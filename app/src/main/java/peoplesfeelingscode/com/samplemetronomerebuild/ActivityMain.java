/*

Sample Metronome
Copyright (C) 2017 People's Feelings

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

*/

package peoplesfeelingscode.com.samplemetronomerebuild;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.WarwickWestonWright.HGDialV2.HGDialInfo;
import com.WarwickWestonWright.HGDialV2.HGDialV2;

import java.io.File;

public class ActivityMain extends ActivityBase {
    final static int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_IMPORT = 3476;

    int permissionCheck;

    MyService service;
    ServiceConnection connection;
    boolean bound;

    HGDialV2 hgDialV2;
    HGDialV2.IHGDial ihgDial;

    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;

    TextView txtBpm;
    Button btnSamples;
    Button btnStartStop;
    Spinner rateSpinner;
    TextWatcher textWatcher;
    ArrayAdapter<String> spinnerAdapter;

    FragmentMainActivityWelcome welcomeDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bound = false;

        hgDialV2 = (HGDialV2) findViewById(R.id.hgDialV2);
        txtBpm = (TextView) findViewById(R.id.txtBpm);
        btnSamples = (Button) findViewById(R.id.btnSamples);
        btnStartStop = (Button) findViewById(R.id.btnStartStop);
        rateSpinner = (Spinner) findViewById(R.id.rateSpinner);

        sharedPrefs = getSharedPreferences(Storage.SHARED_PREF_FILE_NAME, MODE_PRIVATE);
        editor = sharedPrefs.edit();

        welcomeDialog = new FragmentMainActivityWelcome();

        checkIfFirstRun();

        setUpServiceConnection();
        doBindService();

        getPermissionForWrite();
        checkForOldFolder();

        Log.d(Dry.TAG, "activity oncreate");
    }

    @Override
    protected void onStart() {
        super.onStart();

        doBindService();
    }

    @Override
    protected void onStop() {
        super.onStop();

        doUnbindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bound && !service.playing) {
            stopService(new Intent(this, MyService.class));
        }

        Log.d(Dry.TAG, "activity ondestroy");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_IMPORT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Storage.makeDirectoryIfNeeded();
                    Storage.writeSamplePack(this);
                    Storage.writeNoMediaFile(this);
                    if (bound) {
                        service.loadFile(Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, this));
                    }
                } else {
                    Toast toast = new Toast(this);
                    toast.setView(getLayoutInflater().inflate(R.layout.toast, null));
                    toast.setDuration(Toast.LENGTH_LONG);
                    TextView text = (TextView) toast.getView().findViewById(R.id.txt);
                    text.setText(getResources().getString(R.string.toastWritePermissionNotGranted));
                    toast.show();

                    finish();
                }
                return;
            }
        }
    }

    void checkForOldFolder() {
        if (Storage.oldPath.exists()) {
            Storage.makeDirectoryIfNeeded();
            Storage.copyOldToNew();
            Storage.writeNoMediaFile(this);
            Storage.deleteRecursive(Storage.oldPath);
        } else {

        }
    }

    void doBindService() {
        Context context = getApplicationContext();
        if (!Dry.serviceRunning(context, MyService.class)) {
            startService(new Intent(context, MyService.class));
        }

        if (!bound) {
            Intent intent = new Intent(context, MyService.class);
            bindService(intent, connection, Context.BIND_ABOVE_CLIENT);
            bound = true;
        }
    }

    void doUnbindService() {
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }

    void setUpServiceConnection() {
        connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder iBinder) {
                MyService.MyBinder binder = (MyService.MyBinder) iBinder;
                service = binder.getService();

                if (service.playing) {
                    btnStartStop.setText(getResources().getString(R.string.btnStop));
                }

                setUpDial();
                setUpEditText();
                setUpSpinner();
                setUpListeners();

                Log.d(Dry.TAG, "serviceconnection connected");
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                service = null;
                bound = false;

                Log.d(Dry.TAG, "serviceconnection disconnected");
            }
        };
    }

    void getPermissionForWrite() {
        permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_IMPORT);
        } else {
            //
        }
    }

    void setUpListeners() {
        btnSamples.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ActivityMain.this, ActivitySample.class));
            }
        });
        btnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!service.playing) {
                    if (!new File(Storage.path, Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, ActivityMain.this)).exists()) {
                        Toast toast = Toast.makeText(ActivityMain.this, R.string.toastSampleNotSelected, Toast.LENGTH_LONG);
                        toast.show();
                        return;
                    }
                    service.setInterval(hgDialV2.getFullTextureAngle());
                    service.start();
                    btnStartStop.setText(getResources().getString(R.string.btnStop));
                } else {
                    btnStartStop.setText(getResources().getString(R.string.btnStart));
                    service.stop();
                }
            }
        });
    }

    void setUpSpinner() {
        spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.rates)) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ((TextView) v).setGravity(Gravity.CENTER);
                return v;
            }
            public View getDropDownView(int position, View convertView,ViewGroup parent) {
                View v = super.getDropDownView(position, convertView,parent);
                ((TextView) v).setGravity(Gravity.CENTER);
                return v;
            }
        };

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rateSpinner.setAdapter(spinnerAdapter);

        rateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                service.rate = rateSpinnerPosToFloat(pos);
                service.setInterval(hgDialV2.getFullTextureAngle());
                Storage.setSharedPrefInt(pos, Storage.SHARED_PREF_RATE_KEY, ActivityMain.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        rateSpinner.setSelection(Storage.getSharedPrefInt(Storage.SHARED_PREF_RATE_KEY, this));
        service.rate = rateSpinnerPosToFloat(rateSpinner.getSelectedItemPosition());
    }

    double rateSpinnerPosToFloat(int pos) {
        switch(pos) {
            case 0:
                return 0.5;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 4;
        }

        return -1;
    }

    void setUpEditText() {
        textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String input = txtBpm.getText().toString();
                if (input.length() > 0 && !input.equals(".")) {
                    double fta = Storage.bpmToFta(Double.parseDouble(txtBpm.getText().toString()));
                    hgDialV2.doRapidDial(fta);
                    hgDialV2.doManualGestureDial(fta);
                    service.setInterval(hgDialV2.getFullTextureAngle());
                    Storage.setSharedPrefDouble(editor, fta, Storage.SHARED_PREF_FTA_KEY, ActivityMain.this);
                }
            }
        };

        txtBpm.addTextChangedListener(textWatcher);
    }

    void setUpDial() {
        ihgDial = new HGDialV2.IHGDial() {
            @Override
            public void onDown(HGDialInfo hgDialInfo) { /* Do Your Thing */ }
            @Override
            public void onPointerDown(HGDialInfo hgDialInfo) { /* Do Your Thing */ }
            @Override
            public void onMove(HGDialInfo hgDialInfo) {
                txtBpm.setText(Double.toString(Storage.ftaToBpm(preventNegative(hgDialV2.getFullTextureAngle()))));
                service.setInterval(preventNegative(hgDialV2.getFullTextureAngle()));
            }
            @Override
            public void onPointerUp(HGDialInfo hgDialInfo) { /* Do Your Thing */ }
            @Override
            public void onUp(HGDialInfo hgDialInfo) {
                double fta = preventNegative(hgDialV2.getFullTextureAngle());
                double bpm = Storage.ftaToBpm(fta);

                txtBpm.setText(Double.toString(bpm));
                service.setInterval(fta);

                Storage.setSharedPrefDouble(editor, fta, Storage.SHARED_PREF_FTA_KEY, ActivityMain.this);
            }
        };

        hgDialV2.registerCallback(ihgDial);
        hgDialV2.setFlingTolerance(100, 500);
        hgDialV2.setSpinAnimation(0,0,4000);

        loadStoredAngle();
    }

    double preventNegative(double fta) {
        if (fta < 0) {
            hgDialV2.setFullTextureAngle(0);

            return 0;
        }

        return fta;
    }

    void loadStoredAngle() {
        double fta = Storage.getSharedPrefDouble(sharedPrefs, Storage.SHARED_PREF_FTA_KEY, this);

        if (fta == -1) {
            fta = Storage.bpmToFta(Storage.DEFAULT_BPM);
        }

        txtBpm.setText(Double.toString(Storage.ftaToBpm(fta)));

        hgDialV2.doRapidDial(fta);
        hgDialV2.doManualGestureDial(fta);
    }

    void checkIfFirstRun() {
        boolean hasRunBefore = Storage.getSharedPrefBool(Storage.SHARED_PREF_HAS_RUN_KEY, this);

        if (!hasRunBefore) {
            //set default settings
            Storage.setSharedPrefDouble(editor, Storage.bpmToFta(Storage.DEFAULT_BPM), Storage.SHARED_PREF_FTA_KEY, this);
            Storage.setSharedPrefInt(Storage.DEFAULT_RATE, Storage.SHARED_PREF_RATE_KEY, this);
            Storage.setSharedPrefString(Storage.DEFAULT_SELECTED_FILE_STRING, Storage.SHARED_PREF_SELECTED_FILE_KEY, this);

            Storage.setSharedPref(true, Storage.SHARED_PREF_HAS_RUN_KEY, this);

            welcomeDialog.show(getFragmentManager().beginTransaction(), "");

            Storage.makeDirectoryIfNeeded();
            Storage.writeSamplePack(this);
            Storage.writeNoMediaFile(this);

        }
    }
}
