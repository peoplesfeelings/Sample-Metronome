package peoplesfeelingscode.com.samplemetronomerebuild;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.WarwickWestonWright.HGDialV2.HGDialInfo;
import com.WarwickWestonWright.HGDialV2.HGDialV2;

public class ActivityMain extends AppCompatActivity {

    HGDialV2 hgDialV2;
    HGDialV2.IHGDial ihgDial;

    SharedPreferences sharedPrefs;
    SharedPreferences.Editor editor;

    TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hgDialV2 = (HGDialV2) findViewById(R.id.hgDialV2);
        output = (TextView) findViewById(R.id.output);

        sharedPrefs = getSharedPreferences(Storage.SHARED_PREF_FILE_NAME, MODE_PRIVATE);
        editor = sharedPrefs.edit();

        setUpDial();
    }

    void setUpDial() {
        HGDialV2.IHGDial ihgDial = new HGDialV2.IHGDial() {
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
            fta = Storage.bpmToFta(Storage.INITIAL_BPM);
        }

        output.setText(Double.toString(Storage.ftaToBpm(fta)));

        hgDialV2.setFullTextureAngle(fta);
    }
}
