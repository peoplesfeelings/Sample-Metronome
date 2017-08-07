package peoplesfeelingscode.com.samplemetronomerebuild;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.WarwickWestonWright.HGDialV2.HGDialInfo;
import com.WarwickWestonWright.HGDialV2.HGDialV2;

public class ActivityMain extends AppCompatActivity {

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

    TextView output;

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

        setUpDial();
        setUpEditText();
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
}
