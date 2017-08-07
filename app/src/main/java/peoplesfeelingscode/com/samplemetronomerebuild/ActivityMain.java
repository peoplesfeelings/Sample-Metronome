package peoplesfeelingscode.com.samplemetronomerebuild;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import com.WarwickWestonWright.HGDialV2.HGDialInfo;
import com.WarwickWestonWright.HGDialV2.HGDialV2;

public class ActivityMain extends ActivityBase {

    HGDialV2 hgDialV2;
    HGDialV2.IHGDial ihgDial;

    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;

    TextView txtBpm;
    Button btnSamples;
    Button btnStartStop;
    Spinner rateSpinner;
    TextView txtAbout;

    TextWatcher textWatcher;
    ArrayAdapter<String> spinnerAdapter;

    FragmentMainActivityWelcome welcomeDialog;

    TextView output;

    float rate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hgDialV2 = (HGDialV2) findViewById(R.id.hgDialV2);
        output = (TextView) findViewById(R.id.output);
        txtBpm = (TextView) findViewById(R.id.txtBpm);
        btnSamples = (Button) findViewById(R.id.btnSamples);
        btnStartStop = (Button) findViewById(R.id.btnStartStop);
        rateSpinner = (Spinner) findViewById(R.id.rateSpinner);
        txtAbout = (TextView) findViewById(R.id.txtAbout);

        sharedPrefs = getSharedPreferences(Storage.SHARED_PREF_FILE_NAME, MODE_PRIVATE);
        editor = sharedPrefs.edit();

        welcomeDialog = new FragmentMainActivityWelcome();

        checkIfFirstRun();

        setUpDial();
        setUpEditText();
        setUpSpinner();
        setUpListeners();
    }

    void setUpListeners() {
//        btnSamples.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                startActivity(new Intent(ActivityMain.this, ActivitySample.class));
//            }
//        });
//        btnStartStop.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (!loopRunning) {
//                    loopRunning = true;
//                    btnStartStop.setText(getResources().getString(R.string.btnStop));
//                    loop();
//                } else {
//                    btnStartStop.setText(getResources().getString(R.string.btnStart));
//                    loopRunning = false;
//                }
//            }
//        });
        txtAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ActivityMain.this, ActivityAbout.class));
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

        rateSpinner.setSelection(Storage.getSharedPrefInt(Storage.SHARED_PREF_RATE_KEY, this));

        rateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                rate = rateSpinnerPosToFloat(pos);
                Storage.setSharedPrefInt(pos, Storage.SHARED_PREF_RATE_KEY, ActivityMain.this);
                Log.d("*********", "" + rate);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        rate = rateSpinnerPosToFloat(rateSpinner.getSelectedItemPosition());
        Log.d("**********", "" + rate);
    }

    float rateSpinnerPosToFloat(int pos) {
        switch(pos) {
            case 0:
                return 0.5f;
            case 1:
                return 1f;
            case 2:
                return 2f;
            case 3:
                return 4f;
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
                    Storage.setSharedPrefDouble(editor, fta, Storage.SHARED_PREF_FTA_KEY, ActivityMain.this);
                    output.setText(Double.toString(Storage.ftaToBpm(hgDialV2.getFullTextureAngle())));
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
                output.setText(Double.toString(Storage.ftaToBpm(hgDialV2.getFullTextureAngle())));
            }
            @Override
            public void onPointerUp(HGDialInfo hgDialInfo) { /* Do Your Thing */ }
            @Override
            public void onUp(HGDialInfo hgDialInfo) {
                double fta = hgDialV2.getFullTextureAngle();
                double bpm = Storage.ftaToBpm(fta);

                output.setText(Double.toString(bpm));

                Storage.setSharedPrefDouble(editor, fta, Storage.SHARED_PREF_FTA_KEY, ActivityMain.this);
            }
        };

        hgDialV2.registerCallback(ihgDial);

        loadStoredAngle();
    }

    void loadStoredAngle() {
        double fta = Storage.getSharedPrefDouble(sharedPrefs, Storage.SHARED_PREF_FTA_KEY, this);

        if (fta == -1) {
            fta = Storage.bpmToFta(Storage.DEFAULT_BPM);
        }

        output.setText(Double.toString(Storage.ftaToBpm(fta)));

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

            Storage.setSharedPref(true, Storage.SHARED_PREF_HAS_RUN_KEY, this);

            welcomeDialog.show(getFragmentManager().beginTransaction(), "");
        }

    }
}
