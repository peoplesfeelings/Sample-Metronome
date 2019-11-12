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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

public class ActivityMain extends ActivityBase implements ServiceCallbacks {
    final static int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_IMPORT = 3476;
    final static int TEMPO_CHANGE_POLLING_MS = 1;


    int permissionCheck;

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
    FragmentMainActivityProblem problemDialog;
    private long lastBpmChangeMillis;

    // activity life cycle stuff
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(Dry.TAG, "onCreate !!!!!!!!!!!!!!!!!!!");

        setContentView(R.layout.activity_main);

        hgDialV2 = (HGDialV2) findViewById(R.id.hgDialV2);
        txtBpm = (TextView) findViewById(R.id.txtBpm);
        btnSamples = (Button) findViewById(R.id.btnSamples);
        btnStartStop = (Button) findViewById(R.id.btnStartStop);
        rateSpinner = (Spinner) findViewById(R.id.rateSpinner);

        sharedPrefs = getSharedPreferences(Storage.SHARED_PREF_FILE_NAME, MODE_PRIVATE);
        editor = sharedPrefs.edit();

        welcomeDialog = new FragmentMainActivityWelcome();
        problemDialog = new FragmentMainActivityProblem();

        getPermissionForWrite();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isBound() && !getSeq().isPlaying()) {
            stopService(new Intent(this, getServiceClass()));
        }
    }

    // permission stuff
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_IMPORT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    versionSetUp();
                    if (isBound()) {
//                        service.loadFile(Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, this));
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
    void getPermissionForWrite() {
        permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_IMPORT);
        } else {
            versionSetUp();
        }
    }

    @Override
    public void onConnect() {
        super.onConnect();

        setUpDial();
        setUpEditText();
        setUpSpinner();
        setUpListeners();
    }

    // app setup stuff
    void versionSetUp() {
        int lastVersionSetUp = getLastVersionSetUp();
        int currentVersion = BuildConfig.VERSION_CODE;

        if (currentVersion > lastVersionSetUp) {
            switch (lastVersionSetUp) {
                case 0:
                    setDefaultSettings();
                    setUpDirectory();
                    welcomeDialog.show(getFragmentManager().beginTransaction(), "");
                default:
                    Storage.writeSamplePack(this, lastVersionSetUp);
                    Storage.setSharedPrefInt(currentVersion, Storage.SHARED_PREF_LAST_VERSION_SET_UP, this);
            }
        }
    }
    int getLastVersionSetUp() {
        boolean hasRunBefore = Storage.getSharedPrefBool(Storage.SHARED_PREF_VERSION_1_WAS_SET_UP_KEY, this);
        int lastVersionSetUp = Storage.getSharedPrefInt(Storage.SHARED_PREF_LAST_VERSION_SET_UP, this);

        if (lastVersionSetUp == Storage.DEFAULT_SHARED_PREF_INT) {
            if (hasRunBefore) {
                /* 8 is last before version-check was implemented */
                lastVersionSetUp = 8;
            } else {
                lastVersionSetUp = 0;
            }
        }

        return lastVersionSetUp;
    }
    void setUpDirectory() {
        Storage.makeDirectoryIfNeeded();
        Storage.writeNoMediaFile(this);
    }
    void setDefaultSettings() {
        //set default settings
        Storage.setSharedPrefDouble(editor, Storage.bpmToFta(Storage.DEFAULT_BPM), Storage.SHARED_PREF_FTA_KEY, this);
        Storage.setSharedPrefInt(Storage.DEFAULT_RATE, Storage.SHARED_PREF_RATE_KEY, this);
        Storage.setSharedPrefString(Storage.DEFAULT_SELECTED_FILE_STRING, Storage.SHARED_PREF_SELECTED_FILE_KEY, this);

        Storage.setSharedPref(true, Storage.SHARED_PREF_VERSION_1_WAS_SET_UP_KEY, this);
    }

    // ui component setup stuff
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
                if (!getSeq().isPlaying()) {
                    if (!new File(Storage.path, Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, ActivityMain.this)).exists()) {
                        Toast toast = Toast.makeText(ActivityMain.this, R.string.toastSampleNotSelected, Toast.LENGTH_LONG);
                        toast.show();
                        return;
                    }
                    if (isBound()) {
                        getSeq().setBpm(Storage.ftaToBpm(hgDialV2.getFullTextureAngle()));
                        lastBpmChangeMillis = System.currentTimeMillis();
                        getSeq().play();
                        btnStartStop.setText(getResources().getString(R.string.btnStop));
                    }
                } else {
                    if (isBound()) {
                        getSeq().stop();
                        btnStartStop.setText(getResources().getString(R.string.btnStart));
                    }
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
                setSeqRate(pos);

                Storage.setSharedPrefInt(pos, Storage.SHARED_PREF_RATE_KEY, ActivityMain.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        int storedRate = Storage.getSharedPrefInt(Storage.SHARED_PREF_RATE_KEY, this);
        /* after first install, ui set-up methods run before versionSetUp runs */
        if (storedRate == Storage.DEFAULT_SHARED_PREF_INT) {
            storedRate = Storage.DEFAULT_RATE;
        }

        rateSpinner.setSelection(storedRate);
        setSeqRate(rateSpinner.getSelectedItemPosition());
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

                    getSeq().setBpm(Storage.ftaToBpm(hgDialV2.getFullTextureAngle()));

                    Storage.setSharedPrefDouble(editor, fta, Storage.SHARED_PREF_FTA_KEY, ActivityMain.this);
                }
            }
        };

        txtBpm.addTextChangedListener(textWatcher);
    }
    void setUpDial() {
        ihgDial = new HGDialV2.IHGDial() {
            @Override
            public void onDown(HGDialInfo hgDialInfo) {
                //
            }
            @Override
            public void onPointerDown(HGDialInfo hgDialInfo) { /* Do Your Thing */ }
            @Override
            public void onMove(HGDialInfo hgDialInfo) {
                if(System.currentTimeMillis() > lastBpmChangeMillis + TEMPO_CHANGE_POLLING_MS) {
                    double acceptedBpm = getSeq().setBpm(Storage.ftaToBpm(preventNegative(hgDialV2.getFullTextureAngle())));
                    lastBpmChangeMillis = System.currentTimeMillis();
                    txtBpm.setText(Double.toString(acceptedBpm));
                }
            }
            @Override
            public void onPointerUp(HGDialInfo hgDialInfo) { /* Do Your Thing */ }
            @Override
            public void onUp(HGDialInfo hgDialInfo) {
                double bpm = Storage.ftaToBpm(preventNegative(hgDialV2.getFullTextureAngle()));

                double acceptedBpm = getSeq().setBpm(bpm);
                lastBpmChangeMillis = System.currentTimeMillis();
                txtBpm.setText(Double.toString(acceptedBpm));

                double acceptedFta = Storage.bpmToFta(acceptedBpm);
                Storage.setSharedPrefDouble(editor, acceptedFta, Storage.SHARED_PREF_FTA_KEY, ActivityMain.this);
            }
        };

        hgDialV2.registerCallback(ihgDial);

        // load stored fta
        double fta = Storage.getSharedPrefDouble(sharedPrefs, Storage.SHARED_PREF_FTA_KEY, this);
        /* after first install, ui set-up methods run before versionSetUp runs */
        if (fta == Storage.DEFAULT_SHARED_PREF_INT) {
            fta = Storage.bpmToFta(Storage.DEFAULT_BPM);
        }
        txtBpm.setText(Double.toString(Storage.ftaToBpm(fta)));
        hgDialV2.doRapidDial(fta);
        hgDialV2.doManualGestureDial(fta);
    }
    private void setSeqTempo() {
        //
    }

    double preventNegative(double fta) {
        if (fta < 0) {
            hgDialV2.setFullTextureAngle(0);

            return 0;
        }

        return fta;
    }

    public void showProblemInfo(String message) {
        btnStartStop.setText(getResources().getString(R.string.btnStart));
        
        problemDialog.setArgs(message);
        problemDialog.show(getFragmentManager().beginTransaction(), "");
    }
}
