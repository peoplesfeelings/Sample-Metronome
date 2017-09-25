/*

Precise Sample Metronome
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
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.Timer;

public class ActivityMain extends ActivityBase {

    final static int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_IMPORT = 3476;
    int permissionCheck;

    static final int MAX_STREAMS = 16;

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

    Timer timer;

    double rate;
    boolean loopRunning;
    long timeReference;
    long lastCycle = System.currentTimeMillis();
    int cycle = 0;
    long period;

    String fileLocation;
    int soundId;
    SoundPool sounds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hgDialV2 = (HGDialV2) findViewById(R.id.hgDialV2);
        txtBpm = (TextView) findViewById(R.id.txtBpm);
        btnSamples = (Button) findViewById(R.id.btnSamples);
        btnStartStop = (Button) findViewById(R.id.btnStartStop);
        rateSpinner = (Spinner) findViewById(R.id.rateSpinner);

        sharedPrefs = getSharedPreferences(Storage.SHARED_PREF_FILE_NAME, MODE_PRIVATE);
        editor = sharedPrefs.edit();

        welcomeDialog = new FragmentMainActivityWelcome();

        timer = new Timer();

        checkIfFirstRun();

        setUpDial();
        setUpEditText();
        setUpSpinner();
        setUpListeners();

        createSoundPool();

        loadFile(Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, this));

        getPermissionForWrite();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        loopRunning = false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FOR_IMPORT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Storage.writeSamplePack(this);
                    loadFile(Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, this));
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

    private void getPermissionForWrite() {
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

    protected void createSoundPool() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            createNewSoundPool();
        } else {
            createOldSoundPool();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void createNewSoundPool(){
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        sounds = new SoundPool.Builder()
                .setAudioAttributes(attributes)
                .setMaxStreams(MAX_STREAMS)
                .build();
    }

    @SuppressWarnings("deprecation")
    protected void createOldSoundPool(){
        sounds = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC,0);
    }

    void loop() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (Storage.fileNeedsToBeLoaded) {
                    loadFile(Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, ActivityMain.this));
                }
                lastCycle = System.currentTimeMillis();
                sounds.play(soundId, 1, 1, 1, 0, 1f);

                while (loopRunning) {
                    if (Storage.fileNeedsToBeLoaded) {
                        loadFile(Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, ActivityMain.this));
                    }
                    if (System.currentTimeMillis() > lastCycle + period) {
                        cycle++;
                        lastCycle = timeReference + cycle * period;

                        sounds.play(soundId, 1, 1, 1, 0, 1f);
                    }
                }
            }
        }).start();
    }

    void loadFile(String fileName) {
        fileLocation = Storage.path + File.separator + fileName;
        soundId = sounds.load(fileLocation, 1);
        Storage.fileNeedsToBeLoaded = false;
    }

    void setPeriod() {
        /*
        loopRunning bool is assigned true after setPeriod() is called in btnStartStop handler so the following
        should only be true if bpm is being changed while loop is running
        */
        if (lastCycle != 0L && loopRunning) {
            timeReference = lastCycle;
        } else {
            timeReference = System.currentTimeMillis();
            lastCycle = timeReference;
        }
        cycle = 0;

        double bpm = Storage.ftaToBpm(hgDialV2.getFullTextureAngle());
        double beat = 60000 / bpm;
        int intervalMillis = (int) (beat / rate);

        period = intervalMillis;
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
                if (!loopRunning) {
                    if (!new File(Storage.path, Storage.getSharedPrefString(Storage.SHARED_PREF_SELECTED_FILE_KEY, ActivityMain.this)).exists()) {
                        Toast toast = Toast.makeText(ActivityMain.this, R.string.toastSampleNotSelected, Toast.LENGTH_LONG);
                        toast.show();
                        return;
                    }
                    setPeriod();
                    loopRunning = true;
                    btnStartStop.setText(getResources().getString(R.string.btnStop));
                    loop();
                } else {
                    btnStartStop.setText(getResources().getString(R.string.btnStart));
                    loopRunning = false;
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
                rate = rateSpinnerPosToFloat(pos);
                setPeriod();
                Storage.setSharedPrefInt(pos, Storage.SHARED_PREF_RATE_KEY, ActivityMain.this);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        rateSpinner.setSelection(Storage.getSharedPrefInt(Storage.SHARED_PREF_RATE_KEY, this));
        rate = rateSpinnerPosToFloat(rateSpinner.getSelectedItemPosition());
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
                    setPeriod();
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
                setPeriod();
            }
            @Override
            public void onPointerUp(HGDialInfo hgDialInfo) { /* Do Your Thing */ }
            @Override
            public void onUp(HGDialInfo hgDialInfo) {
                double fta = preventNegative(hgDialV2.getFullTextureAngle());
                double bpm = Storage.ftaToBpm(fta);

                txtBpm.setText(Double.toString(bpm));
                setPeriod();

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
            Storage.makeDirectoryIfNeeded();

            //set default settings
            Storage.setSharedPrefDouble(editor, Storage.bpmToFta(Storage.DEFAULT_BPM), Storage.SHARED_PREF_FTA_KEY, this);
            Storage.setSharedPrefInt(Storage.DEFAULT_RATE, Storage.SHARED_PREF_RATE_KEY, this);
            Storage.setSharedPrefString(Storage.DEFAULT_SELECTED_FILE_STRING, Storage.SHARED_PREF_SELECTED_FILE_KEY, this);

            Storage.setSharedPref(true, Storage.SHARED_PREF_HAS_RUN_KEY, this);

            welcomeDialog.show(getFragmentManager().beginTransaction(), "");

            Storage.writeSamplePack(this);
        }

    }
}
