package peoplesfeelingscode.com.samplemetronomerebuild;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.WarwickWestonWright.HGDialV2.HGDialInfo;
import com.WarwickWestonWright.HGDialV2.HGDialV2;

public class ActivityMain extends AppCompatActivity {

    HGDialV2 hgDialV2;
    HGDialV2.IHGDial ihgDial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hgDialV2 = (HGDialV2) findViewById(R.id.hgDialV2);

        setUpDial();
    }

    void setUpDial() {
        HGDialV2.IHGDial ihgDial = new HGDialV2.IHGDial() {
            @Override
            public void onDown(HGDialInfo hgDialInfo) { /* Do Your Thing */ }
            @Override
            public void onPointerDown(HGDialInfo hgDialInfo) { /* Do Your Thing */ }
            @Override
            public void onMove(HGDialInfo hgDialInfo) { /* Do Your Thing */ }
            @Override
            public void onPointerUp(HGDialInfo hgDialInfo) { /* Do Your Thing */ }
            @Override
            public void onUp(HGDialInfo hgDialInfo) { /* Do Your Thing */ }
        };

        hgDialV2.registerCallback(ihgDial);
    }
}
